package service.app

import service.di.DaggerServiceComponent

fun main() {
    val component = DaggerServiceComponent.builder().build()
    val greeting = component.greeter().greet()
    println(greeting)
}
