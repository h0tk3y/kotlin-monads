package com.github.h0tk3y.kotlinMonads

import java.io.Serializable
import java.util.*
import kotlin.jvm.internal.CoroutineImpl

fun <M : Monad<M, *>, T> doWith(m: Monad<M, T>,
                                coroutine c: DoController<M, T>.(T) -> Continuation<Unit>): Monad<M, T> {
    return (m bind { x -> doWith(this, x, c) })
}

fun <M : Monad<M, *>, T> doWith(aReturn: Return<M>,
                                defaultValue: T,
                                coroutine c: DoController<M, T>.(T) -> Continuation<Unit>): Monad<M, T> {
    val controller = DoController(aReturn, defaultValue)
    c(controller, defaultValue).resume(Unit)
    return controller.lastResult
}

private fun <T, R> backupLabel(c: Continuation<T>, block: Continuation<T>.() -> R): R {
    val reflect = CoroutineImpl::class.java
    val labelField = reflect.getDeclaredField("label")

    labelField.isAccessible = true
    val l = labelField.get(c)
    labelField.isAccessible = false

    val r = block(c)

    labelField.isAccessible = true
    labelField.set(c, l)
    labelField.isAccessible = false

    return r
}

class DoController<M : Monad<M, *>, T>(val returning: Return<M>,
                                       initialValue: T) : Serializable {
    var lastResult: Monad<M, T> = returning.returns(initialValue)
        private set

    private val stackSignals = Stack<Boolean>().apply { push(false) }

    fun <T> returns(t: T) = returning.returns(t)

    suspend fun bind(m: Monad<M, T>, c: Continuation<T>) {
        stackSignals.pop()
        stackSignals.push(true)
        var anyCont = false
        val o = m.bind { x ->
            stackSignals.push(false)
            backupLabel(c) { c.resume(x) }
            val contHasMonad = stackSignals.pop()
            if (contHasMonad) {
                anyCont = true
                lastResult
            } else {
                returns(x)
            }
        }
        lastResult = if (anyCont) o else m
    }

    suspend fun then(m: Monad<M, T>, c: Continuation<Unit>) {
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