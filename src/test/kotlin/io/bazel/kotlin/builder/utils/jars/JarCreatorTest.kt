package io.bazel.kotlin.builder.utils.jars

import com.google.common.truth.Truth.assertThat
import io.bazel.testing.Temporary
import org.junit.jupiter.api.Test;
import java.util.zip.ZipFile

class JarCreatorTest {
  @Test fun createDirectories() {
    val root = Temporary.directoryFor<JarCreatorTest> {
      file("ibbity/bibbity/zibbity.zee", "Hellity, crackity, bumble-bee.")
    }

    val got = Temporary.directoryFor<JarCreatorTest>().resolve("out.jar").apply {
      JarCreator(this).use { it.addDirectory(root) }
    }

    assertThat(
      ZipFile(got.toFile()).entries().asSequence().map { it.name }.toSet()
    ).containsExactly(
      "META-INF/", "META-INF/MANIFEST.MF", "ibbity/", "ibbity/bibbity/",
      "ibbity/bibbity/zibbity.zee"
    )
  }
}
