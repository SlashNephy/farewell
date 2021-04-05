package blue.starry.farewell

import org.jetbrains.exposed.sql.Table

object FollowingUsers: Table() {
    val id = long("id")
    val screenName = varchar("screen_name", 15)
    val name = varchar("name", 50)
}

object FollowerUsers: Table() {
    val id = long("id")
    val screenName = varchar("screen_name", 15)
    val name = varchar("name", 50)
}

object InactiveUsers: Table() {
    val id = long("id")
    val screenName = varchar("screen_name", 15)
    val name = varchar("name", 50)
}

data class AbstractDBUser(val id: Long, val screenName: String, val name: String)
