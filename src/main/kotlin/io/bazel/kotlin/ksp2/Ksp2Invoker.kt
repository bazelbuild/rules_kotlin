/*
 * Copyright 2025 The Bazel Authors. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.bazel.kotlin.ksp2

import java.io.File
import java.util.ServiceLoader

/**
 * Isolated wrapper for KSP2 invocation.
 *
 * Loaded by a dedicated classloader and invoked reflectively from the worker.
 * The request uses only JDK types to avoid sharing custom classes between loaders.
 */
object Ksp2Invoker {
  private const val KSP_JVM_CONFIG_BUILDER_CLASS = "com.google.devtools.ksp.processing.KSPJvmConfig\$Builder"
  private const val KSP_LOGGER_CLASS = "com.google.devtools.ksp.processing.KspGradleLogger"
  private const val KSP_PROCESSOR_PROVIDER_CLASS = "com.google.devtools.ksp.processing.SymbolProcessorProvider"
  private const val KSP_PROCESSING_IMPL_CLASS = "com.google.devtools.ksp.impl.KotlinSymbolProcessing"

  @JvmStatic
  fun execute(
    classLoader: ClassLoader,
    request: Map<String, Any?>,
  ): Int {
    val processors = loadProcessors(classLoader)
    val kspConfig = buildJvmConfig(classLoader, request)
    val logger = instantiate(classLoader.loadClass(KSP_LOGGER_CLASS), request.requireInt("logLevel"))
    val ksp = instantiateKsp(classLoader, kspConfig, processors, logger)
    val executeResult = invokeNoArgs(ksp, "execute")
    return extractExitCode(executeResult)
  }

  private fun Map<String, Any?>.optionalString(key: String): String? = this[key] as? String

  private fun Map<String, Any?>.requireString(key: String): String =
    optionalString(key) ?: error("Missing KSP2 request value: $key")

  private fun Map<String, Any?>.requireStringList(key: String): List<String> =
    (this[key] as? List<*>)?.map {
      it as? String ?: error("KSP2 request value '$key' must be List<String>")
    } ?: error("Missing KSP2 request value: $key")

  private fun Map<String, Any?>.requireInt(key: String): Int =
    (this[key] as? Number)?.toInt() ?: error("Missing KSP2 request value: $key")

  private fun loadProcessors(classLoader: ClassLoader): List<Any> {
    val providerClass = classLoader.loadClass(KSP_PROCESSOR_PROVIDER_CLASS)
    return ServiceLoader.load(providerClass, classLoader).toList()
  }

  private fun buildJvmConfig(
    classLoader: ClassLoader,
    request: Map<String, Any?>,
  ): Any {
    val builder = instantiate(classLoader.loadClass(KSP_JVM_CONFIG_BUILDER_CLASS))

    setProperty(builder, "moduleName", request.requireString("moduleName"))
    setProperty(builder, "sourceRoots", request.requireStringList("sourceRoots").map(::File))
    setProperty(builder, "javaSourceRoots", request.requireStringList("javaSourceRoots").map(::File))
    setProperty(builder, "libraries", request.requireStringList("libraries").map(::File))
    setProperty(builder, "kotlinOutputDir", File(request.requireString("kotlinOutputDir")))
    setProperty(builder, "javaOutputDir", File(request.requireString("javaOutputDir")))
    setProperty(builder, "classOutputDir", File(request.requireString("classOutputDir")))
    setProperty(builder, "resourceOutputDir", File(request.requireString("resourceOutputDir")))
    setProperty(builder, "cachesDir", File(request.requireString("cachesDir")))
    setProperty(builder, "projectBaseDir", File(request.requireString("projectBaseDir")))
    setProperty(builder, "outputBaseDir", File(request.requireString("outputBaseDir")))
    setProperty(builder, "mapAnnotationArgumentsInJava", true)

    request.optionalString("jvmTarget")?.let { setProperty(builder, "jvmTarget", it) }
    request.optionalString("languageVersion")?.let { setProperty(builder, "languageVersion", it) }
    request.optionalString("apiVersion")?.let { setProperty(builder, "apiVersion", it) }
    request.optionalString("jdkHome")?.let { setProperty(builder, "jdkHome", File(it)) }

    return invokeNoArgs(builder, "build")
  }

