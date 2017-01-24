@file:Suppress("EXPERIMENTAL_FEATURE_WARNING", "UNCHECKED_CAST")

package com.github.h0tk3y.kotlinMonads

import kotlinx.coroutines.experimental.Here
import java.io.Serializable
import java.util.*
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.RestrictsSuspension
import kotlin.coroutines.intrinsics.SUSPENDED_MARKER
import kotlin.coroutines.intrinsics.suspendCoroutineOrReturn
import kotlin.coroutines.startCoroutine

fun <M : Monad<M, *>, T> doWith(m: Monad<M, T>,
                                c: suspend DoController<M, T>.(T) -> Unit): Monad<M, T> =
        m.bind { t -> doWith(this, t, c) }

fun <M : Monad<M, *>, T> doWith(aReturn: Return<M>,
                                defaultValue: T,
                                c: suspend DoController<M, T>.(T) -> Unit): Monad<M, T> {
    val controller = DoController(aReturn, defaultValue)
    val f: suspend DoController<M, T>.() -> Unit = { c(defaultValue) }
    f.startCoroutine(controller, object : Continuation<Unit> {
        override fun resumeWithException(exception: Throwable) {}
        override fun resume(value: Unit) {}
        override val context: CoroutineContext = Here
    })
    return controller.lastResult
}

private val labelField by lazy {
    val jClass = Class.forName("kotlin.jvm.internal.CoroutineImpl")
    return@lazy jClass.getDeclaredField("label").apply { isAccessible = true }
}

private val innerContinuationField by lazy {
    val jClass = Class.forName("kotlinx.coroutines.experimental.DispatchedContinuation")
    return@lazy jClass.getDeclaredField("continuation").apply { isAccessible = true }
}

private var <T> Continuation<T>.label
    get() = labelField.get(innerContinuationField.get(this))
    set(value) = labelField.set(innerContinuationField.get(this@label), value)

private fun <T, R> backupLabel(c: Continuation<T>, block: Continuation<T>.() -> R): R {
    val backupLabel = c.label
    val r = block(c)
    c.label = backupLabel
    return r
}

@RestrictsSuspension
class DoController<M : Monad<M, *>, T>(val returning: Return<M>,
                                       val value: T) : Serializable, Return<M> by returning {
    var lastResult: Monad<M, T> = returning.returns(value)
        internal set

    private val stackSignals = Stack<Boolean>().apply { push(false) }

    suspend fun bind(m: Monad<M, T>): T = suspendCoroutineOrReturn { c ->
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
        SUSPENDED_MARKER
    }

    suspend fun then(m: Monad<M, T>) = suspendCoroutineOrReturn<Unit> { c ->
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
        SUSPENDED_MARKER
    }
}