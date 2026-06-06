package com.example.padlecano.domain.usecase

/**
 * Generates an Americano padel schedule using the circle (round-robin) method.
 *
 * For [playerCount] players (must be a positive multiple of 4):
 * - Produces [playerCount] - 1 rounds.
 * - Every round: [playerCount] / 4 courts, all players participate.
 * - Each player partners with every other player exactly once.
 * - Each player faces every other player as an opponent at least once.
 * - A player faces the same opponent at most twice.
 * - Second meetings are scheduled at least one round apart when possible.
 *
 * Algorithm: fix player index 0, rotate indices 1..[playerCount]-1 by one
 * position per round to build partner pairs. Then search court pairings across
 * rounds to satisfy opponent coverage and rematch spacing rules.
 */
object AmericanoScheduleGenerator {
    private const val MAX_OPPONENT_MATCHES: Int = 2
    private const val MIN_ROUNDS_BETWEEN_OPPONENT_REMATCH: Int = 2
    private const val SPACED_REMATCH_SEARCH_PLAYER_LIMIT: Int = 8

    data class MatchSetup(
        val playerA1: Int,
        val playerA2: Int,
        val playerB1: Int,
        val playerB2: Int,
    )

    private data class OpponentScheduleState(
        val counts: Array<IntArray>,
        val lastRound: Array<IntArray>,
    )

    fun generate(playerCount: Int): List<List<MatchSetup>> {
        require(playerCount >= 4 && playerCount % 4 == 0) {
            "Player count must be a positive multiple of 4, got $playerCount"
        }
        val partnerRounds: List<List<Pair<Int, Int>>> = (0 until playerCount - 1).map { roundIndex: Int ->
            buildPartnerPairs(playerCount, roundIndex)
        }
        val courtOptionsPerRound: List<List<List<Pair<Pair<Int, Int>, Pair<Int, Int>>>>> =
            partnerRounds.map { partnerPairs: List<Pair<Int, Int>> ->
                buildAllCourtPairings(partnerPairs)
            }
        val schedule: List<List<MatchSetup>>? = if (playerCount <= SPACED_REMATCH_SEARCH_PLAYER_LIMIT) {
            findBestSpacedSchedule(
                playerCount = playerCount,
                courtOptionsPerRound = courtOptionsPerRound,
            )
        } else {
            buildSchedule(
                playerCount = playerCount,
                courtOptionsPerRound = courtOptionsPerRound,
                enforceSpacedRematches = false,
            )
        }
        requireNotNull(schedule) {
            "Could not build a schedule with full opponent coverage for $playerCount players"
        }
        return schedule
    }

    private fun buildSchedule(
        playerCount: Int,
        courtOptionsPerRound: List<List<List<Pair<Pair<Int, Int>, Pair<Int, Int>>>>>,
        enforceSpacedRematches: Boolean,
    ): List<List<MatchSetup>>? {
        val opponentState: OpponentScheduleState = createOpponentScheduleState(playerCount)
        val selectedCourts: MutableList<List<Pair<Pair<Int, Int>, Pair<Int, Int>>>> = mutableListOf()
        return searchSchedule(
            playerCount = playerCount,
            roundIndex = 0,
            courtOptionsPerRound = courtOptionsPerRound,
            opponentState = opponentState,
            selectedCourts = selectedCourts,
            enforceSpacedRematches = enforceSpacedRematches,
        )
    }

    private fun findBestSpacedSchedule(
        playerCount: Int,
        courtOptionsPerRound: List<List<List<Pair<Pair<Int, Int>, Pair<Int, Int>>>>>,
    ): List<List<MatchSetup>>? {
        val opponentState: OpponentScheduleState = createOpponentScheduleState(playerCount)
        val selectedCourts: MutableList<List<Pair<Pair<Int, Int>, Pair<Int, Int>>>> = mutableListOf()
        var bestSchedule: List<List<MatchSetup>>? = null
        var bestSpacedRematches: Int = -1
        var bestConsecutiveRematches: Int = Int.MAX_VALUE
        var bestPairsWithTwoMatches: Int = -1
        searchAllSchedules(
            playerCount = playerCount,
            roundIndex = 0,
            courtOptionsPerRound = courtOptionsPerRound,
            opponentState = opponentState,
            selectedCourts = selectedCourts,
            onComplete = { schedule: List<List<MatchSetup>>, rematchStats: Triple<Int, Int, Int> ->
                val spacedRematches: Int = rematchStats.first
                val consecutiveRematches: Int = rematchStats.second
                val pairsWithTwoMatches: Int = rematchStats.third
                if (spacedRematches > bestSpacedRematches ||
                    (spacedRematches == bestSpacedRematches && consecutiveRematches < bestConsecutiveRematches) ||
                    (spacedRematches == bestSpacedRematches &&
                        consecutiveRematches == bestConsecutiveRematches &&
                        pairsWithTwoMatches > bestPairsWithTwoMatches)
                ) {
                    bestSpacedRematches = spacedRematches
                    bestConsecutiveRematches = consecutiveRematches
                    bestPairsWithTwoMatches = pairsWithTwoMatches
                    bestSchedule = schedule
                }
            },
        )
        return bestSchedule
    }

