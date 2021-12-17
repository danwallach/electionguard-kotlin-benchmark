package electionguard

import java.math.BigInteger
import java.security.SecureRandom
import java.util.*
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import mu.KotlinLogging

// Constants used by ElectionGuard
internal val big189 = BigInteger.valueOf(189)
internal val bigMinus1 = BigInteger.valueOf(-1)
internal val big0 = BigInteger.valueOf(0)
internal val big1 = BigInteger.valueOf(1)
internal val big2 = BigInteger.valueOf(2)

internal val jsonUnknown: JsonElement = JsonPrimitive("?")

/**
 * Helper function to convert [Long] values to [BigInteger], ensuring that small values go to
 * pre-allocated instances, and delegating to [BigInteger.valueOf] in the general case.
 */
fun Long.toBigInteger(): BigInteger =
    when (this) {
        -1L -> bigMinus1
        0L -> big0
        1L -> big1
        2L -> big2
        else -> BigInteger.valueOf(this)
    }

/**
 * Helper function to convert [Int] values to [BigInteger], ensuring that small values go to
 * pre-allocated instances, and delegating to [BigInteger.valueOf] in the general case.
 */
fun Int.toBigInteger() = toLong().toBigInteger()

/**
 * Helper function to convert [Number] values to [BigInteger], ensuring that small values go to
 * pre-allocated instances, and delegating to [BigInteger.valueOf] in the general case.
 */
fun Number.toBigInteger() = toLong().toBigInteger()

private val logger = KotlinLogging.logger {}

/** Thrown if a computation is out of bounds, typically an input out of range */
class GroupException(s: String) : RuntimeException(s)

val Q: BigInteger = big2.pow(256) - big189

val P: BigInteger =
    BigInteger(
        "104438888141315250669175271071662438257996424904738378038423348328395390797155364353772" +
            "99931268758839021736340177774163605029260829463779429557044985420976148418252467735" +
            "80689398386320439747911160897731551074903967243883427132918813748016269754522343505" +
            "28589881677721176191239277291448552115552164104927344620757896193984061946614580685" +
            "92750534765609732951587038233957102103293147097152392517365523840808458360487786673" +
            "18931418338422443891025911884723433084701207771901944593286624979917391350564662632" +
            "72370300796422984915475619689061525228653308964318490270692608174414928951741824915" +
            "36341783420753818741316460134447968945821068705315358036662545796026324531037414525" +
            "69793905551901541856173251385047414840392753585581909950158046256810542678368121278" +
            "50996052095762473794291460031064660979266501285839738143575590285131207124810259944" +
            "23089513270392508188924937674233296637837091907161620235296692173009397831714158082" +
            "33146823000766917789286154006042281423733706462905243774854543127239500245873582012" +
            "66366643058386277816736954760301634424272959224454460827940599975939109977566774640" +
            "16336683086981867211722382550079626585644438589276348504157753488390520266757856948" +
            "26386930175303143450046575460843879941791946313299322976993405829119",
        10
    )

val R: BigInteger = ((P - big1) * Q.modPow(bigMinus1, P)).mod(P)

val G: BigInteger =
    BigInteger(
        "142451090912947413867511543423235210035430598652619116033406695222181598980700933278385" +
            "95045175067897363301047764229640327930333001123401070596314469603183633790452807428" +
            "41677571792318294958387538183391237088987457211208696630049860736450176449481195601" +
            "78811988274003274032520391844488888776447816105948010537532354533825085439069935712" +
            "48387749420874609737451803650021788641249940534081464232937193671929586747339353451" +
            "02171275240622527625501028100485723304324133252782191160441358244291599383377489022" +
            "87054957873572340069327558769726328407605993995140283935423450354331351595110998777" +
            "73857622699742816228063106927776147867040336649025152771036361273329385354927395836" +
            "33020631107257768389266447507072040844725763560689192012379160253851851652487366420" +
            "50346981945616730195355642732047440763360221304539636481143210501739942596206110151" +
            "89498335966173440411967562175734606706258335095991140827763942280037063180207172918" +
            "76992171200340000792388808429668526923329837114363088301121374508220740547997841808" +
            "99177682425925571728349211859908769605270133866939099610933022896461932957251352385" +
            "95082039133488721800071459503353417574248679728577942863659802016004283193163470835" +
            "709405666994892499382890912238098413819320185166580019604608311466",
        10
    )

val Q_MINUS_ONE: BigInteger = Q - big1

