package express

@JsModule("express")
external fun express(): dynamic

val app = express()

fun main(args: Array<String>) {
    // register the routes.
    routes(app)

    app.listen(3000, {
        println("Listening on port 3000")
    })
}