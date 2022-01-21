package examples.android.lib

import org.junit.Assert.assertEquals
import org.junit.Test

class SomeTest {

  @Test
  fun someTest() {
    val value = TestKtValue.create {
      setName("foo")
    }
    assertEquals("foo_internal", value.nameInternal())
  }
}
