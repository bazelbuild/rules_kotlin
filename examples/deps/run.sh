#!/usr/bin/env bash

function die () {
    echo "ERROR: $*"
    exit 1
}

function invalidate() {
	sed -i -e "s/abiChangeVariable/abiChangeVariable_/g" $1
	rm "$1-e"
}

TARGETS="libJava1:src_main libAndroid1:src_main libKt1:src_main libKtAndroid1:src_main libKtAndroid1:src_main_kt"
BAZEL_BINARY=${BAZEL_BINARY:-bazel}

$BAZEL_BINARY --version || die "Bazel binary invalid or 'bazel' is not on classpath"
rm -rf profile.gz profile

# Run initial clean build
$BAZEL_BINARY clean
$BAZEL_BINARY build $TARGETS $@ || die "Fail initial build targets"

# ABI invalidation
invalidate libJava4/src/main/java/examples/deps/Dummy4.java
invalidate libAndroid4/src/main/java/examples/deps/Dummy4.java
invalidate libKt4/src/main/kt/examples/deps/KtDummy4.kt
invalidate libKtAndroid4/src/main/kt/examples/deps/KtDummy4.kt

# Run incremental build
$BAZEL_BINARY build $TARGETS $@ --profile=profile.gz || die "Failed to build targets"

# Parse profile
gzip -d profile.gz -f

NUM_JAVA_TARGETS=$(cat profile | grep "action processing" | grep "Building libJava" | wc -l | xargs)
NUM_ANDROID_TARGETS=$(cat profile | grep "action processing" | grep "Building libJava" | wc -l | xargs)
NUM_KT_TARGETS=$(cat profile | grep "action processing" | grep "KotlinCompile //libKt" | grep -v "KotlinCompile //libKtAndroid" | wc -l | xargs)
NUM_KT_ANDROID_TARGETS=$(cat profile | grep "action processing" | grep "KotlinCompile //libKtAndroid" | wc -l | xargs)

echo Rebuilt libjava targets: $NUM_JAVA_TARGETS
echo Rebuilt libandroid targets: $NUM_ANDROID_TARGETS
echo Rebuilt libkt targets: $NUM_KT_TARGETS
echo Rebuilt libktandroid targets: $NUM_KT_ANDROID_TARGETS

# Cleanup
git checkout -- libJava4/src/main/java/examples/deps/Dummy4.java
git checkout -- libAndroid4/src/main/java/examples/deps/Dummy4.java
git checkout -- libKt4/src/main/kt/examples/deps/KtDummy4.kt
git checkout -- libKtAndroid4/src/main/kt/examples/deps/KtDummy4.kt