data class Monom(val coef: Int, val exx: Int, val exy: Int)

typealias Polinom = MutableList<Monom>

fun Polinom.reducere(): Polinom =
    this
        .groupBy { Pair(it.exx, it.exy) }
        .mapNotNull { (cheie, monoame) ->
            val coefTotal = monoame
                .map { it.coef }
                .reduce { acc, c -> acc + c }

            if (coefTotal != 0) Monom(coefTotal, cheie.first, cheie.second) else null
        }
        .toMutableList()

infix fun Polinom.plus(other: Polinom): Polinom {
    val rezultat = mutableListOf<Monom>()
    rezultat.addAll(this)
    rezultat.addAll(other)
    return rezultat.reducere()
}

infix fun Polinom.ori(other: Polinom): Polinom {
    val rezultat = mutableListOf<Monom>()

    for (m1 in this) {
        for (m2 in other) {
            rezultat.add(
                Monom(
                    m1.coef * m2.coef,
                    m1.exx + m2.exx,
                    m1.exy + m2.exy
                )
            )
        }
    }

    return rezultat.reducere()
}

fun Polinom.afisare() = joinToString("+") { "${it.coef}x^${it.exx}y^${it.exy}" }

fun main() {
    val P: Polinom = mutableListOf(
        Monom(2, 2, 1),
        Monom(5, 0, 2),
        Monom(3, 1, 0),
        Monom(2, 1, 1)
    )

    val Q: Polinom = mutableListOf(
        Monom(7, 2, 3),
        Monom(1, 1, 2),
        Monom(2, 0, 2),
        Monom(3, 1, 1)
    )

    println("P(x,y) = ${P.afisare()}")
    println("Q(x,y) = ${Q.afisare()}")

    val suma = P plus Q
    println("P + Q = ${suma.afisare()}")

    val produs = P ori Q
    println("P * Q = ${produs.afisare()}")
}