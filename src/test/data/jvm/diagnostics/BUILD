package(default_visibility = ["//visibility:private"])
load("//kotlin:kotlin.bzl", "kt_jvm_binary", "kt_jvm_library", "kt_jvm_test")

kt_jvm_library(
    name = "test_info_file",
    srcs = ["InfoFile.kt"],
)

kt_jvm_library(
    name = "test_warning_and_info_file",
    srcs = ["WarningAndInfoFile.kt"],
)

filegroup(
    name = "diagnostics",
    srcs = [
        ":test_info_file.diagnosticsproto",
        ":test_warning_and_info_file.diagnosticsproto",
    ],
    visibility = ["//visibility:public"],
)