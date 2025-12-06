package com.example

fun main() {
    println("Environment variable demo:")
    println("GREETING = ${System.getenv("GREETING") ?: "not set"}")
    println("MESSAGE = ${System.getenv("MESSAGE") ?: "not set"}")
    println("HOME = ${System.getenv("HOME") ?: "not set"}")
}
