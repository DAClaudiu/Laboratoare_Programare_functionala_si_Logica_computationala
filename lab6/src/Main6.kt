interface ImultiSet<T> {
    val counts: Map<T, Int>

    fun add(item: T, amount: Int = 1): MultiSet<T>
    fun union(other: MultiSet<T>): MultiSet<T>
    fun isSubMultisetOf(other: MultiSet<T>): Boolean
    fun subtract(other: MultiSet<T>): MultiSet<T>

    companion object {
        fun <T> singleton(x: T): MultiSet<T> =
            MultiSet(mapOf(x to 1))

        fun <T> empty(): MultiSet<T> =
            MultiSet(emptyMap())
    }
}

data class MultiSet<T>(
    override val counts: Map<T, Int> = emptyMap()
) : ImultiSet<T> {

    init {
        require(counts.values.all { it >= 0 }) {
            "Numarul de aparitii nu poate fi negativ."
        }
    }

    private fun clean(map: Map<T, Int>): MultiSet<T> =
        MultiSet(map.filterValues { it > 0 })

    override fun add(item: T, amount: Int): MultiSet<T> {
        require(amount >= 0) {
            "Amount trebuie sa fie >= 0."
        }

        val newCount = counts.getOrDefault(item, 0) + amount
        return clean(counts + (item to newCount))
    }

    override fun union(other: MultiSet<T>): MultiSet<T> {
        val result = counts.toMutableMap()

        for ((item, amount) in other.counts) {
            result[item] = result.getOrDefault(item, 0) + amount
        }

        return clean(result)
    }

    override fun isSubMultisetOf(other: MultiSet<T>): Boolean {
        for ((item, amount) in counts) {
            if (amount > other.counts.getOrDefault(item, 0)) {
                return false
            }
        }

        return true
    }

    override fun subtract(other: MultiSet<T>): MultiSet<T> {
        require(other.isSubMultisetOf(this)) {
            "Nu se poate scadea un multiset care nu este sub-multiset."
        }

        val result = counts.toMutableMap()

        for ((item, amount) in other.counts) {
            result[item] = result.getOrDefault(item, 0) - amount
        }

        return clean(result)
    }

    fun repeated(times: Int): MultiSet<T> {
        require(times >= 0) {
            "times trebuie sa fie >= 0."
        }

        return clean(counts.mapValues { it.value * times })
    }

    fun <R> map(f: (T) -> R): MultiSet<R> {
        val result = mutableMapOf<R, Int>()

        for ((item, amount) in counts) {
            val mappedItem = f(item)
            result[mappedItem] = result.getOrDefault(mappedItem, 0) + amount
        }

        return MultiSet(result)
    }

    override fun toString(): String {
        if (counts.isEmpty()) return "∅"

        return counts.entries
            .sortedBy { it.key.toString() }
            .joinToString(" + ") { (item, amount) ->
                if (amount == 1) "$item" else "${amount}$item"
            }
    }

    companion object {
        fun <T> singleton(x: T): MultiSet<T> =
            MultiSet(mapOf(x to 1))

        fun <T> empty(): MultiSet<T> =
            MultiSet(emptyMap())
    }
}

data class Transition<T>(
    val input: MultiSet<T>,
    val output: MultiSet<T>
)

fun <T> fire(
    transition: Transition<T>,
    marking: MultiSet<T>
): MultiSet<T> {
    return if (transition.input.isSubMultisetOf(marking)) {
        marking.subtract(transition.input).union(transition.output)
    } else {
        marking
    }
}

data class Petri<T>(
    val transitions: List<Transition<T>>,
    val result: MultiSet<T>
)

fun <A, B> Petri<A>.map(f: (A) -> B): Petri<B> {
    val mappedTransitions = transitions.map { transition ->
        Transition(
            input = transition.input.map(f),
            output = transition.output.map(f)
        )
    }

    return Petri(
        transitions = mappedTransitions,
        result = result.map(f)
    )
}

fun <T> pure(x: T): Petri<T> {
    return Petri(
        transitions = emptyList(),
        result = MultiSet.singleton(x)
    )
}

