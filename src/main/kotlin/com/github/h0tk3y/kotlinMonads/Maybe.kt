package com.github.h0tk3y.kotlinMonads

sealed class Maybe<out T> : Monad<Maybe<*>, T> {
    override fun <R> bind(f: Binder<Maybe<*>, T, R>): Maybe<R> = when (this) {
        is Just -> f(MaybeReturn, value) as Maybe
        is None -> none()
    }
}

data class Just<T>(val value: T) : Maybe<T>() {
    override fun toString() = "Just $value"
}

object None : Maybe<Nothing>() {
    override fun toString() = "None"
}

object MaybeReturn : Return<Maybe<*>> {
    override fun <T> returns(t: T) = Just(t)
}

@Suppress("UNCHECKED_CAST")
fun <T> none(): Maybe<T> = None

fun <T> just(t: T) = Just(t)