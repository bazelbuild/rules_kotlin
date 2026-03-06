object ResourceTest {
    private fun loadResource(path: String): String? =
        Thread.currentThread().contextClassLoader.getResource(path)?.readText()?.trim()

    private fun assertResource(path: String, expected: String, description: String) {
        val content = loadResource(path)
        check(content != null) { "FAIL [$description]: resource '$path' not found on classpath" }
        check(content == expected) {
            "FAIL [$description]: expected '$expected' but got '$content'"
        }
        println("PASS [$description]: '$path' = '$expected'")
    }

    @JvmStatic
    fun main(args: Array<String>) {
        assertResource(
            "source_data.txt", "source_with_prefix",
            "source resource with resource_strip_prefix",
        )
        assertResource(
            "static_resources/source_no_prefix.txt", "source_without_prefix",
            "source resource without resource_strip_prefix",
        )
        assertResource(
            "generated_data.txt", "generated_with_prefix",
            "generated resource with resource_strip_prefix",
        )
        assertResource(
            "generated_no_prefix.txt", "generated_without_prefix",
            "generated resource without resource_strip_prefix (issue #1469)",
        )
        assertResource(
            "conventional.txt", "generated_conventional",
            "generated resource under src/main/resources/ conventional prefix",
        )
        assertResource(
            "resource.txt", "world",
            "cross-module resource with resource_strip_prefix",
        )
        println("\nAll resource loading tests passed.")
    }
}
