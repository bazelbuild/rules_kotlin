# Do not edit. bazel-deps autogenerates this file from dependencies.yaml.
def _jar_artifact_impl(ctx):
    jar_name = "%s.jar" % ctx.name
    ctx.download(
        output=ctx.path("jar/%s" % jar_name),
        url=ctx.attr.urls,
        sha256=ctx.attr.sha256,
        executable=False
    )
    src_name="%s-sources.jar" % ctx.name
    srcjar_attr=""
    has_sources = len(ctx.attr.src_urls) != 0
    if has_sources:
        ctx.download(
            output=ctx.path("jar/%s" % src_name),
            url=ctx.attr.src_urls,
            sha256=ctx.attr.src_sha256,
            executable=False
        )
        srcjar_attr ='\n    srcjar = ":%s",' % src_name

    build_file_contents = """
package(default_visibility = ['//visibility:public'])
java_import(
    name = 'jar',
    tags = ['maven_coordinates={artifact}'],
    jars = ['{jar_name}'],{srcjar_attr}
)
filegroup(
    name = 'file',
    srcs = [
        '{jar_name}',
        '{src_name}'
    ],
    visibility = ['//visibility:public']
)\n""".format(artifact = ctx.attr.artifact, jar_name = jar_name, src_name = src_name, srcjar_attr = srcjar_attr)
    ctx.file(ctx.path("jar/BUILD"), build_file_contents, False)
    return None

jar_artifact = repository_rule(
    attrs = {
        "artifact": attr.string(mandatory = True),
        "sha256": attr.string(mandatory = True),
        "urls": attr.string_list(mandatory = True),
        "src_sha256": attr.string(mandatory = False, default=""),
        "src_urls": attr.string_list(mandatory = False, default=[]),
    },
    implementation = _jar_artifact_impl
)

def jar_artifact_callback(hash):
    src_urls = []
    src_sha256 = ""
    source=hash.get("source", None)
    if source != None:
        src_urls = [source["url"]]
        src_sha256 = source["sha256"]
    jar_artifact(
        artifact = hash["artifact"],
        name = hash["name"],
        urls = [hash["url"]],
        sha256 = hash["sha256"],
        src_urls = src_urls,
        src_sha256 = src_sha256
    )
    native.bind(name = hash["bind"], actual = hash["actual"])


