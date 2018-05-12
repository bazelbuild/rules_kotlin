package io.bazel.kotlin.builder;

import io.bazel.kotlin.builder.mode.jvm.actions.JacocoProcessor;
import io.bazel.kotlin.builder.mode.jvm.actions.KotlinCompiler;
import org.junit.Test;

public class KotlinBuilderActionTests extends KotlinBuilderTestCase {
  @Test
  public void testCompileSimple() {
    addSource("AClass.kt", "package something;" + "class AClass{}");
    instance(KotlinCompiler.class).compile(builderCommand());
    assertFileExists(DirectoryType.CLASSES, "something/AClass.class");
    assertFileDoesNotExist(outputs().getJar());
  }

  @Test
  public void testCoverage() {
    addSource("AClass.kt", "package something;" + "class AClass{}");
    instance(KotlinCompiler.class).compile(builderCommand());
    instance(JacocoProcessor.class).instrument(builderCommand());
    assertFileExists(DirectoryType.CLASSES, "something/AClass.class");
    assertFileExists(DirectoryType.CLASSES, "something/AClass.class.uninstrumented");
    assertFileDoesNotExist(outputs().getJar());
  }
}
