package core.api

import org.junit.Assert.assertEquals
import org.junit.Test

class CoreApiTest {
  @Test fun testCamelCaseVar() {
    val foo = MyType("foo_bar")
    assertEquals("FooBar", foo.camelName)
  }

  @Test fun testCamelCaseFun() {
    assertEquals("FooBar", "foo_bar".camelCase())
  }

  @Test fun flatMapError() {
    val success = Result.of("success")
    val failure = Result.error(Exception("failure"))

    val v1 = success.flatMapError { Result.error(IllegalArgumentException()) }
    val v2 = failure.flatMapError { Result.error(IllegalArgumentException()) }
  }
}
