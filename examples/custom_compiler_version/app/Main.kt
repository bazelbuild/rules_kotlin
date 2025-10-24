package custom

fun main() {
    println("Hello from Kotlin ${KotlinVersion.CURRENT}!")
    println("This example uses Kotlin 2.1.21 which includes trove4j")

    // Simple demonstration that the build works
    val numbers = listOf(1, 2, 3, 4, 5)
    val doubled = numbers.map { it * 2 }
    println("Doubled numbers: $doubled")
}
