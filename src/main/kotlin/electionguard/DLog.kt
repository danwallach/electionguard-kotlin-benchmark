package electionguard

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal const val MAX_DLOG: Int = 1_000_000_000

class DLog(val context: GroupContext) {
    private val dLogMapping: ConcurrentMap<ElementModP, Int> =
        ConcurrentHashMap<ElementModP, Int>().apply { this[context.ONE_MOD_P] = 0 }

    private var dLogMaxElement = context.ONE_MOD_P
    private var dLogMaxExponent = 0

    private val mutex = Mutex()

    /**
     * Computes the discrete log of the input. This function uses memoization so it can reuse prior
     * results. It's also guaranteed to be thread-safe. To avoid potentially huge running times,
     * there's an internal timeout for elements whose discrete log might be larger than a billion.
     * If the limit is hit, `null` is returned and an error is logged.
     */
    fun dLog(input: ElementModP): Int? =
        if (input in dLogMapping) {
            dLogMapping[input]
        } else {
            runBlocking {
                mutex.withLock {
                    // We need to check the map again; it might have changed.
                    if (input in dLogMapping) {
                        dLogMapping[input]
                    } else {
                        val max = MAX_DLOG
                        var error = false

                        while (input != dLogMaxElement) {
                            if (dLogMaxExponent++ > max) {
                                //                                logger.error { "Uncomputable
                                // discrete log" }
                                error = true
                                break
                            } else {
                                dLogMaxElement *= context.G_MOD_P
                                dLogMapping[dLogMaxElement] = dLogMaxExponent
                            }
                        }

                        if (error) null else dLogMaxExponent
                    }
                }
            }
        }
}