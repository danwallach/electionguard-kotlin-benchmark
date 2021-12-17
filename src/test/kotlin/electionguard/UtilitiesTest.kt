package electionguard

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class UtilitiesTest {
    @Test
    fun testMapArray() {
        assertTrue(
            arrayOf(1, 4, 9, 16, 25).contentDeepEquals(arrayOf(1, 2, 3, 4, 5).mapArray { it * it })
        )
    }
}
