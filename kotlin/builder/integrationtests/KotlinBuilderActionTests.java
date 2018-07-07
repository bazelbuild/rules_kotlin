package io.bazel.kotlin.builder;

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
}
