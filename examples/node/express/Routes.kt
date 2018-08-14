package express

import express.auth.isAuthenticated

//import express.auth.isAuthenticated

fun routes(app: dynamic) {
    app.get("/") { req, res ->
        if(!isAuthenticated("bob")) {
            res.send(401, "you sir, are not authorized !")
        } else {
            res.type("text/plain")
            res.send("hello world")
        }
    }
}