def list_dependencies():
    return [
    {"artifact": "junit:junit:4.12", "lang": "java", "sha1": "2973d150c0dc1fefe998f834810d68f278ea58ec", "sha256": "59721f0805e223d84b90677887d9ff567dc534d7c502ca903c0c2b17f05c116a", "repository": "https://repo.maven.apache.org/maven2/", "url": "https://repo.maven.apache.org/maven2/junit/junit/4.12/junit-4.12.jar", "source": {"sha1": "a6c32b40bf3d76eca54e3c601e5d1470c86fcdfa", "sha256": "9f43fea92033ad82bcad2ae44cec5c82abc9d6ee4b095cab921d11ead98bf2ff", "repository": "https://repo.maven.apache.org/maven2/", "url": "https://repo.maven.apache.org/maven2/junit/junit/4.12/junit-4.12-sources.jar"} , "name": "junit_junit", "actual": "@junit_junit//jar", "bind": "jar/junit/junit"},
    {"artifact": "org.hamcrest:hamcrest-core:1.3", "lang": "java", "sha1": "42a25dc3219429f0e5d060061f71acb49bf010a0", "sha256": "66fdef91e9739348df7a096aa384a5685f4e875584cce89386a7a47251c4d8e9", "repository": "https://repo.maven.apache.org/maven2/", "url": "https://repo.maven.apache.org/maven2/org/hamcrest/hamcrest-core/1.3/hamcrest-core-1.3.jar", "source": {"sha1": "1dc37250fbc78e23a65a67fbbaf71d2e9cbc3c0b", "sha256": "e223d2d8fbafd66057a8848cc94222d63c3cedd652cc48eddc0ab5c39c0f84df", "repository": "https://repo.maven.apache.org/maven2/", "url": "https://repo.maven.apache.org/maven2/org/hamcrest/hamcrest-core/1.3/hamcrest-core-1.3-sources.jar"} , "name": "org_hamcrest_hamcrest_core", "actual": "@org_hamcrest_hamcrest_core//jar", "bind": "jar/org/hamcrest/hamcrest_core"},
    {"artifact": "org.jetbrains.kotlin:kotlin-stdlib:1.2.70", "lang": "kotlin", "sha1": "b5b9449f73ce7bf312e89a7560cb3c209a0fa13e", "sha256": "7d20d0a56dd0ea6176137759a6aad331bbfae67436b45e5f0a4d8dafb6985c81", "repository": "https://repo.maven.apache.org/maven2/", "url": "https://repo.maven.apache.org/maven2/org/jetbrains/kotlin/kotlin-stdlib/1.2.70/kotlin-stdlib-1.2.70.jar", "source": {"sha1": "645811a161689d7b2d4362eaee1e30abf22a27d7", "sha256": "63c4e703f0b427c9f081a1877926c171283d07c17e9077b3493c068082c0bf82", "repository": "https://repo.maven.apache.org/maven2/", "url": "https://repo.maven.apache.org/maven2/org/jetbrains/kotlin/kotlin-stdlib/1.2.70/kotlin-stdlib-1.2.70-sources.jar"} , "name": "org_jetbrains_kotlin_kotlin_stdlib", "actual": "@org_jetbrains_kotlin_kotlin_stdlib//jar:file", "bind": "jar/org/jetbrains/kotlin/kotlin_stdlib"},
    {"artifact": "org.jetbrains.kotlinx:atomicfu-common:0.11.10", "lang": "kotlin", "sha1": "b77c47045af8f0eb4628b7ffa3b179b333f94baf", "sha256": "68392cc1ca9d193336d0cb695e8be41af432260a71a408e6ef8497bd1a10a126", "repository": "https://repo.maven.apache.org/maven2/", "url": "https://repo.maven.apache.org/maven2/org/jetbrains/kotlinx/atomicfu-common/0.11.10/atomicfu-common-0.11.10.jar", "source": {"sha1": "db08a73440e0a619deecbbb256b8ebc8036592fb", "sha256": "4c91fbb6f0adeea54e3117d2b5d0f7cfed5b23d10aa968dd6b67ef4989037078", "repository": "https://repo.maven.apache.org/maven2/", "url": "https://repo.maven.apache.org/maven2/org/jetbrains/kotlinx/atomicfu-common/0.11.10/atomicfu-common-0.11.10-sources.jar"} , "name": "org_jetbrains_kotlinx_atomicfu_common", "actual": "@org_jetbrains_kotlinx_atomicfu_common//jar:file", "bind": "jar/org/jetbrains/kotlinx/atomicfu_common"},
    {"artifact": "org.jetbrains.kotlinx:kotlinx-coroutines-core-common:0.30.1", "lang": "kotlin", "sha1": "e5aca39b26ff9ed62d76232167607a9e6e041990", "sha256": "e2d4ee0350a4be8e58afa95f7331138aa736cd603b5908d57761295eb1f4b5a5", "repository": "https://repo.maven.apache.org/maven2/", "url": "https://repo.maven.apache.org/maven2/org/jetbrains/kotlinx/kotlinx-coroutines-core-common/0.30.1/kotlinx-coroutines-core-common-0.30.1.jar", "source": {"sha1": "bf5dd5fe6d1c12344eff11479867cbd85b1b90eb", "sha256": "29b305432bedd1614f894de6daffb5fc40e2146686a81f3a79ddd731256a371a", "repository": "https://repo.maven.apache.org/maven2/", "url": "https://repo.maven.apache.org/maven2/org/jetbrains/kotlinx/kotlinx-coroutines-core-common/0.30.1/kotlinx-coroutines-core-common-0.30.1-sources.jar"} , "name": "org_jetbrains_kotlinx_kotlinx_coroutines_core_common", "actual": "@org_jetbrains_kotlinx_kotlinx_coroutines_core_common//jar:file", "bind": "jar/org/jetbrains/kotlinx/kotlinx_coroutines_core_common"},
    {"artifact": "org.jetbrains.kotlinx:kotlinx-coroutines-core:0.30.1", "lang": "kotlin", "sha1": "78779c75434b7c6638daea4179703d8727ea02d8", "sha256": "9d1f5a72f29bd0e53c03ad9c5deee76a9279caa8bf2876a8d6d863105541f302", "repository": "https://repo.maven.apache.org/maven2/", "url": "https://repo.maven.apache.org/maven2/org/jetbrains/kotlinx/kotlinx-coroutines-core/0.30.1/kotlinx-coroutines-core-0.30.1.jar", "source": {"sha1": "6998fee8385e5d33e1b4b729f6633bdbf1b42d6a", "sha256": "5c24e95f552bfb7aa29ea1b8fb0cf5faffbfe353e1afae841a4dcb1a57e36b8a", "repository": "https://repo.maven.apache.org/maven2/", "url": "https://repo.maven.apache.org/maven2/org/jetbrains/kotlinx/kotlinx-coroutines-core/0.30.1/kotlinx-coroutines-core-0.30.1-sources.jar"} , "name": "org_jetbrains_kotlinx_kotlinx_coroutines_core", "actual": "@org_jetbrains_kotlinx_kotlinx_coroutines_core//jar:file", "bind": "jar/org/jetbrains/kotlinx/kotlinx_coroutines_core"},
    {"artifact": "org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:0.30.1", "lang": "kotlin", "sha1": "4e30010a05ac8cb08400dc648600cc0945a1ed1a", "sha256": "5eb48dedaf6a0258a368e0c5648191b4a68634d3ee8dedb93b3031f3ea09228b", "repository": "https://repo.maven.apache.org/maven2/", "url": "https://repo.maven.apache.org/maven2/org/jetbrains/kotlinx/kotlinx-coroutines-jdk8/0.30.1/kotlinx-coroutines-jdk8-0.30.1.jar", "source": {"sha1": "3bba44187873cb7b71eacaf7d76479a816f2aee3", "sha256": "c0b4ee9907e6aa91038798c20180ea107b0a2215b9170eb7b21459c49eec683e", "repository": "https://repo.maven.apache.org/maven2/", "url": "https://repo.maven.apache.org/maven2/org/jetbrains/kotlinx/kotlinx-coroutines-jdk8/0.30.1/kotlinx-coroutines-jdk8-0.30.1-sources.jar"} , "name": "org_jetbrains_kotlinx_kotlinx_coroutines_jdk8", "actual": "@org_jetbrains_kotlinx_kotlinx_coroutines_jdk8//jar:file", "bind": "jar/org/jetbrains/kotlinx/kotlinx_coroutines_jdk8"},
    {"artifact": "org.jetbrains:annotations:13.0", "lang": "java", "sha1": "919f0dfe192fb4e063e7dacadee7f8bb9a2672a9", "sha256": "ace2a10dc8e2d5fd34925ecac03e4988b2c0f851650c94b8cef49ba1bd111478", "repository": "https://repo.maven.apache.org/maven2/", "url": "https://repo.maven.apache.org/maven2/org/jetbrains/annotations/13.0/annotations-13.0.jar", "source": {"sha1": "5991ca87ef1fb5544943d9abc5a9a37583fabe03", "sha256": "42a5e144b8e81d50d6913d1007b695e62e614705268d8cf9f13dbdc478c2c68e", "repository": "https://repo.maven.apache.org/maven2/", "url": "https://repo.maven.apache.org/maven2/org/jetbrains/annotations/13.0/annotations-13.0-sources.jar"} , "name": "org_jetbrains_annotations", "actual": "@org_jetbrains_annotations//jar", "bind": "jar/org/jetbrains/annotations"},
    ]

def maven_dependencies(callback = jar_artifact_callback):
    for hash in list_dependencies():
        callback(hash)
