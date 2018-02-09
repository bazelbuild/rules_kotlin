load(
    "//kotlin/rules:defs.bzl",
    _KotlinPluginInfo = "KotlinPluginInfo",
)

def _mk_processor_entry(l,p):
    return struct(
          label=l,
          processor_class=p.processor_class,
          classpath=[cp.path for cp in java_common.merge([j[JavaInfo] for j in p.deps]).full_compile_jars],
          generates_api=p.generates_api,
    )

def merge_plugin_infos(attrs):
    tally={}
    processors=[]
    for info in [a[_KotlinPluginInfo] for a in attrs]:
        for p in info.processors:
            if p.label not in tally:
                tally[p.label] = True
                processors.append(p)
    return _KotlinPluginInfo(processors=processors)

def _restore_label(l):
    lbl = l.workspace_root
    if lbl.startswith("external/"):
        lbl = lbl.replace("external/", "@")
    return lbl + "//" + l.package + ":" + l.name

_EMPTY_PLUGIN_INFO = [_KotlinPluginInfo(processors = [])]

def _kt_jvm_plugin_aspect_impl(target, ctx):
    if ctx.rule.kind == "java_plugin":
        return [_KotlinPluginInfo(
            processors = [_mk_processor_entry(_restore_label(ctx.label),ctx.rule.attr)]
        )]
    else:
      if ctx.rule.kind == "java_library":
          return [merge_plugin_infos(ctx.rule.attr.exported_plugins)]
      else:
          return _EMPTY_PLUGIN_INFO

kt_jvm_plugin_aspect = aspect(
    attr_aspects = [
        "plugins",
        "exported_plugins",
    ],
    implementation = _kt_jvm_plugin_aspect_impl,
)

"""renders a java info into a KotlinPluginInfo."""
