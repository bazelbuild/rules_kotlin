package io.bazel.kotlin.plugin.jdeps.k2

import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.fir.java.JavaBinarySourceElement
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
internal fun isJvmClass(className: String): Boolean =
  className.startsWith("java") ||
    className.startsWith("modules/java.base/java/")

internal fun DeserializedContainerSource.classId(): ClassId? =
  when (this) {
    is JvmPackagePartSource -> classId
    is KotlinJvmBinarySourceElement -> binaryClass.classId
    else -> null
  }

internal fun SourceElement.binaryClass(): String? =
  when (this) {
    is KotlinJvmBinarySourceElement -> binaryClass.location
    is JvmPackagePartSource -> this.knownJvmBinaryClass?.location
    is JavaBinarySourceElement -> this.javaClass.virtualFile.path
    else -> null
  }

internal fun DeserializedContainerSource.binaryClass(): String? =
  when (this) {
    is JvmPackagePartSource -> this.knownJvmBinaryClass?.location
    is KotlinJvmBinarySourceElement -> binaryClass.location
    else -> null
  }
