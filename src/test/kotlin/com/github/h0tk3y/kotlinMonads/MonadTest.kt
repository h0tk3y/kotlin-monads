package com.github.h0tk3y.kotlinMonads

import org.junit.Assert.assertEquals
import org.junit.Test

class MonadTest {
    @Test fun testThen() {
        val j1 = just(1) then just(2) then just(3)
        assertEquals(just(3), j1)
        val j2 = just(1) then none<Int>() then just(3)
        assertEquals(none<Int>(), j2)
    }

    @Test fun testMap() {
        val m = just(2)
        val l = monadListOf(1, 2, 3)
        val f: (Int) -> Int = { it * it }

        val m1 = m map f
        assertEquals(just(4), m1)

        val l1 = l map f
        assertEquals(monadListOf(1, 4, 9), l1)
    }
}