/**
 * [ElementModP] and [ElementModQ] both implement this interface, which is useful for functions and
 * methods that can work on either type. The [formula] field allows arithmetic operations on
 * elements to retain the formula used to build them. These are useful for debugging, but can
 * potentially reveal secrets, so they're not serialized.
 *
 * Note that formulas are not considered as part of element equality testing or hashing.
 */
sealed interface Element {
    val element: BigInteger

    val formula: JsonElement

    /**
     * Converts the [formula] to a printable string, indented, suitable for human readability.
     * Ignores the [element]. Alternately, consider [toString] to get both, in human-readable
     * single-line format, or you can call `formula.toString()`, to get a single-line JSON version
     * of the formula, suitable for machine-readability. The JSON expression is roughly the same as
     * an equivalent LISP-style s-expression.
     */
    fun toFormulaString(): String
}

private fun Element.toStringHelper(className: String): String {
    val formulaStr = if (formula == jsonUnknown) "" else "formula = ${formula}, "
    val elementStr = element.toString(10)
    return "$className(${formulaStr}element = $elementStr)"
}

private fun JsonElement.toFormulaString(className: String) =
    className +
        prettyPrint(true)
            .let { formulaStr ->
                if (formulaStr.startsWith("[") || formulaStr.startsWith("{"))
                    formulaStr
                else
                    "($formulaStr)"
            }

/**
 * General-purpose holder of elements in [0, P). Don't use the constructor directly. Instead, use
 * the helper functions, like [Long.toElementModP] or [base64ElementModP], which will check for
 * errors.
 *
 * Note that the [formula] is "transient", which means that serialization libraries will hopefully
 * skip it rather than writing it out.
 */
data class ElementModP(
    override val element: BigInteger,
    @Transient
    override val formula: JsonElement = jsonUnknown
) : Element {
    constructor(
        element: BigInteger,
        formulaStr: String
    ) : this(element, if (formulaStr == "?") jsonUnknown else JsonPrimitive(formulaStr))

    override fun equals(other: Any?) =
        when (other) {
            is Element -> other.element == element
            else -> false
        }

    override fun hashCode() = element.hashCode()

    override fun toString() = toStringHelper("ElementModP")

    override fun toFormulaString() = formula.toFormulaString("ElementModP")
}

/**
 * General-purpose holder of elements in [0, Q). Don't use the constructor directly. Instead, use
 * the helper functions, like [Long.toElementModQ] or [base64ElementModQ], which will check for
 * errors.
 *
 * Note that the [formula] is "transient", which means that serialization libraries will hopefully
 * skip it rather than writing it out.
 */
data class ElementModQ(
    override val element: BigInteger,
    @Transient
    override val formula: JsonElement = jsonUnknown
) : Element {
    constructor(
        element: BigInteger,
        formulaStr: String
    ) : this(element, if (formulaStr == "?") jsonUnknown else JsonPrimitive(formulaStr))

    override fun equals(other: Any?) =
        when (other) {
            is Element -> other.element == element
            else -> false
        }

    override fun hashCode() = element.hashCode()

    override fun toString() = toStringHelper("ElementModQ")

    override fun toFormulaString() = formula.toFormulaString("ElementModQ")
}

// constructors, destructors, and validators

/**
 * Normal computations should ensure that every [ElementModP] is in [1, P), but deserialization of
 * hostile inputs or buggy code might not preserve this property, so it's valuable to have a way to
 * check. This method allows anything in [0, P).
 */
fun ElementModP.inBounds() = element >= big0 && element < P

/**
 * Normal computations should ensure that every [ElementModP] is in [1, P), but deserialization of
 * hostile inputs or buggy code might not preserve this property, so it's valuable to have a way to
 * check. This method allows anything in [1, P).
 */
fun ElementModP.inBoundsNoZero() = element >= big1 && element < P

/**
 * Normal computations should ensure that every [ElementModQ] is in [0, Q), but deserialization of
 * hostile inputs or buggy code might not preserve this property, so it's valuable to have a way to
 * check. This method allows anything in [0, Q).
 */
fun ElementModQ.inBounds() = element >= big0 && element < Q

/**
 * Normal computations should ensure that every [ElementModQ] is in [0, Q), but deserialization of
 * hostile inputs or buggy code might not preserve this property, so it's valuable to have a way to
 * check. This method allows anything in [1, Q).
 */
fun ElementModQ.inBoundsNoZero() = element >= big1 && element < Q

private val b64encoder = Base64.getEncoder()
private val b64decoder = Base64.getDecoder()

