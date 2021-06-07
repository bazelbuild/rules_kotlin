package core.impl

import com.nhaarman.mockito_kotlin.mock
import org.junit.Test

class TestDataTest {
  @Test
  fun test1() {
    val a = object : TestData {
      override val a: Int = 1
    }
  }
}
