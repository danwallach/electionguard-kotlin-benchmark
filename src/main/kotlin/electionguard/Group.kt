package electionguard

import java.security.SecureRandom

/**
 * The GroupContext class provides all the necessary context to define the arithmetic that we'll be
 * doing, such as the moduli P and Q, the generator G, and so forth. This also allows us to
 * encapsulate acceleration data structures that we'll use to support various operations.
 */
interface GroupContext {
    /**
     * Returns whether we're using "production primes" (bigger, slower, secure) versus "test primes"
     * (smaller, faster, but insecure).
     */
    fun isProductionStrength(): Boolean

    /** Useful constant: zero mod p */
    val ZERO_MOD_P: ElementModP

    /** Useful constant: one mod p */
    val ONE_MOD_P: ElementModP

    /** Useful constant: two mod p */
    val TWO_MOD_P: ElementModP

    /** Useful constant: the group generator */
    val G_MOD_P: ElementModP

    /** Useful constant: the group generator, squared */
    val G_SQUARED_MOD_P: ElementModP

    /** Useful constant: the modulus of the ElementModQ group */
    val Q_MOD_P: ElementModP

    /** Useful constant: zero mod q */
    val ZERO_MOD_Q: ElementModQ

    /** Useful constant: one mod q */
    val ONE_MOD_Q: ElementModQ

    /** Useful constant: two mod q */
    val TWO_MOD_Q: ElementModQ

    /**
     * Identifies whether the two GroupContexts are "compatible", so elements made in one context
     * would work in the other. Groups with the same primes should be compatible.
     */
    fun isCompatible(ctx: GroupContext): Boolean

    /**
     * Converts a [ByteArray] to an [ElementModP]. The input array is assumed to be in big-endian
     * byte-order: the most significant byte is in the zeroth element; this is the same behavior as
     * Java's BigInteger. Guarantees the result is in [minimum, P), by computing the result mod P.
     */
    fun safeBinaryToElementModP(b: ByteArray, minimum: Int = 0): ElementModP

    /**
     * Converts a [ByteArray] to an [ElementModQ]. The input array is assumed to be in big-endian
     * byte-order: the most significant byte is in the zeroth element; this is the same behavior as
     * Java's BigInteger. Guarantees the result is in [minimum, Q), by computing the result mod Q.
     */
    fun safeBinaryToElementModQ(b: ByteArray, minimum: Int = 0): ElementModQ

    /**
     * Converts a [ByteArray] to an [ElementModP]. The input array is assumed to be in big-endian
     * byte-order: the most significant byte is in the zeroth element; this is the same behavior as
     * Java's BigInteger. Returns null if the number is out of bounds.
     */
    fun binaryToElementModP(b: ByteArray): ElementModP?

    /**
     * Converts a [ByteArray] to an [ElementModQ]. The input array is assumed to be in big-endian
     * byte-order: the most significant byte is in the zeroth element; this is the same behavior as
     * Java's BigInteger. Returns null if the number is out of bounds.
     */
    fun binaryToElementModQ(b: ByteArray): ElementModQ?

    /**
     * Converts a [ULong] to an [ElementModP]. Out-of-bounds might happen with the test group,
     * causing an exception. Will never happen with the production group.
     */
    fun ulongToElementModP(u: ULong): ElementModP

    /**
     * Converts a [ULong] to an [ElementModQ]. Out-of-bounds might happen with the test group,
     * causing an exception. Will never happen with the production group.
     */
    fun ulongToElementModQ(u: ULong): ElementModQ

    /**
     * Computes G^e mod p, where G is our generator. Optimized for small values of e, which occur
     * commonly when encoding vote counters (G raised to 0 or 1).
     */
    fun gPowPSmall(e: Int): ElementModP

    /** Computes G^e mod p, where G is our generator */
    fun gPowP(e: ElementModQ): ElementModP

    /**
     * Computes the discrete log, base g, of p. Only yields an answer for "small" exponents,
     * otherwise returns null.
     */
    fun dLog(p: ElementModP): Int?
}

interface ElementModQ : Element, Comparable<ElementModQ> {
    /** Modular addition */
    operator fun plus(other: ElementModQ): ElementModQ

    /** Modular subtraction */
    operator fun minus(other: ElementModQ): ElementModQ

    /** Modular multiplication */
    operator fun times(other: ElementModQ): ElementModQ

    /** Finds the multiplicative inverse */
    fun multInv(): ElementModQ

    /** Computes the additive inverse */
    operator fun unaryMinus(): ElementModQ

    /** Multiplies by the modular inverse of [denominator] */
    infix operator fun div(denominator: ElementModQ): ElementModQ

    /** Allows elements to be compared (<, >, <=, etc.) using the usual arithmetic operators. */
    override operator fun compareTo(other: ElementModQ): Int
}