/**
 * Converts from any [Element] to a base64 string representation. See [base64ElementModP] and
 * [base64ElementModQ] for inverses.
 */
fun Element.base64(): String = b64encoder.encodeToString(element.toByteArray())

/**
 * Converts from any [Element] to a base16 (hexadecimal) string representation. See
 * [base16ElementModP] and [base16ElementModQ] for inverses.
 */
fun Element.base16(): String = element.toString(16)

/**
 * Converts from any [Element] to a base10 (decimal) string representation. See [base10ElementModP]
 * and [base10ElementModQ] for inverses.
 */
fun Element.base10(): String = element.toString(10)

/**
 * Converts from any [Element] to a compact [ByteArray] representation. See [bytesElementModP] and
 * [bytesElementModQ] for inverses.
 */
fun Element.byteArray(): ByteArray = element.toByteArray()

/** Allows all elements to be compared (<, >, <=, etc.) using the usual arithmetic operators. */
operator fun Element.compareTo(other: Element) = element.compareTo(other.element)

/** Allows all elements to be compared (<, >, <=, etc.) using the usual arithmetic operators. */
operator fun Element.compareTo(other: Int) = element.compareTo(other.toBigInteger())

/** Allows all elements to be compared (<, >, <=, etc.) using the usual arithmetic operators. */
operator fun Element.compareTo(other: Number) = element.compareTo(other.toBigInteger())

/** Allows all elements to be compared (<, >, <=, etc.) using the usual arithmetic operators. */
operator fun Element.compareTo(other: Long) = element.compareTo(other.toBigInteger())

/**
 * Converts any BigInteger to an ElementModP. For untrusted input, you should instead use
 * [base64ElementModP], [base16ElementModP], or [base10ElementModP], which log errors and return
 * `null`.
 *
 * @param formulaStr optional string to document the equation used to generate this element
 * @throws GroupException if the BigInteger is outside [0, P)
 */
fun BigInteger.toElementModP(formulaStr: String = "?") =
    if (this < big0 || this >= P) {
        throw GroupException("value $this out of bounds for ElementModP($formulaStr)")
    } else {
        ElementModP(this, formulaStr)
    }

/**
 * Converts any BigInteger to an ElementModQ. For untrusted input, you should instead use
 * [base64ElementModQ], [base16ElementModQ], or [base10ElementModQ], which log errors and return
 * `null`.
 *
 * @param formulaStr optional string to document the equation used to generate this element
 * @throws GroupException if the BigInteger is outside [0, Q)
 */
fun BigInteger.toElementModQ(formulaStr: String = "?") =
    if (this < big0 || this >= Q) {
        throw GroupException("value $this out of bounds for ElementModQ($formulaStr)")
    } else {
        ElementModQ(this, formulaStr)
    }

/**
 * Converts any BigInteger to an ElementModP. For untrusted input, you should instead use
 * [base64ElementModP], [base16ElementModP], or [base10ElementModP], which log errors and return
 * `null`.
 *
 * @param formula optional [JsonElement] to document the equation used to generate this element
 * @throws GroupException if the BigInteger is outside [0, P)
 */
fun BigInteger.toElementModP(formula: JsonElement = jsonUnknown) =
    if (this < big0 || this >= P) {
        throw GroupException("value $this out of bounds for ElementModP($formula})")
    } else {
        ElementModP(this, formula)
    }

/**
 * Converts any BigInteger to an ElementModQ. For untrusted input, you should instead use
 * [base64ElementModQ], [base16ElementModQ], or [base10ElementModQ], which log errors and return
 * `null`.
 *
 * @param formula optional [JsonElement] to document the equation used to generate this element
 * @throws GroupException if the BigInteger is outside [0, Q)
 */
fun BigInteger.toElementModQ(formula: JsonElement = jsonUnknown) =
    if (this < big0 || this >= Q) {
        throw GroupException("value $this out of bounds for ElementModQ($formula)")
    } else {
        ElementModQ(this, formula)
    }

/**
 * Converts a [Long] to an [ElementModP]. For untrusted input, you should instead use
 * [base64ElementModQ], [base16ElementModQ], or [base10ElementModQ], which log errors and return
 * `null`.
 *
 * @param formulaStr optional string to document the equation used to generate this element
 * @throws GroupException if the input is outside [0, P)
 */
fun Long.toElementModP(formulaStr: String = "?") = toBigInteger().toElementModP(formulaStr)

