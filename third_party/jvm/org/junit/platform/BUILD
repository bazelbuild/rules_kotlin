licenses(["notice"])
java_library(
    name = "junit_platform_commons",
    exports = [
        "//external:jar/io_bazel_rules_kotlin_org/junit/platform/junit_platform_commons"
    ],
    runtime_deps = [
        "//third_party/jvm/org/apiguardian:apiguardian_api"
    ],
    visibility = [
        "//visibility:public"
    ]
)



java_library(
    name = "junit_platform_console",
    exports = [
        "//external:jar/io_bazel_rules_kotlin_org/junit/platform/junit_platform_console"
    ],
    runtime_deps = [
        "//third_party/jvm/org/apiguardian:apiguardian_api",
        ":junit_platform_launcher"
    ],
    visibility = [
        "//visibility:public"
    ]
)



java_library(
    name = "junit_platform_engine",
    exports = [
        "//external:jar/io_bazel_rules_kotlin_org/junit/platform/junit_platform_engine"
    ],
    runtime_deps = [
        "//third_party/jvm/org/apiguardian:apiguardian_api",
        "//third_party/jvm/org/opentest4j:opentest4j",
        ":junit_platform_commons"
    ],
    visibility = [
        "//visibility:public"
    ]
)



java_library(
    name = "junit_platform_launcher",
    exports = [
        "//external:jar/io_bazel_rules_kotlin_org/junit/platform/junit_platform_launcher"
    ],
    runtime_deps = [
        "//third_party/jvm/org/apiguardian:apiguardian_api",
        ":junit_platform_engine"
    ],
    visibility = [
        "//visibility:public"
    ]
)



java_library(
    name = "junit_platform_runner",
    exports = [
        "//external:jar/io_bazel_rules_kotlin_org/junit/platform/junit_platform_runner"
    ],
    runtime_deps = [
        "//third_party/jvm/junit:junit",
        "//third_party/jvm/org/apiguardian:apiguardian_api",
        ":junit_platform_launcher",
        ":junit_platform_suite_api"
    ],
    visibility = [
        "//visibility:public"
    ]
)



java_library(
    name = "junit_platform_suite_api",
    exports = [
        "//external:jar/io_bazel_rules_kotlin_org/junit/platform/junit_platform_suite_api"
    ],
    runtime_deps = [
        "//third_party/jvm/org/apiguardian:apiguardian_api",
        ":junit_platform_commons"
    ],
    visibility = [
        "//visibility:public"
    ]
)


