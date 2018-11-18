package x

import kotlinx.coroutines.experimental.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class ExampleTest {

  @Test
  fun aTestCase(): Unit = runBlocking {
    assertEquals("a greeting", greet())
  }
}
