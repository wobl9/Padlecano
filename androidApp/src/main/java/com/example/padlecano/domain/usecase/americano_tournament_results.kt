package com.example.padlecano.domain.usecase

import com.example.padlecano.domain.model.OtherPlayerScheduleCheck
import com.example.padlecano.domain.model.PlayerScheduleVerification
import com.example.padlecano.domain.model.PlayerStandingRow
import com.example.padlecano.domain.model.ScheduleVerificationReport
import com.example.padlecano.domain.model.TournamentMatchRecord

object AmericanoTournamentResults {

    fun computeStandings(
        playerNames: List<String>,
        matches: List<TournamentMatchRecord>,
    ): List<PlayerStandingRow> {
        val n: Int = playerNames.size
        if (n == 0) {
            return emptyList()
        }
        val points: IntArray = IntArray(n)
        val wins: IntArray = IntArray(n)
        val played: IntArray = IntArray(n)
        for (m: TournamentMatchRecord in matches) {
            if (!m.isScoreSet) {
                continue
            }
            val teamA: IntArray = intArrayOf(m.playerA1Index, m.playerA2Index)
            val teamB: IntArray = intArrayOf(m.playerB1Index, m.playerB2Index)
            for (p: Int in teamA) {
                if (p in 0 until n) {
                    played[p]++
                    points[p] += m.scoreA
                }
            }
            for (p: Int in teamB) {
                if (p in 0 until n) {
                    played[p]++
                    points[p] += m.scoreB
                }
            }
            when {
                m.scoreA > m.scoreB -> {
                    if (m.playerA1Index in 0 until n) {
                        wins[m.playerA1Index]++
                    }
                    if (m.playerA2Index in 0 until n) {
                        wins[m.playerA2Index]++
                    }
                }
                m.scoreB > m.scoreA -> {
                    if (m.playerB1Index in 0 until n) {
                        wins[m.playerB1Index]++
                    }
                    if (m.playerB2Index in 0 until n) {
                        wins[m.playerB2Index]++
                    }
                }
            }
        }
        return playerNames.mapIndexed { index: Int, name: String ->
            PlayerStandingRow(
                playerIndex = index,
                displayName = name,
                totalPoints = points[index],
                matchesPlayed = played[index],
                wins = wins[index],
            )
        }
    }

    fun verifyScheduleAgainstIdeal(
        playerNames: List<String>,
        allScheduledMatches: List<TournamentMatchRecord>,
    ): ScheduleVerificationReport {
        val n: Int = playerNames.size
        if (n < 4 || n % 4 != 0) {
            return ScheduleVerificationReport(
                playerNames = playerNames,
                byPlayer = emptyList(),
                allOk = false,
            )
        }
        val schedule: List<List<AmericanoScheduleGenerator.MatchSetup>> =
            AmericanoScheduleGenerator.generate(n)
        val expectedPartner: Map<Pair<Int, Int>, Int> = countPartners(schedule)
        val expectedOpponent: Map<Pair<Int, Int>, Int> = countOpponents(schedule)
        val actualPartner: Map<Pair<Int, Int>, Int> = countPartnersFromRecords(allScheduledMatches)
        val actualOpponent: Map<Pair<Int, Int>, Int> = countOpponentsFromRecords(allScheduledMatches)

        val byPlayer: List<PlayerScheduleVerification> = (0 until n).map { i: Int ->
            val checks: List<OtherPlayerScheduleCheck> = (0 until n).filter { j: Int -> j != i }.map { j: Int ->
                val key: Pair<Int, Int> = pairKey(i, j)
                OtherPlayerScheduleCheck(
                    otherPlayerIndex = j,
                    timesAsPartner = actualPartner.getOrDefault(key, 0),
                    expectedTimesAsPartner = expectedPartner.getOrDefault(key, 0),
                    timesAsOpponent = actualOpponent.getOrDefault(key, 0),
                    expectedTimesAsOpponent = expectedOpponent.getOrDefault(key, 0),
                )
            }
            PlayerScheduleVerification(playerIndex = i, withEachOtherPlayer = checks)
        }

        val allOk: Boolean = (0 until n).all { i: Int ->
            (0 until n).filter { j: Int -> j != i }.all { j: Int ->
                val key: Pair<Int, Int> = pairKey(i, j)
                actualPartner.getOrDefault(key, 0) == expectedPartner.getOrDefault(key, 0) &&
                    actualOpponent.getOrDefault(key, 0) == expectedOpponent.getOrDefault(key, 0)
            }
        }

        return ScheduleVerificationReport(
            playerNames = playerNames,
            byPlayer = byPlayer,
            allOk = allOk && allScheduledMatches.isNotEmpty(),
        )
    }

