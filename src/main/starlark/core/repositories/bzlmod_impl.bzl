def configure_modules_and_repositories(modules, kotlin_repositories, kotlinc_version):
    kotlinc = None
    for mod in modules:
        for override in mod.tags.kotlinc_version:
            if kotlinc:
                fail("Only one kotlinc_version is supported right now!")
            kotlinc = kotlinc_version(release = override.version, sha256 = override.sha256)

    kotlin_repositories_args = dict(is_bzlmod = True)
    if kotlinc:
        kotlin_repositories_args["compiler_version"] = kotlinc.version

    kotlin_repositories(**kotlin_repositories_args)

_version_tag = tag_class(
    attrs = {
        "sha256": attr.string(mandatory = True),
        "version": attr.string(mandatory = True),
    },
)

tag_classes = {
    "kotlinc_version": _version_tag,
}
