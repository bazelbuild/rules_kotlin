package express

fun main() {
  val app = express()

  app.get("/") { _, res ->
    res.send("Hello from Kotlin/JS Node server!")
  }

  app.listen(3000) {
    println("Server started on http://localhost:3000")
  }
}
