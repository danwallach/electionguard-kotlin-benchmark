package electionguard

import java.util.*
import kotlin.IllegalArgumentException
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray

data class Tuple4<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)

data class Tuple5<A, B, C, D, E>(val a: A, val b: B, val c: C, val d: D, val e: E)

data class Tuple6<A, B, C, D, E, F>(val a: A, val b: B, val c: C, val d: D, val e: E, val f: F)

data class Tuple7<A, B, C, D, E, F, G>(
    val a: A,
    val b: B,
    val c: C,
    val d: D,
    val e: E,
    val f: F,
    val g: G
)

fun <A, B> tupleOf(a: A, b: B) = Pair(a, b)

fun <A, B, C> tupleOf(a: A, b: B, c: C) = Triple(a, b, c)

fun <A, B, C, D> tupleOf(a: A, b: B, c: C, d: D) = Tuple4(a, b, c, d)

fun <A, B, C, D, E> tupleOf(a: A, b: B, c: C, d: D, e: E) = Tuple5(a, b, c, d, e)

fun <A, B, C, D, E, F> tupleOf(a: A, b: B, c: C, d: D, e: E, f: F) = Tuple6(a, b, c, d, e, f)

fun <A, B, C, D, E, F, G> tupleOf(a: A, b: B, c: C, d: D, e: E, f: F, g: G) =
    Tuple7(a, b, c, d, e, f, g)

/** Convert a ByteArray to a Base64 string */
fun ByteArray.toBase64(): String = Base64.getEncoder().encodeToString(this)

/** Convert a Base64 string to a ByteArray, or null if something failed */
fun String.fromBase64(): ByteArray? =
    try {
        fromBase64OrCrash()
    } catch (e: IllegalArgumentException) {
        null
    }

/**
 * Convert a Base64 string to a ByteArray, or throws `IllegalArgumentException` if something failed
 */
fun String.fromBase64OrCrash(): ByteArray {
    if (this == "") throw IllegalArgumentException("empty strings not accepted")
    return Base64.getDecoder().decode(this)
}

/**
 * Fast helper function for mapping a function on an array and getting another array. Normally
 * [Array.map] gives a [List], which you might then have to copy back to another array.
 */
inline fun <T, reified R> Array<out T>.mapArray(mapper: (T) -> R): Array<R> =
    Array(size) { mapper(this[it]) }

/**
 * Given any iterable that of a type that might be `null`, checks if any of the values are actually
 * `null`. If so, `null` is returned. Otherwise, a list without the `null` possibility is returned.
 * Similar to [requireNoNulls] except without throwing any exceptions.
 */
fun <T> Iterable<T?>.noNullsOrNull(): List<T>? {
    val list = this.toList()
    return if (list.contains(null)) {
        null
    } else {
        @Suppress("UNCHECKED_CAST")
        list as List<T>
    }
}

/** Similar to `Iterable.zip`, but takes two arguments, returns triples. */
fun <A, B, C> Iterable<A>.zip(b: Iterable<B>, c: Iterable<C>): Iterable<Triple<A, B, C>> {
    val aIter = this.iterator()
    val bIter = b.iterator()
    val cIter = c.iterator()
    val sequence =
        generateSequence {
            if (aIter.hasNext() && bIter.hasNext() && cIter.hasNext()) {
                Triple(aIter.next(), bIter.next(), cIter.next())
            } else {
                null
            }
        }
    return sequence.asIterable()
}

/** Determines if every element in a list is unique. */
fun <T> Iterable<T>.hasNoDuplicates() = toList().size == toSet().size

// the next block of utilities are meant to help with Kotlin serialization to JSON

private const val MAX_LINE_LENGTH = 80 // excludes indentation
private const val INDENTATION_DELTA = 4

/** Uses the serialization library's internal string escaper */
private fun jsonEscapeString(s: String): String = JsonPrimitive(s).toString()

/**
 * A general-purpose recursive JSON pretty-printer that's a bit fancier than what's included in
 * kotlinx.serialization. Slower on huge inputs, while it's searching for nice-looking outputs.
 */
fun JsonElement.prettyPrint(multiLine: Boolean = true, startingIndentation: Int = 0): String =
    if (!multiLine) toString() else {
        val prefix = " ".repeat(startingIndentation)
        val nextPrefix = " ".repeat(startingIndentation + INDENTATION_DELTA)
        when (this) {
            is JsonPrimitive -> toString()
            is JsonObject -> {
                val flat = toString()
                if (flat.length <= MAX_LINE_LENGTH)
                    flat
                else
                    entries.joinToString(
                        separator = ",\n$nextPrefix",
                        prefix = "{\n$nextPrefix",
                        postfix = "\n$prefix}"
                    ) { (k, v) ->
                        val kEscape = jsonEscapeString(k)
                        val kvFlat = "$kEscape : $v"
                        if (kvFlat.length <= MAX_LINE_LENGTH)
                            kvFlat
                        else
                            "$kEscape: " +
                                v.prettyPrint(multiLine, startingIndentation + INDENTATION_DELTA)
                    }
            }
            is JsonArray -> {
                val flat = toString()
                if (flat.length <= MAX_LINE_LENGTH)
                    flat
                else
                    joinToString(
                        prefix = "[\n$nextPrefix",
                        separator = ",\n$nextPrefix",
                        postfix = "\n$prefix]"
                    ) { it.prettyPrint(multiLine, startingIndentation + INDENTATION_DELTA) }
            }
        }
    }

/** Helper function to build JSON arrays that represent our formulas. */
fun jexprOf(funcName: String, vararg elements: JsonElement): JsonElement =
    buildJsonArray {
        add(funcName)
        elements.forEach { add(it) }
    }