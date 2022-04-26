package io.bazel.kotlin.builder.jobs.jvm

import java.nio.file.FileSystem

interface CompilationInputs {
  val fileSystem: FileSystem
  val classpath: List<Artifact>
  val directDependencies: List<Artifact>
  val depsArtifacts: List<Artifact>
  val sources: List<Artifact>
  val sourcesJars: List<Artifact>
  val processorPath: List<Artifact>
  val processors: List<Artifact>
  val stubsPluginOptions: List<Artifact>
  val stubsPluginClassPath: List<Artifact>
  val compilerPluginOptions: List<Artifact>
  val compilerPluginClassPath: List<Artifact>
  val ruleKind: String
  val moduleName: String?
  val passthroughFlags: List<String>
  val apiVersion: String?
  val languageVersion: String?
  val jvmTarget: String?
  val friendsPaths: List<Artifact>
  val jsPassthroughFlags: List<String>
  val jsLibraries: List<Artifact>
  val debug: List<String>
  val taskId: String
  val abiJar: Artifact?
  val generatedJavaSrcJar: Artifact?
  val generatedJavaStubJar: Artifact?
  val generatedClassJar: Artifact?
  val buildKotlin: String?
  val strictKotlinDeps: Boolean
  val reducedClasspathMode: Boolean
  val instrumentCoverage: Boolean
  val targetLabel: String
}
