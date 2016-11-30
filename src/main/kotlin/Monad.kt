interface Return<M> {
    fun <T> returns(t: T): Monad<M, T>
}

typealias Binder<M, T, R> = Return<M>.(T) -> Monad<M, R>

interface Monad<This, out T> {
    infix fun <R> bind(f: Binder<This, T, R>): Monad<This, R>
}

infix fun <M : Monad<M, *>, T, R> Monad<M, T>.then(t: Monad<M, R>) = bind { t }

infix fun <T, R, M : Monad<M, *>> Monad<M, T>.map(f: (T) -> R) = bind { returns(f(it)) }

tailrec fun <T, M : Monad<M, *>> Monad<M, T>.bindTimes(times: Int, f: Binder<M, T, T>): Monad<M, T> =
        if (times <= 0)
            this else
            bind(f).bindTimes(times - 1, f)