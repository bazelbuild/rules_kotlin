def _kotlin_compiler_impl(repository_ctx):
    """Creates the kotlinc repository."""
    attr = repository_ctx.attr
    repository_ctx.download_and_extract(
        attr.urls,
        sha256 = attr.sha256,
        stripPrefix = "kotlinc",
    )
    repository_ctx.file(
        "WORKSPACE",
        content = """workspace(name = "%s")""" % attr.name,
    )
    repository_ctx.file(
        "compiler_shade.jarjar",
        content = """rule dagger.** io.bazel.kotlin.builder.dagger.@1
rule com.intellij.** org.jetbrains.kotlin.com.intellij.@1
rule com.google.** org.jetbrains.kotlin.com.google.@1
rule com.sampullara.** org.jetbrains.kotlin.com.sampullara.@1
rule org.apache.** org.jetbrains.kotlin.org.apache.@1
rule org.jdom.** org.jetbrains.kotlin.org.jdom.@1
rule org.picocontainer.** org.jetbrains.kotlin.org.picocontainer.@1
rule org.jline.** org.jetbrains.kotlin.org.jline.@1
rule org.fusesource.** org.jetbrains.kotlin.org.fusesource.@1
rule net.jpountz.** org.jetbrains.kotlin.net.jpountz.@1
rule one.util.streamex.** org.jetbrains.kotlin.one.util.streamex.@1
rule it.unimi.dsi.fastutil.** org.jetbrains.kotlin.it.unimi.dsi.fastutil.@1
rule kotlinx.collections.immutable.** org.jetbrains.kotlin.kotlinx.collections.immutable.@1"""
    )
    repository_ctx.template(
        "BUILD.bazel",
        attr._template,
        substitutions = {
            "{{.KotlinRulesRepository}}": attr.kotlin_rules,
        },
        executable = False,
    )

kotlin_compiler_repository = repository_rule(
    implementation = _kotlin_compiler_impl,
    attrs = {
        "urls": attr.string_list(
            doc = "A list of urls for the kotlin compiler",
            mandatory = True,
        ),
        "kotlin_rules": attr.string(
            doc = "target of the kotlin rules.",
            mandatory = True,
        ),
        "sha256": attr.string(
            doc = "sha256 of the compiler archive",
        ),
        "_template": attr.label(
            doc = "repository build file template",
            default = ":BUILD.com_github_jetbrains_kotlin.bazel",
        ),
    },
)
