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
      tally: the dict to merge into
      dep: the dep to collect jars from.
      target: label of the target being processed.
      id: one of (self, exports)
    """
    if JavaInfo not in dep:
        return
    for jar in dep[JavaInfo].compile_jars:
        _merge_label_into_dict(tally, jar, [target.label])
    for jar in dep[JavaInfo].transitive_runtime_jars:
        _merge_label_into_dict(tally, jar, [target.label])

def _kt_jvm_jars_to_labels_aspect_impl(target, ctx):
    tally={}
    # Merge the targets dependencies, use the label of the target itself.
    _merge_from_java_info(tally, target, target, "self")

    for exp in getattr(ctx.rule.attr, "exports", []):
        # Merge the targets exports, use the label of the target itself.
        _merge_from_java_info(tally, exp, target, "exports")
        # Pull up step.
        _merge_jars_to_labels(tally, ctx.rule.attr.exports, target, "exports")

#    for rt in getattr(ctx.rule.attr, "runtime_deps", []):
#        _merge_from_java_info(tally, rt, target, "rt")
#        _merge_jars_to_labels(tally, ctx.rule.attr.runtime_deps, target, "exports")

    # Pull up step.
    _merge_jars_to_labels(tally, getattr(ctx.rule.attr, "deps", []), target, "deps")

    return [kt.info.JarsToLabelsInfo(jars_to_labels=tally)]

_kt_jvm_jars_to_labels_aspect = aspect(
    attr_aspects = ["deps","runtime_deps", "exports"],
    provides = [kt.info.JarsToLabelsInfo],
    implementation = _kt_jvm_jars_to_labels_aspect_impl,
    doc = "Gathers a dict of jars to a depset of owning labels."
)

def _add_labels_to_category(jars_to_labels, cat, jars, id):
    """
    Args:
        id: one of (indirect(ct), indirect(rt), direct(ct))
    """
    for jar in jars:
        if jar not in jars_to_labels:
            fail("missing label for jar: " + jar.path + " category: " + id)
        else:
            cat[jar] = jars_to_labels[jar].to_list()

def _jars_to_labels_from(ctx, deps, runtime_deps, implicit_jars):
    j_to_l={}
    _merge_jars_to_labels(j_to_l, deps, ctx.label, "analyze(deps)")
    _merge_jars_to_labels(j_to_l, runtime_deps, ctx.label, "analyze(rt_deps)")
    for ij in implicit_jars:
        j_to_l[ij] = depset([ij.owner])
    return j_to_l

def _analyze_deps(ctx, deps, runtime_deps, implicit_jars=[]):
    """Given a list of deps constructs the classpath, direct and indirect dependency maps. Selection of the label is deferred to the caller.
    """
    classpath = depset()
    direct_dependencies={}
    indirect_dependencies={}

    j_to_l=_jars_to_labels_from(ctx, deps, runtime_deps, implicit_jars)

    for dep in deps:
        info=dep[JavaInfo]
        classpath += info.transitive_compile_time_jars
        _add_labels_to_category(j_to_l, direct_dependencies, info.compile_jars, "direct(ct)")
        _add_labels_to_category(j_to_l, indirect_dependencies, info.transitive_compile_time_jars, "indirect(ct)")
        _add_labels_to_category(j_to_l, indirect_dependencies, info.transitive_runtime_jars, "indirect(ct)")

    for rt_dep in runtime_deps:
        info=rt_dep[JavaInfo]
        classpath += info.transitive_runtime_jars
        _add_labels_to_category(j_to_l, indirect_dependencies, info.transitive_compile_time_jars, "indirect(rt)")
        _add_labels_to_category(j_to_l, indirect_dependencies, info.transitive_runtime_jars, "indirect(rt)")

    # remove direct dependencies as they are doubled up.
    for k in direct_dependencies.keys() + implicit_jars:
        indirect_dependencies.pop(k, None)

    return struct(
        classpath=classpath + implicit_jars,
        direct_dependencies=direct_dependencies,
        indirect_dependencies=indirect_dependencies
    )

analysis = struct(
    analyze_deps = _analyze_deps,
    jars_to_labels_aspect = _kt_jvm_jars_to_labels_aspect,
)