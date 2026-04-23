package com.example.padlecano.domain.usecase

/**
 * Generates an Americano padel schedule using the circle (round-robin) method.
 *
 * For [playerCount] players (must be a positive multiple of 4):
 * - Produces [playerCount] - 1 rounds.
 * - Every round: [playerCount] / 4 courts, all players participate.
 * - Each player partners with every other player exactly once.
 *
 * Algorithm: fix player index 0, rotate indices 1..[playerCount]-1 by one
 * position per round. Pair positions symmetrically in the circle and split
 * each consecutive pair of partner-pairs into one court match.
 */
object AmericanoScheduleGenerator {
    data class MatchSetup(
        val playerA1: Int,
        val playerA2: Int,
        val playerB1: Int,
        val playerB2: Int,
    )

    fun generate(playerCount: Int): List<List<MatchSetup>> {
        require(playerCount >= 4 && playerCount % 4 == 0) {
            "Player count must be a positive multiple of 4, got $playerCount"
        }
        val n = playerCount
        return (0 until n - 1).map { round ->
            val rotated: List<Int> = (0 until n).map { pos ->
                if (pos == 0) 0 else ((pos - 1 + round) % (n - 1)) + 1
            }
            val pairs: List<Pair<Int, Int>> = (0 until n / 2).map { i ->
                rotated[i] to rotated[n - 1 - i]
            }
            (0 until n / 4).map { courtIndex ->
                val (a1, a2) = pairs[courtIndex * 2]
                val (b1, b2) = pairs[courtIndex * 2 + 1]
                MatchSetup(playerA1 = a1, playerA2 = a2, playerB1 = b1, playerB2 = b2)
            }
        }
    }
}
