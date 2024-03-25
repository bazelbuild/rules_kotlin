package io.bazel.kotlin

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class KotlinJvmKspAssertionTest: KotlinAssertionTestCase("src/test/data/jvm/ksp") {

    @Test
    fun testKotlinOnlyKSP() {
        jarTestCase(
            name = "ksp_kotlin_resources.jar",
            description = "KSP should work",
        ) {
            assertContainsEntries(
                "src/test/data/jvm/ksp/CoffeeAppModelJsonAdapter.class",
            )
        }
        jarTestCase(
            "ksp_kotlin_resources_missing_plugin.jar",
            description = "KSP should not generate files"
        ) {
            assertDoesNotContainEntries(
                "src/test/data/jvm/ksp/CoffeeAppModelJsonAdapter.class",
            )
        }
    }

    @Test
    fun testMixedModeKSP() {
        jarTestCase(
            name = "ksp_mixed_resources.jar",
            description = "KSP should work for mixed mode targets",
        ) {
            assertContainsExactEntries(
                "META-INF/",
                "META-INF/MANIFEST.MF",
                "META-INF/src_test_data_jvm_ksp-ksp_mixed_resources.kotlin_module",
                "src/",
                "src/test/",
                "src/test/data/",
                "src/test/data/jvm/",
                "src/test/data/jvm/ksp/",
                "src/test/data/jvm/ksp/CoffeeApp.class",
                "src/test/data/jvm/ksp/CoffeeApp\$CoffeeShop.class",
                "src/test/data/jvm/ksp/CoffeeApp\$Companion.class",
                "src/test/data/jvm/ksp/CoffeeBean.class",
                "src/test/data/jvm/ksp/CoffeeMaker.class",
                "src/test/data/jvm/ksp/CoffeeMaker_Factory.class",
                "src/test/data/jvm/ksp/DaggerCoffeeApp_CoffeeShop.class",
                "src/test/data/jvm/ksp/DaggerCoffeeApp_CoffeeShop\$1.class",
                "src/test/data/jvm/ksp/DaggerCoffeeApp_CoffeeShop\$Builder.class",
                "src/test/data/jvm/ksp/DaggerCoffeeApp_CoffeeShop\$CoffeeShopImpl.class",
                "src/test/data/jvm/ksp/DripCoffeeModule.class",
                "src/test/data/jvm/ksp/DripCoffeeModule_ProvideHeaterFactory.class",
                "src/test/data/jvm/ksp/ElectricHeater.class",
                "src/test/data/jvm/ksp/Heater.class",
            )
        }
    }

    @Test
    fun testKSPGeneratesJavaOnlyWithPluginGeneratesJavaFlagEnabled() {
        jarTestCase(
            name = "ksp_kotlin_resources_multiple_plugins.jar",
            description = "KSP should generate java",
        ) {
            assertContainsEntries(
                "src/test/data/jvm/ksp/CoffeeAppModelJsonAdapter.class",
                "src/test/data/jvm/ksp/DripCoffeeModule_ProvideHeaterFactory.class",
            )
        }
        jarTestCase(
            "ksp_kotlin_resources_multiple_plugins_no_java_gen.jar",
            description = "KSP should not generate java files"
        ) {
            assertContainsEntries(
                "src/test/data/jvm/ksp/CoffeeAppModelJsonAdapter.class",
            )
            assertDoesNotContainEntries(
                "src/test/data/jvm/ksp/DripCoffeeModule_ProvideHeaterFactory.class",
            )
        }
    }

    @Test
    fun testKSPCopiesAllFilesFromMetaINF() {
        jarTestCase(
            name = "ksp_mixed_resources_multiple_plugins.jar",
            description = "Generated jar with ksp plugins contains all meta-inf contents",
        ) {
            assertContainsExactEntries(
                "META-INF/",
                "META-INF/MANIFEST.MF",
                "META-INF/proguard/",
                "META-INF/proguard/moshi-src.test.data.jvm.ksp.CoffeeAppModel.pro",
                "META-INF/services/",
                "META-INF/services/java.lang.Object",
                "META-INF/src_test_data_jvm_ksp-ksp_mixed_resources_multiple_plugins.kotlin_module",
                "src/",
                "src/test/",
                "src/test/data/",
                "src/test/data/jvm/",
                "src/test/data/jvm/ksp/",
                "src/test/data/jvm/ksp/CoffeeApp.class",
                "src/test/data/jvm/ksp/CoffeeAppModel.class",
                "src/test/data/jvm/ksp/CoffeeAppModelJsonAdapter.class",
                "src/test/data/jvm/ksp/CoffeeAppService.class",
                "src/test/data/jvm/ksp/CoffeeApp\$CoffeeShop.class",
                "src/test/data/jvm/ksp/CoffeeApp\$Companion.class",
                "src/test/data/jvm/ksp/CoffeeMaker.class",
                "src/test/data/jvm/ksp/CoffeeMaker_Factory.class",
                "src/test/data/jvm/ksp/DaggerCoffeeApp_CoffeeShop.class",
                "src/test/data/jvm/ksp/DaggerCoffeeApp_CoffeeShop\$1.class",
                "src/test/data/jvm/ksp/DaggerCoffeeApp_CoffeeShop\$Builder.class",
                "src/test/data/jvm/ksp/DaggerCoffeeApp_CoffeeShop\$CoffeeShopImpl.class",
                "src/test/data/jvm/ksp/DripCoffeeModule.class",
                "src/test/data/jvm/ksp/DripCoffeeModule_ProvideHeaterFactory.class",
                "src/test/data/jvm/ksp/ElectricHeater.class",
                "src/test/data/jvm/ksp/Heater.class",
            )
        }
    }
}
