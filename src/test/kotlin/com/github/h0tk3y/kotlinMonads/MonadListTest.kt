package com.github.h0tk3y.kotlinMonads

import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Test

class MonadListTest {
    @Test fun testFlattens() {
        val l = monadListOf(123, 456, 789, 999)
        val l1 = l bind { MonadList(it.toString().toList()) }
        assertEquals("123456789999".toList(), l1.list)
    }

    @Test fun testEmpty() {
        val l = monadListOf(1, 2, 3)
        val l1 = l bind { if (it % 2 == 0) returns(it) else emptyMonadList() }
        assertEquals(monadListOf(2), l1)
    }

    @Test fun testBindTimes() {
        val l1 = monadListOf(0)
        val l1024 = l1.bindTimes(10) { i -> monadListOf(i + 1, i + 1) } as MonadList
        Assert.assertTrue(l1024.all { it == 10 })
        assertEquals(1024, l1024.size)
    }

    @Test fun simple() {
        val m = doWith(monadListOf(0)) {
            val x = bind(monadListOf(1, 2, 3))
            val y = bind(monadListOf(x, x))
            then(monadListOf(y, y + 1))
        } as MonadList
        assertEquals(monadListOf(1, 2, 1, 2, 2, 3, 2, 3, 3, 4, 3, 4), m)
    }
}