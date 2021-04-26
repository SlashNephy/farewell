package blue.starry.farewell

import blue.starry.penicillin.core.exceptions.PenicillinException
import blue.starry.penicillin.core.exceptions.PenicillinTwitterApiException
import blue.starry.penicillin.core.exceptions.TwitterApiError
import blue.starry.penicillin.endpoints.followers
import blue.starry.penicillin.endpoints.followers.listUsers
import blue.starry.penicillin.endpoints.friends
import blue.starry.penicillin.endpoints.friends.listUsers
import blue.starry.penicillin.endpoints.friendships
import blue.starry.penicillin.endpoints.friendships.showByUserId
import blue.starry.penicillin.endpoints.users
import blue.starry.penicillin.endpoints.users.showByUserId
import blue.starry.penicillin.extensions.cursor.untilLast
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.flow.toList
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

object Farewell {
    init {
        transaction(FarewellDatabase) {
            SchemaUtils.create(FollowerUsers)
            SchemaUtils.create(FollowingUsers)
            SchemaUtils.create(InactiveUsers)
        }
    }

    suspend fun check() {
        val followingUsers = FarewellTwitterClient.friends.listUsers(count = 200).untilLast().toList()
        val followingUserIds = followingUsers.map { it.id }
        val followerUsers = FarewellTwitterClient.followers.listUsers(count = 200).untilLast().toList()
        val followerUserIds = followerUsers.map { it.id }

        val (previousFollowingUsers, previousFollowerUsers, previousInactiveUsers) = transaction(FarewellDatabase) {
            listOf(
                FollowingUsers.selectAll().map {
                    AbstractDBUser(
                        id = it[FollowingUsers.id],
                        screenName = it[FollowingUsers.screenName],
                        name = it[FollowingUsers.name]
                    )
                },
                FollowerUsers.selectAll().map {
                    AbstractDBUser(
                        id = it[FollowerUsers.id],
                        screenName = it[FollowerUsers.screenName],
                        name = it[FollowerUsers.name]
                    )
                },
                InactiveUsers.selectAll().map {
                    AbstractDBUser(
                        id = it[InactiveUsers.id],
                        screenName = it[InactiveUsers.screenName],
                        name = it[InactiveUsers.name]
                    )
                }
            )
        }
        val previousFollowingUserIds = previousFollowingUsers.map { it.id }
        val previousFollowerUserIds = previousFollowerUsers.map { it.id }

        // 復活したアカウントを探す
        for (inactiveUser in previousInactiveUsers) {
            // フォローまたはフォロワーから検索
            val user = followingUsers.find { it.id == inactiveUser.id } ?: followerUsers.find { it.id == inactiveUser.id } ?: continue

            val embed = DiscordEmbed(
                title = "アカウントが復活しました",
                author = DiscordEmbed.Author(
                    name = "${user.name} @${user.screenName}",
                    url = "https://twitter.com/${user.screenName}",
                    iconUrl = user.profileImageUrlHttps
                )
            )
            sendToDiscord(embed)

            transaction(FarewellDatabase) {
                InactiveUsers.deleteWhere { InactiveUsers.id eq inactiveUser.id }
            }
        }

        // フォロワーとフォローの減少分を探す
        val followingUserIdsDelta = previousFollowingUserIds.filter { it !in followingUserIds }
        val followerUserIdsDelta = previousFollowerUserIds.filter { it !in followerUserIds }
        val delta = (followingUserIdsDelta + followerUserIdsDelta - Env.IGNORE_USER_IDS).toSet()
        for (userId in delta) {
            val user = try {
                FarewellTwitterClient.users.showByUserId(userId = userId).execute().result
            } catch (e: PenicillinTwitterApiException) {
                // フォローまたはフォロワーから検索
                val user = previousFollowerUsers.find { it.id == userId } ?: previousFollowingUsers.find { it.id == userId } ?: return

                when (e.error) {
                    TwitterApiError.UserNotFound -> {
                        val embed = DiscordEmbed(
                            title = "アカウントが削除されました",
                            author = DiscordEmbed.Author(
                                name = "${user.name} @${user.screenName}",
                                url = "https://twitter.com/${user.screenName}"
                            )
                        )
                        sendToDiscord(embed)
                    }
                    TwitterApiError.SuspendedUser -> {
                        val embed = DiscordEmbed(
                            title = "アカウントが凍結されました",
                            author = DiscordEmbed.Author(
                                name = "${user.name} @${user.screenName}",
                                url = "https://twitter.com/${user.screenName}"
                            )
                        )
                        sendToDiscord(embed)
                    }
                    else -> {
                        logger.error(e) { "ユーザ ID: $userId の取得中にエラーが発生しました。" }
                    }
                }

                transaction(FarewellDatabase) {
                    InactiveUsers.insert {
                        it[id] = user.id
                        it[screenName] = user.screenName
                        it[name] = user.name
                    }
                }

                continue
            }

            if (!user.profileInterstitialType.isNullOrBlank()) {
                val embed = DiscordEmbed(
                    title = "アカウントロックされました",
                    author = DiscordEmbed.Author(
                        name = "${user.name} @${user.screenName}",
                        url = "https://twitter.com/${user.screenName}",
                        iconUrl = user.profileImageUrlHttps
                    )
                )
                sendToDiscord(embed)

                continue
            }

            val relationship = try {
                FarewellTwitterClient.friendships.showByUserId(targetId = userId).execute().result.relationship
            } catch (e: PenicillinException) {
                logger.error(e) { "ユーザ: @${user.screenName} の取得中にエラーが発生しました。" }

                continue
            }

            when {
                relationship.source.blockedBy -> {
                    val embed = DiscordEmbed(
                        title = "ブロックされました",
                        author = DiscordEmbed.Author(
                            name = "${user.name} @${user.screenName}",
                            url = "https://twitter.com/${user.screenName}",
                            iconUrl = user.profileImageUrlHttps
                        )
                    )
                    sendToDiscord(embed)
                }
                relationship.source.blocking -> {
                    val embed = DiscordEmbed(
                        title = "ブロックしました",
                        author = DiscordEmbed.Author(
                            name = "${user.name} @${user.screenName}",
                            url = "https://twitter.com/${user.screenName}",
                            iconUrl = user.profileImageUrlHttps
                        )
                    )
                    sendToDiscord(embed)
                }
                else -> {
                    val embed = DiscordEmbed(
                        title = "リムーブされました",
                        author = DiscordEmbed.Author(
                            name = "${user.name} @${user.screenName}",
                            url = "https://twitter.com/${user.screenName}",
                            iconUrl = user.profileImageUrlHttps
                        ),
                        description = "現在, ${if (relationship.source.following) "片思い" else "関係消滅"}中です",
                        fields = listOf(
                            DiscordEmbed.Field(
                                name = "フォロワー / フォロー",
                                value = if (user.friendsCount != 0) String.format("%.3f", user.followersCount.toDouble() / user.friendsCount) else user.followersCount.toString()
                            )
                        )
                    )
                    sendToDiscord(embed)
                }
            }
        }

        transaction(FarewellDatabase) {
            FollowingUsers.deleteAll()
            followingUsers.forEach { user ->
                FollowingUsers.insert {
                    it[id] = user.id
                    it[screenName] = user.screenName
                    it[name] = user.name
                }
            }

            FollowerUsers.deleteAll()
            followerUsers.forEach { user ->
                FollowerUsers.insert {
                    it[id] = user.id
                    it[screenName] = user.screenName
                    it[name] = user.name
                }
            }
        }
    }

    //    override fun onFollow(event: UserStreamUserEvent) {
    //        if (event.target.id == account.userObj.id) {
    //            account.client.friend.followingList(userId = event.source.id, count = 100).queue {
    //                val commonFollowers = it.untilLast().allUsers.map { "@${it.screenName}" }
    //
    //                if (commonFollowers.isNotEmpty()) {
    //                    slack.message("#twitter") {
    //                        username = "Twitter Notice")
    //                        icon = ":desktop_computer:")
    //                        textBuilder {
    //                            appendln("@${event.source.screenName} との共通のフォロワー:")
    //                            append(commonFollowers.joinToString(", "))
    //                        }
    //                    }
    //                }
    //            }
    //        }
    //    }

    private suspend fun sendToDiscord(embed: DiscordEmbed) {
        logger.trace { embed }

        if (Env.DRYRUN) {
            return
        }

        FarewellHttpClient.post<Unit>(Env.DISCORD_WEBHOOK_URL) {
            contentType(ContentType.Application.Json)

            body = DiscordWebhookMessage(
                embeds = listOf(embed)
            )
        }
    }
}
