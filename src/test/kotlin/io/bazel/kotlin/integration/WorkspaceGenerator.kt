package io.bazel.kotlin.integration

import io.bazel.kotlin.integration.WriteWorkspace.Block
import io.bazel.kotlin.integration.WriteWorkspace.KotlinSource

class WorkspaceGenerator {


  class PkgName(parts: List<String>) : List<String> by parts {
    constructor(vararg parts: String) : this(parts.toList())

    private val pkg = when {
      size > 1 -> dropLast(1)
      else -> this
    }

    val jvmPackage = pkg.joinToString(".")
    val path = pkg.joinToString("/")
    val target = "//$path:${last()}"
    val className = last().replaceFirstChar(Char::uppercase)
    val classFile = "$className.kt"
    val qualifiedName = "$jvmPackage.$className"

    fun resolve(postfix: String): PkgName {
      return PkgName(plus(postfix))
    }

    fun append(postfix: String): PkgName {
      return PkgName(pkg + (last() + postfix.replaceFirstChar(Char::uppercase)))
    }

    override fun equals(other: Any?): Boolean {
      return (other as? PkgName)
        ?.let { it.toList() == toList() }
        ?: false
    }

    override fun hashCode(): Int {
      return path.hashCode()
    }
  }


  data class Library(
    val name: PkgName,
    val types: List<Type>,
    val provide: List<Type>
  ) {
    fun remove(library: Library) {
      types.forEach { type ->
        library.provide.forEach { dependency ->
          type.remove(dependency)
        }
      }
    }

    fun add(library: Library) {
      types.forEach { type ->
        library.provide.forEach { dependency ->
          type.add(dependency)
        }
      }
    }

    fun <T> write(pkg: T)
      where T : WriteWorkspace.SubPackage<T>,
            T : WriteWorkspace.Package {

    }

  }

  class Type(
    val name: String,
    val parents: MutableList<Type>,
    val methods: MutableList<Method>,
    val define: KotlinSource.(List<String>, KotlinSource.() -> Unit) -> Unit,
  ) {
    fun remove(dependency: Type) {
      parents.remove(dependency)
    }

    fun add(dependency: Type) {
      parents.remove(dependency)
    }

    fun write(source: KotlinSource) {
      source.apply {
        define(parents.map { it.name }) {
          parents.flatMap { it.methods }.forEach { method ->
            apply(method.override)
          }
        }
        methods.forEach { method ->
          apply(method.define)
        }
      }
    }
  }
}

class Method(
  val define: Block<KotlinSource>.() -> Unit,
  val override: Block<KotlinSource>.() -> Unit,
)


}