/**
 * Converts a [Number] to an [ElementModP]. For untrusted input, you should instead use
 * [base64ElementModQ], [base16ElementModQ], or [base10ElementModQ], which log errors and return
 * `null`.
 *
 * @param formulaStr optional string to document the equation used to generate this element
 * @throws GroupException if the input is outside [0, P)
 */
fun Number.toElementModP(formulaStr: String = "?") = toBigInteger().toElementModP(formulaStr)

/**
 * Converts an [Int] to an [ElementModP]. For untrusted input, you should instead use
 * [base64ElementModQ], [base16ElementModQ], or [base10ElementModQ], which log errors and return
 * `null`.
 *
 * @param formulaStr optional string to document the equation used to generate this element
 * @throws GroupException if the input is outside [0, P)
 */
fun Int.toElementModP(formulaStr: String = "?") = toBigInteger().toElementModP(formulaStr)

/**
 * Converts a [Long] to an [ElementModQ]. For untrusted input, you should instead use
 * [base64ElementModQ], [base16ElementModQ], or [base10ElementModQ], which log errors and return
 * `null`.
 *
 * @param formulaStr optional string to document the equation used to generate this element
 * @throws GroupException if the input is outside [0, Q)
 */
fun Long.toElementModQ(formulaStr: String = "?") = toBigInteger().toElementModQ(formulaStr)

/**
 * Converts a [Number] to an [ElementModQ]. For untrusted input, you should instead use
 * [base64ElementModQ], [base16ElementModQ], or [base10ElementModQ], which log errors and return
 * `null`.
 *
 * @param formulaStr optional string to document the equation used to generate this element
 * @throws GroupException if the input is outside [0, Q)
 */
fun Number.toElementModQ(formulaStr: String = "?") = toBigInteger().toElementModQ(formulaStr)

/**
 * Converts an [Int] to an [ElementModQ]. For untrusted input, you should instead use
 * [base64ElementModQ], [base16ElementModQ], or [base10ElementModQ], which log errors and return
 * `null`.
 *
 * @param formulaStr optional string to document the equation used to generate this element
 * @throws GroupException if the input is outside [0, Q)
 */
fun Int.toElementModQ(formulaStr: String = "?") = toBigInteger().toElementModQ(formulaStr)

/**
 * Given an arbitrary array of bytes, extracts an [ElementModP].
 *
 * @param formulaStr optional string to document the equation used to generate this element
 * @throws GroupException if the input is outside [0, P)
 */
fun bytesElementModP(s: ByteArray, formulaStr: String = "?") =
    BigInteger(s).toElementModP(formulaStr)

/**
 * Given an arbitrary array of bytes, extracts an [ElementModQ].
 *
 * @param formulaStr optional string to document the equation used to generate this element
 * @throws GroupException if the input is outside [0, Q)
 */
fun bytesElementModQ(s: ByteArray, formulaStr: String = "?") =
    BigInteger(s).toElementModQ(formulaStr)

/**
 * Given an arbitrary array of bytes, extracts an [ElementModP], guaranteeing that the result is a
 * valid [ElementModP] in [0, P) by performing a modulus operation.
 *
 * @param formulaStr optional string to document the equation used to generate this element
 */
fun safeBytesElementModP(s: ByteArray, formulaStr: String = "?") =
    (BigInteger(s) % P).toElementModP(formulaStr)

/**
 * Given an arbitrary array of bytes, extracts an [ElementModQ], guaranteeing that the result is a
 * valid [ElementModQ] in [0, Q) by performing a modulus operation.
 *
 * @param formulaStr optional string to document the equation used to generate this element
 */
fun safeBytesElementModQ(s: ByteArray, formulaStr: String = "?") =
    (BigInteger(s) % Q).toElementModQ(formulaStr)

/**
 * Given a base64 string containing an [ElementModP], returns that element or `null` if there was
 * some sort of input error. The error is also logged.
 *
 * @param s string input
 * @param formulaStr optional string to document the equation used to generate this element
 */
fun base64ElementModP(s: String, formulaStr: String = "?"): ElementModP? =
    try {
        BigInteger(b64decoder.decode(s)).toElementModP(formulaStr)
    } catch (e: Throwable) {
        logger.error { "failed to decode $s as an element mod p" }
        null
    }

/**
 * Given a base64 string containing an [ElementModQ], returns that element or `null` if there was
 * some sort of input error. The error is also logged.
 *
 * @param s string input
 * @param formulaStr optional string to document the equation used to generate this element
 */
