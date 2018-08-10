package io.bazel.kotlin.builder;

import org.junit.Test;

public class KotlinBuilderActionTests extends KotlinBuilderTestCase {
  @Test
  public void testCompileSimple() {
    addSource("AClass.kt", "package something;" + "class AClass{}");
    component().jvmTaskExecutor().compileKotlin(builderCommand(), context(), true);
    assertFileExists(DirectoryType.CLASSES, "something/AClass.class");
    assertFileDoesNotExist(outputs().getJar());
  }
}
