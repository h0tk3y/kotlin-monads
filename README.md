# kotlin-monads

[![](https://jitpack.io/v/h0tk3y/kotlin-monads.svg)](https://jitpack.io/#h0tk3y/kotlin-monads) [![](https://img.shields.io/badge/kotlin-1.1--M03-blue.svg)](http://kotlinlang.org/)

An attempt to implement monads in Kotlin.

_Note: this project uses Kotlin 1.1 EAP build. Use the 1.1 EAP IDE plugin to work with it._

## The monad type

Monadic types are represented by the `Monad<M, T>` interface, 
where `M` **should be the type of the implementation** with its `T` star-projected. Examples: `Maybe<T> : Monad<Maybe<*>, T>`, `State<S, T> : Monad<State<S, *>, T>`. 

The purpose is: with `Monad` defined in this way, we
are almost able to say that a function returns the same `Monad` implementation but with a different type parameter:

    fun <T, R, M : Monad<M, *>> Monad<M, T>.map(f: (T) -> R) = bind { returns(f(it)) }

    val m = just(3).map { it * 2 } as Maybe
    
We still need the downcast `as Maybe`, but at least it's checked.

## Usage

Add as a dependency:

    repositories {
        ...
        maven { url 'https://jitpack.io' }
    }
    
    dependencies {
	    compile 'com.github.h0tk3y:kotlin-monads:0.1'
	}

See the usage examples in [tests](https://github.com/h0tk3y/kotlin-monads/tree/master/src/test/kotlin/com/github/h0tk3y/kotlinMonads).

## How to implement a monad

`Monad<M, T>` is defined as follows:

    interface Return<M> {
        fun <T> returns(t: T): Monad<M, T>
    }

    interface Monad<This, out T> {
        infix fun <R> bind(f: Return<This>.(T) -> Monad<This, R>): Monad<This, R>
    }
    
The monad implementation should only provide one function `bind` (Haskell: `>>=`), 
no separate `return` is there, instead, if you look at the signature of `bind`, you'll see that the function to bind with is `f: Return<This>.(T) -> Monad<This, R>`. 

It means that a monad implementation should provide the `Return<M>` as well and pass it to `f` each time, so that inside `f` its `returns` could be used:

    just(3) bind { returns(it * it) }
    
I found no direct equivalent to `return` in Haskell, which could be used even outside bind functions. Outside the `bind` blocks, you should either
wrap the values into your monads manually or require a `Return<M>`, which can wrap `T` into `Monad<M, T>` for you.

Also, it's good to make the return type of `bind` narrower, e.g. `bind` of `Maybe<T>` would rather return `Maybe<R>` than `Monad<Maybe<*>, R>`, it allows not to cast 
the result of a `bind` called on a known monad type.

    val m = monadListOf(1, 2, 3) bind { monadListOf("$it", "$it") } // already `MonadList<String>`, no need to cast

Example implementation:

    sealed class Maybe<out T> : Monad<Maybe<*>, T> {
        class Just<T>(val value: T) : Maybe<T>()
        class None : Maybe<Nothing>()

        override fun <R> bind(f: Binder<Maybe<*>, T, R>): Maybe<R> = when (this) {
            is Just -> f(MaybeReturn, value) as Maybe
            is None -> None()
        }
    }

    object MaybeReturn : Return<Maybe<*>> {
        override fun <T> returns(t: T) = Maybe.Just(t)
    }

## Monads implementations bundled

* `Maybe<T>`
* `Either<F, T>`
* `MonadList<T>`
* `Reader<E, T>`
* `Writer<T>` (no monoid for now, just `String`)
* `State<S, T>`

## Do notation

With the power of Kotlin coroutines, we can have a limited variant of do notation:

    val m = doWith(monadListOf(0)) {
        val x = bind(monadListOf(1, 2, 3))
        val y = bind(monadListOf(x, x))
        then(monadListOf(y, y + 1))
    } as MonadList
    
    assertEquals(monadListOf(1, 2, 1, 2, 2, 3, 2, 3, 3, 4, 3, 4), m)
    
 The limitation is that the intermediate results in a single _do_ block are restricted to the same value type `T`. You can, however, use nested _do_ blocks to use different result types.
 
 **Be careful with mutable state** in _do_ blocks, since all continuation calls will share it, resulting into something unexpected:
 
     val m = doWith(monadListOf(0)) {
         for (i in 1..10)
             bind(monadListOf(0, 0))
     } as MonadList
     
 One would expect 1024 items here, but the result only contains 11! That's because `i` is mutable and is shared between all the calls that `bind` makes.
 
 
 
 
