typealias Fun<A, B> = (A) -> B

infix fun <A, B> A.pipe(f: (A) -> B): B = f(this)


// MONADA STATE


data class State<S, A>(
    val run: (S) -> Pair<A, S>
)

fun <S, A, B> State<S, A>.map(
    f: (A) -> B
): State<S, B> =
    State { s ->
        val (a, newState) = this.run(s)
        Pair(f(a), newState)
    }


fun <S, A, B> State<S, A>.flatMap(
    f: (A) -> State<S, B>
): State<S, B> =
    State { s ->
        val (a, s1) = this.run(s)
        f(a).run(s1)
    }

fun <S, A> pure(a: A): State<S, A> =
    State { s -> Pair(a, s) }

fun <S> get(): State<S, S> =
    State { s -> Pair(s, s) }

fun <S> put(newState: S): State<S, Unit> =
    State { _ -> Pair(Unit, newState) }

fun <S> modify(
    f: (S) -> S
): State<S, Unit> =
    State { s ->
        Pair(Unit, f(s))
    }


// ===========================
// DFA
// ===========================

class DFA<S, A>(
    val states: Set<S>,
    val alphabet: Set<A>,
    val initialState: S,
    val acceptingStates: Set<S>,
    val transitionTable: Map<Pair<S, A>, S>
) {

    init {
        require(initialState in states)
        require(acceptingStates.all { it in states })
    }

    // 1)
    private fun delta(
        state: S,
        symbol: A
    ): S {
        return transitionTable[state to symbol]
            ?: error("Tranzitie invalida")
    }

    // 2)
    private fun step(
        symbol: A
    ): State<S, Unit> =
        State { currentState ->
            val next = delta(currentState, symbol)
            Pair(Unit, next)
        }

    // 3)
    private fun process(
        input: List<A>
    ): State<S, Unit> {

        return input.fold(
            pure<S, Unit>(Unit)
        ) { acc, symbol ->

            acc.flatMap {
                step(symbol)
            }
        }
    }

    // 4)
    fun accepts(
        input: List<A>
    ): Boolean {

        val (_, finalState) =
            process(input).run(initialState)

        return finalState in acceptingStates
    }
}


// ===========================
// TEST DFA
// ===========================

enum class AlfaState {
    q1, q2, q3
}

val transitionTableAlfa = mapOf(
    (AlfaState.q1 to 'a') to AlfaState.q1,
    (AlfaState.q1 to 'b') to AlfaState.q2,
    (AlfaState.q1 to 'c') to AlfaState.q3,
    (AlfaState.q2 to 'b') to AlfaState.q1,
    (AlfaState.q3 to 'a') to AlfaState.q3
)

val alfaDFA = DFA(
    states = setOf(
        AlfaState.q1,
        AlfaState.q2,
        AlfaState.q3
    ),
    alphabet = setOf('a', 'b', 'c'),
    initialState = AlfaState.q1,
    acceptingStates = setOf(
        AlfaState.q2,
        AlfaState.q3
    ),
    transitionTable = transitionTableAlfa
)


// ===========================
// AUTOMAT MEALY
// ===========================

class MealyMachine<S, A, O>(

    val states: Set<S>,
    val alphabet: Set<A>,
    val initialState: S,

    val transitionTable:
    Map<Pair<S, A>, Pair<S, O>>
) {

    // 6)
    private fun delta(
        state: S,
        symbol: A
    ): Pair<S, O> {

        return transitionTable[state to symbol]
            ?: error("Tranzitie invalida")
    }

    // 7)
    fun step(
        symbol: A
    ): State<S, O> =

        State { currentState ->

            val (nextState, output) =
                delta(currentState, symbol)

            Pair(output, nextState)
        }

    // 8)
    fun process(
        input: List<A>
    ): Pair<List<O>, S> {

        val initial =
            pure<S, List<O>>(emptyList())

        val computation =
            input.fold(initial) { acc, symbol ->

                acc.flatMap { outputs ->

                    step(symbol).map { out ->
                        outputs + out
                    }
                }
            }

        return computation.run(initialState)
    }
}


// ===========================
// 9) COMPUNERE KLEISLI
// ===========================

infix fun <S1, S2, A, B, C>
        MealyMachine<S1, A, B>.fish(
    other: MealyMachine<S2, B, C>
): MealyMachine<Pair<S1, S2>, A, C> {

    val newStates =
        mutableSetOf<Pair<S1, S2>>()

    for (s1 in this.states) {
        for (s2 in other.states) {
            newStates.add(s1 to s2)
        }
    }

    val newTransitionTable =
        mutableMapOf<
                Pair<Pair<S1, S2>, A>,
                Pair<Pair<S1, S2>, C>
                >()

    for (statePair in newStates) {

        val (s1, s2) = statePair

        for (a in this.alphabet) {

            val (nextS1, b) =
                this.transitionTable[s1 to a]
                    ?: continue

            val (nextS2, c) =
                other.transitionTable[s2 to b]
                    ?: continue

            newTransitionTable[
                statePair to a
            ] =
                (nextS1 to nextS2) to c
        }
    }

    return MealyMachine(
        states = newStates,
        alphabet = this.alphabet,
        initialState =
            this.initialState to other.initialState,
        transitionTable = newTransitionTable
    )
}


// ===========================
// TEST MEALY
// ===========================

val myMealy1 =
    MealyMachine(
        states = setOf("s1", "s2"),
        alphabet = setOf("i1", "i2"),
        initialState = "s1",
        transitionTable = mapOf(
            ("s1" to "i1") to ("s1" to "o1"),
            ("s1" to "i2") to ("s2" to "o2"),
            ("s2" to "i2") to ("s2" to "o1"),
            ("s2" to "i1") to ("s1" to "o2")
        )
    )

val myMealy2 =
    MealyMachine(
        states = setOf("s"),
        alphabet = setOf("o1", "o2"),
        initialState = "s",
        transitionTable = mapOf(
            ("s" to "o1") to ("s" to "UNU"),
            ("s" to "o2") to ("s" to "DOI")
        )
    )


// ===========================
// MAIN
// ===========================

fun main() {

    println("===== DFA =====")

    println(
        alfaDFA.accepts(
            listOf('a', 'a', 'b')
        )
    )

    println(
        alfaDFA.accepts(
            listOf('c', 'a', 'a')
        )
    )

    println(
        alfaDFA.accepts(
            listOf('b')
        )
    )

    println(
        alfaDFA.accepts(
            listOf('a', 'b', 'b')
        )
    )


    println("\n===== MEALY 1 =====")

    val result1 =
        myMealy1.process(
            listOf("i1", "i2", "i2")
        )

    println(result1)


    println("\n===== COMPUNERE =====")

    val composed =
        myMealy1 fish myMealy2

    val result2 =
        composed.process(
            listOf("i1", "i2", "i2")
        )

    println(result2)
}