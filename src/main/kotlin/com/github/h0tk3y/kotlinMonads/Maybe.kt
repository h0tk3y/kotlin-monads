package com.github.h0tk3y.kotlinMonads

sealed class Maybe<out T> : Monad<Maybe<*>, T> {
    data class Just<T>(val value: T) : Maybe<T>() {
        override fun toString() = "Just $value"
    }

    override fun <R> bind(f: Binder<Maybe<*>, T, R>): Maybe<R> = when (this) {
        is Just -> f(MaybeReturn, value) as Maybe
        is None -> none()
    }

    object None : Maybe<Nothing>() {
        override fun toString() = "None"
    }
}

object MaybeReturn : Return<Maybe<*>> {
    override fun <T> returns(t: T) = just(t)
}

@Suppress("UNCHECKED_CAST")
fun <T> none(): Maybe<T> = Maybe.None

fun <T> just(t: T) = Maybe.Just(t)