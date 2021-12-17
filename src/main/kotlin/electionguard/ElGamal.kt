package electionguard

typealias ElGamalSecretKey = ElementModQ
typealias ElGamalPublicKey = ElementModP

/** A public and private keypair, suitable for doing ElGamal cryptographic operations. */
data class ElGamalKeypair(val secretKey: ElGamalSecretKey, val publicKey: ElGamalPublicKey)

/**
 * An "exponential ElGamal ciphertext" (i.e., with the plaintext in the exponent to allow for
 * homomorphic addition). Create one with `elgamal_encrypt`. Add them with `elgamal_add`. Decrypt
 * using one of the supplied instance methods. (See
 * [ElGamal 1982](https://ieeexplore.ieee.org/abstract/document/1057074))
 */
data class ElGamalCiphertext(val pad: ElementModP, val data: ElementModP) : CryptoHashable {
    override fun cryptoHash() = hashElements(pad, data)
}

/**
 * Given an ElGamal secret key, derives the corresponding secret/public key pair.
 *
 * @throws GroupException if the secret key is less than two
 */
fun elGamalKeyPairFromSecret(secret: ElGamalSecretKey) =
    if (secret < 2.toElementModQ())
        throw GroupException("secret key must be in [2, Q)")
    else
        ElGamalKeypair(secret, gPowP(secret))

/** Generates a random ElGamal keypair. */
fun elGamalKeyPairFromRandom() =
    elGamalKeyPairFromSecret(randRangeQ(minimum = 2).updateFormula("randomSecretKey"))

/**
 * Uses an ElGamal public key to encrypt a message. An optional nonce can be specified to make this
 * deterministic, or it will be chosen at random.
 *
 * @throws GroupException if the nonce is zero or if the message is negative
 */
fun ElGamalPublicKey.encrypt(
    message: Int,
    nonce: ElementModQ = randRangeQ(minimum = 1, "nonce")
): ElGamalCiphertext {

    if (nonce == ZERO_MOD_Q) {
        throw GroupException("Can't use a zero nonce for ElGamal encryption")
    }

    if (message < 0) {
        throw GroupException("Can't encrypt a negative message")
    }

    // We don't have to check if message >= Q, because it's an integer, and Q
    // is much larger than that.

    val pad = gPowP(nonce)
    val expM = gPowP(message)
    val keyN = this powP nonce
    val data = expM * keyN

    return ElGamalCiphertext(pad, data)
}

/**
 * Uses an ElGamal public key to encrypt a message. An optional nonce can be specified to make this
 * deterministic, or it will be chosen at random.
 */
fun ElGamalKeypair.encrypt(message: Int, nonce: ElementModQ = randRangeQ(minimum = 1, "nonce")) =
    publicKey.encrypt(message, nonce)

/** Uses an ElGamal secret key to decrypt a message. If the decryption fails, `null` is returned. */
fun ElGamalSecretKey.decrypt(ciphertext: ElGamalCiphertext): Int? {
    val blind = ciphertext.pad powP this
    val gPowM = ciphertext.data divP blind
    return dLog(gPowM)
}

/** Decrypts using the secret key. */
fun ElGamalCiphertext.decrypt(secretKey: ElGamalSecretKey): Int? = secretKey.decrypt(this)

/** Uses an ElGamal secret key to decrypt a message. If the decryption fails, `null` is returned. */
fun ElGamalKeypair.decrypt(ciphertext: ElGamalCiphertext) = secretKey.decrypt(ciphertext)

/** Decrypts using the secret key from the keypair. */
fun ElGamalCiphertext.decrypt(keypair: ElGamalKeypair) = keypair.secretKey.decrypt(this)

/**
 * Uses an ElGamal public key to decrypt a message, while also knowing the nonce. If the decryption
 * fails, `null` is returned.
 */
