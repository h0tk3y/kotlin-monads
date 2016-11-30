data class Writer<T>(val result: T, val log: String) : Monad<Writer<*>, T> {
    override fun <R> bind(f: Return<Writer<*>>.(T) -> Monad<Writer<*>, R>): Writer<R> {
        val w = f(WriterReturn, result) as Writer
        return Writer(w.result, log + w.log)
    }
}

object WriterReturn : Return<Writer<*>> {
    override fun <T> returns(t: T): Monad<Writer<*>, T> = Writer(t, "")
}

fun tell(s: String): Writer<Unit> = Writer(Unit, s)
fun <T> listen(w: Writer<T>): Writer<Pair<T, String>> = Writer(w.result to w.log, w.log)
fun <T> censor(w: Writer<T>, f: (String) -> String) = Writer(w.result, f(w.log))
fun <T> pass(w: Writer<Pair<T, (String) -> String>>) = Writer(w.result.first, w.result.second(w.log))