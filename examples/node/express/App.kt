package express

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*

@JsModule("express")
external fun express(): dynamic

val app = express()

@ExperimentalCoroutinesApi
fun main(args: Array<String>) {
    val scope = CoroutineScope(Dispatchers.Default)

    // register the routes.
    val hitCountChannel = routes(app)
    scope.launch {
        hitCountChannel.consumeEach { 
            println("Hits so far: $it")
        }
    }

    app.listen(3000, {
        println("Listening on port 3000")
    })
}