  private fun instantiateKsp(
    classLoader: ClassLoader,
    kspConfig: Any,
    processors: List<Any>,
    logger: Any,
  ): Any {
    val kspClass = classLoader.loadClass(KSP_PROCESSING_IMPL_CLASS)
    val ctor =
      kspClass.constructors.firstOrNull { constructor ->
        constructor.parameterCount == 3 &&
          isAssignable(constructor.parameterTypes[0], kspConfig) &&
          isAssignable(constructor.parameterTypes[1], processors) &&
          isAssignable(constructor.parameterTypes[2], logger)
      } ?: error("No compatible KotlinSymbolProcessing constructor found")
    return ctor.newInstance(kspConfig, processors, logger)
  }

  private fun setProperty(
    target: Any,
    property: String,
    value: Any,
  ) {
    val setterName = "set" + property.replaceFirstChar { it.uppercase() }
    val setter =
      target.javaClass.methods.firstOrNull { method ->
        method.name == setterName &&
          method.parameterCount == 1 &&
          isAssignable(method.parameterTypes[0], value)
      } ?: error("Setter not found: ${target.javaClass.name}.$setterName(${value.javaClass.name})")
    setter.invoke(target, value)
  }

  private fun invokeNoArgs(
    target: Any,
    methodName: String,
  ): Any {
    val method =
      target.javaClass.methods.firstOrNull { it.name == methodName && it.parameterCount == 0 }
        ?: error("Method not found: ${target.javaClass.name}.$methodName()")
    return method.invoke(target) ?: error("Method returned null: ${target.javaClass.name}.$methodName()")
  }

  private fun instantiate(
    clazz: Class<*>,
    vararg args: Any,
  ): Any {
    val ctor =
      clazz.constructors.firstOrNull { constructor ->
        constructor.parameterCount == args.size &&
          constructor.parameterTypes.indices.all { idx ->
            isAssignable(constructor.parameterTypes[idx], args[idx])
          }
      } ?: error("No matching constructor for ${clazz.name}(${args.joinToString { it.javaClass.name }})")
    return ctor.newInstance(*args)
  }

  private fun extractExitCode(result: Any): Int {
    if (result is Number) return result.toInt()

    result.javaClass.methods.firstOrNull { it.name == "getCode" && it.parameterCount == 0 }?.let { method ->
      val code = method.invoke(result)
      if (code is Number) return code.toInt()
    }

    result.javaClass.declaredFields.firstOrNull { it.name == "code" }?.let { field ->
      field.isAccessible = true
      val code = field.get(result)
      if (code is Number) return code.toInt()
    }

    if (result is Enum<*>) {
      return if (result.name == "OK" || result.name == "SUCCESS") 0 else 1
    }

    error("Unsupported KSP execute() return type: ${result.javaClass.name}")
  }

  private fun isAssignable(
    expectedType: Class<*>,
    value: Any,
  ): Boolean {
    if (expectedType.isPrimitive) {
      return primitiveWrapper(expectedType).isInstance(value)
    }
    return expectedType.isAssignableFrom(value.javaClass)
  }

  private fun primitiveWrapper(type: Class<*>): Class<*> =
    when (type) {
      java.lang.Boolean.TYPE -> Boolean::class.javaObjectType
      java.lang.Byte.TYPE -> Byte::class.javaObjectType
      java.lang.Character.TYPE -> Char::class.javaObjectType
      java.lang.Short.TYPE -> Short::class.javaObjectType
      java.lang.Integer.TYPE -> Int::class.javaObjectType
      java.lang.Long.TYPE -> Long::class.javaObjectType
      java.lang.Float.TYPE -> Float::class.javaObjectType
      java.lang.Double.TYPE -> Double::class.javaObjectType
      java.lang.Void.TYPE -> Void::class.javaObjectType
      else -> type
    }
}
