package electionguard

// Implementation of "Group" using java.math.BigInteger

import java.math.BigInteger

private val testGroupContext =
    GroupContextJMB(
        pBytes = b64TestP.fromBase64OrCrash(),
        qBytes = b64TestQ.fromBase64OrCrash(),
        gBytes = b64TestG.fromBase64OrCrash(),
        rBytes = b64TestR.fromBase64OrCrash(),
        strong = false,
        name = "16-bit test group",
        powRadixOption = PowRadixOption.NO_ACCELERATION
    )

private val productionGroups: HashMap<PowRadixOption, GroupContextJMB> =
    hashMapOf(
        PowRadixOption.NO_ACCELERATION to
            GroupContextJMB(
                pBytes = b64ProductionP.fromBase64OrCrash(),
                qBytes = b64ProductionQ.fromBase64OrCrash(),
                gBytes = b64ProductionG.fromBase64OrCrash(),
                rBytes = b64ProductionR.fromBase64OrCrash(),
                strong = true,
                name = "production group, no acceleration",
                powRadixOption = PowRadixOption.NO_ACCELERATION
            ),
        PowRadixOption.LOW_MEMORY_USE to
            GroupContextJMB(
                pBytes = b64ProductionP.fromBase64OrCrash(),
                qBytes = b64ProductionQ.fromBase64OrCrash(),
                gBytes = b64ProductionG.fromBase64OrCrash(),
                rBytes = b64ProductionR.fromBase64OrCrash(),
                strong = true,
                name = "production group, low memory use",
                powRadixOption = PowRadixOption.LOW_MEMORY_USE
            ),
        PowRadixOption.HIGH_MEMORY_USE to
            GroupContextJMB(
                pBytes = b64ProductionP.fromBase64OrCrash(),
                qBytes = b64ProductionQ.fromBase64OrCrash(),
                gBytes = b64ProductionG.fromBase64OrCrash(),
                rBytes = b64ProductionR.fromBase64OrCrash(),
                strong = true,
                name = "production group, high memory use",
                powRadixOption = PowRadixOption.HIGH_MEMORY_USE
            ),
        PowRadixOption.EXTREME_MEMORY_USE to
            GroupContextJMB(
                pBytes = b64ProductionP.fromBase64OrCrash(),
                qBytes = b64ProductionQ.fromBase64OrCrash(),
                gBytes = b64ProductionG.fromBase64OrCrash(),
                rBytes = b64ProductionR.fromBase64OrCrash(),
                strong = true,
                name = "production group, extreme memory use",
                powRadixOption = PowRadixOption.EXTREME_MEMORY_USE
            )
    )

fun productionGroupJMB(option: PowRadixOption): GroupContextJMB =
    productionGroups[option] ?: throw Error("internal error: uninitialized PowRadix storage")

fun allProductionGroupsJMB(): List<GroupContextJMB> = productionGroups.values.toList()

fun allButExtremeProductionGroupsJMB(): List<GroupContextJMB> =
    PowRadixOption.values()
        .filter { it != PowRadixOption.EXTREME_MEMORY_USE }
        .map { productionGroupJMB(it) }

fun testGroupJMB(): GroupContext = testGroupContext

/** Convert an array of bytes, in big-endian format, to a BigInteger */
internal fun UInt.toBigIntegerJMB() = BigInteger.valueOf(this.toLong())
internal fun ByteArray.toBigIntegerJMB() = BigInteger(1, this)

