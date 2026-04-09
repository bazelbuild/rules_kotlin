package com.example.pathmapping

class Calculator(private val greeter: Greeter) {
    fun add(a: Int, b: Int): Int = a + b
    fun subtract(a: Int, b: Int): Int = a - b
    fun multiply(a: Int, b: Int): Int = a * b
}
