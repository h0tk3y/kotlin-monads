class State<S, out T>(val runState: (S) -> Pair<S, T>) : Monad<State<S, *>, T> {
    override fun <R> bind(f: Binder<State<S, *>, T, R>): State<S, R> = State { s ->
        val (newState, value) = runState(s)
        val state = f(stateReturn(), value) as State
        state.runState(newState)
    }
}

fun <S> stateReturn(): Return<State<S, *>> = object : Return<State<S, *>> {
    override fun <T> returns(t: T) = State<S, T> { s -> Pair(s, t) }
}

fun <S> getState(): State<S, S> = State { s -> Pair(s, s) }
fun <S> putState(newState: S) = State<S, Unit> { Pair(newState, Unit) }