fun ElGamalPublicKey.decryptWithNonce(ciphertext: ElGamalCiphertext, nonce: ElementModQ): Int? {
    val blind = this powP nonce
    val gPowM = ciphertext.data divP blind
    return dLog(gPowM)
}

/**
 * Uses an ElGamal public key to decrypt a message, while also knowing the nonce. If the decryption
 * fails, `null` is returned.
 */
fun ElGamalKeypair.decryptWithNonce(ciphertext: ElGamalCiphertext, nonce: ElementModQ) =
    publicKey.decryptWithNonce(ciphertext, nonce)

/** Decrypts a message by knowing the nonce. */
fun ElGamalCiphertext.decryptWithNonce(publicKey: ElGamalPublicKey, nonce: ElementModQ) =
    publicKey.decryptWithNonce(this, nonce)

/** Homomorphically "adds" two ElGamal ciphertexts together through piecewise multiplication. */
operator fun ElGamalCiphertext.plus(o: ElGamalCiphertext) =
    ElGamalCiphertext(pad * o.pad, data * o.data)

/**
 * Homomorphically "adds" a sequence of ElGamal ciphertexts through piecewise multiplication.
 *
 * @throws GroupException if the sequence is empty
 */
fun Iterable<ElGamalCiphertext>.encryptedSum(): ElGamalCiphertext =
    // This operation isn't defined on an empty list -- we'd have to have some way of getting
    // an encryption of zero, but we don't have the public key handy -- so we'll just raise
    // an exception on that, and otherwise we're fine.
    asSequence()
        .let {
            it.ifEmpty { throw GroupException("Cannot sum an empty list of ciphertexts") }
                .reduce { a, b -> a + b }
        }

/**
 * Combines multiple ElGamal public keys into a single public key. The corresponding secret keys can
 * do "partial decryption" operations that can be later combined. See, e.g.,
 * [ElGamalCiphertext.partialDecryption] and [combinePartialDecryptions].
 */
fun combinePublicKeys(vararg publicKeys: ElGamalPublicKey): ElGamalPublicKey {
    // TODO: implement this for part 1
    return multP(*publicKeys)
}

/**
 * Combines multiple ElGamal public keys into a single public key. The corresponding secret keys can
 * do "partial decryption" operations that can be later combined. See, e.g.,
 * [ElGamalCiphertext.partialDecryption] and [combinePartialDecryptions].
 */
fun Iterable<ElGamalPublicKey>.combinePublicKeys(): ElGamalPublicKey =
    combinePublicKeys(*(toList().toTypedArray()))

typealias ElGamalPartialDecryption = ElementModP

/**
 * Computes a partial decryption of the ciphertext with a secret key. See
 * [ElGamalCiphertext.combinePartialDecryptions] for extracting the plaintext.
 */
fun ElGamalSecretKey.partialDecryption(ciphertext: ElGamalCiphertext): ElGamalPartialDecryption {
    // TODO: implement this for part 1
    return ciphertext.pad powP this
}

/** Computes a partial decryption of the ciphertext. */
fun ElGamalKeypair.partialDecryption(ciphertext: ElGamalCiphertext) =
    secretKey.partialDecryption(ciphertext)

/** Computes a partial decryption of the ciphertext. */
fun ElGamalCiphertext.partialDecryption(secretKey: ElGamalSecretKey) =
    secretKey.partialDecryption(this)

/** Computes a partial decryption of the ciphertext. */
fun ElGamalCiphertext.partialDecryption(keypair: ElGamalKeypair) =
    keypair.secretKey.partialDecryption(this)

/**
 * Given a series of partial decryptions of the ciphertext, combines them together to complete the
 * decryption process.
 */
fun ElGamalCiphertext.combinePartialDecryptions(
    vararg partialDecryptions: ElGamalPartialDecryption
): Int? {
    // TODO: implement this for part 1
    val blind = multP(*partialDecryptions)
    val gPowM = data divP blind
    return dLog(gPowM)
}
