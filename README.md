# kotlin-monads

[![](https://jitpack.io/v/h0tk3y/kotlin-monads.svg)](https://jitpack.io/#h0tk3y/kotlin-monads) [![](https://img.shields.io/badge/kotlin-1.1.0-blue.svg)](http://kotlinlang.org/)

An attempt to implement monads in Kotlin, deeply inspired by Haskell monads, but restricted within the Kotlin type system.

## The monad type

Monadic types are represented by the `Monad<M, T>` interface, 
where `M` **should be the type of the implementation** with only its `T` star-projected. Examples: `Maybe<T> : Monad<Maybe<*>, T>`, `State<S, T> : Monad<State<S, *>, T>`. 

With `Monad` defined in this way, we
are almost able to say in terms of the Kotlin type system that a function returns the same `Monad` implementation but 
with a different type argument `R` instead of `T`:

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
	    compile 'com.github.h0tk3y:kotlin-monads:0.4'
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
It means that a `Monad<M, T>` implementation should provide the `Return<M>` as well and pass it to `f` each time, so that inside `f` its `returns` could be used:

    just(3) bind { returns(it * it) }
    
There seems to be no direct equivalent to Haskell `return`, which could be used outside any context like `bind` blocks. Outside the `bind` blocks, you should either
wrap the values into your monads manually or require a `Return<M>`, which can wrap `T` into `Monad<M, T>` for you. 

Mind the [monad laws](https://wiki.haskell.org/Monad_laws). A correct monad implementation follows these three rules (rephrased in terms of `kotlin-monads`):

1. **Left identity**: `returns(x) bind { f(it) }` should be equivalent to `f(x)`
 
2. **Right identity**: `m bind { returns(it) }` should be equivalent to `m`

3. **Associativity**: `m bind { f(it) } bind { g(it) }` should be equivalent to `m bind { f(it) bind { g(it) } }`

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

With the power of Kotlin coroutines, we can even have an equivalent of the [*Haskell do notation*](https://en.wikibooks.org/wiki/Haskell/do_notation):

Simple example that performs a monadic list nondeterministic expansion:

    val m = doReturning(MonadListReturn) {
        val x = bind(monadListOf(1, 2, 3))
        val y = bind(monadListOf(x, x + 1))
        monadListOf(y, x * y)
    }
    
    assertEquals(monadListOf(1, 2, 1, 2, 2, 3, 2, 3, 3, 4, 3, 4), m)
    
Or applied to an existing monad for convenience:

    val m = monadListOf(1, 2, 3).bindDo { x ->
        val y = bind(monadListOf(x, x + 1))
        monadListOf(y, x * y)
    }
    
This is effectively equivalent to the following code written with only simple `bind`:

    val m = monadListOf(1, 2, 3).bind { x ->
        monadListOf(x, x + 1).bind { y -> 
            monadList(y, x * y)
        }
    }
    
Note that, with simple `bind`, each *transformation* requires another inner scope if it uses the variables bound outside, 
which would lead to some kind of callback hell. 
This problem is effectively solved using the Kotlin coroutines: the compiler performs the CPS transformation of a plain
 code block under the hood. However, this coroutines use case is somewhat out of conventions: it might resume the same continuation
 several times and uses quite a dirty hack to do that.
    
The result type parameter (`R` in `Monad<M, R>`) is usually inferred, and the compiler controls the flow inside a *do block*, but still you need to
 downcast the `Monad<M, R>` to your actual monad type (e.g. `Monad<Maybe<*>, R>` to `Maybe`), because the type system doesn't seem to allow this to be done
 automatically (if you know a way, please tell me).
 
 **Be careful with mutable state** in _do_ blocks, since all continuation calls will share it, sometimes resulting into counter-intuitive results:
 
     val m = doReturning(MonadListReturn) {
         for (i in 1..10)
             bind(monadListOf(0, 0))
         returns(0)
     } as MonadList
     
 One would expect 1024 items here, but the result only contains 11! That's because `i` is mutable and is shared between all the calls that `bind` makes.
 
 
 
 
