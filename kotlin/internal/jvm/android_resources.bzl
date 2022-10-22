load(
     ":utils.bzl",
     "get_transitive_prerequisites",
     "get_android_sdk",
     "get_android_toolchain",
     "get_host_javabase",
     "get_java_toolchain",
     "utils")
load("@io_bazel_rules_android//rules:resources.bzl", _rules_android_resources = "resources")
load("@io_bazel_rules_android//rules/android_local_test:resources.bzl", _rules_android_local_test_processors = "PROCESSORS_FOR_ANDROID_LOCAL_TEST")

TRISTATE_AUTO = -1
TRISTATE_YES = 1

MANIFEST_PROCESSOR = "ManifestProcessor" # this is not really needed but using it to simplify integration
RESOURCE_PROCESSOR = "ResourceProcessor"

# Executes the starlark rules_android resource processing pipeline
def process_resources(ctx, java_package):

    """Collects the transitive dependencies from passed attributes.

    Args:
      ctx: The context.
      java_package: the java package of the target

    Returns:
      A resource_ctx dict of resources_providers.
    """
    # exports_manifest can be overridden by a bazel flag.
    if ctx.attr.exports_manifest == TRISTATE_AUTO:
        exports_manifest = ctx.fragments.android.get_exports_manifest_default
    else:
        exports_manifest = ctx.attr.exports_manifest == TRISTATE_YES

    return _rules_android_resources.process(
        ctx,
        manifest = ctx.file.manifest,
        resource_files = ctx.attr.resource_files,
        assets = ctx.attr.assets,
        assets_dir = ctx.attr.assets_dir,
        exports_manifest = exports_manifest,
        java_package = java_package,
        custom_package = ctx.attr.custom_package,
        neverlink = ctx.attr.neverlink,
        enable_data_binding = False, #ctx.attr.enable_data_binding,
        deps = ctx.attr.deps,
        exports = ctx.attr.exports,

        # Processing behavior changing flags.
        enable_res_v3 = False, #_flags.get(ctx).android_enable_res_v3,
        # TODO(b/144163743): remove fix_resource_transitivity, which was only added to emulate
        # misbehavior on the Java side.
        fix_resource_transitivity = bool(ctx.attr.srcs),
        fix_export_exporting = True, #acls.in_fix_export_exporting_rollout(str(ctx.label)),
        propagate_resources = True, # not ctx.attr._android_test_migration,

        # Tool and Processing related inputs
        aapt = get_android_toolchain(ctx).aapt2.files_to_run,
        android_jar = get_android_sdk(ctx).android_jar,
        android_kit = get_android_toolchain(ctx).android_kit.files_to_run,
        busybox = get_android_toolchain(ctx).android_resources_busybox.files_to_run,
        java_toolchain = get_java_toolchain(ctx),
        host_javabase = get_host_javabase(ctx),
        instrument_xslt = False, #utils.only(get_android_toolchain(ctx).add_g3itr_xslt.files.to_list()),
        res_v3_dummy_manifest = utils.only(
            get_android_toolchain(ctx).res_v3_dummy_manifest.files.to_list(),
        ),
        res_v3_dummy_r_txt = utils.only(
            get_android_toolchain(ctx).res_v3_dummy_r_txt.files.to_list(),
        ),
        xsltproc = None, #get_android_toolchain(ctx).xsltproc_tool.files_to_run,
        zip_tool = get_android_toolchain(ctx).zip_tool.files_to_run,
    )

def process_resources_for_android_local_test(ctx, java_package):
    manifest_ctx = _rules_android_local_test_processors[MANIFEST_PROCESSOR](ctx).value
    return _rules_android_local_test_processors[RESOURCE_PROCESSOR](ctx, manifest_ctx, java_package).value

