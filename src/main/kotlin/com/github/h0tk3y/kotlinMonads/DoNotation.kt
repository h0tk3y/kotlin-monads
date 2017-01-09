@file:Suppress("EXPERIMENTAL_FEATURE_WARNING")

package com.github.h0tk3y.kotlinMonads

import java.io.Serializable
import java.util.*
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineIntrinsics
import kotlin.coroutines.startCoroutine

fun <M : Monad<M, *>, T> doWith(m: Monad<M, T>,
                                c: suspend DoController<M, T>.() -> Unit): Monad<M, T> =
        m.bind { t -> doWith(this, t, c) }


fun <M : Monad<M, *>, T> doWith(aReturn: Return<M>,
                                defaultValue: T,
                                c: suspend DoController<M, T>.() -> Unit): Monad<M, T> {
    val controller = DoController(aReturn, defaultValue)
    c.startCoroutine(controller, object : Continuation<Unit> {
        override fun resume(value: Unit) {}
        override fun resumeWithException(exception: Throwable) = throw exception
    })
    return controller.lastResult
}

val labelField by lazy {
    val jClass = Class.forName("kotlin.jvm.internal.RestrictedCoroutineImpl")
    return@lazy jClass.getDeclaredField("label").apply { isAccessible = true }
}

var <T> Continuation<T>.label
    get() = labelField.get(this)
    set(value) = labelField.set(this@label, value)

private fun <T, R> backupLabel(c: Continuation<T>, block: Continuation<T>.() -> R): R {
    val backupLabel = c.label
    val r = block(c)
    c.label = backupLabel
    return r
}

class DoController<M : Monad<M, *>, T>(val returning: Return<M>,
                                       val value: T) : Serializable, Return<M> by returning {
    var lastResult: Monad<M, T> = returning.returns(value)
        internal set

    private val stackSignals = Stack<Boolean>().apply { push(false) }

    suspend fun bind(m: Monad<M, T>): T = CoroutineIntrinsics.suspendCoroutineOrReturn { c ->
        stackSignals.pop()
        stackSignals.push(true)
        var anyCont = false
        val o = m.bind { x ->
            stackSignals.push(false)
            backupLabel(c) {
                c.resume(x)
            }
            val contHasMonad = stackSignals.pop()
            if (contHasMonad) {
                anyCont = true
                lastResult
            } else {
                returns(x)
            }
        }
        lastResult = if (anyCont) o else m
        CoroutineIntrinsics.SUSPENDED
    }

    suspend fun then(m: Monad<M, T>) = CoroutineIntrinsics.suspendCoroutineOrReturn<Unit> { c ->
        stackSignals.pop()
        stackSignals.push(true)
        var anyCont = false
        val o = m.bind { x ->
            stackSignals.push(false)
            backupLabel(c) { c.resume(Unit) }
            val contHasMonad = stackSignals.pop()
            if (contHasMonad) {
                anyCont = true
                lastResult
            } else {
                returning.returns(x)
            }
        }
        lastResult = if (anyCont) o else m
        CoroutineIntrinsics.SUSPENDED
    }
}

fun main(args: Array<String>) {
    val m = doWith(monadListOf(0)) {
        val x = bind(monadListOf(1, 2, 3))
        val y = bind(monadListOf(x, x))
        then(monadListOf(y, y + 1))
    }
    println(m)
}