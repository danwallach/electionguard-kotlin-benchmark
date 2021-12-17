package electionguard

import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory

class GroupTestKTM {
    val groupTests = (allButExtremeProductionGroupsKTM() + testGroupKTM()).map { GroupTest(it) }

    fun runTests(test: (GroupTest) -> Unit): Iterable<DynamicTest> =
        groupTests.map { gt -> DynamicTest.dynamicTest(gt.context.toString()) { test(gt) } }

    @TestFactory
    fun basics() = runTests { it.basics() }

    @TestFactory
    fun comparisonOperations() = runTests { it.comparisonOperations() }

    @TestFactory
    fun generatorsWork() = runTests { it.generatorsWork() }

    @TestFactory
    fun validResiduesForGPowP() = runTests { it.validResiduesForGPowP() }

    @TestFactory
    fun binaryArrayRoundTrip() = runTests { it.binaryArrayRoundTrip() }

    @TestFactory
    fun base64RoundTrip() = runTests { it.base64RoundTrip() }

    @TestFactory
    fun baseConversionFails(): Iterable<DynamicTest> =
        groupTests.flatMap { it.baseConversionFails() }

    @TestFactory
    fun additionBasics() = runTests { it.additionBasics() }

    @TestFactory
    fun multiplicationBasicsP() = runTests { it.multiplicationBasicsP() }

    @TestFactory
    fun multiplicationBasicsQ() = runTests { it.multiplicationBasicsQ() }

    @TestFactory
    fun subtractionBasics() = runTests { it.subtractionBasics() }

    @TestFactory
    fun negation() = runTests { it.negation() }

    @TestFactory
    fun multiplicativeInversesP() = runTests { it.multiplicativeInversesP() }

    @TestFactory
    fun multiplicativeInversesQ() = runTests { it.multiplicativeInversesQ() }

    @TestFactory
    fun divisionP() = runTests { it.divisionP() }

    @TestFactory
    fun exponentiation() = runTests { it.exponentiation() }
}