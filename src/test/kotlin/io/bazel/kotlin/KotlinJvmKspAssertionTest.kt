package io.bazel.kotlin

import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class KotlinJvmKspAssertionTest: KotlinAssertionTestCase("src/test/data/jvm/ksp") {

    @Test
    fun testKSPCopiesAllFilesFromMetaINF() {
        jarTestCase("coffee_lib.jar", description = "Generated jar with ksp plugin contains meta-inf contents") {
            assertContainsExactEntries(
                "src/",
                "src/test/",
                "src/test/data/",
                "src/test/data/jvm/",
                "src/test/data/jvm/ksp/",
                "src/test/data/jvm/ksp/CoffeeAppService.class",
                "META-INF/",
                "META-INF/MANIFEST.MF"
            )
        }
    }

    @Test
    fun testKSPCopiesAllFilesFromMetaINFInMoshiLib() {
        jarTestCase("moshi_lib.jar", description = "Generated jar with ksp plugin contains meta-inf contents") {
            assertContainsExactEntries(
                "src/",
                "src/test/",
                "src/test/data/",
                "src/test/data/jvm/",
                "src/test/data/jvm/ksp/",
                "src/test/data/jvm/ksp/CoffeeAppModel.class",
                "src/test/data/jvm/ksp/CoffeeAppModelJsonAdapter.class",
                "META-INF/",
                "META-INF/MANIFEST.MF",
                "META-INF/proguard/",
                "META-INF/proguard/moshi-src.test.data.jvm.ksp.CoffeeAppModel.pro",
                "META-INF/src_test_data_jvm_ksp-moshi_lib.kotlin_module"
            )
        }
    }
}
