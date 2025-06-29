@file:JsModule("express")
@file:JsNonModule

external fun express(): ExpressApp

external interface ExpressApp {
  fun get(path: String, handler: (Request, Response) -> Unit)
  fun listen(port: Int, callback: () -> Unit)
}

external interface Request
external interface Response {
  fun send(body: String)
}

fun main() {
  val app = express()

  app.get("/") { _, res ->
    res.send("Hello from Kotlin/JS Node server!")
  }

  app.listen(3000) {
    println("Server started on http://localhost:3000")
  }
}
