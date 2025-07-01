package express

@JsModule("express")
@JsNonModule
external fun express(): ExpressApp

external interface ExpressApp {
  fun get(path: String, handler: (Request, Response) -> Unit)
  fun listen(port: Int, callback: () -> Unit)
}

external interface Request
external interface Response {
  fun send(body: String)
}
