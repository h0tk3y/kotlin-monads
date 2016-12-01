package com.github.h0tk3y.kotlinMonads

import org.junit.Assert.assertEquals
import org.junit.Test

class ReaderTest {
    @Test fun testSimple() {
        val context = 1
        val r = ask<Int>() bind { returns(it * 2) } bind { returns(it + 3) }
        assertEquals(5, r.runReader(context))
    }

    @Test fun testLocal() {
        val context = 1
        val negate = Reader<Int, Int> { -it }
        val r = ask<Int>()
                .bind { local(negate) { it * 2 } }
                .bind { returns(it - 1) }
        assertEquals(-3, r.runReader(context))
    }
}