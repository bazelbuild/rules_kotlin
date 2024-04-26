package io.bazel.kotlin.plugin.jdeps.k2.checker.declaration

import io.bazel.kotlin.plugin.jdeps.k2.recordTypeRef
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirCallableDeclarationChecker
import org.jetbrains.kotlin.fir.declarations.FirAnonymousFunction
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.utils.isExtension

internal object CallableChecker : FirCallableDeclarationChecker(MppCheckerKind.Common) {
  /**
   * Tracks the return type & type parameters of a callable declaration. Function parameters are
   * tracked in [FunctionChecker].
   */
  override fun check(
    declaration: FirCallableDeclaration,
    context: CheckerContext,
    reporter: DiagnosticReporter,
  ) {
    // return type
    declaration.returnTypeRef.recordTypeRef(context)

    // type params
    declaration.typeParameters.forEach { typeParam ->
      typeParam.symbol.resolvedBounds.forEach { typeParamBound ->
        typeParamBound.recordTypeRef(context)
      }
    }

    // receiver param for extensions
    if (declaration !is FirAnonymousFunction) {
      declaration.receiverParameter?.typeRef
        ?.recordTypeRef(context, isExplicit = declaration.isExtension)
    }
  }
}
