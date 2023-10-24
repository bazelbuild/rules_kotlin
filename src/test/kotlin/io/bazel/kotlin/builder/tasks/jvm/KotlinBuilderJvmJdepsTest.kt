/*
 * Copyright 2020 The Bazel Authors. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package io.bazel.kotlin.builder.tasks.jvm;

import com.google.common.truth.Truth.assertThat
import com.google.devtools.build.lib.view.proto.Deps
import io.bazel.kotlin.builder.Deps.*
import io.bazel.kotlin.builder.KotlinJvmTestBuilder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.BufferedInputStream
import java.nio.file.Files
import java.nio.file.Paths
import java.util.function.Consumer

@RunWith(Parameterized::class)
class KotlinBuilderJvmJdepsTest(private val enableK2Compiler: Boolean) {

  companion object {
    @JvmStatic
    @Parameterized.Parameters(name = "enableK2Compiler={0}")
    fun data(): Collection<Array<Any>> {
      return listOf(
        // TODO: Enable k2 for tests
//        arrayOf(true),
        arrayOf(false)
      )
    }
  }

  val ctx = KotlinJvmTestBuilder()

  val TEST_FIXTURES_DEP = Dep.fromLabel(":JdepsParserTestFixtures")

  @Test
  fun `no kotlin source produces empty jdeps`() {

    val deps = runJdepsCompileTask { c: KotlinJvmTestBuilder.TaskBuilder ->
      c.addSource(
        "AnotherClass.java",
        """
          package something;
          
          class AnotherClass {
          }
        """
      )
    }
    val jdeps = depsProto(deps)

    assertThat(jdeps.dependencyCount).isEqualTo(0)
    assertThat(jdeps.ruleLabel).isEqualTo(deps.label())
  }

  @Test
  fun `no dependencies`() {

    val deps = runJdepsCompileTask { c: KotlinJvmTestBuilder.TaskBuilder ->
      c.addDirectDependencies(TEST_FIXTURES_DEP)
    }
    val jdeps = depsProto(deps)

    assertThat(jdeps.dependencyCount).isEqualTo(0)
    assertThat(jdeps.ruleLabel).isEqualTo(deps.label())
  }

  @Test
  fun `java class static reference`() {
    val dependingTarget = runJdepsCompileTask { c: KotlinJvmTestBuilder.TaskBuilder ->
      c.addSource(
        "AClass.kt",
        """
          package something
          
          val result = JavaClass.staticMethod()
        """
      )
      c.addDirectDependencies(TEST_FIXTURES_DEP)
    }
    val jdeps = depsProto(dependingTarget)

    assertExplicit(jdeps).containsExactly(TEST_FIXTURES_DEP.singleCompileJar())
    assertImplicit(jdeps).isEmpty()
    assertUnused(jdeps).isEmpty()
    assertIncomplete(jdeps).isEmpty()
  }

  @Test
  fun `java constant reference`() {
    val dependingTarget = runJdepsCompileTask { c: KotlinJvmTestBuilder.TaskBuilder ->
      c.addSource(
        "AnotherClass.kt",
        """
          package something.other
          
          import something.Constants
          
          class AnotherClass {
            val ref = Constants.HELLO_CONSTANT
          }
        """
      )
      c.addDirectDependencies(TEST_FIXTURES_DEP)
    }
    val jdeps = depsProto(dependingTarget)

    assertThat(jdeps.ruleLabel).isEqualTo(dependingTarget.label())

    assertExplicit(jdeps).containsExactly(TEST_FIXTURES_DEP.singleCompileJar())
    assertImplicit(jdeps).isEmpty()
    assertUnused(jdeps).isEmpty()
    assertIncomplete(jdeps).isEmpty()
  }

  @Test
  fun `java annotation reference`() {
    val dependingTarget = runJdepsCompileTask { c: KotlinJvmTestBuilder.TaskBuilder ->
      c.addSource(
        "AnotherClass.kt",
        """
          package something.other
          
          import something.JavaAnnotation
 
          abstract class AnotherClass {
            @JavaAnnotation
            internal abstract fun hasAnnotation()
          }
        """
      )
      c.addDirectDependencies(TEST_FIXTURES_DEP)
    }
    val jdeps = depsProto(dependingTarget)

    assertThat(jdeps.ruleLabel).isEqualTo(dependingTarget.label())

    assertExplicit(jdeps).contains(TEST_FIXTURES_DEP.singleCompileJar())
    assertImplicit(jdeps).isEmpty()
    assertUnused(jdeps).isEmpty()
    assertIncomplete(jdeps).isEmpty()
  }


  @Test
  fun `annotation on class is an explict dep`() {

    val dependentTarget = runJdepsCompileTask { c: KotlinJvmTestBuilder.TaskBuilder ->
      c.addSource(
        "Annotation.kt",
        """
          package something;
          
          @Target(AnnotationTarget.CLASS)
          @Retention(AnnotationRetention.SOURCE)
          annotation class ClassAnnotation
        """
      )
    }

    val dependingTarget = runJdepsCompileTask { c: KotlinJvmTestBuilder.TaskBuilder ->
      c.addSource(
        "AnotherClass.kt",
        """
          package something

          @ClassAnnotation
          class AnotherClass { }
        """
      )
      c.addDirectDependencies(dependentTarget)
    }
    val jdeps = depsProto(dependingTarget)

    assertThat(jdeps.ruleLabel).isEqualTo(dependingTarget.label())

    assertExplicit(jdeps).contains(dependentTarget.singleCompileJar())
    assertImplicit(jdeps).isEmpty()
    assertUnused(jdeps).isEmpty()
    assertIncomplete(jdeps).isEmpty()
  }

  @Test
  fun `cyclic generic type references`() {
    val dependingTarget = runJdepsCompileTask { c: KotlinJvmTestBuilder.TaskBuilder ->
      c.addSource(
        "FooAssert.kt",
        """
          package something.other
          
          import pkg.assertion.AbstractObjectAssert
          
          class FooAssert : AbstractObjectAssert<FooAssert, String>()

          fun fooAssert(): AbstractObjectAssert<*, String> = AbstractObjectAssert<FooAssert, String>()
        """
      )
      c.addDirectDependencies(TEST_FIXTURES_DEP)
    }
    val jdeps = depsProto(dependingTarget)

    assertThat(jdeps.ruleLabel).isEqualTo(dependingTarget.label())

    assertExplicit(jdeps).contains(TEST_FIXTURES_DEP.singleCompileJar())
    assertImplicit(jdeps).isEmpty()
    assertUnused(jdeps).isEmpty()
    assertIncomplete(jdeps).isEmpty()
  }

  @Test
  fun `java annotation on property is an explict dep`() {
    val dependingTarget = runJdepsCompileTask { c: KotlinJvmTestBuilder.TaskBuilder ->
      c.addSource(
        "AnotherClass.kt",
        """
          package something

          class AnotherClass {
            @JavaAnnotation
            val property = 42
          }
        """
      )
      c.addDirectDependencies(TEST_FIXTURES_DEP)
    }
    val jdeps = depsProto(dependingTarget)

    assertThat(jdeps.ruleLabel).isEqualTo(dependingTarget.label())

    assertExplicit(jdeps).contains(TEST_FIXTURES_DEP.singleCompileJar())
    assertImplicit(jdeps).isEmpty()
    assertUnused(jdeps).isEmpty()
    assertIncomplete(jdeps).isEmpty()
  }

  @Test
  fun `java annotation with field target on companion object property is an explict dep`() {
    val dependingTarget = runJdepsCompileTask { c: KotlinJvmTestBuilder.TaskBuilder ->
      c.addSource(
        "AnotherClass.kt",
        """
          package something

          import androidx.annotation.LayoutRes

          class AnotherClass {
              companion object {
                
                @JvmField @LayoutRes
                val property = 42
            }
          }
        """
      )
      c.addDirectDependencies(TEST_FIXTURES_DEP)
    }
    val jdeps = depsProto(dependingTarget)

    assertThat(jdeps.ruleLabel).isEqualTo(dependingTarget.label())

    assertExplicit(jdeps).contains(TEST_FIXTURES_DEP.singleCompileJar())
    assertImplicit(jdeps).isEmpty()
    assertUnused(jdeps).isEmpty()
    assertIncomplete(jdeps).isEmpty()
  }


  @Test
  fun `unused dependency listed`() {
    val dependentTarget = runJdepsCompileTask { c: KotlinJvmTestBuilder.TaskBuilder ->
      c.addSource(
        "AClass.kt",
        """
          package something
          
          class AClass{}
        """
      )
    }

    val dependingTarget = runJdepsCompileTask { c: KotlinJvmTestBuilder.TaskBuilder ->
      c.addSource(
        "HasNoReferenceToDep.kt",
        """
          package something
        """
      )
      c.addDirectDependencies(dependentTarget)
    }
    val jdeps = depsProto(dependingTarget)

    assertThat(jdeps.ruleLabel).isEqualTo(dependingTarget.label())

    assertExplicit(jdeps).isEmpty()
    assertImplicit(jdeps).isEmpty()
    assertUnused(jdeps).containsExactly(dependentTarget.singleCompileJar())
    assertIncomplete(jdeps).isEmpty()
  }

  @Test
  fun `unused transitive dependency not listed`() {
    val transitiveDep = runJdepsCompileTask { c: KotlinJvmTestBuilder.TaskBuilder ->
      c.addSource(
        "TransitiveClass.kt",
        """
          package something
          
          class TransitiveClass{}
        """
      )
    }

    val dependentTarget = runJdepsCompileTask { c: KotlinJvmTestBuilder.TaskBuilder ->
      c.addSource(
        "AClass.kt",
        """
          package something
          
          class AClass{}
        """
      )
      c.addDirectDependencies(transitiveDep)
    }

    val dependingTarget = runJdepsCompileTask { c: KotlinJvmTestBuilder.TaskBuilder ->
      c.addSource(
        "HasNoReferenceToDep.kt",
        """
          package something
        """
      )
      c.addDirectDependencies(dependentTarget)
    }
    val jdeps = depsProto(dependingTarget)

    assertThat(jdeps.ruleLabel).isEqualTo(dependingTarget.label())

    assertExplicit(jdeps).isEmpty()
    assertImplicit(jdeps).isEmpty()
    assertUnused(jdeps).containsExactly(dependentTarget.singleCompileJar())
    assertIncomplete(jdeps).isEmpty()
  }

  @Test
  fun `kotlin property reference`() {
    val dependentTarget = runJdepsCompileTask { c: KotlinJvmTestBuilder.TaskBuilder ->
      c.addSource(
        "AClass.kt",
        """
            package something

            class AClass{}
            """
      )
    }

    val dependingTarget = runJdepsCompileTask { c: KotlinJvmTestBuilder.TaskBuilder ->
      c.addSource(
        "HasPropertyDependency.kt",
        """
            package something
            
            val property2 =  AClass()
          """
      )
      c.addDirectDependencies(dependentTarget)
    }
    val jdeps = depsProto(dependingTarget)

    assertThat(jdeps.ruleLabel).isEqualTo(dependingTarget.label())

    assertExplicit(jdeps).containsExactly(dependentTarget.singleCompileJar())
    assertImplicit(jdeps).isEmpty()
    assertUnused(jdeps).isEmpty()
    assertIncomplete(jdeps).isEmpty()
  }

  @Test
  fun `kotlin indirect property reference on object calling helloWorld function`() {
    val transitivePropertyTarget = runCompileTask { c: KotlinJvmTestBuilder.TaskBuilder ->
      c.addSource(
        "Bar.kt",
        """
            package something

            class Bar {
              fun helloWorld() {}
            }
            """
      )
    }

    val dependentTarget = runCompileTask { c: KotlinJvmTestBuilder.TaskBuilder ->
      c.addSource(
        "Foo.kt",
        """
            package something

            class Foo {
              val bar = Bar()
            }
            """
      )
      c.addDirectDependencies(transitivePropertyTarget)
    }

    val dependingTarget = runJdepsCompileTask { c: KotlinJvmTestBuilder.TaskBuilder ->
      c.addSource(
        "HasPropertyDependency.kt",
        """
            package something
            
            fun something(foo: Foo) {
              val foo =  Foo()
              foo.bar.helloWorld()
            }
          """
      )
      c.addDirectDependencies(dependentTarget)
      c.addTransitiveDependencies(transitivePropertyTarget)
    }
    val jdeps = depsProto(dependingTarget)

    assertThat(jdeps.ruleLabel).isEqualTo(dependingTarget.label())

    assertExplicit(jdeps).contains(dependentTarget.singleCompileJar())
    assertExplicit(jdeps).contains(transitivePropertyTarget.singleCompileJar())
    assertUnused(jdeps).isEmpty()
    assertIncomplete(jdeps).isEmpty()
  }

  @Test
  fun `kotlin indirect property reference on object without calling helloWorld function`() {
    val transitivePropertyTarget = runCompileTask { c: KotlinJvmTestBuilder.TaskBuilder ->
      c.addSource(
        "Bar.kt",
        """
            package something

            class Bar {
              fun helloWorld() {}
            }
            """
      )
    }

    val dependentTarget = runCompileTask { c: KotlinJvmTestBuilder.TaskBuilder ->
      c.addSource(
        "Foo.kt",
        """
            package something

            class Foo {
              val bar = Bar()
            }
            """
      )
      c.addDirectDependencies(transitivePropertyTarget)
    }

    val dependingTarget = runJdepsCompileTask { c: KotlinJvmTestBuilder.TaskBuilder ->
      c.addSource(
        "HasPropertyDependency.kt",
        """
            package something
            
            class HasPropertyDependency {
              fun something(): Any {
                val foo = Foo()
                return foo.bar
              }
            }
          """
      )
      c.addDirectDependencies(dependentTarget)
      c.addTransitiveDependencies(transitivePropertyTarget)
    }
    val jdeps = depsProto(dependingTarget)

    assertThat(jdeps.ruleLabel).isEqualTo(dependingTarget.label())
    assertThat(jdeps.dependencyCount).isEqualTo(2)
    assertExplicit(jdeps).contains(dependentTarget.singleCompileJar())
    assertImplicit(jdeps).contains(transitivePropertyTarget.singleCompileJar())
    assertUnused(jdeps).isEmpty()
    assertIncomplete(jdeps).isEmpty()
  }

  @Test
  fun `kotlin extension property reference`() {
    val dependentTarget = runCompileTask { c: KotlinJvmTestBuilder.TaskBuilder ->
      c.addSource(
        "AClass.kt",
        """
            package something

            val String.doubleLength
                get() = length * 2
            """
      )
    }

    val dependingTarget = runJdepsCompileTask { c: KotlinJvmTestBuilder.TaskBuilder ->
      c.addSource(
        "HasPropertyDependency.kt",
        """
            package something
            
            val property2 = "Hello".doubleLength
          """
      )
      c.addDirectDependencies(dependentTarget)
    }
    val jdeps = depsProto(dependingTarget)

    assertThat(jdeps.ruleLabel).isEqualTo(dependingTarget.label())

    assertExplicit(jdeps).containsExactly(dependentTarget.singleCompileJar())
    assertImplicit(jdeps).isEmpty()
    assertUnused(jdeps).isEmpty()
    assertIncomplete(jdeps).isEmpty()
  }

  @Test
  fun `kotlin extension property reference 2`() {
    val dependentTarget = runCompileTask { c: KotlinJvmTestBuilder.TaskBuilder ->
      c.addSource(
        "AClass.kt",
        """
            package something
            
            object Extensions {
              @JvmStatic
              fun String.doubleLength() = length * 2
            }
            """
      )
    }

    val dependingTarget = runJdepsCompileTask { c: KotlinJvmTestBuilder.TaskBuilder ->
      c.addSource(
        "HasPropertyDependency.kt",
        """
            package something
            
            import something.Extensions.doubleLength
            
            val property2 = "Hello".doubleLength()
          """
      )
      c.addDirectDependencies(dependentTarget)
    }
    val jdeps = depsProto(dependingTarget)

    assertThat(jdeps.ruleLabel).isEqualTo(dependingTarget.label())

    assertExplicit(jdeps).containsExactly(dependentTarget.singleCompileJar())
    assertImplicit(jdeps).isEmpty()
    assertUnused(jdeps).isEmpty()
    assertIncomplete(jdeps).isEmpty()
  }

  @Test
  fun `kotlin extension property reference 3`() {
    val dependentTarget = runCompileTask { c: KotlinJvmTestBuilder.TaskBuilder ->
      c.addSource(
        "AClass.kt",
        """
            package something

            class AClass {
            }
            """
      )
    }

    val dependingTarget = runJdepsCompileTask { c: KotlinJvmTestBuilder.TaskBuilder ->
      c.addSource(
        "HasPropertyDependency.kt",
        """
            package something

            fun AClass?.foo(): String {
                return "foo"
            }
          """
      )
      c.addDirectDependencies(dependentTarget)
    }
    val jdeps = depsProto(dependingTarget)

    assertThat(jdeps.ruleLabel).isEqualTo(dependingTarget.label())

    assertExplicit(jdeps).contains(dependentTarget.singleCompileJar())
    assertImplicit(jdeps).isEmpty()
    assertUnused(jdeps).isEmpty()
    assertIncomplete(jdeps).isEmpty()
  }

  @Test
  fun `kotlin property definition`() {
    val dependingTarget = runJdepsCompileTask { c: KotlinJvmTestBuilder.TaskBuilder ->
      c.addSource(
        "HasPropertyDefinition.kt",
        """
            package something
            
            interface HasPropertyDefinition {

                val callFactory: JavaClass.InnerJavaClass
            }
          """
      )
      c.addDirectDependencies(TEST_FIXTURES_DEP)
    }
    val jdeps = depsProto(dependingTarget)

    assertThat(jdeps.ruleLabel).isEqualTo(dependingTarget.label())

    assertExplicit(jdeps).containsExactly(TEST_FIXTURES_DEP.singleCompileJar())
    assertImplicit(jdeps).isEmpty()
    assertUnused(jdeps).isEmpty()
    assertIncomplete(jdeps).isEmpty()
  }

  @Test
  fun `java enum reference`() {
    val dependingTarget = runJdepsCompileTask { c: KotlinJvmTestBuilder.TaskBuilder ->
      c.addSource(
        "HasPropertyDefinition.kt",
        """
            package something
            
            class Foo {

                val result = InnerJavaEnum.A_VALUE.name
            }
          """
      )
      c.addDirectDependencies(TEST_FIXTURES_DEP)
    }
    val jdeps = depsProto(dependingTarget)

    assertThat(jdeps.ruleLabel).isEqualTo(dependingTarget.label())

    assertExplicit(jdeps).containsExactly(TEST_FIXTURES_DEP.singleCompileJar())
    assertImplicit(jdeps).isEmpty()
    assertUnused(jdeps).isEmpty()
    assertIncomplete(jdeps).isEmpty()
  }

  @Test
  fun `kotlin method reference`() {
    val dependentTarget = runJdepsCompileTask { c: KotlinJvmTestBuilder.TaskBuilder ->
      c.addSource(
        "AClass.kt",
        """
            package something

            fun String.aFunction() {}
            """
      )
    }

    val dependingTarget = runJdepsCompileTask { c: KotlinJvmTestBuilder.TaskBuilder ->
      c.addSource(
        "HasFunctionDependency.kt",
        """
            package something.other
            
            import something.aFunction
            
            val functionRef = String::aFunction
          """
      )
      c.addDirectDependencies(dependentTarget)
    }
    val jdeps = depsProto(dependingTarget)

    assertThat(jdeps.ruleLabel).isEqualTo(dependingTarget.label())

    assertExplicit(jdeps).contains(dependentTarget.singleCompileJar())
  }

  @Test
  fun `kotlin generic type reference`() {
    val dependentTarget = runJdepsCompileTask { c: KotlinJvmTestBuilder.TaskBuilder ->
      c.addSource(
        "AClass.kt",
        """
            package something

            class AClass{}
            """
      )
    }

    val dependingTarget = runJdepsCompileTask { c: KotlinJvmTestBuilder.TaskBuilder ->
      c.addSource(
        "HasGenericTypeDependency.kt",
        """
            package something
            
            val property2 =  listOf<AClass>()
          """
      )
      c.addDirectDependencies(dependentTarget)
    }
    val jdeps = depsProto(dependingTarget)

    assertThat(jdeps.ruleLabel).isEqualTo(dependingTarget.label())

    assertExplicit(jdeps).contains(dependentTarget.singleCompileJar())
    assertImplicit(jdeps).isEmpty()
    assertUnused(jdeps).isEmpty()
    assertIncomplete(jdeps).isEmpty()
  }

  @Test
  fun `inlined constant dependency recorded`() {
    val dependentTarget = runJdepsCompileTask { c: KotlinJvmTestBuilder.TaskBuilder ->
      c.addSource(
        "ContainsConstant.kt",
        """
          package dependency
            
          object ConstHolder {
            const val CONSTANT_VAL = 42
          }
        """
      )
    }

    val dependingTarget = runJdepsCompileTask { c: KotlinJvmTestBuilder.TaskBuilder ->
      c.addSource(
        "HasPropertyDependency.kt",
        """
            package something
            import dependency.ConstHolder
            val property2 = ConstHolder.CONSTANT_VAL
          """
      )
      c.addDirectDependencies(dependentTarget)
    }
    val jdeps = depsProto(dependingTarget)

    assertThat(jdeps.ruleLabel).isEqualTo(dependingTarget.label())

    assertExplicit(jdeps).containsExactly(dependentTarget.singleCompileJar())
    assertImplicit(jdeps).isEmpty()
    assertUnused(jdeps).isEmpty()
    assertIncomplete(jdeps).isEmpty()
  }

  @Test
  fun `object inlined constant dependency recorded`() {
    val dependentTarget = runCompileTask { c: KotlinJvmTestBuilder.TaskBuilder ->
      c.addSource(
        "HasConstants.kt",
        """
          package dependency
          object HasConstants {
            const val CONSTANT_VAL = 42
          }
        """
      )
    }

    val dependingTarget = runJdepsCompileTask { c: KotlinJvmTestBuilder.TaskBuilder ->
      c.addSource(
        "HasPropertyDependency.kt",
        """
            package something
            import dependency.HasConstants.CONSTANT_VAL
            val property2 = CONSTANT_VAL
          """
      )
      c.addDirectDependencies(dependentTarget)
    }
    val jdeps = depsProto(dependingTarget)

    assertExplicit(jdeps).containsExactly(dependentTarget.singleCompileJar())
    assertImplicit(jdeps).isEmpty()
    assertUnused(jdeps).isEmpty()
    assertIncomplete(jdeps).isEmpty()
  }

  @Test
  fun `companion object inlined constant dependency recorded`() {
    val dependentTarget = runJdepsCompileTask { c: KotlinJvmTestBuilder.TaskBuilder ->
      c.addSource(
        "ContainsConstant.kt",
        """
          package dependency
          class HasCompanion {
            companion object {
              const val CONSTANT_VAL = 42
            }            
          }
        """
      )
    }

    val dependingTarget = runJdepsCompileTask { c: KotlinJvmTestBuilder.TaskBuilder ->
      c.addSource(
        "HasPropertyDependency.kt",
        """
            package something
            import dependency.HasCompanion.Companion.CONSTANT_VAL
            val property2 = CONSTANT_VAL
          """
      )
      c.addDirectDependencies(dependentTarget)
    }
    val jdeps = depsProto(dependingTarget)

    assertThat(jdeps.ruleLabel).isEqualTo(dependingTarget.label())

    assertExplicit(jdeps).containsExactly(dependentTarget.singleCompileJar())
    assertImplicit(jdeps).isEmpty()
    assertUnused(jdeps).isEmpty()
    assertIncomplete(jdeps).isEmpty()
  }

  @Test
  fun `constructor param inner class recorded`() {
    val dependingTarget = runJdepsCompileTask { c: KotlinJvmTestBuilder.TaskBuilder ->
      c.addSource(
        "HasConstructorDependency.kt",
        """
            package something.otherthan
            
            import something.JavaClass
            
            class HasConstructorDependency constructor(javaClass: JavaClass.InnerJavaClass) {}
          """
      )
      c.addDirectDependencies(TEST_FIXTURES_DEP)
    }
    val jdeps = depsProto(dependingTarget)

    assertThat(jdeps.ruleLabel).isEqualTo(dependingTarget.label())

    assertExplicit(jdeps).containsExactly(TEST_FIXTURES_DEP.singleCompileJar())
    assertImplicit(jdeps).isEmpty()
    assertUnused(jdeps).isEmpty()
    assertIncomplete(jdeps).isEmpty()
  }

  @Test
  fun `call function in dependency`() {
    val dependentTarget = runJdepsCompileTask { c: KotlinJvmTestBuilder.TaskBuilder ->
      c.addSource(
        "ContainsFunction.kt",
        """
          package dependency
            
          fun someFunction() = 42
        """
      )
      c.compileKotlin()
    }

    val dependingTarget = runJdepsCompileTask { c: KotlinJvmTestBuilder.TaskBuilder ->
      c.addSource(
        "HasFunctionDependency.kt",
        """
            package something
            import dependency.someFunction
            val property2 = someFunction()
          """
      )
      c.addDirectDependencies(dependentTarget)
    }
    val jdeps = depsProto(dependingTarget)

    assertThat(jdeps.ruleLabel).isEqualTo(dependingTarget.label())

    assertExplicit(jdeps).containsExactly(dependentTarget.singleCompileJar())
    assertImplicit(jdeps).isEmpty()
    assertUnused(jdeps).isEmpty()
    assertIncomplete(jdeps).isEmpty()
  }

  @Test
  fun `implement interface reference should be an explicit dependency`() {
    val indirectInterfaceDef = runJdepsCompileTask { c: KotlinJvmTestBuilder.TaskBuilder ->
      c.addSource(
        "IndirectInterface.kt",
        """
            package something

            interface IndirectInterface {
                fun doFoo()
            }
            """
      )
    }
    val directInterfaceDef = runJdepsCompileTask { c: KotlinJvmTestBuilder.TaskBuilder ->
      c.addSource(
        "DirectInterface.kt",
        """
            package something

            interface DirectInterface : IndirectInterface {
                fun doBar()
            }
            """
      )
      c.addDirectDependencies(indirectInterfaceDef)
    }

    val dependingTarget = runJdepsCompileTask { c: KotlinJvmTestBuilder.TaskBuilder ->
      c.addSource(
        "ReferencesClassWithSuperClass.kt",
        """
            package something

            interface SubInterface : DirectInterface
          """
      )
      c.addDirectDependencies(directInterfaceDef)
      c.addTransitiveDependencies(indirectInterfaceDef)
    }
    val jdeps = depsProto(dependingTarget)

    assertThat(jdeps.ruleLabel).isEqualTo(dependingTarget.label())

    assertExplicit(jdeps).containsExactly(directInterfaceDef.singleCompileJar())
    assertImplicit(jdeps).containsExactly(indirectInterfaceDef.singleCompileJar())
    assertUnused(jdeps).isEmpty()
    assertIncomplete(jdeps).isEmpty()
  }

  @Test
  fun `indirect super class reference should be an implicit dependency`() {
    val implicitSuperClassDep = runJdepsCompileTask { c: KotlinJvmTestBuilder.TaskBuilder ->
      c.addSource(
        "Base.kt",
        """
            package something

            open class Base(p: Int)
            """
      )
    }

    val explicitSuperClassDep = runJdepsCompileTask { c: KotlinJvmTestBuilder.TaskBuilder ->
      c.addSource(
        "Derived.kt",
        """
            package something

            class Derived(p: Int) : Base(p)
            """
      )
      c.addDirectDependencies(implicitSuperClassDep)
    }

    val dependingTarget = runJdepsCompileTask { c: KotlinJvmTestBuilder.TaskBuilder ->
      c.addSource(
        "ReferencesClassWithSuperClass.kt",
        """
            package something

            val classRef = Derived(42)
          """
      )
      c.addDirectDependencies(explicitSuperClassDep)
      c.addTransitiveDependencies(implicitSuperClassDep)
    }
    val jdeps = depsProto(dependingTarget)

    assertThat(jdeps.ruleLabel).isEqualTo(dependingTarget.label())

    assertExplicit(jdeps).containsExactly(explicitSuperClassDep.singleCompileJar())
    assertImplicit(jdeps).containsExactly(implicitSuperClassDep.singleCompileJar())
    assertUnused(jdeps).isEmpty()
    assertIncomplete(jdeps).isEmpty()
  }

  @Test
  fun `property reference should collect indirect super class as implicit dependency`() {
    val implicitSuperClassDep = runCompileTask { c: KotlinJvmTestBuilder.TaskBuilder ->
      c.addSource(
        "Base.kt",
        """
            package something

            open class Base(p: Int)
            """
      )
    }

    val explicitSuperClassDep = runCompileTask { c: KotlinJvmTestBuilder.TaskBuilder ->
      c.addSource(
        "Derived.kt",
        """
            package something

            object Derived : Base(41) {
              @JvmField
              val SOME_CONST = 42
            }
            """
      )
      c.addDirectDependencies(implicitSuperClassDep)
    }

    val dependingTarget = runJdepsCompileTask { c: KotlinJvmTestBuilder.TaskBuilder ->
      c.addSource(
        "ReferencesClassWithSuperClass.kt",
        """
            package something

            val classRef = Derived.SOME_CONST
          """
      )
      c.addDirectDependencies(explicitSuperClassDep)
      c.addTransitiveDependencies(implicitSuperClassDep)
    }
    val jdeps = depsProto(dependingTarget)

    assertThat(jdeps.ruleLabel).isEqualTo(dependingTarget.label())

    assertExplicit(jdeps).containsExactly(explicitSuperClassDep.singleCompileJar())
    assertImplicit(jdeps).containsExactly(implicitSuperClassDep.singleCompileJar())
    assertUnused(jdeps).isEmpty()
    assertIncomplete(jdeps).isEmpty()
  }

  @Test
  fun `function call on a class should collect the indirect super class of that class as an implicit dependency`() {
    val implicitSuperClassDep = runCompileTask { c: KotlinJvmTestBuilder.TaskBuilder ->
      c.addSource(
        "Base.kt",
        """
            package something

            open class Base
            """
      )
    }

    val explicitSuperClassDep = runCompileTask { c: KotlinJvmTestBuilder.TaskBuilder ->
      c.addSource(
        "Derived.kt",
        """
            package something

            class Derived : Base() {
              fun hi(): String {
                return "Hello"
              }
            }
            """
      )
      c.addDirectDependencies(implicitSuperClassDep)
    }

    val dependingTarget = runJdepsCompileTask { c: KotlinJvmTestBuilder.TaskBuilder ->
      c.addSource(
        "ReferencesClassWithSuperClass.kt",
        """
            package something

            class ReferencesClassWithSuperClass {
                fun stuff(): String {
                    return Derived().hi()
                }
            }
          """
      )
      c.addDirectDependencies(explicitSuperClassDep)
      c.addTransitiveDependencies(implicitSuperClassDep)
    }
    val jdeps = depsProto(dependingTarget)

    assertThat(jdeps.ruleLabel).isEqualTo(dependingTarget.label())

    assertExplicit(jdeps).containsExactly(explicitSuperClassDep.singleCompileJar())
    assertImplicit(jdeps).containsExactly(implicitSuperClassDep.singleCompileJar())
    assertUnused(jdeps).isEmpty()
    assertIncomplete(jdeps).isEmpty()
  }

  @Test
  fun `creating a kotlin class should collect the indirect java super class, with a kotlin type param class, of that class as an implicit dependency`() {
    val implicitSuperClassGenericTypeParamDep = runCompileTask { c: KotlinJvmTestBuilder.TaskBuilder ->
      c.addSource(
          "BaseGenericType.kt",
          """
            package something

            class BaseGenericType
            """
      )
    }

    val explicitClassWithTypeParamJavaSuperclassDep = runCompileTask { c: KotlinJvmTestBuilder.TaskBuilder ->
      c.addSource(
          "Derived.kt",
          """
            package something
            import something.JavaBaseWithTypeParam

            class Derived : JavaBaseWithTypeParam<BaseGenericType>() {
              fun hi(): String {
                return "Hello"
              }
            }
            """
      )
      c.addDirectDependencies(TEST_FIXTURES_DEP)
      c.addDirectDependencies(implicitSuperClassGenericTypeParamDep)
    }

    val dependingTarget = runJdepsCompileTask { c: KotlinJvmTestBuilder.TaskBuilder ->
      c.addSource(
          "ReferencesClassWithSuperClass.kt",
          """
            package something

            class ReferencesClassWithSuperClass {
                fun stuff(): String {
                    val derived = Derived()
                    return derived.toString()
                }
            }
          """
      )
      c.addDirectDependencies(explicitClassWithTypeParamJavaSuperclassDep)
      c.addTransitiveDependencies(TEST_FIXTURES_DEP)
      c.addTransitiveDependencies(implicitSuperClassGenericTypeParamDep)
    }
    val jdeps = depsProto(dependingTarget)

    assertThat(jdeps.ruleLabel).isEqualTo(dependingTarget.label())
    assertExplicit(jdeps).containsExactly(explicitClassWithTypeParamJavaSuperclassDep.singleCompileJar())
    assertImplicit(jdeps).contains(TEST_FIXTURES_DEP.singleCompileJar())
    assertImplicit(jdeps).contains(implicitSuperClassGenericTypeParamDep.singleCompileJar())
    assertUnused(jdeps).isEmpty()
    assertIncomplete(jdeps).isEmpty()
  }

  @Test
  fun `class declaration all super class references should be an implicit dependency`() {
    val implicitSuperClassDep = runCompileTask { c: KotlinJvmTestBuilder.TaskBuilder ->
      c.addSource(
        "Base.kt",
        """
            package something

            open class Base
            """
      )
    }

    val explicitSuperClassDep = runCompileTask { c: KotlinJvmTestBuilder.TaskBuilder ->
      c.addSource(
        "Derived.kt",
        """
            package something

            open class Derived : Base()
            """
      )
      c.addSource(
        "Derived2.kt",
        """
            package something

            open class Derived2 : Derived()
            """
      )
      c.addDirectDependencies(implicitSuperClassDep)
    }

    val dependingTarget = runJdepsCompileTask { c: KotlinJvmTestBuilder.TaskBuilder ->
      c.addSource(
        "DependingClass.kt",
        """
            package something

            abstract class DependingClass : Derived2()
          """
      )
      c.addDirectDependencies(explicitSuperClassDep)
      c.addTransitiveDependencies(implicitSuperClassDep)
    }
    val jdeps = depsProto(dependingTarget)

    assertExplicit(jdeps).containsExactly(explicitSuperClassDep.singleCompileJar())
    assertImplicit(jdeps).containsExactly(implicitSuperClassDep.singleCompileJar())
    assertUnused(jdeps).isEmpty()
    assertIncomplete(jdeps).isEmpty()
  }

  @Test
  fun `generic type as constructor parameter`() {
    val implicitSuperClassDep = runJdepsCompileTask { c: KotlinJvmTestBuilder.TaskBuilder ->
      c.addSource(
        "Base.kt",
        """
            package something.base

            open class Base(p: Int)
            """
      )
    }

    val explicitSuperClassDep = runJdepsCompileTask { c: KotlinJvmTestBuilder.TaskBuilder ->
      c.addSource(
        "Derived.kt",
        """
            package something.derived
            
            import something.base.Base

            class Derived(p: Int) : Base(p)
            """
      )
      c.addDirectDependencies(implicitSuperClassDep)
    }

    val dependingTarget = runJdepsCompileTask { c: KotlinJvmTestBuilder.TaskBuilder ->
      c.addSource(
        "ReferencesGenericTypeWithSuperClass.kt",
        """
            package something.reference
            
            import something.derived.Derived
            import something.AnotherClass

            internal class HasConstructorDependency constructor(genericRef: AnotherClass<Derived>) {}
          """
      )
      c.addDirectDependencies(TEST_FIXTURES_DEP)
      c.addDirectDependencies(explicitSuperClassDep)
      c.addTransitiveDependencies(implicitSuperClassDep)
    }
    val jdeps = depsProto(dependingTarget)

    assertThat(jdeps.ruleLabel).isEqualTo(dependingTarget.label())

    assertExplicit(jdeps).contains(TEST_FIXTURES_DEP.singleCompileJar())
    assertExplicit(jdeps).contains(explicitSuperClassDep.singleCompileJar())
    assertImplicit(jdeps).contains(implicitSuperClassDep.singleCompileJar())
    assertUnused(jdeps).isEmpty()
    assertIncomplete(jdeps).isEmpty()
  }

  @Test
  fun `generic type as lazy property`() {
    val implicitSuperClassDep = runJdepsCompileTask { c: KotlinJvmTestBuilder.TaskBuilder ->
      c.addSource(
        "Base.kt",
        """
            package something.base

            open class Base(p: Int)
            """
      )
    }

    val explicitSuperClassDep = runJdepsCompileTask { c: KotlinJvmTestBuilder.TaskBuilder ->
      c.addSource(
        "Derived.kt",
        """
            package something.derived
            
            import something.base.Base

            class Derived(p: Int) : Base(p)
            """
      )
      c.addDirectDependencies(implicitSuperClassDep)
    }

    val dependingTarget = runJdepsCompileTask { c: KotlinJvmTestBuilder.TaskBuilder ->
      c.addSource(
        "AnotherClass.java",
        """
          package something.reference;
          
          class AnotherClass<T> {
          }
        """
      )
      c.addSource(
        "ReferencesGenericTypeWithSuperClass.kt",
        """
            package something.reference
            
            import something.derived.Derived

            private val lazyProperty by lazy { AnotherClass<Derived>() }
          """
      )
      c.addDirectDependencies(explicitSuperClassDep)
      c.addTransitiveDependencies(implicitSuperClassDep)
    }
    val jdeps = depsProto(dependingTarget)

    assertThat(jdeps.ruleLabel).isEqualTo(dependingTarget.label())

    assertExplicit(jdeps).contains(explicitSuperClassDep.singleCompileJar())
    assertImplicit(jdeps).contains(implicitSuperClassDep.singleCompileJar())
    assertUnused(jdeps).isEmpty()
    assertIncomplete(jdeps).isEmpty()
  }

  @Test
  fun `function call return type`() {
    val depWithReturnTypesSuperType = runJdepsCompileTask { c: KotlinJvmTestBuilder.TaskBuilder ->
      c.addSource(
        "SomeSuperType.kt",
        """
            package something

            open class SomeSuperType
            """
      )
      c.setLabel("depWithReturnType")
    }
    val depWithReturnType = runJdepsCompileTask { c: KotlinJvmTestBuilder.TaskBuilder ->
      c.addSource(
        "SomeType.kt",
        """
            package something

            class SomeType : SomeSuperType() {
            }
            """
      )
      c.setLabel("depWithReturnType")
      c.addDirectDependencies(depWithReturnTypesSuperType)
    }

    val depWithFunction = runCompileTask { c: KotlinJvmTestBuilder.TaskBuilder ->
      c.addSource(
        "ContainsFunction.kt",
        """
            package something

            fun returnSomeType() = SomeType()
            """
      )
      c.addDirectDependencies(depWithReturnType)
      c.setLabel("depWithFunction")
    }

    val dependingTarget = runJdepsCompileTask { c: KotlinJvmTestBuilder.TaskBuilder ->
      c.addSource(
        "ReferencesClassWithSuperClass.kt",
        """
            package something

            fun foo() {
              returnSomeType()
            }
          """
      )
      c.addDirectDependencies(depWithFunction)
      c.addTransitiveDependencies(depWithReturnType)
      c.addTransitiveDependencies(depWithReturnTypesSuperType)
      c.setLabel("dependingTarget")
    }
    val jdeps = depsProto(dependingTarget)

    assertThat(jdeps.ruleLabel).isEqualTo(dependingTarget.label())

    assertExplicit(jdeps).contains(depWithFunction.singleCompileJar())
    assertImplicit(jdeps).contains(depWithReturnType.singleCompileJar())
    assertImplicit(jdeps).doesNotContain(depWithReturnTypesSuperType)
  }

  @Test
  fun `function call receiver type`() {
    val depWithReceiverTypeSuperType = runJdepsCompileTask { c: KotlinJvmTestBuilder.TaskBuilder ->
      c.addSource(
        "SomeSuperType.kt",
        """
            package something

            open class SomeSuperType {}
            """
      )
      c.setLabel("depWithSuperType")
    }
    val depWithReceiverType = runJdepsCompileTask { c: KotlinJvmTestBuilder.TaskBuilder ->
      c.addSource(
        "SomeType.kt",
        """
            package something

            class SomeType : SomeSuperType() {}
            """
      )
      c.setLabel("depWithReceiverType")
      c.addDirectDependencies(depWithReceiverTypeSuperType)
    }

    val depWithFunction = runCompileTask { c: KotlinJvmTestBuilder.TaskBuilder ->
      c.addSource(
        "ContainsFunction.kt",
        """
            package something

            fun receiverSomeType(arg: SomeType.() -> Unit) {}
            """
      )
      c.addDirectDependencies(depWithReceiverType)
      c.addTransitiveDependencies(depWithReceiverTypeSuperType)
      c.setLabel("depWithFunction")
    }

    val dependingTarget = runJdepsCompileTask { c: KotlinJvmTestBuilder.TaskBuilder ->
      c.addSource(
        "CallsFunctionWithReceiver.kt",
        """
            package something

            fun foo() {
              receiverSomeType {
              }
            }
          """
      )
      c.addDirectDependencies(depWithFunction)
      c.addTransitiveDependencies(depWithReceiverType, depWithReceiverTypeSuperType)
      c.setLabel("dependingTarget")
    }
    val jdeps = depsProto(dependingTarget)

    assertThat(jdeps.ruleLabel).isEqualTo(dependingTarget.label())

    assertExplicit(jdeps).contains(depWithFunction.singleCompileJar())
    assertImplicit(jdeps).contains(depWithReceiverType.singleCompileJar())
    assertImplicit(jdeps).contains(depWithReceiverTypeSuperType.singleCompileJar())
  }

  @Test
  fun `constructor parameters are required implicit dependencies`() {
    val fooDep = runCompileTask { c: KotlinJvmTestBuilder.TaskBuilder ->
      c.addSource(
        "FooClass.kt",
        """
          package something

          class FooClass
          """
      )
    }
    val barDep = runCompileTask { c: KotlinJvmTestBuilder.TaskBuilder ->
      c.addSource(
        "BarClass.kt",
        """
          package something

          class BarClass(private val foo: FooClass = FooClass()) { }
          """
      )
      c.addDirectDependencies(fooDep)
    }

    val dependingTarget = runJdepsCompileTask { c: KotlinJvmTestBuilder.TaskBuilder ->
      c.addSource(
        "ReferencesClassWithSuperClass.kt",
        """
          package something

          class Dummy {
            val result = BarClass()
          }
        """
      )
      c.addDirectDependencies(barDep)
      c.addTransitiveDependencies(fooDep)
    }
    val jdeps = depsProto(dependingTarget)

    assertExplicit(jdeps).contains(barDep.singleCompileJar())
    assertImplicit(jdeps).contains(fooDep.singleCompileJar())
  }

  @Test
  fun `function call return type type parameter should not be a dependency`() {
    val depWithTypeParameter = runCompileTask { c: KotlinJvmTestBuilder.TaskBuilder ->
      c.addSource(
        "SomeType.kt",
        """
            package something

            class SomeType {
              val booleanValue = true
            }
            """
      )
      c.setLabel("depWithReturnType")
    }

    val depWithFunction = runCompileTask { c: KotlinJvmTestBuilder.TaskBuilder ->
      c.addSource(
        "ContainsFunction.kt",
        """
            package something

            fun returnSomeType() = setOf<SomeType>()
            """
      )
      c.addDirectDependencies(depWithTypeParameter)
      c.setLabel("depWithFunction")
    }

    val dependingTarget = runJdepsCompileTask { c: KotlinJvmTestBuilder.TaskBuilder ->
      c.addSource(
        "ReferencesClassWithSuperClass.kt",
        """
            package something

            fun foo() {
              returnSomeType()
            }
          """
      )
      c.addDirectDependencies(depWithFunction)
      c.addTransitiveDependencies(depWithTypeParameter)
      c.setLabel("dependingTarget")
    }
    val jdeps = depsProto(dependingTarget)

    assertThat(jdeps.ruleLabel).isEqualTo(dependingTarget.label())

    assertExplicit(jdeps).contains(depWithFunction.singleCompileJar())
    assertExplicit(jdeps).doesNotContain(depWithTypeParameter.singleCompileJar())
    assertImplicit(jdeps).doesNotContain(depWithTypeParameter.singleCompileJar())
  }

  @Test
  fun `function call parameter type nested type parameters should be an explicit dependency`() {

    val foo = runCompileTask { c: KotlinJvmTestBuilder.TaskBuilder ->
      c.addSource(
        "Foo.kt",
        """
            package something

            class Foo { }
            """
      )
    }

    val bar = runCompileTask { c: KotlinJvmTestBuilder.TaskBuilder ->
      c.addSource(
        "Bar.kt",
        """
            package something

            class Bar<T> {
              val booleanValue = true
            }
            """
      )
    }

    val dependingTarget = runJdepsCompileTask { c: KotlinJvmTestBuilder.TaskBuilder ->
      c.addSource(
        "FunctionWithTypeParams.kt",
        """
            package something

            fun foo(param: Set<Bar<Foo>>) {

            }
          """
      )
      c.addDirectDependencies(foo, bar)
    }
    val jdeps = depsProto(dependingTarget)

    assertExplicit(jdeps).contains(bar.singleCompileJar())
    assertExplicit(jdeps).contains(foo.singleCompileJar())
  }

  @Test
  fun `assignment from function call`() {
    val depWithReturnTypesSuperType = runCompileTask { c: KotlinJvmTestBuilder.TaskBuilder ->
      c.addSource(
        "SomeSuperType.kt",
        """
            package something

            open class SomeSuperType
            """
      )
      c.setLabel("depWithReturnType")
    }
    val depWithReturnType = runCompileTask { c: KotlinJvmTestBuilder.TaskBuilder ->
      c.addSource(
        "SomeType.kt",
        """
            package something

            class SomeType : SomeSuperType() {
              val stringValue = "Hello World"
            }
            """
      )
      c.setLabel("depWithReturnType")
      c.addDirectDependencies(depWithReturnTypesSuperType)
    }

    val depWithFunction = runCompileTask { c: KotlinJvmTestBuilder.TaskBuilder ->
      c.addSource(
        "ContainsFunction.kt",
        """
            package something

            fun returnSomeType() = SomeType()
            """
      )
      c.addDirectDependencies(depWithReturnType)
      c.setLabel("depWithFunction")
    }

    val dependingTarget = runJdepsCompileTask { c: KotlinJvmTestBuilder.TaskBuilder ->
      c.addSource(
        "ReferencesClassWithSuperClass.kt",
        """
            package something

            fun foo() {
              val assignment = returnSomeType()
              print(assignment.stringValue)
            }
          """
      )
      c.addDirectDependencies(depWithFunction)
      c.addTransitiveDependencies(depWithReturnType, depWithReturnTypesSuperType)
      c.setLabel("dependingTarget")
    }
    val jdeps = depsProto(dependingTarget)

    assertThat(jdeps.ruleLabel).isEqualTo(dependingTarget.label())

    assertExplicit(jdeps).containsAtLeast(depWithFunction.singleCompileJar(), depWithReturnType.singleCompileJar())
    assertImplicit(jdeps).doesNotContain(depWithReturnType.singleCompileJar())
    assertImplicit(jdeps).doesNotContain(depWithReturnTypesSuperType)
  }

  private fun depsProto(jdeps: Dep) =
    Deps.Dependencies.parseFrom(BufferedInputStream(Files.newInputStream(Paths.get(jdeps.jdeps()!!))))

  private fun assertExplicit(jdeps: Deps.Dependencies) = assertThat(
    jdeps.dependencyList.filter { it.kind == Deps.Dependency.Kind.EXPLICIT }.map { it.path }
  )

  private fun assertImplicit(jdeps: Deps.Dependencies) = assertThat(
    jdeps.dependencyList.filter { it.kind == Deps.Dependency.Kind.IMPLICIT }.map { it.path }
  )

  private fun assertUnused(jdeps: Deps.Dependencies) = assertThat(
    jdeps.dependencyList.filter { it.kind == Deps.Dependency.Kind.UNUSED }.map { it.path }
  )

  private fun assertIncomplete(jdeps: Deps.Dependencies) = assertThat(
    jdeps.dependencyList.filter { it.kind == Deps.Dependency.Kind.INCOMPLETE }.map { it.path }
  )

  private fun runCompileTask(block: (c: KotlinJvmTestBuilder.TaskBuilder) -> Unit): Dep {
    return ctx.runCompileTask(Consumer { c: KotlinJvmTestBuilder.TaskBuilder ->
      if (enableK2Compiler) {
        c.useK2()
      }
      block(c.outputJar().compileKotlin())
    })
  }
  private fun runJdepsCompileTask(block: (c: KotlinJvmTestBuilder.TaskBuilder) -> Unit): Dep {
    return runCompileTask { c -> block(c.outputJdeps()) }
  }
}
