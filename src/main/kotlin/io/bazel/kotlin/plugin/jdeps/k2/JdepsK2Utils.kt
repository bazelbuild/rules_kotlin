package io.bazel.kotlin.plugin.jdeps.k2

import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.java.JavaBinarySourceElement
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.load.kotlin.JvmPackagePartSource
import org.jetbrains.kotlin.load.kotlin.KotlinJvmBinarySourceElement
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource

/**
 * Returns whether class is coming from JVM runtime env. There is no need to track these classes.
 *
 * @param className the class name of the class
 * @return whether class is provided by JSM runtime or not
 */
internal fun isJvmClass(className: String): Boolean {
  return className.startsWith("java") ||
    className.startsWith("modules/java.base/java/")
}

internal fun DeserializedContainerSource.classId(): ClassId? {
  return when (this) {
    is JvmPackagePartSource -> classId
    is KotlinJvmBinarySourceElement -> binaryClass.classId
    else -> null
  }
}

internal fun SourceElement.binaryClass(): String? {
  return when (this) {
    is KotlinJvmBinarySourceElement -> binaryClass.location
    is JvmPackagePartSource -> this.knownJvmBinaryClass?.location
    is JavaBinarySourceElement -> this.javaClass.virtualFile.path
    else -> null
  }
}

internal fun DeserializedContainerSource.binaryClass(): String? {
  return when (this) {
    is JvmPackagePartSource -> this.knownJvmBinaryClass?.location
    is KotlinJvmBinarySourceElement -> binaryClass.location
    else -> null
  }
}

// Extension functions to clean up checker logic

internal fun FirClassLikeSymbol<*>.recordClass(
  context: CheckerContext,
  isExplicit: Boolean = true,
  collectTypeArguments: Boolean = true,
  visited: MutableSet<Pair<ClassId, Boolean>> = mutableSetOf(),
) {
  ClassUsageRecorder.recordClass(this, context, isExplicit, collectTypeArguments, visited)
}

internal fun FirTypeRef.recordTypeRef(
  context: CheckerContext,
  isExplicit: Boolean = true,
  collectTypeArguments: Boolean = true,
  visited: MutableSet<Pair<ClassId, Boolean>> = mutableSetOf(),
) {
  ClassUsageRecorder.recordTypeRef(this, context, isExplicit, collectTypeArguments, visited)
}

internal fun ConeKotlinType.recordConeType(
  context: CheckerContext,
  isExplicit: Boolean = true,
  collectTypeArguments: Boolean = true,
) {
  ClassUsageRecorder.recordConeType(this, context, isExplicit, collectTypeArguments)
}

internal fun String.recordClassBinaryPath(isExplicit: Boolean = true) {
  ClassUsageRecorder.recordClass(this, isExplicit)
}