class GroupContextJMB(
    pBytes: ByteArray,
    qBytes: ByteArray,
    gBytes: ByteArray,
    rBytes: ByteArray,
    strong: Boolean,
    val name: String,
    val powRadixOption: PowRadixOption,
) : GroupContext {
    val p: BigInteger
    val q: BigInteger
    val g: BigInteger
    val r: BigInteger
    val zeroModP: ElementModPJMB
    val oneModP: ElementModPJMB
    val twoModP: ElementModPJMB
    val gModP: ElementModPJMB
    val gSquaredModP: ElementModPJMB
    val qModP: ElementModPJMB
    val zeroModQ: ElementModQJMB
    val oneModQ: ElementModQJMB
    val twoModQ: ElementModQJMB
    val productionStrength: Boolean = strong
    val dlogger: DLog
    val powRadix: Lazy<PowRadix>

    init {
        p = pBytes.toBigIntegerJMB()
        q = qBytes.toBigIntegerJMB()
        g = gBytes.toBigIntegerJMB()
        r = rBytes.toBigIntegerJMB()
        zeroModP = ElementModPJMB(0U.toBigIntegerJMB(), this)
        oneModP = ElementModPJMB(1U.toBigIntegerJMB(), this)
        twoModP = ElementModPJMB(2U.toBigIntegerJMB(), this)
        gModP = ElementModPJMB(g, this)
        gSquaredModP = ElementModPJMB((g * g) % p, this)
        qModP = ElementModPJMB(q, this)
        zeroModQ = ElementModQJMB(0U.toBigIntegerJMB(), this)
        oneModQ = ElementModQJMB(1U.toBigIntegerJMB(), this)
        twoModQ = ElementModQJMB(2U.toBigIntegerJMB(), this)
        dlogger = DLog(this)
        powRadix = lazy { PowRadix(gModP, powRadixOption) }
    }

    override fun isProductionStrength() = productionStrength

    override fun toString() = name

    override val ZERO_MOD_P
        get() = zeroModP

    override val ONE_MOD_P
        get() = oneModP

    override val TWO_MOD_P
        get() = twoModP

    override val G_MOD_P
        get() = gModP

    override val G_SQUARED_MOD_P
        get() = gSquaredModP

    override val Q_MOD_P
        get() = qModP

    override val ZERO_MOD_Q
        get() = zeroModQ

    override val ONE_MOD_Q
        get() = oneModQ

    override val TWO_MOD_Q
        get() = twoModQ

    override fun isCompatible(ctx: GroupContext) =
        ctx is GroupContextJMB && this.productionStrength == ctx.productionStrength

    override fun safeBinaryToElementModP(b: ByteArray, minimum: Int): ElementModP {
        if (minimum < 0) {
            throw IllegalArgumentException("minimum $minimum may not be negative")
        }

        val tmp = b.toBigIntegerJMB() % p

        val mv = minimum.toBigInteger()
        val tmp2 = if (tmp < mv) tmp + mv else tmp
        val result = ElementModPJMB(tmp2, this)

        return result
    }

    override fun safeBinaryToElementModQ(b: ByteArray, minimum: Int): ElementModQ {
        if (minimum < 0) {
            throw IllegalArgumentException("minimum $minimum may not be negative")
        }

        val tmp = b.toBigIntegerJMB() % q

        //        assert(tmp < q) { "modulo didn't work! $tmp > $q "}

        val mv = minimum.toBigInteger()
        val tmp2 = if (tmp < mv) tmp + mv else tmp
        val result = ElementModQJMB(tmp2, this)

        //        assert(result.inBounds()) { "result not in bounds! ${result.element} > $q" }

        return result
    }

    override fun binaryToElementModP(b: ByteArray): ElementModP? {
        val tmp = b.toBigIntegerJMB()
        return if (tmp >= p) null else ElementModPJMB(tmp, this)
    }

    override fun binaryToElementModQ(b: ByteArray): ElementModQ? {
        val tmp = b.toBigIntegerJMB()
        return if (tmp >= q) null else ElementModQJMB(tmp, this)
    }

    override fun ulongToElementModP(u: ULong): ElementModP {
        val bytes = ByteArray(8) {
            // big-endian
            ((u shr (8 * (7 - it))) and 0xFFU).toByte()
        }
        return binaryToElementModP(bytes) ?: throw ArithmeticException("input $u too big for mod p")
    }

    override fun ulongToElementModQ(u: ULong): ElementModQ {
        val bytes = ByteArray(8) {
            // big-endian
            ((u shr (8 * (7 - it))) and 0xFFU).toByte()
        }
        return binaryToElementModQ(bytes) ?: throw ArithmeticException("input $u too big for mod q")
    }

    override fun gPowPSmall(e: Int) =
        when {
            e == 0 -> oneModP
            e == 1 -> gModP
            e == 2 -> gSquaredModP
            e < 0 -> throw IllegalArgumentException("gPowPSmall requires e > 0")
            else -> gPowP(ulongToElementModQ(e.toULong()))
        }

    override fun gPowP(e: ElementModQ) = powRadix.value.pow(e)

    override fun dLog(p: ElementModP) = dlogger.dLog(p)
}

