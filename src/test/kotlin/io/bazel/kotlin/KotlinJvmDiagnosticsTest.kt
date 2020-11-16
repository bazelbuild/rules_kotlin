package io.bazel.kotlin

import org.junit.Test
import io.bazel.kotlin.model.diagnostics.Diagnostics


class KotlinJvmDiagnosticsTest: KotlinAssertionTestCase("src/test/data/jvm/diagnostics"){
  //No tests for error compilation messages were added, since that would make it uncompilable
  @Test
  fun testDiagnostics(){
    val infoDiagnostic = Diagnostics.Diagnostic
      .newBuilder()
      .setSeverity(Diagnostics.Severity.INFORMATION)
      .build()

    val warningDiagnostic = Diagnostics.Diagnostic
      .newBuilder()
      .setSeverity(Diagnostics.Severity.WARNING)
      .setRange(
        Diagnostics.Range
          .newBuilder()
          .setStart(Diagnostics.Position.newBuilder().setLine(1).setCharacter(6))
      )
      .build()

    diagnosticsTestCase(
      "test_info_file.diagnosticsproto",
      description = "Output of an info diagnostics message",
      expectedDiagnostics =  listOf(infoDiagnostic)
    )

    diagnosticsTestCase(
      "test_warning_and_info_file.diagnosticsproto",
      description = "Output of a warning and an info diagnostics message",
      expectedDiagnostics =  listOf(infoDiagnostic, warningDiagnostic)
    )
  }
}
