package io.bazel.kotlin.plugin.jdeps.k2.checker.declaration

import io.bazel.kotlin.plugin.jdeps.k2.recordClass
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirBasicDeclarationChecker
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.toAnnotationClassLikeSymbol

internal object BasicDeclarationChecker : FirBasicDeclarationChecker(MppCheckerKind.Common) {
  override fun check(
    declaration: FirDeclaration,
    context: CheckerContext,
    reporter: DiagnosticReporter,
  ) {
    declaration.annotations.forEach { annotation ->
      annotation.toAnnotationClassLikeSymbol(context.session)?.recordClass(context)
    }
  }
}