    private fun searchAllSchedules(
        playerCount: Int,
        roundIndex: Int,
        courtOptionsPerRound: List<List<List<Pair<Pair<Int, Int>, Pair<Int, Int>>>>>,
        opponentState: OpponentScheduleState,
        selectedCourts: MutableList<List<Pair<Pair<Int, Int>, Pair<Int, Int>>>>,
        onComplete: (List<List<MatchSetup>>, Triple<Int, Int, Int>) -> Unit,
    ) {
        if (roundIndex == playerCount - 1) {
            if (!hasMinimumOpponentCoverage(opponentState, playerCount)) {
                return
            }
            val schedule: List<List<MatchSetup>> = selectedCourts.map { roundCourts: List<Pair<Pair<Int, Int>, Pair<Int, Int>>> ->
                roundCourts.map { court: Pair<Pair<Int, Int>, Pair<Int, Int>> ->
                    val pairA: Pair<Int, Int> = court.first
                    val pairB: Pair<Int, Int> = court.second
                    MatchSetup(
                        playerA1 = pairA.first,
                        playerA2 = pairA.second,
                        playerB1 = pairB.first,
                        playerB2 = pairB.second,
                    )
                }
            }
            val rematchStats: Triple<Int, Int, Int> = countSpacedRematchStats(selectedCourts, playerCount)
            onComplete(schedule, rematchStats)
            return
        }
        for (courtPairings: List<Pair<Pair<Int, Int>, Pair<Int, Int>>> in courtOptionsPerRound[roundIndex]) {
            if (!isValidCourtPairings(
                    courtPairings = courtPairings,
                    roundIndex = roundIndex,
                    opponentState = opponentState,
                    enforceSpacedRematches = false,
                )
            ) {
                continue
            }
            val nextState: OpponentScheduleState = cloneOpponentState(opponentState, playerCount)
            applyCourtPairings(courtPairings, roundIndex, nextState)
            selectedCourts.add(courtPairings)
            searchAllSchedules(
                playerCount = playerCount,
                roundIndex = roundIndex + 1,
                courtOptionsPerRound = courtOptionsPerRound,
                opponentState = nextState,
                selectedCourts = selectedCourts,
                onComplete = onComplete,
            )
            selectedCourts.removeAt(selectedCourts.lastIndex)
        }
    }

    private fun countSpacedRematchStats(
        selectedCourts: List<List<Pair<Pair<Int, Int>, Pair<Int, Int>>>>,
        playerCount: Int,
    ): Triple<Int, Int, Int> {
        val opponentRounds: MutableMap<Long, MutableList<Int>> = mutableMapOf()
        selectedCourts.forEachIndexed { roundIndex: Int, roundCourts: List<Pair<Pair<Int, Int>, Pair<Int, Int>>> ->
            roundCourts.forEach { court: Pair<Pair<Int, Int>, Pair<Int, Int>> ->
                recordOpponentRounds(opponentRounds, court, roundIndex, playerCount)
            }
        }
        var spacedRematches: Int = 0
        var consecutiveRematches: Int = 0
        var pairsWithTwoMatches: Int = 0
        for (playerA: Int in 0 until playerCount) {
            for (playerB: Int in playerA + 1 until playerCount) {
                val key: Long = pairKey(playerA, playerB, playerCount)
                val rounds: List<Int> = opponentRounds.getOrDefault(key, emptyList()).sorted()
                if (rounds.size == MAX_OPPONENT_MATCHES) {
                    pairsWithTwoMatches++
                }
                if (rounds.size >= 2) {
                    if (rounds[1] - rounds[0] >= MIN_ROUNDS_BETWEEN_OPPONENT_REMATCH) {
                        spacedRematches++
                    } else {
                        consecutiveRematches++
                    }
                }
            }
        }
        return Triple(spacedRematches, consecutiveRematches, pairsWithTwoMatches)
    }

