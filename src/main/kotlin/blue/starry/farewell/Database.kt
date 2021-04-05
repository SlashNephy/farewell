package blue.starry.farewell

import org.jetbrains.exposed.dao.id.LongIdTable

object FollowingUsers: LongIdTable() {
    val screenName = varchar("screen_name", 15)
    val name = varchar("name", 50)
}

object FollowerUsers: LongIdTable() {
    val screenName = varchar("screen_name", 15)
    val name = varchar("name", 50)
}

object InactiveUsers: LongIdTable() {
    val screenName = varchar("screen_name", 15)
    val name = varchar("name", 50)
}

data class AbstractDBUser(val id: Long, val screenName: String, val name: String)
