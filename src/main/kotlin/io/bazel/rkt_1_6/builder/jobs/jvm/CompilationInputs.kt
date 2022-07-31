package io.bazel.rkt_1_6.builder.jobs.jvm

import java.nio.file.FileSystem
import java.nio.file.Path

interface CompilationInputs {
  val fileSystem: FileSystem
  val classpath: List<Path>
  val directDependencies: List<Path>
  val depsPaths: List<Path>
  val sources: List<Path>
  val sourcesJars: List<Path>
  val processorPath: List<Path>
  val processors: List<Path>
  val stubsPluginOptions: List<Path>
  val stubsPluginClassPath: List<Path>
  val compilerPluginOptions: List<Path>
  val compilerPluginClassPath: List<Path>
  val ruleKind: String
  val moduleName: String?
  val passthroughFlags: List<String>
  val apiVersion: String?
  val languageVersion: String?
  val jvmTarget: String?
  val friendsPaths: List<Path>
  val jsPassthroughFlags: List<String>
  val jsLibraries: List<Path>
  val debug: List<String>
  val taskId: String
  val abiJar: Path?
  val generatedJavaSrcJar: Path?
  val generatedJavaStubJar: Path?
  val generatedClassJar: Path?
  val buildKotlin: String?
  val strictKotlinDeps: Boolean
  val reducedClasspathMode: Boolean
  val instrumentCoverage: Boolean
  val targetLabel: String
}
