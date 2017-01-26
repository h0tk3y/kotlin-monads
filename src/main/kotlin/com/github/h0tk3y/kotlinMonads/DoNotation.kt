@file:Suppress("EXPERIMENTAL_FEATURE_WARNING", "UNCHECKED_CAST")

package com.github.h0tk3y.kotlinMonads

import java.io.Serializable
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.RestrictsSuspension
import kotlin.coroutines.intrinsics.SUSPENDED_MARKER
import kotlin.coroutines.intrinsics.suspendCoroutineOrReturn
import kotlin.coroutines.startCoroutine

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

private val labelField by lazy {
    val jClass = Class.forName("kotlin.jvm.internal.CoroutineImpl")
    return@lazy jClass.getDeclaredField("label").apply { isAccessible = true }
}

private var <T> Continuation<T>.label
    get() = labelField.get(this)
    set(value) = labelField.set(this@label, value)

@RestrictsSuspension
class DoController<M : Monad<M, *>>(private val returning: Return<M>) :
        Serializable, Return<M> by returning, Continuation<Monad<M, *>> {

    override val context = EmptyCoroutineContext

    override fun resume(value: Monad<M, *>) {
        returnedMonad = value //here is where the biggest magic happens
    }

    override fun resumeWithException(exception: Throwable) {
        throw exception
    }

    internal lateinit var returnedMonad: Monad<M, *>

    suspend fun <T> bind(m: Monad<M, T>): T = suspendCoroutineOrReturn { c ->
        val labelHere = c.label // save the label
        returnedMonad = m.bind { x ->
            c.label = labelHere
            c.resume(x)
            returnedMonad
        }
        SUSPENDED_MARKER
    }

    suspend fun <T> then(m: Monad<M, T>): Unit {
        bind(m)
    }
}