interface ElementModP : Element, Comparable<ElementModP> {
    /**
     * Validates that this element is a quadratic residue (and is reachable from
     * [GroupContext.gPowP]). Returns true if everything is good.
     */
    fun isValidResidue(): Boolean

    /** Computes b^e mod p */
    infix fun powP(e: ElementModQ): ElementModP

    /** Modular multiplication */
    operator fun times(other: ElementModP): ElementModP

    /** Finds the multiplicative inverse */
    fun multInv(): ElementModP

    /** Multiplies by the modular inverse of [denominator] */
    infix operator fun div(denominator: ElementModP): ElementModP

    /** Allows elements to be compared (<, >, <=, etc.) using the usual arithmetic operators. */
    override operator fun compareTo(other: ElementModP): Int
}

// 4096-bit P and 256-bit Q primes, plus generator G and cofactor R
internal val b64ProductionP =
    "AP//////////////////////////////////////////k8Rn432wx6TRvj+BAVLLVqHOzDr2XMAZDAPfNHCa/72OS1n6A6nw7tBknMtiEFfREFaukTITWgjkO0Zz10uv6ljeuHjMhtcz2+e/OBVLNs+KltFWeJmqrgwJ1Mi2t7hv0qHqHeYv+GQ+x8Jxgnl3Il5qwvC9YcdGlhVCo8476l21T+cOY+bQn4/ChljoBWekfP3mDudB5dhae9RpMc7YIgNlWUlkuDmJb8qrzMmzGVnAg/Iq0+5ZHDL6ssdEjyoFfbLbSe5S4BgnQeU4ZfAEzI5wS3xcQL8wTE2MTxPt9gR8VVMC0iONjOEd8kJPG2bCxdI40HRNtnmvKJBIcDH5wK6hxLtv6VVO5Sj98bBeWyViI7LwkhXzcZ+cfMxp3fFy0NYjQhf8wAN/GLk+9TiRMLemYeXCblQhQGi7yv6jKmeBi9MHWtH1x+nMPRc3+ygXG6+E27ZhK3iBwaSOQ5zQOpK/UiJaKzjmVC6fcivOFaOBtXU+qEJ2M4HMroNRKzBRGzLl6NgDYhSa0DCqul86V5i7Iqp+wbbQ8XkD9OIthAc0qoWXP3mpP/uCp1xHwD1D0vnKAtAxmbrO3dRTOlJWav//////////////////////////////////////////"
internal val b64ProductionQ = "AP////////////////////////////////////////9D"
internal val b64ProductionP256MinusQ = "AL0="
internal val b64ProductionR =
    "AQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAC8k8Rn432wx6TRvj+BAVLLVqHOzDr2XMAZDAPfNHCbivamTAztzy1VnanZfwlcMHbGhgN2GRSNLIbDFxAq+iFIAx8ERArA/wyaQXqJISUS52B7JQHapNOKLBQQxINhSeK9uMgmDmJ8RkaWPv/p4W5JXUi9IVxtjsnRZnZXoqHIUG8hE/+tGaayvHxFdgRWcZGDMJ+HS8ms5XD/2od6orI6LW8pHBVUyi6xLxLNAJuLhzSmStUeuJO9iRdQuFFiJB2QjwyXCYeXWOfoIz6rO/LWq1OvoyqhU61mguWgZIiXyb4YoNUL7OAww0MjNq2RY+M/jn2vSY8UuyhSr/qBSEHrGN1fDolRbVV3dihcFgcdIRGU7hw/NGQgNquIbj7CiILOQAPeozW02TW65LWCNbn7K6txPI9wWhx95CIgIJ1rvKzEZzGGAVZScuSmPjjiSZdUrkk6wajoNGnu81yifCcbx5Lu4hFW5he5IuqPcTwizygtxdY4W7EoaOt4Enj6CrKolY/Mtf/i5cNh/BdEIBIrAWPKSkYwjIxGyR6nRXwTan2f1Kf1Kf1Kf1Kf1Kf1Kf1Kf1Kf1Kf1Kf1Kf1Kf1Kf1Kg=="
internal val b64ProductionG =
    "HUHknEd+Feru8MXkrAjUpGwmjNNCT8AdE3ab20NnMhhYe8hsTBRI0AagNpnzq65f6xnilvXRQ8xeSj/IkIjJ9FI9Fm7jrp1fsDwL3Xet1cAX9sVeLsksIm/vXGwd8ufDbZDn6q3gmCQdNAmYO8zStTeek5H7xi+fjZOdEgixYDZ8E0JkEiGJWV7IXIzb5fnTB/RpEsBJMvjBaBWna0aCvWvcDtUrANjTD1nHMdWn/66BZdU8+WZJqsK3Q9pW8U8Z2sxSNvKbGrn5vvxpaXKT1d6ti1v13purbeZ8RXGeVjRKPL3zYJgksbV4406utt0xkKs1cdbWccUSKCwdp702tCUdJYT63qgLnhQUIwdN2bX7g6y96tTIelj/9Rf5d6gwgDcKOwz5ihvCl4xHqsKWEf1sQOL5h1w11QRDqao/SWEdzToNb/PLP6zzFHG9thhguSxZTU5GVpuzn+6t/x/WTINqbW24XGunJBdmt6tWv3OWM7BUFH9xcJIUEulI2eR0AtFbscJXMYYSwSHDa4DrhDPAjn0LcUnjqwqHNaku3Oj/lD4ootzqz8xp7DGJCcsEe+HFhYhEta1E8i7rKJ5MxVT3peLz3qAmh3/5KFGBYHHOAo64aNllzLLSKVqMVb0cBws5sJrgazfSk0O52Jl9wkTEaLmAlwcxc27gGLutuYc="

