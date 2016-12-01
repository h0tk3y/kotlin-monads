package com.github.h0tk3y.kotlinMonads

import org.junit.Assert.assertEquals
import org.junit.Test

class StateTest {
    @Test fun testStack() {
        fun <T> push(t: T): State<List<T>, Unit> = State { s -> Pair(s + t, Unit) }
        fun <T> peek(): State<List<T>, T?> = State { s -> Pair(s, s.lastOrNull()) }
        fun <T> pop(): State<List<T>, T?> = State { s -> Pair(s.dropLast(1), s.lastOrNull()) }

        val s = peek<Int>().bind { i -> push(i ?: 0)
                     .bind { push(1) }
                     .bind { push(2) }
                     .bind { peek<Int>() bind { p -> push(if (p == i) 1 else 2) } }
                     .bind { pop<Int>() }
                }

        assertEquals(listOf(2, 2, 1, 2) to 1, s.runState(listOf(2)))
        assertEquals(listOf(1, 1, 1, 2) to 2, s.runState(listOf(1)))
        assertEquals(listOf(0, 1, 2) to 2, s.runState(emptyList()))
    }
}