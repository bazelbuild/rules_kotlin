package unused_deps.B

import unused_deps.C.C

class B {
    fun doWork() {
        println("I do work for A and use and link against C correctly")
        C().doWork()
    }
}