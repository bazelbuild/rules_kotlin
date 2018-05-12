licenses(["notice"])
java_library(
    name = "asm",
    exports = [
        "//external:jar/io_bazel_rules_kotlin_org/ow2/asm/asm"
    ],
    visibility = [
        "//visibility:public"
    ]
)



java_library(
    name = "asm_commons",
    exports = [
        "//external:jar/io_bazel_rules_kotlin_org/ow2/asm/asm_commons"
    ],
    runtime_deps = [
        ":asm_tree"
    ],
    visibility = [
        "//visibility:public"
    ]
)



java_library(
    name = "asm_tree",
    exports = [
        "//external:jar/io_bazel_rules_kotlin_org/ow2/asm/asm_tree"
    ],
    runtime_deps = [
        ":asm"
    ],
    visibility = [
        "//visibility:public"
    ]
)


