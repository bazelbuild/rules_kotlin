package io.bazel.kotlin.builder;

import org.junit.Test;

public class KotlinBuilderActionTests extends KotlinBuilderTestCase {
  @Test
  public void testCompileSimple() {
    addSource("AClass.kt", "package something;" + "class AClass{}");
    component().jvmCompiler().compile(builderCommand());
    assertFileExists(DirectoryType.CLASSES, "something/AClass.class");
    assertFileDoesNotExist(outputs().getJar());
  }
}
