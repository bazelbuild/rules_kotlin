load(
    "@com_github_jetbrains_kotlin//:providers.bzl",
    _KspPluginInfo = "KspPluginInfo",
    _KtCompilerPluginInfo = "KtCompilerPluginInfo",
    _KtJsInfo = "KtJsInfo",
    _KtJvmInfo = "KtJvmInfo",
)

KtJvmInfo = _KtJvmInfo
KtJsInfo = _KtJsInfo
KtCompilerPluginInfo = _KtCompilerPluginInfo
KspPluginInfo = _KspPluginInfo
