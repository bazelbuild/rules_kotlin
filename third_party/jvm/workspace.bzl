# Do not edit. bazel-deps autogenerates this file from third_party/dependencies.yaml.

def declare_maven(hash):
    native.maven_jar(
        name = hash["name"],
        artifact = hash["artifact"],
        sha1 = hash["sha1"],
        repository = hash["repository"]
    )
    native.bind(
        name = hash["bind"],
        actual = hash["actual"]
    )

def list_dependencies():
    return [
    {"artifact": "com.google.auto.service:auto-service:1.0-rc4", "lang": "java", "sha1": "44954d465f3b9065388bbd2fc08a3eb8fd07917c", "repository": "https://repo.maven.apache.org/maven2/", "name": "io_bazel_rules_kotlin_com_google_auto_service_auto_service", "actual": "@io_bazel_rules_kotlin_com_google_auto_service_auto_service//jar", "bind": "jar/io_bazel_rules_kotlin_com/google/auto/service/auto_service"},
    {"artifact": "com.google.auto.value:auto-value:1.5.3", "lang": "java", "sha1": "514df6a7c7938de35c7f68dc8b8f22df86037f38", "repository": "https://repo.maven.apache.org/maven2/", "name": "io_bazel_rules_kotlin_com_google_auto_value_auto_value", "actual": "@io_bazel_rules_kotlin_com_google_auto_value_auto_value//jar", "bind": "jar/io_bazel_rules_kotlin_com/google/auto/value/auto_value"},
    {"artifact": "com.google.auto:auto-common:0.8", "lang": "java", "sha1": "c6f7af0e57b9d69d81b05434ef9f3c5610d498c4", "repository": "https://repo.maven.apache.org/maven2/", "name": "io_bazel_rules_kotlin_com_google_auto_auto_common", "actual": "@io_bazel_rules_kotlin_com_google_auto_auto_common//jar", "bind": "jar/io_bazel_rules_kotlin_com/google/auto/auto_common"},
    {"artifact": "com.google.code.findbugs:jsr305:3.0.2", "lang": "java", "sha1": "25ea2e8b0c338a877313bd4672d3fe056ea78f0d", "repository": "https://repo.maven.apache.org/maven2/", "name": "io_bazel_rules_kotlin_com_google_code_findbugs_jsr305", "actual": "@io_bazel_rules_kotlin_com_google_code_findbugs_jsr305//jar", "bind": "jar/io_bazel_rules_kotlin_com/google/code/findbugs/jsr305"},
    {"artifact": "com.google.dagger:dagger-compiler:2.9", "lang": "java", "sha1": "276b9c7acc73acb449e1837a47623a7b94afd90b", "repository": "https://repo.maven.apache.org/maven2/", "name": "io_bazel_rules_kotlin_com_google_dagger_dagger_compiler", "actual": "@io_bazel_rules_kotlin_com_google_dagger_dagger_compiler//jar", "bind": "jar/io_bazel_rules_kotlin_com/google/dagger/dagger_compiler"},
    {"artifact": "com.google.dagger:dagger-producers:2.9", "lang": "java", "sha1": "159090ee31e1752408dedda6fa92ede36a312834", "repository": "https://repo.maven.apache.org/maven2/", "name": "io_bazel_rules_kotlin_com_google_dagger_dagger_producers", "actual": "@io_bazel_rules_kotlin_com_google_dagger_dagger_producers//jar", "bind": "jar/io_bazel_rules_kotlin_com/google/dagger/dagger_producers"},
    {"artifact": "com.google.dagger:dagger:2.9", "lang": "java", "sha1": "75a739e59d7ede2f7f425f369955bdd1c2ac122b", "repository": "https://repo.maven.apache.org/maven2/", "name": "io_bazel_rules_kotlin_com_google_dagger_dagger", "actual": "@io_bazel_rules_kotlin_com_google_dagger_dagger//jar", "bind": "jar/io_bazel_rules_kotlin_com/google/dagger/dagger"},
# duplicates in com.google.errorprone:error_prone_annotations promoted to 2.1.3
# - com.google.guava:guava:23.5-jre wanted version 2.0.18
# - com.google.truth:truth:0.39 wanted version 2.1.3
    {"artifact": "com.google.errorprone:error_prone_annotations:2.1.3", "lang": "java", "sha1": "39b109f2cd352b2d71b52a3b5a1a9850e1dc304b", "repository": "https://repo.maven.apache.org/maven2/", "name": "io_bazel_rules_kotlin_com_google_errorprone_error_prone_annotations", "actual": "@io_bazel_rules_kotlin_com_google_errorprone_error_prone_annotations//jar", "bind": "jar/io_bazel_rules_kotlin_com/google/errorprone/error_prone_annotations"},
# duplicates in com.google.guava:guava promoted to 23.5-jre
# - com.google.auto.service:auto-service:1.0-rc4 wanted version 23.5-jre
# - com.google.dagger:dagger-compiler:2.9 wanted version 20.0-rc1
# - com.google.dagger:dagger-producers:2.9 wanted version 20.0-rc1
# - com.google.truth:truth:0.39 wanted version 23.4-android
    {"artifact": "com.google.guava:guava:23.5-jre", "lang": "java", "sha1": "e9ce4989adf6092a3dab6152860e93d989e8cf88", "repository": "https://repo.maven.apache.org/maven2/", "name": "io_bazel_rules_kotlin_com_google_guava_guava", "actual": "@io_bazel_rules_kotlin_com_google_guava_guava//jar", "bind": "jar/io_bazel_rules_kotlin_com/google/guava/guava"},
    {"artifact": "com.google.j2objc:j2objc-annotations:1.1", "lang": "java", "sha1": "ed28ded51a8b1c6b112568def5f4b455e6809019", "repository": "https://repo.maven.apache.org/maven2/", "name": "io_bazel_rules_kotlin_com_google_j2objc_j2objc_annotations", "actual": "@io_bazel_rules_kotlin_com_google_j2objc_j2objc_annotations//jar", "bind": "jar/io_bazel_rules_kotlin_com/google/j2objc/j2objc_annotations"},
    {"artifact": "com.google.truth:truth:0.39", "lang": "java", "sha1": "bd1bf5706ff34eb7ff80fef8b0c4320f112ef899", "repository": "https://repo.maven.apache.org/maven2/", "name": "io_bazel_rules_kotlin_com_google_truth_truth", "actual": "@io_bazel_rules_kotlin_com_google_truth_truth//jar", "bind": "jar/io_bazel_rules_kotlin_com/google/truth/truth"},
    {"artifact": "com.squareup.moshi:moshi-kotlin:1.5.0", "lang": "java", "sha1": "d00c03088d77c40bf95504d03b254160e5f9dd51", "repository": "https://repo.maven.apache.org/maven2/", "name": "io_bazel_rules_kotlin_com_squareup_moshi_moshi_kotlin", "actual": "@io_bazel_rules_kotlin_com_squareup_moshi_moshi_kotlin//jar", "bind": "jar/io_bazel_rules_kotlin_com/squareup/moshi/moshi_kotlin"},
    {"artifact": "com.squareup.moshi:moshi:1.5.0", "lang": "java", "sha1": "c3c147f3967bb3b4bc4bae928a065aa9fcc51a31", "repository": "https://repo.maven.apache.org/maven2/", "name": "io_bazel_rules_kotlin_com_squareup_moshi_moshi", "actual": "@io_bazel_rules_kotlin_com_squareup_moshi_moshi//jar", "bind": "jar/io_bazel_rules_kotlin_com/squareup/moshi/moshi"},
    {"artifact": "com.squareup.okio:okio:1.13.0", "lang": "java", "sha1": "a9283170b7305c8d92d25aff02a6ab7e45d06cbe", "repository": "https://repo.maven.apache.org/maven2/", "name": "io_bazel_rules_kotlin_com_squareup_okio_okio", "actual": "@io_bazel_rules_kotlin_com_squareup_okio_okio//jar", "bind": "jar/io_bazel_rules_kotlin_com/squareup/okio/okio"},
    {"artifact": "javax.inject:javax.inject:1", "lang": "java", "sha1": "6975da39a7040257bd51d21a231b76c915872d38", "repository": "https://repo.maven.apache.org/maven2/", "name": "io_bazel_rules_kotlin_javax_inject_javax_inject", "actual": "@io_bazel_rules_kotlin_javax_inject_javax_inject//jar", "bind": "jar/io_bazel_rules_kotlin_javax/inject/javax_inject"},
    {"artifact": "junit:junit:4.12", "lang": "java", "sha1": "2973d150c0dc1fefe998f834810d68f278ea58ec", "repository": "https://repo.maven.apache.org/maven2/", "name": "io_bazel_rules_kotlin_junit_junit", "actual": "@io_bazel_rules_kotlin_junit_junit//jar", "bind": "jar/io_bazel_rules_kotlin_junit/junit"},
    {"artifact": "org.checkerframework:checker-qual:2.0.0", "lang": "java", "sha1": "518929596ee3249127502a8573b2e008e2d51ed3", "repository": "https://repo.maven.apache.org/maven2/", "name": "io_bazel_rules_kotlin_org_checkerframework_checker_qual", "actual": "@io_bazel_rules_kotlin_org_checkerframework_checker_qual//jar", "bind": "jar/io_bazel_rules_kotlin_org/checkerframework/checker_qual"},
    {"artifact": "org.codehaus.mojo:animal-sniffer-annotations:1.14", "lang": "java", "sha1": "775b7e22fb10026eed3f86e8dc556dfafe35f2d5", "repository": "https://repo.maven.apache.org/maven2/", "name": "io_bazel_rules_kotlin_org_codehaus_mojo_animal_sniffer_annotations", "actual": "@io_bazel_rules_kotlin_org_codehaus_mojo_animal_sniffer_annotations//jar", "bind": "jar/io_bazel_rules_kotlin_org/codehaus/mojo/animal_sniffer_annotations"},
    {"artifact": "org.hamcrest:hamcrest-core:1.3", "lang": "java", "sha1": "42a25dc3219429f0e5d060061f71acb49bf010a0", "repository": "https://repo.maven.apache.org/maven2/", "name": "io_bazel_rules_kotlin_org_hamcrest_hamcrest_core", "actual": "@io_bazel_rules_kotlin_org_hamcrest_hamcrest_core//jar", "bind": "jar/io_bazel_rules_kotlin_org/hamcrest/hamcrest_core"},
    ]

def maven_dependencies(callback = declare_maven):
    for hash in list_dependencies():
        callback(hash)
