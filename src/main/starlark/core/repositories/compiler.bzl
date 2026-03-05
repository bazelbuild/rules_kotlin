"""Legacy compatibility shim for historical Kotlin compiler repository APIs.

rules_kotlin no longer provisions compiler distributions from repository rules.
Compiler jars are resolved via maven coordinates in //kotlin/compiler aliases.
"""

def kotlin_compiler_repository(name, urls, sha256, compiler_version):
    _ = (name, urls, sha256, compiler_version)
