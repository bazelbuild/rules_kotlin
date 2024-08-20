package io.bazel.kotlin.plugin.jdeps.k2.checker.declaration

import io.bazel.kotlin.plugin.jdeps.k2.ClassUsageRecorder
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirBasicDeclarationChecker
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.toAnnotationClassLikeSymbol

internal class BasicDeclarationChecker(
  private val classUsageRecorder: ClassUsageRecorder,
) : FirBasicDeclarationChecker(MppCheckerKind.Common) {
  override fun check(
    declaration: FirDeclaration,
    context: CheckerContext,
    reporter: DiagnosticReporter,
  ) {
    declaration.annotations.forEach { annotation ->
      annotation.toAnnotationClassLikeSymbol(context.session)?.let {
        classUsageRecorder.recordClass(it, context)
      }
    }
  }
}
