package io.bazel.kotlin.plugin.jdeps.k2.checker.declaration

import io.bazel.kotlin.plugin.jdeps.k2.recordClass
import io.bazel.kotlin.plugin.jdeps.k2.recordConeType
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirClassLikeChecker
import org.jetbrains.kotlin.fir.declarations.FirClassLikeDeclaration
import org.jetbrains.kotlin.fir.resolve.getSuperTypes

internal object ClassLikeChecker : FirClassLikeChecker(MppCheckerKind.Common) {
  override fun check(
    declaration: FirClassLikeDeclaration,
    context: CheckerContext,
    reporter: DiagnosticReporter,
  ) {
    declaration.symbol.recordClass(context)
    // [recordClass] also handles supertypes, but this marks direct supertypes as explicit
    declaration.symbol.getSuperTypes(context.session, recursive = false).forEach {
      it.recordConeType(context)
    }
  }
}
