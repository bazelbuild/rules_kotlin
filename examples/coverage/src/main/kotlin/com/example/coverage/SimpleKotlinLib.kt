package com.example.coverage

class SimpleKotlinLib {
    fun add(a: Int, b: Int): Int {
        return if (a > 0) {
            a + b
        } else {
            b
        }
    }
    
    fun multiply(a: Int, b: Int): Int {
        return a * b
    }
}