    private fun recordOpponentRounds(
        opponentRounds: MutableMap<Long, MutableList<Int>>,
        court: Pair<Pair<Int, Int>, Pair<Int, Int>>,
        roundIndex: Int,
        playerCount: Int,
    ) {
        val teamA: List<Int> = listOf(court.first.first, court.first.second)
        val teamB: List<Int> = listOf(court.second.first, court.second.second)
        for (playerA: Int in teamA) {
            for (playerB: Int in teamB) {
                if (playerA != playerB) {
                    val key: Long = pairKey(minOf(playerA, playerB), maxOf(playerA, playerB), playerCount)
                    val rounds: MutableList<Int> = opponentRounds.getOrPut(key) { mutableListOf() }
                    rounds.add(roundIndex)
                }
            }
        }
    }

    private fun pairKey(playerA: Int, playerB: Int, playerCount: Int): Long {
        return playerA.toLong() * playerCount + playerB
    }

    private fun createOpponentScheduleState(playerCount: Int): OpponentScheduleState {
        return OpponentScheduleState(
            counts = Array(playerCount) { IntArray(playerCount) },
            lastRound = Array(playerCount) { IntArray(playerCount) { -1 } },
        )
    }

    private fun searchSchedule(
        playerCount: Int,
        roundIndex: Int,
        courtOptionsPerRound: List<List<List<Pair<Pair<Int, Int>, Pair<Int, Int>>>>>,
        opponentState: OpponentScheduleState,
        selectedCourts: MutableList<List<Pair<Pair<Int, Int>, Pair<Int, Int>>>>,
        enforceSpacedRematches: Boolean,
    ): List<List<MatchSetup>>? {
        if (roundIndex == playerCount - 1) {
            if (!hasMinimumOpponentCoverage(opponentState, playerCount)) {
                return null
            }
            return selectedCourts.map { roundCourts: List<Pair<Pair<Int, Int>, Pair<Int, Int>>> ->
                roundCourts.map { court: Pair<Pair<Int, Int>, Pair<Int, Int>> ->
                    val pairA: Pair<Int, Int> = court.first
                    val pairB: Pair<Int, Int> = court.second
                    MatchSetup(
                        playerA1 = pairA.first,
                        playerA2 = pairA.second,
                        playerB1 = pairB.first,
                        playerB2 = pairB.second,
                    )
                }
            }
        }
        val sortedOptions: List<List<Pair<Pair<Int, Int>, Pair<Int, Int>>>> =
            courtOptionsPerRound[roundIndex]
                .filter { option: List<Pair<Pair<Int, Int>, Pair<Int, Int>>> ->
                    isValidCourtPairings(
                        courtPairings = option,
                        roundIndex = roundIndex,
                        opponentState = opponentState,
                        enforceSpacedRematches = enforceSpacedRematches,
                    )
                }
                .sortedWith(buildCourtOptionComparator(roundIndex, opponentState, enforceSpacedRematches))
        for (courtPairings: List<Pair<Pair<Int, Int>, Pair<Int, Int>>> in sortedOptions) {
            val nextState: OpponentScheduleState = cloneOpponentState(opponentState, playerCount)
            applyCourtPairings(courtPairings, roundIndex, nextState)
            selectedCourts.add(courtPairings)
            val result: List<List<MatchSetup>>? = searchSchedule(
                playerCount = playerCount,
                roundIndex = roundIndex + 1,
                courtOptionsPerRound = courtOptionsPerRound,
                opponentState = nextState,
                selectedCourts = selectedCourts,
                enforceSpacedRematches = enforceSpacedRematches,
            )
            if (result != null) {
                return result
            }
            selectedCourts.removeAt(selectedCourts.lastIndex)
        }
        return null
    }

    private fun buildPartnerPairs(playerCount: Int, roundIndex: Int): List<Pair<Int, Int>> {
        val rotated: List<Int> = (0 until playerCount).map { position: Int ->
            if (position == 0) {
                0
            } else {
                ((position - 1 + roundIndex) % (playerCount - 1)) + 1
            }
        }
        return (0 until playerCount / 2).map { index: Int ->
            rotated[index] to rotated[playerCount - 1 - index]
        }
    }

