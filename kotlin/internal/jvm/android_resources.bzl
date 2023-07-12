load(
    "@rules_android//rules/android_local_test:resources.bzl",
    _PROCESSORS = "PROCESSORS",
)

MANIFEST_PROCESSOR = "ManifestProcessor"  # this is not really needed but using it to simplify integration
RESOURCE_PROCESSOR = "ResourceProcessor"

def process_resources_for_android_local_test(ctx, java_package):
    manifest_ctx = _PROCESSORS[MANIFEST_PROCESSOR](ctx).value
    return _PROCESSORS[RESOURCE_PROCESSOR](ctx, manifest_ctx, java_package).value
