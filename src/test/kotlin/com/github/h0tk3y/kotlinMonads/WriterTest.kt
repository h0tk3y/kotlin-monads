package com.github.h0tk3y.kotlinMonads

import org.junit.Assert
import org.junit.Test

class WriterTest {
    @Test fun testListen() {
        val w = Writer(Unit, "")
        val w1 = w
                .bind { tell("123") }
                .bind { tell("456") }
                .bind { returns(123456) }
        val w2 = listen(w1)
        Assert.assertEquals(123456 to "123456", w2.result)
    }

    @Test fun testCensor() {
        fun tellAndInc(i: Int) = Writer(i + 1, "$i")

        val w = Writer(90, "")
                .bind { tellAndInc(it) }
                .bind { tellAndInc(it) }
                .bind { tellAndInc(it) }
                .bind { tellAndInc(it) }
        val w1 = censor(w) { it.filter { it != '9' } }
        Assert.assertEquals(Writer(94, "0123"), w1)
    }
}