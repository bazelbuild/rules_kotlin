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

package io.bazel.kotlin.builder.utils

import com.google.common.truth.Truth.assertThat
import io.bazel.kotlin.builder.utils.Arguments
import org.junit.Test
import java.nio.file.Files
import java.nio.file.StandardOpenOption

class ArgumentsTest {

  private val tmp by lazy {
    Files.createTempDirectory(javaClass.canonicalName)
  }

  class FlagsForest(a: Arguments) {
    val little by a.flag("little", "frolicking animal", default = "rabbit")

    val surname by a.flag("surname", "surname", emptyList<String>()) {
      split(",")
    }

    val bops by a.flag("bop", "head bop count", 0) { last ->
      RuntimeException().printStackTrace()
      toInt().plus(last)
    }

    val fairy by a.flag<Any>("fairy", "parole officer") {
      object {}
    }
  }
  @Test
  fun flags() {
    Arguments(
      "--little", "bunny",
      "--surname", "foo,foo,foo,foo",
      "--bop", "1",
      "--bop", "1",
      "--bop", "1",
      "--bop", "1"
    ).parseInto(::FlagsForest).then { status ->
      status.ifError {
        assertThat(errs).isEmpty()
      }
      apply {
        assertThat(little).isEqualTo("bunny")
        assertThat(surname).containsExactly("foo", "foo", "foo", "foo")
        assertThat(bops).isEqualTo(4)
        assertThat(fairy).isNull()
      }
    }
  }

  class CustomFlagForest(a: Arguments) {
    val locomotion by a.custom.flag(
      "loco",
      "moving through the forest",
      "wiggle"
    ) {
      asSequence().joinToString(",")
    }

    val mammal by a.flag(
      "mammal",
      "",
      default = "worm"
    )
  }


  @Test
  fun customFlag() {
    Arguments("--loco", "hop", "hop", "--mammal", "bunny").parseInto(::CustomFlagForest).then { status ->
      status.ifError {
        assertThat(errs).isEmpty()
      }
      apply {
        assertThat(locomotion).isEqualTo("hop,hop")
        assertThat(mammal).isEqualTo("bunny")
      }
    }
  }

  class RequiredForest(a: Arguments) {
    val fairy by a.flag<Any>("fairy", "mice loving (required)", "no fairy", true) {
      object {}
    }
  }
  @Test
  fun required() {

    assertThat(Arguments().parseInto(::RequiredForest).then { status ->
      status.ifError {
        assertThat(errs).containsExactly("--fairy is required")
        assertThat(help()).isEqualTo(
          """
                      Flags:
                        --fairy: mice loving (required)
                      """.trimIndent()
        )
      }
    }).isNull()
  }

  class ExpandForest(a: Arguments) {
    val fairy by a.flag("fairy", "peacekeeper", default = "anarchy")

    val bopper by a.flag("bopper", "miscreant", default = "big")
  }

  @Test
  fun expand() {

    val params = Files.write(
      tmp.resolve("forest.params"),
      listOf("--bopper", "foo foo"),
      StandardOpenOption.CREATE_NEW
    )
    Arguments(
      "--fairy", "authoritarian", "@$params"
    ).parseInto(::ExpandForest).then { status ->
      status.ifError {
        assertThat(errs).isEmpty()
      }
      apply {
        assertThat(fairy).isEqualTo("authoritarian")
        assertThat(bopper).isEqualTo("foo foo")
      }
    }
  }
}