fun base64ElementModQ(s: String, formulaStr: String = "?"): ElementModQ? =
    try {
        BigInteger(b64decoder.decode(s)).toElementModQ(formulaStr)
    } catch (e: Throwable) {
        logger.error { "failed to decode $s as an element mod q" }
        null
    }

/**
 * Given a hexadecimal string containing an [ElementModP], returns that element or `null` if there
 * was some sort of input error. The error is also logged.
 *
 * @param s string input
 * @param formulaStr optional string to document the equation used to generate this element
 */
fun base16ElementModP(s: String, formulaStr: String = "?"): ElementModP? =
    try {
        s.toBigInteger(16).toElementModP(formulaStr)
    } catch (e: Throwable) {
        logger.error { "failed to decode $s as an element mod p" }
        null
    }

/**
 * Given a hexadecimal string containing an [ElementModQ], returns that element or `null` if there
 * was some sort of input error. The error is also logged.
 *
 * @param s string input
 * @param formulaStr optional string to document the equation used to generate this element
 */
fun base16ElementModQ(s: String, formulaStr: String = "?"): ElementModQ? =
    try {
        s.toBigInteger(16).toElementModQ(formulaStr)
    } catch (e: Throwable) {
        logger.error { "failed to decode $s as an element mod q" }
        null
    }

/**
 * Given a decimal string containing an [ElementModP], returns that element or `null` if there was
 * some sort of input error. The error is also logged.
 *
 * @param s string input
 * @param formulaStr optional string to document the equation used to generate this element
 */
fun base10ElementModP(s: String, formulaStr: String = "?"): ElementModP? =
    try {
        s.toBigInteger(10).toElementModP(formulaStr)
    } catch (e: Throwable) {
        logger.error { "failed to decode $s as an element mod p" }
        null
    }

/**
 * Given a decimal string containing an [ElementModQ], returns that element or `null` if there was
 * some sort of input error. The error is also logged.
 *
 * @param s string input
 * @param formulaStr optional string to document the equation used to generate this element
 */
fun base10ElementModQ(s: String, formulaStr: String = "?"): ElementModQ? =
    try {
        s.toBigInteger(10).toElementModQ(formulaStr)
    } catch (e: Throwable) {
        logger.error { "failed to decode $s as an element mod q" }
        null
    }

/** Returns a copy of the element, but with the new formula */
fun ElementModP.updateFormula(s: String) = this.copy(formula = JsonPrimitive(s))

/** Returns a copy of the element, but with the new formula */
fun ElementModP.updateFormula(j: JsonElement) = this.copy(formula = j)

/** Returns a copy of the element, but with the new formula */
fun ElementModQ.updateFormula(s: String) = this.copy(formula = JsonPrimitive(s))

/** Returns a copy of the element, but with the new formula */
fun ElementModQ.updateFormula(j: JsonElement) = this.copy(formula = j)

val ZERO_MOD_P = 0.toElementModP("0")
val ONE_MOD_P = 1.toElementModP("1")
val TWO_MOD_P = 2.toElementModP("2")

val G_MOD_P = G.toElementModP("G")

val ZERO_MOD_Q = 0.toElementModQ("0")
val ONE_MOD_Q = 1.toElementModQ("1")
val TWO_MOD_Q = 2.toElementModQ("2")

/**
 * Validates that this element is a quadratic residue (and is reachable from [gPowP]). Returns true
 * if everything is good.
 */
fun ElementModP.isValidResidue(): Boolean {
    val residue = this powP ElementModQ(Q) == ONE_MOD_P
    return inBounds() && residue
}

// Kotlin by default gives us the "remainder" operator, which preserves sign,
// while we want to keep everything greater than zero. We're overriding this
// behavior within our own code, but being careful not to expose this to
// external users of this code as a library.

internal operator fun BigInteger.rem(o: BigInteger) = mod(o)

/** Modular addition on ElementModP */
operator fun ElementModP.plus(other: ElementModP) =
    ((element + other.element) % P).toElementModP(jexprOf("addP", formula, other.formula))

/** Modular addition on ElementModQ */
operator fun ElementModQ.plus(other: ElementModQ) =
    ((element + other.element) % Q).toElementModQ(jexprOf("addQ", formula, other.formula))

/** Modular subtraction on ElementModP */
operator fun ElementModP.minus(other: ElementModP) =
    ((element - other.element) % P).toElementModP(jexprOf("minusP", formula, other.formula))

