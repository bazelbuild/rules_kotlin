load(
    "//kotlin/internal:defs.bzl",
    _TOOLCHAIN_TYPE = "TOOLCHAIN_TYPE",
)

def get_transitive_prerequisites(ctx, provider, attributes = ["deps", "exports"]):
    """Collects the transitive dependencies from passed attributes.

    Args:
      ctx: The context.
      provider: the provider to collect from the attributes.
      attributes: the list of attributes from context.

    Returns:
        A list of all the collected providers
    """
    result = []
    for attribute in attributes:
        for target in getattr(ctx.attr, attribute):
            if provider in target:
                result.append(target[provider])
    return result

def get_java_toolchain(ctx):
    if not hasattr(ctx.attr, "_java_toolchain"):
        fail("Missing _java_toolchain attr")
    return ctx.attr._java_toolchain

def get_host_javabase(ctx):
    if not hasattr(ctx.attr, "_host_javabase"):
        fail("Missing _host_javabase attr")
    return ctx.attr._host_javabase

def get_android_toolchain(ctx):
    return ctx.toolchains["@io_bazel_rules_android//toolchains/android:toolchain_type"]

def get_android_sdk(ctx):
    if getattr(ctx.fragments.android, "incompatible_use_toolchain_resolution", False):
        return ctx.toolchains["@io_bazel_rules_android//toolchains/android_sdk:toolchain_type"].android_sdk_info
    else:
        return ctx.attr.android_sdk[AndroidSdkInfo]

def _only(collection):
    """Returns the only item in the collection."""
    if len(collection) != 1:
        fail("Expected one element, has %s." % len(collection))
    return _first(collection)

def _first(collection):
    """Returns the first item in the collection."""
    for i in collection:
        return i
    return fail("The collection is empty.")

def _list_or_depset_to_list(list_or_depset):
    if type(list_or_depset) == "list":
        return list_or_depset
    elif type(list_or_depset) == "depset":
        return list_or_depset.to_list()
    else:
        return fail("Expected a list or a depset. Got %s" % type(list_or_depset))

utils = struct(
    only = _only,
    first = _first,
    list_or_depset_to_list = _list_or_depset_to_list,
)

