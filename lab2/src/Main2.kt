//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
typealias Fun<A, B> = (A) -> B
typealias Fun2<A, B, C> = (A, B) -> C
data class World(val x: Int, val y: Int)
data class Arrow( val Source:World, val Target:World)
typealias setOfWorld = MutableSet<World>
typealias setOfArrow = MutableSet<Arrow>

lateinit var Xglobal: setOfWorld

fun <A, B, C> curry(f: Fun2<A, B, C>): (A) -> (B) -> C = { a -> { b -> f(a, b) } }

val refl: Fun2<setOfWorld, setOfArrow, setOfArrow> = { X, R ->
    val result = R.toMutableSet()
    X.forEach { world ->
        result.add(Arrow(world, world))
    }
    result
}

fun reflexiva(X: setOfWorld) = curry(refl)(X)

val simetrica: Fun<setOfArrow, setOfArrow> = { R ->
    val result = R.toMutableSet()
    R.forEach { arrow ->
        result.add(Arrow(arrow.Target, arrow.Source))
    }
    result
}

val tranz: Fun2<setOfWorld, setOfArrow, setOfArrow> = { X, R ->
    val result = R.toMutableSet()
    val listX = X.toList() // Convertim în listă pentru a avea indexare (i, j, k)

    // j este nodul intermediar (nodul pivot)
    for (j in listX) {
        // i este nodul de start
        for (i in listX) {
            // k este nodul de sfârșit
            for (k in listX) {

                // Verificăm dacă există deja drum i -> j și j -> k
                val existsIJ = result.any { it.Source == i && it.Target == j }
                val existsJK = result.any { it.Source == j && it.Target == k }

                if (existsIJ && existsJK) {
                    result.add(Arrow(i, k))
                }
            }
        }
    }
    result
}

fun tranzitiva(X: setOfWorld) = curry(tranz)(X)

val reflSimTranz: Fun<setOfArrow, setOfArrow> = { R ->
    tranzitiva(Xglobal)(
        simetrica(
            reflexiva(Xglobal)(R)
        )
    )
}

fun main() {
    val w1 = World(1, 10)
    val w2 = World(2, 20)
    val w3 = World(3, 30)
    val w4 = World(4, 40)

    val multime: setOfWorld = mutableSetOf(w1, w2, w3, w4)
    val relatie: setOfArrow = mutableSetOf(
        Arrow(w1, w2),
        Arrow(w2, w3),
        Arrow(w3, w4)
    )

    Xglobal = multime

    val funcReflexiva = reflexiva(multime)
    val funcTranzitiva = tranzitiva(multime)

    println("Relația inițială: $relatie")

    val rReflexiva = funcReflexiva(relatie)
    println("După reflexivitate: $rReflexiva")

    val rSimetrica = simetrica(relatie)
    println("După simetrie: $rSimetrica")

    val rTranzitiva = funcTranzitiva(relatie)
    println("După tranzitivitate: $rTranzitiva")

    val rezultatFinal = reflSimTranz(relatie)
    println("Rezultat închidere echivalență: $rezultatFinal")
}