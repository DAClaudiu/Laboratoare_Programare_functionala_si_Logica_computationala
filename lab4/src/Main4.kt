class Obj<T>(
    val name: String,
    val elements: Set<T>
){
    override fun toString() = name
}

class Mor<A,B>(
    val name: String,
    val from: Obj<A>,
    val to: Obj<B>,
    val f: (A)->B
){
    fun apply(a:A) = f(a)
    override fun toString() =
        "$name : ${from.name} → ${to.name}"
}

class Category{
    val objects = mutableListOf<Obj<*>>()
    val morphisms = mutableListOf<Mor<*,*>>()
    fun <T> add(o:Obj<T>):Obj<T>{
        objects.add(o)
        return o
    }
    fun <A,B> add(m:Mor<A,B>):Mor<A,B>{
        morphisms.add(m)
        return m
    }

    fun describe(){
        println("Objects:")
        objects.forEach{println(it)}
        println("\nMorphisms:")
        morphisms.forEach{println(it)}
    }
}

fun <T> obj(name:String,vararg elems:T) =
    Obj(name,elems.toSet())

fun <A,B> Obj<A>.arrow(
    name:String,
    target:Obj<B>,
    f:(A)->B
)= Mor(name,this,target,f)

// a)
infix fun <A, B, C> Mor<A, B>.before(g: Mor<B, C>): Mor<A, C> =
    Mor("${this.name}_${g.name}", this.from, g.to) { a -> g.f(this.f(a)) }

// b)
fun <A, B> Mor<A, B>.equalsOnDomain(other: Mor<A, B>): Boolean {
    if (this.from.elements != other.from.elements) return false
    return this.from.elements.all { this.f(it) == other.f(it) }
}

// c)
fun <A, B> Mor<A, B>.isMono(): Boolean {
    val domainElements = this.from.elements.toList()
    for (i in domainElements.indices) {
        for (j in i + 1 until domainElements.size) {
            if (this.f(domainElements[i]) == this.f(domainElements[j])) return false
        }
    }
    return true
}

// d)
fun <A, B> Mor<A, B>.isEpi(): Boolean = this.image().size == this.to.elements.size

// e)
fun <A, B> Mor<A, B>.image(): Set<B> = this.from.elements.map { this.f(it) }.toSet()

// f)
fun <A, B, C> Category.pullback(f: Mor<A, C>, g: Mor<B, C>, name: String = "PB"): Obj<Pair<A, B>> {
    val pbElements = mutableSetOf<Pair<A, B>>()
    for (a in f.from.elements) {
        for (b in g.from.elements) {
            if (f.f(a) == g.f(b)) pbElements.add(Pair(a, b))
        }
    }
    val pbObj = this.add(Obj(name, pbElements))
    this.add(pbObj.arrow("p1", f.from) { it.first })
    this.add(pbObj.arrow("p2", g.from) { it.second })
    return pbObj
}


// g) Pushout
fun <C, A, B> Category.pushout(
    f: Mor<C, A>,
    g: Mor<C, B>,
    name: String = "PO"
): Obj<Set<Any?>> {
    val disjointUnion = mutableSetOf<Any?>()

    f.to.elements.forEach { disjointUnion.add("A" to it) }
    g.to.elements.forEach { disjointUnion.add("B" to it) }

    val parent = mutableMapOf<Any?, Any?>()

    fun find(i: Any?): Any? {
        if (parent[i] == null) parent[i] = i
        if (parent[i] != i) {
            parent[i] = find(parent[i])
        }
        return parent[i]
    }

    fun union(i: Any?, j: Any?) {
        val rootI = find(i)
        val rootJ = find(j)
        if (rootI != rootJ) parent[rootI] = rootJ
    }

    f.from.elements.forEach { z ->
        union("A" to f.f(z), "B" to g.f(z))
    }

    if (f.to.elements == g.to.elements) {
        f.to.elements.forEach { x ->
            union("A" to x, "B" to x)
        }
    }

    val groups = disjointUnion.groupBy { find(it) }
    val poElements = groups.values.map { it.toSet() }.toSet()
    val poObj = this.add(Obj(name, poElements))

    this.add(f.to.arrow("i1", poObj) { a ->
        poElements.first { it.contains("A" to a) }
    })

    this.add(g.to.arrow("i2", poObj) { b ->
        poElements.first { it.contains("B" to b) }
    })

    return poObj
}

fun main() {
    val cat = Category()

    val A = cat.add(obj("A", "student1", "student2", "student3"))
    val B = cat.add(obj("B", "nota9", "nota10", "nota8"))
    val C = cat.add(obj("C", "Aprobat", "Respins"))

    val f = cat.add(A.arrow("f", C) {
        if (it == "student3") "Respins" else "Aprobat"
    })
    val g = cat.add(B.arrow("g", C) {
        if (it == "nota8") "Respins" else "Aprobat"
    })

    println("--- Testare Pullback ---")
    val pb = cat.pullback(f, g)
    println("Obiect Pullback ${pb.name} elemente: ${pb.elements}")

    println("\n--- Testare Pushout (Span: A <- C -> B) ---")

    val sursa = cat.add(obj("Sursa", 1, 2))
    val catA = cat.add(obj("CatA", "Portocala", "Lamaie"))
    val catB = cat.add(obj("CatB", "Dulce", "Acru"))

    val f_po = cat.add(sursa.arrow("f_po", catA) { if (it == 1) "Portocala" else "Lamaie" })
    val g_po = cat.add(sursa.arrow("g_po", catB) { if (it == 1) "Dulce" else "Acru" })

    val po = cat.pushout(f_po, g_po)
    println("Obiect Pushout ${po.name} număr clase: ${po.elements.size}")

    println("\n--- Verificare Proprietăți ---")
    println("f_po este mono? ${f_po.isMono()}")
    println("f_po este epi? ${f_po.isEpi()}")

    println("\n--- h) Pushout pentru graful G ---")

    val V = cat.add(obj("V", "v", "w", "x", "y", "z"))
    val Arce = cat.add(obj("A", "f", "g", "h", "i", "j", "k"))

    val src = cat.add(
        Arce.arrow("src", V) {
            when (it) {
                "f" -> "v"
                "g" -> "w"
                "h" -> "w"
                "i" -> "y"
                "j" -> "y"
                "k" -> "z"
                else -> "?"
            }
        }
    )

    val tgt = cat.add(
        Arce.arrow("tgt", V) {
            when (it) {
                "f" -> "w"
                "g" -> "x"
                "h" -> "x"
                "i" -> "y"
                "j" -> "v"
                "k" -> "y"
                else -> "?"
            }
        }
    )

    val poGraph = cat.pushout(src, tgt, "ComponenteConexe")

    println("Pushout-ul grafului:")
    poGraph.elements.forEach {
        println(it)
    }
}