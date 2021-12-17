package electionguard

import java.time.temporal.ChronoUnit
import kotlin.random.Random
import kotlin.system.exitProcess
import kotlin.system.measureTimeMillis
import me.tongfei.progressbar.ProgressBar
import me.tongfei.progressbar.ProgressBarBuilder
import me.tongfei.progressbar.ProgressBarStyle

// this is a simple benchmark that just measures how fast ElGamal encryption runs

fun main() {
    val N = 1000

    val keypair = elGamalKeyPairFromRandom()
    val nonces = Nonces(randRangeQ(), "benchmark-nonces")
    val random = Random(System.nanoTime()) // not secure, but we don't care

    val deltaTimeMs =
        measureTimeMillis {
            ProgressBar
                .wrap(
                    (0..N).asIterable().toList(),
                    ProgressBarBuilder().setStyle(ProgressBarStyle.COLORFUL_UNICODE_BLOCK)
                        .setInitialMax(N.toLong())
                        .setUpdateIntervalMillis(50)
                        .setSpeedUnit(ChronoUnit.SECONDS)
                        .setUnit("ops", 1L)
                        .setMaxRenderedLength(100)
                        .showSpeed()
                )
                .forEach { i ->
                    val nonce = nonces[i]
                    val message = random.nextInt(1000)
                    val ciphertext = keypair.encrypt(message, nonce)
                    val plaintext = ciphertext.decrypt(keypair)
                    if (plaintext == null) {
                        print("Unexpected decryption failure")
                        exitProcess(1)
                    }
                    if (plaintext != message) {
                        print("Decryption isn't the inverse of encryption?")
                        exitProcess(1)
                    }
                }
        }
    val deltaTime = deltaTimeMs / 1000.0
    print(
        "%d ElGamal encryption/decryption operations in %.2f seconds\n  = %.5f ops/sec".format(
            N,
            deltaTime,
            N / (deltaTime)
        )
    )
}