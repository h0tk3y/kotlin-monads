@file:Suppress("EXPERIMENTAL_FEATURE_WARNING")

package com.github.h0tk3y.kotlinMonads

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class DoNotationTest {
    @Test fun testLinearDo() {
        val m = just(1).bindDo { x ->
            val j = bind(returns(x * 2))
            val k = bind(returns(j * 3))
            returns(k + 1)
        }
        assertEquals(just(7), m)
    }

    @Test fun testControlFlow() {
        var called = false
        val m = just(1).bindDo { x ->
            val j = bind(returns(x * 2))
            val k = bind(if (j % 2 == 0) none() else just(j))
            called = true
            returns(k)
        }
        assertEquals(None, m)
        assertFalse(called)
    }

    @Test fun testMultipleBranches() {
        var iIterations = 0
        var jIterations = 0
        val m = doReturning(MonadListReturn) {
            val i = bind(monadListOf(1, 2, 3))
            ++iIterations
            val j = bind(monadListOf(i, i))
            ++jIterations
            monadListOf(j, j + 1)
        }
        assertEquals(monadListOf(1, 2, 1, 2, 2, 3, 2, 3, 3, 4, 3, 4), m)
        assertEquals(3, iIterations)
        assertEquals(6, jIterations)
    }

    @Test fun testMultipleApplications() {
        val m = doReturning(MonadListReturn) {
            then(monadListOf(1, 1))
            then(monadListOf(1, 1))
            then(monadListOf(1, 1))
            monadListOf(1, 1)
        } as MonadList
        assertEquals(16, m.size)
    }
}