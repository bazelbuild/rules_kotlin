fun printMessage() {
  val resourceName = "resource.txt"
  val resourceUrl = Thread.currentThread().contextClassLoader.getResource(resourceName)
  val resourceText = resourceUrl?.readText()

  println("Hello $resourceText")
}

