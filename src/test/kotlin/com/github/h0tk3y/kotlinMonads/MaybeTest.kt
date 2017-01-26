package com.github.h0tk3y.kotlinMonads

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MaybeTest {
    @Test fun testBindJust() {
        val j = Just(1)
        val j1 = j
                .bind { returns(it * 2) }
                .bind { returns(it * 3) }
                .bind { returns(it * 4) }
        assertEquals(Just(1 * 2 * 3 * 4), j1)
    }

    @Test fun testBindNone() {
        val j = Just(1)
        val j1 = j.bind { returns(it * 3) }
        assertTrue(j1 is Just)
        val j2 = j1
                .bind { if (it > 1) none() else returns(it) }
                .bind { returns(it * 2) }
        assertEquals(None, j2)
    }
}