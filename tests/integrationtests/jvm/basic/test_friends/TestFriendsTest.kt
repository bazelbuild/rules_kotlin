package test

import org.junit.Test

class TestFriendsTest {
    val service: Service = Service()

    @Test
    fun testCanAccessFriendMembers() {
        println(service.value)
        println(service.iSayHolla(DEFAULT_FRIEND))
    }
}