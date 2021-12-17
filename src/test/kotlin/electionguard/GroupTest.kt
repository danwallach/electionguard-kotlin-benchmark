package electionguard

import io.kotest.property.checkAll
import io.kotest.property.forAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.DynamicTest.dynamicTest

class GroupTest(val context: GroupContext) {
    fun basics() {
        val three = 3.toElementModQ(context)
        val four = 4.toElementModQ(context)
        val seven = 7.toElementModQ(context)
        assertEquals(seven, three + four)
    }

    fun comparisonOperations() {
        val three = 3.toElementModQ(context)
        val four = 4.toElementModQ(context)

        assertTrue(three < four)
        assertTrue(three <= four)
        assertTrue(four > three)
        assertTrue(four >= four)
    }

    fun generatorsWork() {
        runProperty {
            forAll(elementsModP(context)) { it.inBounds() }
            forAll(elementsModQ(context)) { it.inBounds() }
        }
    }

    fun validResiduesForGPowP() {
        runProperty {
            forAll(propTestFastConfig, validElementsModP(context)) { it.isValidResidue() }
        }
    }

    fun binaryArrayRoundTrip() {
        runProperty {
            forAll(elementsModP(context)) { it == context.binaryToElementModP(it.byteArray()) }
            forAll(elementsModQ(context)) { it == context.binaryToElementModQ(it.byteArray()) }
        }
    }

    fun base64RoundTrip() {
        runProperty {
            forAll(elementsModP(context)) { it == context.base64ToElementModP(it.base64()) }
            forAll(elementsModQ(context)) { it == context.base64ToElementModQ(it.base64()) }
        }
    }

    fun baseConversionFails(): Iterable<DynamicTest> {
        return listOf("", "@@", "-10", "1234567890".repeat(1000))
            .flatMap {
                listOf(
                    dynamicTest("base64ElementModP($it)") {
                        assertNull(context.base64ToElementModP(it))
                    },
                    dynamicTest("base64ElementModQ($it)") {
                        assertNull(context.base64ToElementModQ(it))
                    },
                )
            }
    }

    fun additionBasics() {
        runProperty {
            checkAll(elementsModQ(context), elementsModQ(context), elementsModQ(context))
                { a, b, c ->
                    assertEquals(a, a + context.ZERO_MOD_Q) // identity
                    assertEquals(a + b, b + a) // commutative
                    assertEquals(a + (b + c), (a + b) + c) // associative
                }
        }
    }

    fun multiplicationBasicsP() {
        runProperty {
            checkAll(
                elementsModPNoZero(context),
                elementsModPNoZero(context),
                elementsModPNoZero(context)
            ) { a, b, c ->
                assertEquals(a, a * context.ONE_MOD_P) // identity
                assertEquals(a * b, b * a) // commutative
                assertEquals(a * (b * c), (a * b) * c) // associative
            }
        }
    }

    fun multiplicationBasicsQ() {
        runProperty {
            checkAll(
                elementsModQNoZero(context),
                elementsModQNoZero(context),
                elementsModQNoZero(context)
            ) { a, b, c ->
                assertEquals(a, a * context.ONE_MOD_Q) // identity
                assertEquals(a * b, b * a) // commutative
                assertEquals(a * (b * c), (a * b) * c) // associative
            }
        }
    }

    fun subtractionBasics() {
        runProperty {
            checkAll(
                elementsModQNoZero(context),
                elementsModQNoZero(context),
                elementsModQNoZero(context)
            ) { a, b, c ->
                assertEquals(a, a - context.ZERO_MOD_Q)
                assertEquals(a - b, -(b - a))
                assertEquals(a - (b - c), (a - b) + c)
            }
        }
    }

    fun negation() {
        runProperty { forAll(elementsModQ(context)) { context.ZERO_MOD_Q == (-it) + it } }
    }

    fun multiplicativeInversesP() {
        runProperty {
            forAll(elementsModPNoZero(context)) { it.multInv() * it == context.ONE_MOD_P }
        }
    }

    fun multiplicativeInversesQ() {
        runProperty {
            forAll(elementsModQNoZero(context)) { it.multInv() * it == context.ONE_MOD_Q }
        }
    }

    fun divisionP() {
        runProperty { forAll(elementsModPNoZero(context)) { it / it == context.ONE_MOD_P } }
    }

    fun exponentiation() {
        runProperty {
            forAll(propTestFastConfig, elementsModQ(context), elementsModQ(context)) { a, b ->
                context.gPowP(a) * context.gPowP(b) == context.gPowP(a + b)
            }
        }
    }

    fun acceleratedExponentiation() {
        runProperty {
            forAll(propTestFastConfig, elementsModQ(context), elementsModQ(context)) { a, b ->
                val ga = context.gPowP(a)
                val normal = ga powP b
                val gaAccelerated = ga.acceleratePow()
                val faster = gaAccelerated powP b
                normal == faster
            }
        }
    }
}