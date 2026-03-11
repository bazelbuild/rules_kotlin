package example

import org.junit.Assert.assertEquals
import org.junit.Test

class CompatTest {
  @Test
  fun kotlinVersionComesFrom2310Toolchain() {
    assertEquals("2.3.10", KotlinVersion.CURRENT.toString())
  }
}
