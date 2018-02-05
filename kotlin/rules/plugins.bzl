load("//kotlin/rules:defs.bzl", _KotlinPluginInfo="KotlinPluginInfo")

def _mk_processor_entry(p):
    return struct(
          processor_class=p.processor_class,
          classpath=[cp.path for cp in java_common.merge([j[JavaInfo] for j in p.deps]).full_compile_jars],
          generates_api=p.generates_api,
    )

def _kt_jvm_plugin_aspect_impl(target, ctx):
    return [_KotlinPluginInfo(
        processor = _mk_processor_entry(ctx.rule.attr)
    )]

kt_jvm_plugin_aspect = aspect(
    implementation = _kt_jvm_plugin_aspect_impl,
    attr_aspects = ["plugins"]
)
"""renders a java info into a KotlinPluginInfo."""