package io.bazel.kotlin.builder.utils.jars

import io.bazel.kotlin.builder.utils.jars.JarHelper.Companion.INJECTING_RULE_KIND
import io.bazel.kotlin.builder.utils.jars.JarHelper.Companion.TARGET_LABEL
import java.io.IOException
import java.io.UncheckedIOException
import java.nio.file.Path
import java.util.jar.JarFile

data class JarOwner(val jar: Path, val label: String? = null, val aspect: String? = null) {
  companion object {
    fun readJarOwnerFromManifest(jarPath: Path): JarOwner {
      try {
        JarFile(jarPath.toFile()).use { jarFile ->
          val manifest = jarFile.manifest ?: return JarOwner(jarPath)
          val attributes = manifest.mainAttributes
          val label = attributes[TARGET_LABEL] as String?
            ?: return JarOwner(jarPath)
          val injectingRuleKind = attributes[INJECTING_RULE_KIND] as String?
          return JarOwner(jarPath, label, injectingRuleKind)
        }
      } catch (e: IOException) {
        // This jar file pretty much has to exist.
        throw UncheckedIOException(e)
      }
    }
  }
}
