""" Analysis aspects and utils."""
load("//kotlin/internal:kt.bzl", "kt")

def _merge_label_into_dict(tally, jar, labels):
    if jar not in tally:
        tally[jar] = depset(labels)
    else:
        # note 2: This form (should) ensure that the most specific label of a dep is always at [0] -- most specific being the closest dep provider to the target
        # under analysis. This function is during aspect processing and analysis. The desired behaviour might not be entirely accurate during analysis -- need
        # to do some experimentation. Regardless, selecting the most appropriate label is not the responsibility of this function.
        tally[jar] = depset(direct=labels, transitive=[tally[jar]])

def _merge_jars_to_labels(tally, deps, target, id):
    """
    Args:
      target: label of the target requesting the merge.
      id: one of (exports, deps, analyze)
    """
    for dep in deps:
        for e in dep[kt.info.JarsToLabelsInfo].jars_to_labels.items():
            _merge_label_into_dict(tally, e[0], e[1].to_list())

def _merge_from_java_info(tally, dep, target, id):
    """
    Args:
      dep: the dep to collect jars from.
      target: label of the target being processed.
      id: one of (self, exports)
    """
    java=dep[JavaInfo]
    if not java:
        return
    for jar in java.compile_jars:
        _merge_label_into_dict(tally, jar, [target.label])

def _kt_jvm_jars_to_labels_aspect_impl(target, ctx):
    tally={}
    _merge_from_java_info(tally, target, target, "self")
    for exp in getattr(ctx.rule.attr, "exports", []):
        _merge_from_java_info(tally, exp, target, "exports")
        _merge_jars_to_labels(tally, ctx.rule.attr.exports, target, "exports")
    _merge_jars_to_labels(tally, getattr(ctx.rule.attr, "deps", []), target, "deps")
    return [kt.info.JarsToLabelsInfo(jars_to_labels=tally)]

_kt_jvm_jars_to_labels_aspect = aspect(
    attr_aspects = ["deps", "exports"],
    provides = [kt.info.JarsToLabelsInfo],
    implementation = _kt_jvm_jars_to_labels_aspect_impl,
    doc = "Gathers a dict of jars to a depset of owning labels."
)

def _add_labels_to_category(jars_to_labels, cat, jars, id):
    """
    Args:
        id: one of (indirect_dependency, direct_dependency)
    """
    for jar in jars:
        if jar not in jars_to_labels:
            fail("missing label for jar: " + jar + "category: " + id)
        else:
            cat[jar] = jars_to_labels[jar].to_list()

def _jars_to_labels_from(ctx, deps, implicit_jars):
    jars_to_labels={}
    _merge_jars_to_labels(jars_to_labels, deps, ctx.label, "analyze")
    for ij in implicit_jars:
        jars_to_labels[ij] = depset([ij.owner])
    return jars_to_labels

def _analyze_deps(ctx, deps, implicit_jars=[]):
    """Given a list of deps constructs the classpath, direct and indirect dependency maps. Selection of the label is deferred to the caller.
    """
    compile_jars = depset()
    direct_dependencies={}
    indirect_dependencies={}
    jars_to_labels=_jars_to_labels_from(ctx, deps, implicit_jars)

    for dep in deps:
        compile_jars += dep[JavaInfo].transitive_compile_time_jars
        _add_labels_to_category(jars_to_labels, direct_dependencies, dep[JavaInfo].compile_jars, "direct_dependency")
        _add_labels_to_category(jars_to_labels, indirect_dependencies, dep[JavaInfo].transitive_compile_time_jars, "indirect_dependency")

    # remove direct dependencies as they are doubled up.
    for k in direct_dependencies:
        indirect_dependencies.pop(k, None)

    # remove impplicit deps from indirect dependencies.
    for k in implicit_jars:
        indirect_dependencies.pop(k, None)

    return struct(
        classpath=compile_jars + implicit_jars,
        direct_dependencies=direct_dependencies,
        indirect_dependencies=indirect_dependencies
    )

analysis = struct(
    analyze_deps = _analyze_deps,
    jars_to_labels_aspect = _kt_jvm_jars_to_labels_aspect,
)