    private fun buildAllCourtPairings(
        partnerPairs: List<Pair<Int, Int>>,
    ): List<List<Pair<Pair<Int, Int>, Pair<Int, Int>>>> {
        if (partnerPairs.isEmpty()) {
            return listOf(emptyList())
        }
        if (partnerPairs.size == 2) {
            return listOf(listOf(partnerPairs[0] to partnerPairs[1]))
        }
        val firstPair: Pair<Int, Int> = partnerPairs.first()
        val remainingPairs: List<Pair<Int, Int>> = partnerPairs.drop(1)
        val results: MutableList<List<Pair<Pair<Int, Int>, Pair<Int, Int>>>> = mutableListOf()
        for (opponentIndex: Int in remainingPairs.indices) {
            val opponentPair: Pair<Int, Int> = remainingPairs[opponentIndex]
            val rest: List<Pair<Int, Int>> = remainingPairs.filterIndexed { index: Int, _: Pair<Int, Int> ->
                index != opponentIndex
            }
            val subPairings: List<List<Pair<Pair<Int, Int>, Pair<Int, Int>>>> = buildAllCourtPairings(rest)
            for (sub: List<Pair<Pair<Int, Int>, Pair<Int, Int>>> in subPairings) {
                results.add(listOf(firstPair to opponentPair) + sub)
            }
        }
        return results
    }

    private fun isValidCourtPairings(
        courtPairings: List<Pair<Pair<Int, Int>, Pair<Int, Int>>>,
        roundIndex: Int,
        opponentState: OpponentScheduleState,
        enforceSpacedRematches: Boolean,
    ): Boolean {
        for (court: Pair<Pair<Int, Int>, Pair<Int, Int>> in courtPairings) {
            val pairA: Pair<Int, Int> = court.first
            val pairB: Pair<Int, Int> = court.second
            val playersA: List<Int> = listOf(pairA.first, pairA.second)
            val playersB: List<Int> = listOf(pairB.first, pairB.second)
            for (playerA: Int in playersA) {
                for (playerB: Int in playersB) {
                    if (playerA != playerB && !canMeetAsOpponents(
                            playerA = playerA,
                            playerB = playerB,
                            roundIndex = roundIndex,
                            opponentState = opponentState,
                            enforceSpacedRematches = enforceSpacedRematches,
                        )
                    ) {
                        return false
                    }
                }
            }
        }
        return true
    }

    private fun canMeetAsOpponents(
        playerA: Int,
        playerB: Int,
        roundIndex: Int,
        opponentState: OpponentScheduleState,
        enforceSpacedRematches: Boolean,
    ): Boolean {
        val currentCount: Int = opponentState.counts[playerA][playerB]
        if (enforceSpacedRematches && currentCount == 1) {
            val lastRound: Int = opponentState.lastRound[playerA][playerB]
            if (lastRound >= 0 && roundIndex - lastRound < MIN_ROUNDS_BETWEEN_OPPONENT_REMATCH) {
                return false
            }
        }
        return true
    }

    private fun buildCourtOptionComparator(
        roundIndex: Int,
        opponentState: OpponentScheduleState,
        enforceSpacedRematches: Boolean,
    ): Comparator<List<Pair<Pair<Int, Int>, Pair<Int, Int>>>> {
        return if (enforceSpacedRematches) {
            compareBy<List<Pair<Pair<Int, Int>, Pair<Int, Int>>>> { option: List<Pair<Pair<Int, Int>, Pair<Int, Int>>> ->
                -countSpacedSecondMeetings(option, roundIndex, opponentState)
            }.thenBy { option: List<Pair<Pair<Int, Int>, Pair<Int, Int>>> ->
                -countNewOpponents(option, opponentState.counts)
            }.thenBy { option: List<Pair<Pair<Int, Int>, Pair<Int, Int>>> ->
                totalCourtCost(option, opponentState.counts)
            }
        } else {
            compareBy<List<Pair<Pair<Int, Int>, Pair<Int, Int>>>> { option: List<Pair<Pair<Int, Int>, Pair<Int, Int>>> ->
                -countNewOpponents(option, opponentState.counts)
            }.thenBy { option: List<Pair<Pair<Int, Int>, Pair<Int, Int>>> ->
                totalCourtCost(option, opponentState.counts)
            }
        }
    }

