package io.bazel.kotlin.integration

import com.google.common.truth.Truth.assertWithMessage
import io.bazel.kotlin.integration.RulesKotlinWorkspace.Companion.build
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.junit.Test
import java.nio.file.Files.copy
import java.nio.file.Files.createTempDirectory
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.zip.GZIPInputStream
import kotlin.io.path.createDirectories

class KspCompileTest {

  private fun unpackReleaseArchive(): Path {
    return RulesKotlinWorkspace::class.java.classLoader.getResourceAsStream("_release.tgz")
      ?.let { stream ->
        createTempDirectory("rules_kotlin_release-")
          .also { tmp ->
            TarArchiveInputStream(GZIPInputStream(stream)).use { archive ->
              generateSequence(archive::getNextEntry).forEach { entry ->
                if (!entry.isDirectory) {
                  copy(
                    archive,
                    tmp.resolve(entry.name).createDirectories(),
                    StandardCopyOption.REPLACE_EXISTING,
                  )
                }
              }
            }
          }
      }
      ?: error("Cannot find release repo")
  }

  @Test
  fun `test ksp compile`() {
    val workspace = WriteWorkspace.using<WriteWorkspace> {
      module {
        "bazel_dep"(
          "name" `=` "rules_kotlin",
        )
        "local_path_override"(
          "module_name" `=` "rules_kotlin",
          "path" `=` unpackReleaseArchive().toString(),
        )
        "bazel_dep"("name" `=` "bazel_skylib", "version" `=` "1.4.2")
        "bazel_dep"("name" `=` "rules_java", "version" `=` "7.4.0")
        "bazel_dep"("name" `=` "rules_jvm_external", "version" `=` "5.3")

        "maven = use_extension"("@rules_jvm_external//:extensions.bzl", "maven")
        "maven.install"(
          "name" `=` "maven",
          "artifacts".list(
            "com.squareup.moshi:moshi:1.14.0",
            "com.squareup.moshi:moshi-kotlin:1.14.0",
            "com.squareup.moshi:moshi-kotlin-codegen:1.14.0",
            "com.google.auto.service:auto-service-annotations:jar:1.1.1",
            "com.google.auto.value:auto-value:1.10.1",
            "com.google.auto.value:auto-value-annotations:1.10.1",
            "dev.zacsweers.autoservice:auto-service-ksp:jar:1.1.0",
          ),
          "repositories".list(
            "https://maven.google.com",
            "https://repo1.maven.org/maven2",
          ),
        )
        "use_repo"(!"maven", "maven")
      }

      build {
        load("@rules_kotlin//kotlin:core.bzl", "define_kt_toolchain")
        "define_kt_toolchain"("name" `=` "kotlin_toolchain")
      }

      "coffee" {
        kotlin("CoffeeAppModel.kt") {
          +"""
            package coffee

            import com.squareup.moshi.JsonClass

            @JsonClass(generateAdapter = true)
            data class CoffeeAppModel(val id: String)
          """.trimIndent()
        }


        java("CoffeeAppJavaModel.java") {
          +"""
          package coffee;

          import com.google.auto.value.AutoValue;
          
          @AutoValue
          public abstract class CoffeeAppJavaModel {
          
              abstract CoffeeAppModel coffeeAppModel();
          
              Builder builder() {
                  return new AutoValue_CoffeeAppJavaModel.Builder();
              }
          
              @AutoValue.Builder
              abstract static class Builder {
          
                  abstract Builder setCoffeeAppModel(CoffeeAppModel coffeeAppModel);
          
                  abstract CoffeeAppJavaModel build();
              }
          }
          """.trimIndent()
        }
        java("CoffeeAppService.java") {
          `package`("coffee")
          +"""
            import com.google.auto.service.AutoService;

            @AutoService(Object.class)
            public class CoffeeAppService {
            }
          """.trimIndent()
        }

        kotlin("CoffeeApp.kt") {
          +"""
            /*
             * Copyright 2018 The Bazel Authors. All rights reserved.
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
            package coffee

            import com.squareup.moshi.Moshi

            class CoffeeApp {

              companion object {

                private val adapter = CoffeeAppModelJsonAdapter(Moshi.Builder().build())
                private val d = AutoValue_CoffeeAppJavaModel.Builder()
                  .setCoffeeAppModel(CoffeeAppModel("1"))
                  .build()

                @JvmStatic
                fun main(args: Array<String>) {
                  println(
                    adapter.toJson(d.coffeeAppModel())
                  )
                }
              }
            }
          """.trimIndent()
        }

        build {
          +"""
            load("@bazel_skylib//rules:build_test.bzl", "build_test")
            load("@rules_java//java:defs.bzl", "java_binary", "java_plugin")
            load("@rules_kotlin//kotlin:core.bzl", "kt_compiler_plugin", "kt_ksp_plugin", "kt_plugin_cfg")
            load("@rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")
            
            package(default_visibility = ["//visibility:public"])
            
            java_plugin(
                name = "autovalue",
                generates_api = True,
                processor_class = "com.google.auto.value.processor.AutoValueProcessor",
                deps = ["@maven//:com_google_auto_value_auto_value"],
            )
            
            kt_ksp_plugin(
                name = "moshi-kotlin-codegen",
                processor_class = "com.squareup.moshi.kotlin.codegen.ksp.JsonClassSymbolProcessorProvider",
                deps = [
                    "@maven//:com_squareup_moshi_moshi",
                    "@maven//:com_squareup_moshi_moshi_kotlin",
                    "@maven//:com_squareup_moshi_moshi_kotlin_codegen",
                ],
            )
            
            kt_ksp_plugin(
                name = "autoservice",
                processor_class = "dev.zacsweers.autoservice.ksp.AutoServiceSymbolProcessor${"$"}Provider",
                deps = [
                    "@maven//:com_google_auto_service_auto_service_annotations",
                    "@maven//:dev_zacsweers_autoservice_auto_service_ksp",
                ],
            )
            
            kt_jvm_library(
                name = "coffee_lib",
                srcs = glob([
                    "*.kt",
                    "*.java",
                ]),
                plugins = [
                    ":moshi-kotlin-codegen",
                    ":autovalue",
                    ":autoservice",
                ],
                deps = [
                    "@maven//:com_google_auto_service_auto_service_annotations",
                    "@maven//:com_google_auto_value_auto_value_annotations",
                    "@maven//:com_squareup_moshi_moshi",
                    "@maven//:com_squareup_moshi_moshi_kotlin",
                ],
            )
            
            java_binary(
                name = "coffee_app",
                main_class = "coffee.CoffeeApp",
                visibility = ["//visibility:public"],
                runtime_deps = [":coffee_lib"],
            )
            
            build_test(
                name = "force_build_app_test",
                targets = [
                    ":coffee_app",
                    # build_test doesn't actually fail unless you force the deploy jar to be built
                    ":coffee_app_deploy.jar",
                ],
            )
          """.trimIndent()
        }
      }
    }
    workspace.build("//coffee:force_build_app_test").run {
      assertWithMessage("\noutput:\n $out\nerr:\n $err").that(exit).isEqualTo(0)
    }
  }
}