private fun Element.getCompat(other: GroupContext): BigInteger {
    context.assertCompatible(other)
    return when (this) {
        is ElementModPJMB -> this.element
        is ElementModQJMB -> this.element
        else -> throw NotImplementedError("should only be two kinds of elements")
    }
}

class ElementModQJMB(val element: BigInteger, val groupContext: GroupContextJMB) : Element,
    Comparable<ElementModQ>, ElementModQ {

    internal fun BigInteger.wrap(): ElementModQ = ElementModQJMB(this, groupContext)
    internal fun BigInteger.modWrap(): ElementModQ = this.mod(groupContext.q).wrap()

    override val context: GroupContext
        get() = groupContext

    override fun inBounds() = element >= groupContext.ZERO_MOD_Q.element && element < groupContext.q

    override fun inBoundsNoZero() =
        element > groupContext.ZERO_MOD_Q.element && element < groupContext.q

    override fun byteArray(): ByteArray = element.toByteArray()

    override operator fun compareTo(other: ElementModQ): Int =
        element.compareTo(other.getCompat(groupContext))

    override operator fun plus(other: ElementModQ) =
        (this.element + other.getCompat(groupContext)).modWrap()

    override operator fun minus(other: ElementModQ) =
        (this.element - other.getCompat(groupContext)).modWrap()

    override operator fun times(other: ElementModQ) =
        (this.element * other.getCompat(groupContext)).modWrap()

    override fun multInv() = element.modInverse(groupContext.q).wrap()

    override operator fun unaryMinus() = (groupContext.q - element).wrap()

    override infix operator fun div(denominator: ElementModQ) =
        (element * denominator.getCompat(groupContext).modInverse(groupContext.q)).modWrap()

    override fun equals(other: Any?) =
        when (other) {
            is ElementModQJMB ->
                other.element == this.element && other.groupContext.isCompatible(this.groupContext)
            else -> false
        }

    override fun hashCode() = element.hashCode()

    override fun toString() = element.toString(10)
}

open class ElementModPJMB(val element: BigInteger, val groupContext: GroupContextJMB) : Element,
    Comparable<ElementModP>, ElementModP {

    internal fun BigInteger.wrap(): ElementModP = ElementModPJMB(this, groupContext)
    internal fun BigInteger.modWrap(): ElementModP = this.remainder(groupContext.p).wrap()

    override val context: GroupContext
        get() = groupContext

    override fun inBounds() = element >= groupContext.ZERO_MOD_P.element && element < groupContext.p

    override fun inBoundsNoZero() =
        element > groupContext.ZERO_MOD_P.element && element < groupContext.p

    override fun byteArray(): ByteArray = element.toByteArray()

    override operator fun compareTo(other: ElementModP): Int =
        element.compareTo(other.getCompat(groupContext))

    override fun isValidResidue(): Boolean {
        val residue =
            this.element.modPow(groupContext.q, groupContext.p) == groupContext.ONE_MOD_P.element
        return inBounds() && residue
    }

    override infix fun powP(e: ElementModQ) =
        this.element.modPow(e.getCompat(groupContext), groupContext.p).wrap()

    override operator fun times(other: ElementModP) =
        (this.element * other.getCompat(groupContext)).modWrap()

    override fun multInv() = element.modInverse(groupContext.p).wrap()

    override infix operator fun div(denominator: ElementModP) =
        (element * denominator.getCompat(groupContext).modInverse(groupContext.p)).modWrap()

    override fun equals(other: Any?) =
        when (other) {
            is ElementModPJMB ->
                other.element == this.element && other.groupContext.isCompatible(this.groupContext)
            else -> false
        }

    override fun hashCode() = element.hashCode()

    override fun toString() = element.toString(10)

    override fun acceleratePow(): ElementModP =
        if (groupContext.powRadixOption == PowRadixOption.NO_ACCELERATION)
            this
        else
            AcceleratedElementModPJMB(
                PowRadix(this, groupContext.powRadixOption),
                element,
                groupContext
            )
}

class AcceleratedElementModPJMB(
    val powRadix: PowRadix,
    element: BigInteger,
    groupContext: GroupContextJMB
) : ElementModPJMB(element, groupContext) {
    override fun acceleratePow(): ElementModP = this

    override infix fun powP(e: ElementModQ) = powRadix.pow(e)
}