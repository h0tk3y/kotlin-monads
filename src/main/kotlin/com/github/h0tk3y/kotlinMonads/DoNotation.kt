@file:Suppress("EXPERIMENTAL_FEATURE_WARNING", "UNCHECKED_CAST")

package com.github.h0tk3y.kotlinMonads

import java.io.Serializable
import kotlin.coroutines.experimental.Continuation
import kotlin.coroutines.experimental.EmptyCoroutineContext
import kotlin.coroutines.experimental.RestrictsSuspension
import kotlin.coroutines.experimental.intrinsics.COROUTINE_SUSPENDED
import kotlin.coroutines.experimental.intrinsics.suspendCoroutineOrReturn
import kotlin.coroutines.experimental.startCoroutine

fun <M : Monad<M, *>, T, R> Monad<M, T>.bindDo(c: suspend DoController<M>.(T) -> Monad<M, R>): Monad<M, R> =
        bind { t ->
            val controller = DoController(this)
            val f: suspend DoController<M>.() -> Monad<M, R> = { c(t) }
            f.startCoroutine(controller, controller)
            controller.returnedMonad as Monad<M, R>
        }

fun <M : Monad<M, *>, R> doReturning(aReturn: Return<M>,
                                     c: suspend DoController<M>.() -> Monad<M, R>): Monad<M, R> {
    val controller = DoController(aReturn)
    val f: suspend DoController<M>.() -> Monad<M, R> = { c() }
    f.startCoroutine(controller, controller)
    return controller.returnedMonad as Monad<M, R>
}

@RestrictsSuspension
class DoController<M : Monad<M, *>>(private val returning: Return<M>) :
        Serializable, Return<M> by returning, Continuation<Monad<M, *>> {

    override val context = EmptyCoroutineContext

    override fun resume(value: Monad<M, *>) {
        returnedMonad = value
    }

    override fun resumeWithException(exception: Throwable) {
        throw exception
    }

    internal lateinit var returnedMonad: Monad<M, *>

    suspend fun <T> bind(m: Monad<M, T>): T = suspendCoroutineOrReturn { c ->
        val labelHere = c.stackLabels // save the whole coroutine stack labels
        returnedMonad = m.bind { x ->
            c.stackLabels = labelHere
            c.resume(x)
            returnedMonad
        }
        COROUTINE_SUSPENDED
    }

    suspend fun <T> then(m: Monad<M, T>): Unit {
        bind(m)
    }
}