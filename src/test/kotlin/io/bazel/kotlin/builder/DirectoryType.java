package io.bazel.kotlin.builder;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.EnumSet;

public enum DirectoryType {
  SOURCES("sources", Paths.get("sources")),
  CLASSES("compiled classes", Paths.get("classes")),
  GENERATED_CLASSES("generated classes", Paths.get("generated_classes")),
  TEMP("temp directory", Paths.get("temp")),
  SOURCE_GEN("generated sources directory", Paths.get("generated_sources"));

  final String name;
  final Path relativePath;

  DirectoryType(String name, Path relativePath) {
    this.name = name;
    this.relativePath = relativePath;
  }

  Path resolve(Path root) {
    return root.resolve(relativePath);
  }

  static void createAll(Path root, EnumSet<DirectoryType> types) {
    for (DirectoryType instanceType : types) {
      try {
        Files.createDirectory(instanceType.resolve(root));
      } catch (IOException e) {
        throw new UncheckedIOException(instanceType.name, e);
      }
    }
  }
}
