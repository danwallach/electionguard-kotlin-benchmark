package electionguard

import kotlin.random.Random
import org.junit.jupiter.api.fail
import org.quicktheories.core.Gen
import org.quicktheories.generators.Generate.constant
import org.quicktheories.generators.SourceDSL.bigIntegers
import org.quicktheories.generators.SourceDSL.integers
import org.quicktheories.generators.SourceDSL.lists

// The methods below adapt QuickTheories to work with Kotlin's Pair and Triple classes
// and also generate up to 7-tuples, using classes we had to add. We use these elsewhere
// as a workaround for QuickTheories's forAll() method only supporting four parameters.
// Sometimes we need many more than that.

/** Generates two-tuples (pairs) of anything. */
fun <A, B> tuples(aGen: Gen<A>, bGen: Gen<B>): Gen<Pair<A, B>> =
    aGen.zip(bGen) { a, b -> tupleOf(a, b) }

/** Generates three-tuples (triples) of anything. */
fun <A, B, C> tuples(aGen: Gen<A>, bGen: Gen<B>, cGen: Gen<C>): Gen<Triple<A, B, C>> =
    aGen.zip(bGen, cGen) { a, b, c -> tupleOf(a, b, c) }

/** Generates 4-tuples of anything */
fun <A, B, C, D> tuples(
    aGen: Gen<A>,
    bGen: Gen<B>,
    cGen: Gen<C>,
    dGen: Gen<D>
): Gen<Tuple4<A, B, C, D>> = aGen.zip(bGen, cGen, dGen) { a, b, c, d -> tupleOf(a, b, c, d) }

/** Generates 5-tuples of anything */
fun <A, B, C, D, E> tuples(
    aGen: Gen<A>,
    bGen: Gen<B>,
    cGen: Gen<C>,
    dGen: Gen<D>,
    eGen: Gen<E>
): Gen<Tuple5<A, B, C, D, E>> =
    aGen.zip(bGen, cGen, dGen, eGen) { a, b, c, d, e -> tupleOf(a, b, c, d, e) }

/** Generates 6-tuples of anything */
fun <A, B, C, D, E, F> tuples(
    aGen: Gen<A>,
    bGen: Gen<B>,
    cGen: Gen<C>,
    dGen: Gen<D>,
    eGen: Gen<E>,
    fGen: Gen<F>
): Gen<Tuple6<A, B, C, D, E, F>> =
    aGen.zip(bGen, cGen, dGen, eGen.zip(fGen) { e, f -> tupleOf(e, f) }) { a, b, c, d, (e, f) ->
        tupleOf(a, b, c, d, e, f)
    }

/** Generates 6-tuples of anything */
fun <A, B, C, D, E, F, G> tuples(
    aGen: Gen<A>,
    bGen: Gen<B>,
    cGen: Gen<C>,
    dGen: Gen<D>,
    eGen: Gen<E>,
    fGen: Gen<F>,
    gGen: Gen<G>
): Gen<Tuple7<A, B, C, D, E, F, G>> =
    aGen.zip(bGen, cGen, dGen, eGen.zip(fGen, gGen) { e, f, g -> tupleOf(e, f, g) })
        { a, b, c, d, (e, f, g) -> tupleOf(a, b, c, d, e, f, g) }

/** Generates elements in [0, P) */
fun elementsModP(formulaStr: String = "?"): Gen<ElementModP> =
    bigIntegers().ofBytes(514).map { i -> ElementModP(i % P, formulaStr) }

/** Generates elements in [0, Q) */
fun elementsModQ(formulaStr: String = "?"): Gen<ElementModQ> =
    bigIntegers().ofBytes(45).map { i -> ElementModQ(i % Q, formulaStr) }

/** Generates elements in [1, P) */
fun elementsModPNoZero(formulaStr: String = "?"): Gen<ElementModP> =
    elementsModP(formulaStr).assuming { it != ZERO_MOD_P }

/** Generates elements in [1, Q) */
fun elementsModQNoZero(formulaStr: String = "?"): Gen<ElementModQ> =
    elementsModQ(formulaStr).assuming { it != ZERO_MOD_Q }

/**
 * Generates elements in [1, P) which can be derived from the generator and elements mod Q. These
 * will be valid quadratic residues as well.
 */
fun validElementsModP(formulaStr: String = "?"): Gen<ElementModP> =
    elementsModQ(formulaStr).map { e -> gPowP(e) }

/** Generates arbitrary ElGamal public/private keypairs. */
fun elGamalKeypairs(formulaStr: String = "?"): Gen<ElGamalKeypair> =
    elementsModQ(formulaStr).assuming { e -> e >= 2.toElementModQ() }
        .map { e -> elGamalKeyPairFromSecret(e) }

