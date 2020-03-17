package express

import express.auth.isAuthenticated
import kotlinx.atomicfu.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*

fun routes(app: dynamic): Channel<Int> {
    val scope = CoroutineScope(Dispatchers.Default)
    val channel = Channel<Int>()
    val hitCounter = atomic(0)

    app.get("/") { req, res ->
        scope.launch {
            val hitsSoFar = hitCounter.updateAndGet { it + 1 }
            channel.send(hitsSoFar)
        }
        if (!isAuthenticated("bob")) {
            res.send(401, "you sir, are not authorized !")
        } else {
            res.type("text/plain")
            res.send("hello world")
        }
    }

    return channel
}
