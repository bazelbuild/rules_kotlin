package com.example.coverage

import org.junit.Test
import org.junit.Assert.assertEquals

class SimpleKotlinTest {
    @Test
    fun testAdd() {
        val lib = SimpleKotlinLib()
        assertEquals(3, lib.add(1, 2))
        assertEquals(2, lib.add(0, 2))
    }
    
    @Test
    fun testMultiply() {
        val lib = SimpleKotlinLib()
        assertEquals(6, lib.multiply(2, 3))
    }
}
