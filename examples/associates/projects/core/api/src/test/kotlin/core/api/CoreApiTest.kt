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
}
