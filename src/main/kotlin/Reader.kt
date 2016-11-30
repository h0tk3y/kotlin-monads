class Reader<E, out T>(val runReader: (E) -> T) : Monad<Reader<E, *>, T> {
    override fun <R> bind(f: Binder<Reader<E, *>, T, R>): Reader<E, R> = Reader { a ->
        val t = runReader(a)
        val r = f(readerReturn(), t) as Reader
        r.runReader(a)
    }
}

fun <E> readerReturn() = object : Return<Reader<E, *>> {
    override fun <T> returns(t: T) = Reader<E, T> { t }
}

fun <T> ask() = Reader<T, T> { it }

fun <C, T> local(reader: Reader<C, T>, modifyEnvironment: (C) -> C) =
        Reader<C, T> { environment ->
            reader.runReader(modifyEnvironment(environment))
        }