data class MonadList<out T>(val list: List<T>) : Monad<MonadList<*>, T>, List<T> by list {
    override fun <R> bind(f: Binder<MonadList<*>, T, R>): MonadList<R> =
            MonadList(list.flatMap { f(MonadListReturn, it) as MonadList })
}

object MonadListReturn : Return<MonadList<*>> {
    override fun <T> returns(t: T) = MonadList(listOf(t))
}

fun <T> monadListOf(vararg items: T) = MonadList(listOf(*items))

fun <T> emptyMonadList() = MonadList(emptyList<T>())