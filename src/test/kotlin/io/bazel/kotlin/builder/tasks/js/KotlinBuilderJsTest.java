package io.bazel.kotlin.builder.tasks.js;

import io.bazel.kotlin.builder.KotlinJsTestBuilder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static com.google.common.truth.Truth.assertThat;

@RunWith(JUnit4.class)
public class KotlinBuilderJsTest {
    private static final KotlinJsTestBuilder builder = new KotlinJsTestBuilder();

    @Test
    public void testSimpleJsCompile() {
        builder.runCompilationTask(
                it -> it.addSource("AClass.kt", "package something", "class AClass{}"));
    }

    @Test
    public void testJsErrorRendering() {
        builder.runFailingCompileTaskAndValidateOutput(
                () ->
                        builder.runCompilationTask(
                                it -> it.addSource("AClass.kt", "package something", "class AClass{")),
                lines -> assertThat(lines).contains(builder.toPlatform("sources/AClass.kt:2:14: error: missing '}")));
    }
}