private val presidents =
    listOf(
        "George Washington",
        "John Adams",
        "Thomas Jefferson",
        "James Madison",
        "James Monroe",
        "John Quincy Adams",
        "Andrew Jackson",
        "Martin Van Buren",
        "William Henry Harrison",
        "John Tyler",
        "James K. Polk",
        "Zachary Taylor",
        "Millard Fillmore",
        "Franklin Pierce",
        "James Buchanan",
        "Abraham Lincoln",
        "Andrew Johnson",
        "Ulysses S. Grant",
        "Rutherford B. Hayes",
        "James Garfield",
        "Chester Arthur",
        "Grover Cleveland",
        "Benjamin Harrison",
        "Grover Cleveland",
        "William McKinley",
        "Theodore Roosevelt",
        "William Howard Taft",
        "Woodrow Wilson",
        "Warren G. Harding",
        "Calvin Coolidge",
        "Herbert Hoover",
        "Franklin D. Roosevelt",
        "Harry S. Truman",
        "Dwight Eisenhower",
        "John F. Kennedy",
        "Lyndon B. Johnson",
        "Richard Nixon",
        "Gerald Ford",
        "Jimmy Carter",
        "Ronald Reagan",
        "George Bush",
        "Bill Clinton",
        "George W. Bush",
        "Barack Obama",
        "Donald Trump",
        "Joe Biden",
    )

/** Generates a `PrivateElectionContext` for an election with a given number of candidates */
fun electionContexts(numCandidates: Int): Gen<PrivateElectionContext> {
    val keypairGen: Gen<ElGamalKeypair> = elGamalKeypairs("keypair")
    val hashHeaderGen: Gen<ElementModQ> = elementsModQ("hashHeader")
    val maxChoicesGen: Gen<Int> = integers().between(1, numCandidates)

    return keypairGen.zip(hashHeaderGen, maxChoicesGen) { keypair, hashHeader, maxChoices ->
        PrivateElectionContext(
            electionName = "Test Election",
            candidateNames = presidents.subList(0, numCandidates),
            maxVotesCast = maxChoices,
            keypair = keypair,
            hashHeader = hashHeader
        )
    }
}

internal fun plaintextBallot(
    context: AnyElectionContext,
    ballotId: String,
    numCast: Int,
    rng: Random
): PlaintextBallot {
    val yesVotes = List(numCast) { 1 }
    val noVotes = List(context.candidateNames.size - numCast) { 0 }
    val shuffledVotes = (yesVotes + noVotes).shuffled(rng)
    return PlaintextBallot(
        ballotId,
        shuffledVotes.mapIndexed { index, selection ->
            PlaintextSelection(context.candidateNames[index], selection)
        }
    )
}

internal fun plaintextBallotsHelper(
    context: AnyElectionContext,
    numBallots: Int,
    maxVotesCast: Int
): Gen<List<PlaintextBallot>> =
    tuples(lists().of(integers().between(0, maxVotesCast)).ofSize(numBallots), integers().all())
        .map { (numCastList, rngSeed) ->
            val rng = Random(rngSeed)
            numCastList.mapIndexed { index, numCast ->
                plaintextBallot(context, "ballot%03d".format(index), numCast, rng)
            }
        }

/** Generates well-formed plaintext ballots */
fun plaintextBallots(context: AnyElectionContext, numBallots: Int) =
    plaintextBallotsHelper(context, numBallots, context.maxVotesCast)

/** Generates plaintext ballots that might include overvotes */
fun plaintextBallotsArbitrary(context: AnyElectionContext, numBallots: Int) =
    plaintextBallotsHelper(context, numBallots, context.candidateNames.size)

/** Generates an election context and a list of random ballots */
fun contextAndBallots(
    numBallots: Int = 10
): Gen<Pair<PrivateElectionContext, List<PlaintextBallot>>> {
    val numCandidatesGen: Gen<Int> = integers().between(1, 10)
    val contextGen = numCandidatesGen.flatMap { i -> electionContexts(i) }
    return contextGen.flatMap { context ->
        tuples(constant(context), plaintextBallots(context, numBallots))
    }
}

/** Generates an election context and a list of random ballots that might include overvotes. */
fun contextAndArbitraryBallots(
    numBallots: Int = 10
): Gen<Pair<PrivateElectionContext, List<PlaintextBallot>>> {
    val numCandidatesGen: Gen<Int> = integers().between(1, 10)
    val contextGen = numCandidatesGen.flatMap { i -> electionContexts(i) }
    return contextGen.flatMap { context ->
        tuples(constant(context), plaintextBallotsArbitrary(context, numBallots))
    }
}

/** Useful in testing, when we expect a value to be non-null. */
fun <T> T?.failIfNull(): T = this ?: fail { "non-null value expected" }
