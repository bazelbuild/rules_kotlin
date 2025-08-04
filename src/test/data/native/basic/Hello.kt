package basic

fun main() {
  val rawNames = listOf("alice", "", "Bob", "charlie", "  ", "dave")

  // Clean up names: trim, filter blanks
  val cleanedNames = rawNames
    .map { it.trim() }
    .filter { it.isNotEmpty() }
    .sortedBy { it.lowercase() }

  // Capitalize each name
  val capitalizedNames = cleanedNames.map { it.replaceFirstChar { c -> c.uppercaseChar() } }

  // Print numbered list
  capitalizedNames.forEachIndexed { index, name ->
    println("${index + 1}. $name")
  }
}