    private fun countSpacedSecondMeetings(
        courtPairings: List<Pair<Pair<Int, Int>, Pair<Int, Int>>>,
        roundIndex: Int,
        opponentState: OpponentScheduleState,
    ): Int {
        return courtPairings.sumOf { court: Pair<Pair<Int, Int>, Pair<Int, Int>> ->
            val pairA: Pair<Int, Int> = court.first
            val pairB: Pair<Int, Int> = court.second
            val playersA: List<Int> = listOf(pairA.first, pairA.second)
            val playersB: List<Int> = listOf(pairB.first, pairB.second)
            playersA.sumOf { playerA: Int ->
                playersB.count { playerB: Int ->
                    playerA != playerB &&
                        opponentState.counts[playerA][playerB] == 1 &&
                        roundIndex - opponentState.lastRound[playerA][playerB] >= MIN_ROUNDS_BETWEEN_OPPONENT_REMATCH
                }
            }
        }
    }

    private fun countNewOpponents(
        courtPairings: List<Pair<Pair<Int, Int>, Pair<Int, Int>>>,
        opponentCounts: Array<IntArray>,
    ): Int {
        return courtPairings.sumOf { court: Pair<Pair<Int, Int>, Pair<Int, Int>> ->
            val pairA: Pair<Int, Int> = court.first
            val pairB: Pair<Int, Int> = court.second
            val playersA: List<Int> = listOf(pairA.first, pairA.second)
            val playersB: List<Int> = listOf(pairB.first, pairB.second)
            playersA.sumOf { playerA: Int ->
                playersB.count { playerB: Int ->
                    playerA != playerB && opponentCounts[playerA][playerB] == 0
                }
            }
        }
    }

    private fun totalCourtCost(
        courtPairings: List<Pair<Pair<Int, Int>, Pair<Int, Int>>>,
        opponentCounts: Array<IntArray>,
    ): Int {
        return courtPairings.sumOf { court: Pair<Pair<Int, Int>, Pair<Int, Int>> ->
            courtOpponentCost(court.first, court.second, opponentCounts)
        }
    }

    private fun courtOpponentCost(
        pairA: Pair<Int, Int>,
        pairB: Pair<Int, Int>,
        opponentCounts: Array<IntArray>,
    ): Int {
        val playersA: List<Int> = listOf(pairA.first, pairA.second)
        val playersB: List<Int> = listOf(pairB.first, pairB.second)
        return playersA.sumOf { playerA: Int ->
            playersB.sumOf { playerB: Int ->
                if (playerA == playerB) {
                    0
                } else {
                    opponentCounts[playerA][playerB]
                }
            }
        }
    }

    private fun applyCourtPairings(
        courtPairings: List<Pair<Pair<Int, Int>, Pair<Int, Int>>>,
        roundIndex: Int,
        opponentState: OpponentScheduleState,
    ) {
        for (court: Pair<Pair<Int, Int>, Pair<Int, Int>> in courtPairings) {
            val teamA: List<Int> = listOf(court.first.first, court.first.second)
            val teamB: List<Int> = listOf(court.second.first, court.second.second)
            for (playerA: Int in teamA) {
                for (playerB: Int in teamB) {
                    if (playerA != playerB) {
                        recordOpponentMeeting(playerA, playerB, roundIndex, opponentState)
                    }
                }
            }
        }
    }

    private fun recordOpponentMeeting(
        playerA: Int,
        playerB: Int,
        roundIndex: Int,
        opponentState: OpponentScheduleState,
    ) {
        opponentState.counts[playerA][playerB]++
        opponentState.counts[playerB][playerA]++
        opponentState.lastRound[playerA][playerB] = roundIndex
        opponentState.lastRound[playerB][playerA] = roundIndex
    }

    private fun cloneOpponentState(
        opponentState: OpponentScheduleState,
        playerCount: Int,
    ): OpponentScheduleState {
        return OpponentScheduleState(
            counts = Array(playerCount) { rowIndex: Int -> opponentState.counts[rowIndex].clone() },
            lastRound = Array(playerCount) { rowIndex: Int -> opponentState.lastRound[rowIndex].clone() },
        )
    }

    private fun hasMinimumOpponentCoverage(
        opponentState: OpponentScheduleState,
        playerCount: Int,
    ): Boolean {
        for (playerA: Int in 0 until playerCount) {
            for (playerB: Int in playerA + 1 until playerCount) {
                if (opponentState.counts[playerA][playerB] < 1) {
                    return false
                }
            }
        }
        return true
    }
}
