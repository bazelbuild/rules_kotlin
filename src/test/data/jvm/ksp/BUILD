load("//kotlin:core.bzl", "kt_ksp_plugin")
load("//kotlin:jvm.bzl", "kt_jvm_library")

package(default_visibility = ["//visibility:private"])

kt_ksp_plugin(
    name = "autoservice",
    processor_class = "dev.zacsweers.autoservice.ksp.AutoServiceSymbolProcessor$Provider",
    deps = [
        "@kotlin_rules_maven//:dev_zacsweers_autoservice_auto_service_ksp",
    ],
)

kt_ksp_plugin(
    name = "moshi",
    processor_class = "com.squareup.moshi.kotlin.codegen.ksp.JsonClassSymbolProcessorProvider",
    deps = [
        "@kotlin_rules_maven//:com_squareup_moshi_moshi",
        "@kotlin_rules_maven//:com_squareup_moshi_moshi_kotlin",
        "@kotlin_rules_maven//:com_squareup_moshi_moshi_kotlin_codegen",
    ],
)

kt_jvm_library(
    name = "moshi_lib",
    srcs = ["CoffeeAppModel.kt"],
    plugins = [":moshi"],
    deps = [
        "@kotlin_rules_maven//:com_squareup_moshi_moshi",
        "@kotlin_rules_maven//:com_squareup_moshi_moshi_kotlin",
        "@kotlin_rules_maven//:com_squareup_moshi_moshi_kotlin_codegen",
    ],
)

kt_jvm_library(
    name = "coffee_lib",
    srcs = ["CoffeeAppService.java"],
    plugins = [":autoservice"],
    deps = [
        "@kotlin_rules_maven//:dev_zacsweers_autoservice_auto_service_ksp",
    ],
)

filegroup(
    name = "ksp",
    srcs = [
        ":coffee_lib.jar",
        ":moshi_lib.jar",
    ],
    visibility = ["//visibility:public"],
)
