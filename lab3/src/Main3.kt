data class World(
    var Nume: String,
    val x: Int,
    val y: Int,
    val p: MutableSet<String>,
    var v: Boolean
)

data class Propozitie(
    var Nume: String,
    val w: World,
    var v: Boolean,
    var ind: Int = 0
)

data class Arrow(val Source: World, val Target: World)

typealias setOfWorld = MutableSet<World>
typealias setOfArrow = MutableSet<Arrow>

class KripkeModel(_currentWorld: World, _frame: setOfArrow) {
    val currentWorld = _currentWorld
    val frame = _frame
    var v = true

    fun nextWorlds(): setOfWorld {
        val wp = frame
            .filter { it.Source.Nume == currentWorld.Nume }
            .map { it.Target }
            .toMutableSet()
        return wp
    }
}

interface Formula
class Prop(val value: Propozitie) : Formula
class sau(val left: Formula, val right: Formula) : Formula
class si(val left: Formula, val right: Formula) : Formula
class non(val elem: Formula) : Formula
class nc(val elem: Formula) : Formula
class ps(val elem: Formula) : Formula

fun valoarePropozitieInLume(nume: String, lume: World): Boolean {
    return nume in lume.p
}

operator fun Propozitie.plus(other: Propozitie): Propozitie {
    return Propozitie(
        "(${this.Nume}∨${other.Nume})",
        this.w,
        this.v || other.v
    )
}

operator fun Propozitie.times(other: Propozitie): Propozitie {
    return Propozitie(
        "(${this.Nume}∧${other.Nume})",
        this.w,
        this.v && other.v
    )
}

operator fun Propozitie.not(): Propozitie {
    return Propozitie(
        "¬(${this.Nume})",
        this.w,
        !this.v
    )
}

lateinit var currentModelForUnary: KripkeModel

operator fun Propozitie.unaryPlus(): Propozitie {
    val worlds = currentModelForUnary.nextWorlds()
    val rez = if (worlds.isEmpty()) {
        true
    } else {
        worlds.all { valoarePropozitieInLume(this.Nume, it) }
    }

    return Propozitie("□(${this.Nume})", this.w, rez)
}

operator fun Propozitie.unaryMinus(): Propozitie {
    val worlds = currentModelForUnary.nextWorlds()
    val rez = if (worlds.isEmpty()) {
        false
    } else {
        worlds.any { valoarePropozitieInLume(this.Nume, it) }
    }

    return Propozitie("◇(${this.Nume})", this.w, rez)
}

fun KripkeModel.eval(e: Formula): Propozitie {
    return when (e) {
        is Prop -> {
            Propozitie(
                e.value.Nume,
                this.currentWorld,
                valoarePropozitieInLume(e.value.Nume, this.currentWorld)
            )
        }

        is sau -> {
            val st = this.eval(e.left)
            val dr = this.eval(e.right)
            st + dr
        }

        is si -> {
            val st = this.eval(e.left)
            val dr = this.eval(e.right)
            st * dr
        }

        is non -> {
            val rez = this.eval(e.elem)
            !rez
        }

        is nc -> {
            val worlds = this.nextWorlds()
            val rez = if (worlds.isEmpty()) {
                true
            } else {
                worlds.all { lume ->
                    val kmNou = KripkeModel(lume, this.frame)
                    kmNou.eval(e.elem).v
                }
            }
            Propozitie("□(...)", this.currentWorld, rez)
        }

        is ps -> {
            val worlds = this.nextWorlds()
            val rez = if (worlds.isEmpty()) {
                false
            } else {
                worlds.any { lume ->
                    val kmNou = KripkeModel(lume, this.frame)
                    kmNou.eval(e.elem).v
                }
            }
            Propozitie("◇(...)", this.currentWorld, rez)
        }

        else -> Propozitie("eroare", this.currentWorld, false)
    }
}

fun main() {
    println("=== EXERCITIUL 2 ===")

    val spEx2 = mutableSetOf("p1", "p2")
    val wEx2 = World("w1", 1, 10, spEx2, true)

    val p1Ex2 = Propozitie("p1", wEx2, valoarePropozitieInLume("p1", wEx2))
    val p2Ex2 = Propozitie("p2", wEx2, valoarePropozitieInLume("p2", wEx2))
    val p3Ex2 = Propozitie("p3", wEx2, valoarePropozitieInLume("p3", wEx2))
    val p4Ex2 = Propozitie("p4", wEx2, valoarePropozitieInLume("p4", wEx2))

    val e1 = (p1Ex2 * p2Ex2) + (p2Ex2 * !p3Ex2)
    val e2 = !((p1Ex2 + !p2Ex2) + p3Ex2) + (p4Ex2 * !p4Ex2)
    val e3 = (p1Ex2 * p2Ex2) * (!p1Ex2 + !p2Ex2)
    val e4 = (p1Ex2 + p2Ex2) + (!p1Ex2 * !p2Ex2)

    println("1. ${e1.Nume} = ${e1.v}")
    println("2. ${e2.Nume} = ${e2.v}")
    println("3. ${e3.Nume} = ${e3.v}")
    println("4. ${e4.Nume} = ${e4.v}")

    println()
    println("=== EXERCITIUL 3 ===")

    val sp1 = mutableSetOf("p1", "p2")
    val sp2 = mutableSetOf("p3")
    val sp3 = mutableSetOf("p3", "p2")
    val sp4 = mutableSetOf("p4", "p3")

    val w1 = World("w1", 1, 10, sp1, true)
    val w2 = World("w2", 2, 20, sp2, true)
    val w3 = World("w3", 3, 30, sp3, true)
    val w4 = World("w4", 4, 40, sp4, true)

    val p1 = Propozitie("p1", w1, valoarePropozitieInLume("p1", w1))
    var p2 = Propozitie("p2", w1, valoarePropozitieInLume("p2", w1))
    val p3 = Propozitie("p3", w1, valoarePropozitieInLume("p3", w1))
    val p4 = Propozitie("p4", w1, valoarePropozitieInLume("p4", w1))

    val w1_w2 = Arrow(w1, w2)
    val w2_w3 = Arrow(w2, w3)
    val w3_w4 = Arrow(w3, w4)

    val relatie: setOfArrow = mutableSetOf(w1_w2, w2_w3, w3_w4)
    val KM = KripkeModel(w1, relatie)

    currentModelForUnary = KM

    val formula1: Formula =
        sau(
            si(
                non(Prop(p1)),
                nc(Prop(p2))
            ),
            ps(
                si(
                    non(Prop(p4)),
                    Prop(p3)
                )
            )
        )

    val rez1 = KM.eval(formula1)
    println("Formula 1 = ${rez1.v}")

    var forms: Formula = Prop(p1)
    forms = sau(forms, Prop(p2))
    val formd: Formula = nc(si(non(Prop(p2)), Prop(p3)))
    val form = sau(non(forms), formd)

    val rez2 = KM.eval(form)
    println("Formula 2 = ${rez2.v}")

    println()
    println("=== TEST OPERATORI MODALI + SI - ===")
    val necesarP2 = +p2
    val posibilP3 = -p3
    println("${necesarP2.Nume} = ${necesarP2.v}")
    println("${posibilP3.Nume} = ${posibilP3.v}")
}