    private fun countPartners(
        schedule: List<List<AmericanoScheduleGenerator.MatchSetup>>,
    ): Map<Pair<Int, Int>, Int> {
        val map: MutableMap<Pair<Int, Int>, Int> = mutableMapOf()
        for (round: List<AmericanoScheduleGenerator.MatchSetup> in schedule) {
            for (m: AmericanoScheduleGenerator.MatchSetup in round) {
                addPartnerCount(map, m.playerA1, m.playerA2)
                addPartnerCount(map, m.playerB1, m.playerB2)
            }
        }
        return map
    }

    private fun countPartnersFromRecords(matches: List<TournamentMatchRecord>): Map<Pair<Int, Int>, Int> {
        val map: MutableMap<Pair<Int, Int>, Int> = mutableMapOf()
        for (m: TournamentMatchRecord in matches) {
            addPartnerCount(map, m.playerA1Index, m.playerA2Index)
            addPartnerCount(map, m.playerB1Index, m.playerB2Index)
        }
        return map
    }

    private fun addPartnerCount(map: MutableMap<Pair<Int, Int>, Int>, i: Int, j: Int) {
        val k: Pair<Int, Int> = pairKey(i, j)
        map[k] = map.getOrDefault(k, 0) + 1
    }

    private fun countOpponents(
        schedule: List<List<AmericanoScheduleGenerator.MatchSetup>>,
    ): Map<Pair<Int, Int>, Int> {
        val map: MutableMap<Pair<Int, Int>, Int> = mutableMapOf()
        for (round: List<AmericanoScheduleGenerator.MatchSetup> in schedule) {
            for (m: AmericanoScheduleGenerator.MatchSetup in round) {
                addAllOpponents(map, m)
            }
        }
        return map
    }

    private fun countOpponentsFromRecords(matches: List<TournamentMatchRecord>): Map<Pair<Int, Int>, Int> {
        val map: MutableMap<Pair<Int, Int>, Int> = mutableMapOf()
        for (m: TournamentMatchRecord in matches) {
            addAllOpponentsFromRecord(map, m)
        }
        return map
    }

    private fun addAllOpponents(map: MutableMap<Pair<Int, Int>, Int>, m: AmericanoScheduleGenerator.MatchSetup) {
        val a: IntArray = intArrayOf(m.playerA1, m.playerA2)
        val b: IntArray = intArrayOf(m.playerB1, m.playerB2)
        for (x: Int in a) {
            for (y: Int in b) {
                if (x != y) {
                    val k: Pair<Int, Int> = pairKey(x, y)
                    map[k] = map.getOrDefault(k, 0) + 1
                }
            }
        }
    }

    private fun addAllOpponentsFromRecord(map: MutableMap<Pair<Int, Int>, Int>, m: TournamentMatchRecord) {
        val a: IntArray = intArrayOf(m.playerA1Index, m.playerA2Index)
        val b: IntArray = intArrayOf(m.playerB1Index, m.playerB2Index)
        for (x: Int in a) {
            for (y: Int in b) {
                if (x != y) {
                    val k: Pair<Int, Int> = pairKey(x, y)
                    map[k] = map.getOrDefault(k, 0) + 1
                }
            }
        }
    }

    private fun pairKey(i: Int, j: Int): Pair<Int, Int> = if (i < j) i to j else j to i
}
