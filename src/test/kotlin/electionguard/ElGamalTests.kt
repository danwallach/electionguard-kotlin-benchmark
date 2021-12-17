package electionguard

import java.util.concurrent.TimeUnit
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.quicktheories.QuickTheory.qt
import org.quicktheories.generators.SourceDSL.integers

class ElGamalTests {
    // many of these tests can run much longer, so we're going to artificially shorten them
    private val TIME_LIMIT = 5L

    @Test
    fun noSmallKeys() {
        assertThrows<GroupException>({ "0 is too small" }) {
            elGamalKeyPairFromSecret(0.toElementModQ())
        }
        assertThrows<GroupException>({ "1 is too small" }) {
            elGamalKeyPairFromSecret(1.toElementModQ())
        }
        assertDoesNotThrow { elGamalKeyPairFromSecret(2.toElementModQ()) }
    }

    @Test
    fun encryptionBasics() {
        qt().withTestingTime(TIME_LIMIT, TimeUnit.SECONDS)
            .forAll(elGamalKeypairs("a"), elementsModQNoZero("nonce"), integers().between(0, 1000))
            .checkAssert { keypair, nonce, message ->
                assertEquals(message, keypair.decrypt(keypair.encrypt(message, nonce)))
            }
    }

    @Test
    fun encryptionBasicsAutomaticNonces() {
        qt().withTestingTime(TIME_LIMIT, TimeUnit.SECONDS)
            .forAll(elGamalKeypairs("a"), integers().between(0, 1000))
            .checkAssert { keypair, message ->
                val encryption = keypair.encrypt(message)
                val decryption1 = keypair.decrypt(encryption)
                val decryption2 = keypair.secretKey.decrypt(encryption)
                val decryption3 = encryption.decrypt(keypair)
                val decryption4 = encryption.decrypt(keypair.secretKey)
                assertEquals(message, decryption1)
                assertEquals(message, decryption2)
                assertEquals(message, decryption3)
                assertEquals(message, decryption4)
            }
    }

    @Test
    fun decryptWithNonce() {
        qt().withTestingTime(TIME_LIMIT, TimeUnit.SECONDS)
            .forAll(elGamalKeypairs("a"), elementsModQNoZero("nonce"), integers().between(0, 1000))
            .checkAssert { keypair, nonce, message ->
                val encryption = keypair.encrypt(message, nonce)
                val decryption0 = keypair.decrypt(encryption)
                val decryption1 = keypair.decryptWithNonce(encryption, nonce)
                val decryption2 = keypair.publicKey.decryptWithNonce(encryption, nonce)
                val decryption3 = encryption.decryptWithNonce(keypair.publicKey, nonce)
                assertEquals(message, decryption0)
                assertEquals(message, decryption1)
                assertEquals(message, decryption2)
                assertEquals(message, decryption3)
            }
    }

    @Test
    fun homomorphicAccumulation() {
        qt().withTestingTime(TIME_LIMIT, TimeUnit.SECONDS)
            .forAll(
                tuples(
                    elGamalKeypairs("a"),
                    integers().between(0, 1000),
                    integers().between(0, 1000),
                    elementsModQNoZero("n1"),
                    elementsModQNoZero("n2")
                )
            )
            .checkAssert { (keypair, p1, p2, n1, n2) ->
                val c1 = keypair.encrypt(p1, n1)
                val c2 = keypair.encrypt(p2, n2)
                val csum = c1 + c2
                val d = keypair.decrypt(csum)
                assertEquals(p1 + p2, d)
            }
    }
}
