sealed class Either<F, out T> : Monad<Either<F, *>, T> {
    class Left<F>(val leftValue: F) : Either<F, Nothing>()
    class Right<T>(val rightValue: T) : Either<Nothing, T>()

    @Suppress("UNCHECKED_CAST")
    override fun <R> bind(f: Binder<Either<F, *>, T, R>): Either<F, R> = when (this) {
        is Right -> f(eitherWrapping(), rightValue) as Either
        is Left -> this
    }
}

fun <F> eitherWrapping(): Return<Either<F, *>> = object : Return<Either<F, *>> {
    @Suppress("UNCHECKED_CAST")
    override fun <T> returns(t: T) = Either.Right(t) as Either<F, T>
}