/** Modular subtraction on ElementModQ */
operator fun ElementModQ.minus(other: ElementModQ) =
    ((element - other.element) % Q).toElementModQ(jexprOf("minusQ", formula, other.formula))

/** Modular multiplication on ElementModP */
operator fun ElementModP.times(other: ElementModP) =
    ((element * other.element) % P).toElementModP(jexprOf("multP", formula, other.formula))

/** Modular multiplication on ElementModP */
operator fun ElementModQ.times(other: ElementModQ) =
    ((element * other.element) % Q).toElementModQ(jexprOf("multQ", formula, other.formula))

private val gPow0 = gPowP(ZERO_MOD_Q)
private val gPow1 = gPowP(ONE_MOD_Q)
private val gPow2 = gPowP(TWO_MOD_Q)

/**
 * Computes G^e mod p, where G is our generator. Optimized for small values of e, which occur
 * commonly when encoding vote counters (G raised to 0 or 1).
 */
fun gPowP(e: Int): ElementModP =
    when (e) {
        0 -> gPow0
        1 -> gPow1
        2 -> gPow2
        else -> gPowP(e.toElementModQ(e.toString()))
    }

/** Computes G^e mod p, where G is our generator */
fun gPowP(e: Element): ElementModP =
    (G.modPow(e.element, P)).toElementModP(jexprOf("gPowP", e.formula))

/** Computes b^e mod p */
infix fun Element.powP(e: Element) =
    (element.modPow(e.element, P)).toElementModP(jexprOf("powP", formula, e.formula))

/** Computes b^e mod q */
infix fun Element.powQ(e: Element) =
    (element.modPow(e.element, Q)).toElementModQ(jexprOf("powQ", formula, e.formula))

/** Computes the sum of the given elements, mod q */
fun addQ(vararg elements: ElementModQ) = elements.asIterable().addQ()

/** Computes the sum of the given elements, mod q */
fun Iterable<ElementModQ>.addQ() =
    fold(big0) { s, e -> (s + e.element) % Q }
        .toElementModQ(jexprOf("addQ", *(map { it.formula }.toTypedArray())))

/** Computes the product of the given elements, mod p */
fun multP(vararg elements: ElementModP) = elements.asIterable().multP()

/** Computes the product of the given elements, mod p */
fun Iterable<ElementModP>.multP() =
    fold(big1) { s, e -> (s * e.element) % P }
        .toElementModP(jexprOf("multP", *(map { it.formula }.toTypedArray())))

/** Computes the multiplicative inverse mod p */
fun ElementModP.multInv() = element.modInverse(P).toElementModP(jexprOf("multInvP", formula))

/** Computes the multiplicative inverse mod q */
fun ElementModQ.multInv() = element.modInverse(Q).toElementModQ(jexprOf("multInvQ", formula))

/** Multiplies by the modular inverse of [denominator], mod p */
infix fun Element.divP(denominator: Element) =
    ((element * denominator.element.modInverse(P)) % P)
        .toElementModP(jexprOf("divP", formula, denominator.formula))

/** Multiplies by the modular inverse of [denominator], mod q */
infix fun Element.divQ(denominator: Element) =
    ((element * denominator.element.modInverse(Q)) % Q)
        .toElementModQ(jexprOf("divQ", formula, denominator.formula))

/** Computes the additive inverse, mod q */
fun negateQ(a: ElementModQ) = ((Q - a.element) % Q).toElementModQ(jexprOf("negQ", a.formula))

/** Computes (a + b * c) mod q */
fun aPlusBCQ(a: Element, b: Element, c: Element) =
    ((a.element + b.element * c.element) % Q)
        .toElementModQ(jexprOf("aPlusBCQ", a.formula, b.formula, c.formula))

private val randomSource = SecureRandom()

/**
 * Returns a random number in [minimum, Q), where minimum defaults to zero. Uses Java's
 * [SecureRandom] to ensure strong randomness.
 *
 * @throws GroupException if the minimum is negative
 */
fun randRangeQ(minimum: Int = 0, formulaStr: String = "?"): ElementModQ {
    if (minimum < 0) {
        throw GroupException("negative minimum not supported ($minimum")
    }

    val bytes = ByteArray(32) // we need exactly 256 random bits = 32 bytes
    randomSource.nextBytes(bytes) // mutates the bytes array

    val minimumBI = minimum.toBigInteger()
    val big = (BigInteger(bytes) % (Q - minimumBI)) + minimumBI
    return big.toElementModQ(jexprOf("randQ", JsonPrimitive(formulaStr)))
}