// 16-bit everything, suitable for accelerated testing
internal val b64TestP = "AP7z"
internal val b64TestQ = "f3k="
internal val b64Test256MinusQ = "AP///////////////////////////////////////4CH"
internal val b64TestG = "Aw=="
internal val b64TestR = "Ag=="

// useful for checking the decoders from base64
internal val intTestP = 65267
internal val intTestQ = 32633
internal val intTestG = 3
internal val intTestR = 2

/**
 * Converts a base-64 string to an [ElementModP]. Returns null if the number is out of bounds or the
 * string is malformed.
 */
fun GroupContext.base64ToElementModP(s: String): ElementModP? =
    s.fromBase64()?.let { binaryToElementModP(it) }

/**
 * Converts a base-64 string to an [ElementModP]. Guarantees the result is in [0, P), by computing
 * the result mod P.
 */
fun GroupContext.safeBase64ToElementModP(s: String): ElementModP =
    safeBinaryToElementModP(s.fromBase64() ?: ByteArray(1) { 0 })

/**
 * Converts a base-64 string to an [ElementModQ]. Guarantees the result is in [0, Q), by computing
 * the result mod Q.
 */
fun GroupContext.safeBase64ToElementModQ(s: String): ElementModQ =
    safeBinaryToElementModQ(s.fromBase64() ?: ByteArray(1) { 0 })

/**
 * Converts a base-64 string to an [ElementModQ]. Returns null if the number is out of bounds or the
 * string is malformed.
 */
fun GroupContext.base64ToElementModQ(s: String): ElementModQ? =
    s.fromBase64()?.let { binaryToElementModQ(it) }

/** Converts from any [Element] to a base64 string representation. */
fun Element.base64(): String = byteArray().toBase64()

/** Converts an integer to an ElementModQ, with optimizations when possible for small integers */
fun Int.toElementModQ(ctx: GroupContext) =
    if (this < 0)
        throw NoSuchElementException("no negative numbers allowed")
    else
        ctx.ulongToElementModQ(this.toULong())

/** Converts an integer to an ElementModQ, with optimizations when possible for small integers */
fun Int.toElementModP(ctx: GroupContext) =
    if (this < 0)
        throw NoSuchElementException("no negative numbers allowed")
    else
        ctx.ulongToElementModP(this.toULong())

interface Element {
    /**
     * Every Element knows the [GroupContext] that was used to create it. This simplifies code that
     * computes with elements, allowing arithmetic expressions to be written in many cases without
     * needing to pass in the context.
     */
    val context: GroupContext
        get

    /**
     * Normal computations should ensure that every [Element] is in the modular bounds defined by
     * the group, but deserialization of hostile inputs or buggy code might not preserve this
     * property, so it's valuable to have a way to check. This method allows anything in [0, N)
     * where N is the group modulus.
     */
    fun inBounds(): Boolean

    /**
     * Normal computations should ensure that every [Element] is in the modular bounds defined by
     * the group, but deserialization of hostile inputs or buggy code might not preserve this
     * property, so it's valuable to have a way to check. This method allows anything in [1, N)
     * where N is the group modulus.
     */
    fun inBoundsNoZero(): Boolean

    /** Converts from any [Element] to a compact [ByteArray] representation. */
    fun byteArray(): ByteArray
}

private val rng = SecureRandom.getInstanceStrong()

/**
 * Returns a random number in [minimum, Q), where minimum defaults to zero. Promises to use a
 * "secure" random number generator, such that the results are suitable for use as cryptographic
 * keys.
 *
 * @throws GroupException if the minimum is negative
 */
fun GroupContext.randomElementModQ(minimum: Int = 0): ElementModQ {
    val bytes = ByteArray(32)
    rng.nextBytes(bytes)
    return safeBinaryToElementModQ(bytes, minimum)
}

/**
 * Throughout our bignum arithmetic, every operation needs to check that its operands are compatible
 * (i.e., that we're not trying to use the test group and the production group interchangeably).
 * This will verify that compatibility and throw an `ArithmeticException` if they're not.
 */
fun GroupContext.assertCompatible(other: GroupContext) {
    if (!this.isCompatible(other)) {
        throw ArithmeticException("incompatible group contexts")
    }
}