package io.bazel.kotlin.plugin.jdeps.k2.checker.expression

import io.bazel.kotlin.plugin.jdeps.k2.binaryClass
import io.bazel.kotlin.plugin.jdeps.k2.recordClassBinaryPath
import io.bazel.kotlin.plugin.jdeps.k2.recordConeType
import io.bazel.kotlin.plugin.jdeps.k2.recordTypeRef
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirQualifiedAccessExpressionChecker
import org.jetbrains.kotlin.fir.declarations.FirConstructor
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.expressions.arguments
import org.jetbrains.kotlin.fir.expressions.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.references.toResolvedFunctionSymbol
import org.jetbrains.kotlin.fir.types.isExtensionFunctionType
import org.jetbrains.kotlin.fir.types.isUnit
import org.jetbrains.kotlin.fir.types.resolvedType

internal object QualifiedAccessChecker :
  FirQualifiedAccessExpressionChecker(MppCheckerKind.Common) {
  override fun check(
    expression: FirQualifiedAccessExpression,
    context: CheckerContext,
    reporter: DiagnosticReporter,
  ) {
    // track function's owning class
    val resolvedCallableSymbol = expression.toResolvedCallableSymbol()
    resolvedCallableSymbol?.containerSource?.binaryClass()?.recordClassBinaryPath()

    // track return type
    val isExplicitReturnType: Boolean = expression is FirConstructor
    resolvedCallableSymbol?.resolvedReturnTypeRef
      ?.recordTypeRef(context, isExplicit = isExplicitReturnType, collectTypeArguments = false)

    // type arguments
    resolvedCallableSymbol?.typeParameterSymbols?.forEach { typeParam ->
      typeParam.resolvedBounds.forEach { it.recordTypeRef(context) }
    }

    // track fun parameter types based on referenced function
    expression.calleeReference.toResolvedFunctionSymbol()
      ?.valueParameterSymbols
      ?.forEach {
        it.resolvedReturnTypeRef.recordTypeRef(context, isExplicit = false)
      }
    // track fun arguments actually passed
    (expression as? FirFunctionCall)?.arguments?.map { it.resolvedType }?.forEach {
      it.recordConeType(context, isExplicit = !it.isExtensionFunctionType)
    }

    // track dispatch receiver
    expression.dispatchReceiver?.resolvedType?.let {
      if (!it.isUnit) {
        it.recordConeType(context, isExplicit = false)
      }
    }
  }
}
