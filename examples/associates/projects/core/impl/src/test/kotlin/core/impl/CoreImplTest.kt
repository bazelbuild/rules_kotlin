package core.impl

import core.api.camelCase
import org.junit.Assert.assertEquals
import org.junit.Test

class CoreImplTest {
  @Test fun testCamelCaseVar() {
    val foo = ImplType("foo_bar")
    assertEquals("FooBar", foo.camelName)
  }

  @Test fun testCamelCaseFun() {
    // Testing transitivity here. TODO: Once strict deps are in place, delete this case.
    assertEquals("FooBar", "foo_bar".camelCase())
  }
}