private fun <T> flattenMultiSet(
    nested: MultiSet<Petri<T>>
): MultiSet<T> {
    var result = MultiSet.empty<T>()

    for ((petri, amount) in nested.counts) {
        result = result.union(petri.result.repeated(amount))
    }

    return result
}

fun <T> flatten(
    nested: Petri<Petri<T>>
): Petri<T> {
    val petriTokens = mutableSetOf<Petri<T>>()

    for (petri in nested.result.counts.keys) {
        petriTokens.add(petri)
    }

    for (transition in nested.transitions) {
        petriTokens.addAll(transition.input.counts.keys)
        petriTokens.addAll(transition.output.counts.keys)
    }

    val innerTransitions = petriTokens.flatMap { it.transitions }

    val outerTransitions = nested.transitions.map { transition ->
        Transition(
            input = flattenMultiSet(transition.input),
            output = flattenMultiSet(transition.output)
        )
    }

    return Petri(
        transitions = innerTransitions + outerTransitions,
        result = flattenMultiSet(nested.result)
    )
}

fun <A, B> Petri<A>.flatMap(
    f: (A) -> Petri<B>
): Petri<B> {
    return flatten(this.map(f))
}

fun <A, B, C> kleisliCompose(
    f: (A) -> Petri<B>,
    g: (B) -> Petri<C>
): (A) -> Petri<C> {
    return { x: A ->
        f(x).flatMap(g)
    }
}

fun <T> fireParallel(
    transitions: List<Transition<T>>,
    marking: MultiSet<T>
): MultiSet<T> {
    val selectedTransitions = mutableListOf<Transition<T>>()
    var available = marking

    for (transition in transitions) {
        if (transition.input.isSubMultisetOf(available)) {
            selectedTransitions.add(transition)
            available = available.subtract(transition.input)
        }
    }

    var produced = MultiSet.empty<T>()

    for (transition in selectedTransitions) {
        produced = produced.union(transition.output)
    }

    return available.union(produced)
}

enum class Place {
    P1, P2, P3, P4, P5, P6, P7
}

fun main() {
    val t1 = Transition(
        input = MultiSet.singleton(Place.P1),
        output = MultiSet.singleton(Place.P2)
    )

    val t2 = Transition(
        input = MultiSet.singleton(Place.P2),
        output = MultiSet(
            mapOf(
                Place.P3 to 1,
                Place.P4 to 1
            )
        )
    )

    val t3 = Transition(
        input = MultiSet.singleton(Place.P3),
        output = MultiSet.singleton(Place.P5)
    )

    val t4 = Transition(
        input = MultiSet.singleton(Place.P4),
        output = MultiSet.singleton(Place.P6)
    )

    val t5 = Transition(
        input = MultiSet(
            mapOf(
                Place.P5 to 1,
                Place.P6 to 1
            )
        ),
        output = MultiSet.singleton(Place.P7)
    )

    val transitions = listOf(t1, t2, t3, t4, t5)

    var marking = MultiSet(
        mapOf(
            Place.P1 to 1,
            Place.P2 to 1,
            Place.P3 to 1,
            Place.P6 to 1
        )
    )

    println("Marcaj initial:")
    println(marking)

    println()
    println("Fire secvential cu t1:")
    println(fire(t1, marking))

    println()
    println("Fire parallel - pasul 1:")
    marking = fireParallel(transitions, marking)
    println(marking)

    println()
    println("Fire parallel - pasul 2:")
    marking = fireParallel(transitions, marking)
    println(marking)

    println()
    println("Test pure:")
    val p = pure(Place.P1)
    println(p)

    println()
    println("Test map:")
    val mapped = p.map { it.name }
    println(mapped)

    println()
    println("Test flatMap:")
    val flatMapped = p.flatMap { place ->
        Petri(
            transitions = emptyList(),
            result = MultiSet.singleton(place.name)
        )
    }
    println(flatMapped)

    println()
    println("Test Kleisli compose:")
    val f: (Place) -> Petri<Place> = { place ->
        Petri(
            transitions = emptyList(),
            result = MultiSet.singleton(place)
        )
    }

    val g: (Place) -> Petri<String> = { place ->
        Petri(
            transitions = emptyList(),
            result = MultiSet.singleton("Locul_$place")
        )
    }

    val composed = kleisliCompose(f, g)
    println(composed(Place.P1))
}