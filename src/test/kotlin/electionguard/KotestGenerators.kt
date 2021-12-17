package electionguard

import io.kotest.common.runBlocking
import io.kotest.property.Arb
import io.kotest.property.PropTestConfig
import io.kotest.property.ShrinkingMode
import io.kotest.property.arbitrary.*
import org.junit.jupiter.api.fail

/** Generates elements in [0, P) */
fun elementsModP(context: GroupContext, minimum: Int = 0): Arb<ElementModP> =
    Arb.byteArray(Arb.constant(512), Arb.byte())
        .map { context.safeBinaryToElementModP(it, minimum) }

/** Generates elements in [0, Q) */
fun elementsModQ(context: GroupContext, minimum: Int = 0): Arb<ElementModQ> =
    Arb.byteArray(Arb.constant(32), Arb.byte()).map { context.safeBinaryToElementModQ(it, minimum) }

/** Generates elements in [1, P) */
fun elementsModPNoZero(context: GroupContext) = elementsModP(context, minimum = 1)

/** Generates elements in [1, Q) */
fun elementsModQNoZero(context: GroupContext) = elementsModQ(context, minimum = 1)

/**
 * Generates elements in [1, P) which can be derived from the generator and elements mod Q. These
 * will be valid quadratic residues as well.
 */
fun validElementsModP(context: GroupContext): Arb<ElementModP> =
    elementsModQ(context).map { context.gPowP(it) }

/** Generates arbitrary ElGamal public/private keypairs. */
fun elGamalKeypairs(context: GroupContext): Arb<ElGamalKeypair> =
    elementsModQ(context, 2).map { e -> elGamalKeyPairFromSecret(e) }

/** Useful in testing, when we expect a value to be non-null. */
fun <T> T?.failIfNull(): T = this ?: fail { "non-null value expected" }

/**
 * Property-based testing can run slowly. This will speed things up by turning off shrinking and
 * using fewer iterations.
 */
val propTestFastConfig =
    PropTestConfig(maxFailure = 1, shrinkingMode = ShrinkingMode.Off, iterations = 10)

/** Runs a property test which needs to happen in a coroutine context. */
fun runProperty(f: suspend () -> Unit) {
    // this needs to be runPromise() in Kotlin/JS
    runBlocking(f)
}