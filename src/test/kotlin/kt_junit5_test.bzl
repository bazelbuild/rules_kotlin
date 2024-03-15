load("//kotlin:jvm.bzl", _kt_jvm_test = "kt_jvm_test")

def kt_jvm_test(name, args = [], deps = [], runtime_deps = [], **kwargs):
    test_package = native.package_name().replace("src/test/kotlin", "").replace("/", ".")

    _kt_jvm_test(
        name = name,
        use_testrunner = False,
        args = args + ["--select-package", test_package],
        main_class = "org.junit.platform.console.ConsoleLauncher",
        deps = deps + [
            "@kotlin_rules_maven//:org_junit_jupiter_junit_jupiter_api",
            "@kotlin_rules_maven//:org_junit_jupiter_junit_jupiter_params",
            "@kotlin_rules_maven//:org_junit_jupiter_junit_jupiter_engine",
        ],
        runtime_deps = runtime_deps + [
            "@kotlin_rules_maven//:org_junit_platform_junit_platform_console",
        ],
        **kwargs
    )
