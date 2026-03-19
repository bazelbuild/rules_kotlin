package example

import org.junit.Assert.assertEquals
import org.junit.Test

class CompatTest {
  @Test
  fun kotlinVersionComesFrom2310StdLib() {
    assertEquals("2.3.10", KotlinVersion.CURRENT.toString())
  }

  @Test
  fun kotlinCompiler2310IsUsedToCompileThisTarget() {
    val generatedVersion = Class.forName("example.CompatKt")
      .getMethod("getGeneratedCompilerVersion")
      .invoke(null) as String

    assertEquals("2.3.10", generatedVersion)
  }
}
