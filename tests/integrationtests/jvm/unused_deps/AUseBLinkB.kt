package unused_deps.A

import unused_deps.B.B

class AClientUseBLinkB {
    fun doWork() {
        println("I use and link against B")
        B().doWork()
    }
}

