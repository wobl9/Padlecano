package com.example.padlecano.domain.usecase

import com.example.padlecano.domain.GoldenSnapshotFormatter
import com.example.padlecano.domain.model.PlayerStandingRow
import com.example.padlecano.domain.model.RawMatchForAudit
import com.example.padlecano.domain.model.RawRoundForAudit
import com.example.padlecano.domain.model.TournamentMatchRecord
import com.example.padlecano.domain.model.TournamentRoundResults
import com.example.padlecano.domain.model.TournamentShareTextLabels
import com.example.padlecano.domain.model.TournamentType
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Characterization (golden) tests for pure domain logic.
 * Snapshots lock current behavior before KMP migration; any output change fails CI.
 */
class DomainLogicGoldenTest {

    private val shareLabels: TournamentShareTextLabels = TournamentShareTextLabels(
        untitledTournament = "Untitled tournament",
        tournamentTypeAmericano = "Americano",
        appFooter = "Padlecano",
        standingsSectionTitle = "STANDINGS",
        matchesSectionTitle = "MATCHES",
        rankColumn = "#",
        playerColumn = "Player",
        pointsColumn = "Pts",
        gamesColumn = "Games",
        winsColumn = "Wins",
        roundHeaderFormat = "Round %d of %d",
        courtLabelFormat = "Court %d",
        teamVsFormat = "%s vs %s",
        scoreLineFormat = "%d : %d",
        scoreNotEntered = "Not entered yet",
        teamPairSeparator = " & ",
    )

    @Test
    fun golden_americanoSchedule_fourPlayers() {
        assertScheduleGolden(playerCount = 4, expected = GOLDEN_SCHEDULE_4_PLAYERS)
    }

    @Test
    fun golden_americanoSchedule_eightPlayers() {
        assertScheduleGolden(playerCount = 8, expected = GOLDEN_SCHEDULE_8_PLAYERS)
    }

    @Test
    fun golden_americanoSchedule_twelvePlayers() {
        assertScheduleGolden(playerCount = 12, expected = GOLDEN_SCHEDULE_12_PLAYERS)
    }

    @Test
    fun golden_standings_eightPlayerPartialScores() {
        val names: List<String> = listOf("P0", "P1", "P2", "P3", "P4", "P5", "P6", "P7")
        val schedule: List<List<AmericanoScheduleGenerator.MatchSetup>> =
            AmericanoScheduleGenerator.generate(names.size)
        val records: List<TournamentMatchRecord> = buildScoredRecordsFromSchedule(
            schedule = schedule,
            scoresByRound = listOf(
                listOf(6 to 2),
                listOf(4 to 4),
            ),
        )
        val actualStandings: String = GoldenSnapshotFormatter.formatStandings(
            AmericanoTournamentResults.computeStandings(names, records),
        )
        assertEquals(GOLDEN_STANDINGS_8_PLAYERS_PARTIAL, actualStandings)
    }

    @Test
    fun golden_scheduleVerification_fourPlayers() {
        val names: List<String> = listOf("A", "B", "C", "D")
        val schedule: List<List<AmericanoScheduleGenerator.MatchSetup>> =
            AmericanoScheduleGenerator.generate(names.size)
        val records: List<TournamentMatchRecord> = scheduleToRecords(schedule)
        val report = AmericanoTournamentResults.verifyScheduleAgainstIdeal(
            playerNames = names,
            allScheduledMatches = records,
        )
        val actual: String = GoldenSnapshotFormatter.formatScheduleVerification(report)
        assertEquals(GOLDEN_VERIFICATION_4_PLAYERS, actual)
    }

    @Test
    fun golden_matchValidityAudit_mixedIssues() {
        val audit = MatchValidityAuditBuilder.build(
            tournamentTitle = "Golden Cup",
            maxCombinedMatchScore = 8,
            playerDisplayNames = listOf("Alice", "Bob", "Carol", "Dave"),
            rounds = listOf(
                RawRoundForAudit(
                    roundNumber = 1,
                    matches = listOf(
                        RawMatchForAudit(
                            matchId = "match-1",
                            playerA1Index = 0,
                            playerA2Index = 1,
                            playerB1Index = 2,
                            playerB2Index = 3,
                            scoreA = 6,
                            scoreB = 2,
                            isScoreSet = true,
                        ),
                        RawMatchForAudit(
                            matchId = "match-2",
                            playerA1Index = 0,
                            playerA2Index = 0,
                            playerB1Index = 1,
                            playerB2Index = 2,
                            scoreA = -1,
                            scoreB = 10,
                            isScoreSet = true,
                        ),
                    ),
                ),
                RawRoundForAudit(
                    roundNumber = 2,
                    matches = listOf(
                        RawMatchForAudit(
                            matchId = "match-3",
                            playerA1Index = 0,
                            playerA2Index = 2,
                            playerB1Index = 1,
                            playerB2Index = 3,
                            scoreA = 0,
                            scoreB = 0,
                            isScoreSet = false,
                        ),
                    ),
                ),
            ),
        )
        val actual: String = GoldenSnapshotFormatter.formatMatchValidityAudit(audit)
        assertEquals(GOLDEN_MATCH_VALIDITY_AUDIT, actual)
    }

    @Test
    fun golden_shareText_fullTournamentSummary() {
        val actualText: String = TournamentShareTextFormatter.format(
            tournamentTitle = "Saturday Americano",
            tournamentType = TournamentType.AMERICANO,
            playerNames = listOf("Alice", "Bob", "Carol", "Dave"),
            standingsRows = listOf(
                PlayerStandingRow(0, "Alice", 12, 2, 2),
                PlayerStandingRow(1, "Bob", 10, 2, 1),
                PlayerStandingRow(2, "Carol", 8, 2, 1),
                PlayerStandingRow(3, "Dave", 6, 2, 0),
            ),
            rounds = listOf(
                TournamentRoundResults(
                    roundNumber = 1,
                    matches = listOf(
                        TournamentMatchRecord(0, 1, 2, 3, 6, 2, true),
                    ),
                ),
                TournamentRoundResults(
                    roundNumber = 2,
                    matches = listOf(
                        TournamentMatchRecord(0, 2, 1, 3, 6, 4, true),
                    ),
                ),
            ),
            labels = shareLabels,
        )
        assertEquals(GOLDEN_SHARE_TEXT, actualText)
    }

    private fun assertScheduleGolden(playerCount: Int, expected: String) {
        val schedule: List<List<AmericanoScheduleGenerator.MatchSetup>> =
            AmericanoScheduleGenerator.generate(playerCount)
        val actual: String = GoldenSnapshotFormatter.formatSchedule(playerCount, schedule)
        assertEquals(expected, actual)
    }

    private fun scheduleToRecords(
        schedule: List<List<AmericanoScheduleGenerator.MatchSetup>>,
    ): List<TournamentMatchRecord> {
        return schedule.flatMap { round: List<AmericanoScheduleGenerator.MatchSetup> ->
            round.map { match: AmericanoScheduleGenerator.MatchSetup ->
                TournamentMatchRecord(
                    playerA1Index = match.playerA1,
                    playerA2Index = match.playerA2,
                    playerB1Index = match.playerB1,
                    playerB2Index = match.playerB2,
                    scoreA = 0,
                    scoreB = 0,
                    isScoreSet = false,
                )
            }
        }
    }

    private fun buildScoredRecordsFromSchedule(
        schedule: List<List<AmericanoScheduleGenerator.MatchSetup>>,
        scoresByRound: List<List<Pair<Int, Int>>>,
    ): List<TournamentMatchRecord> {
        return schedule.mapIndexed { roundIndex: Int, round: List<AmericanoScheduleGenerator.MatchSetup> ->
            val roundScores: List<Pair<Int, Int>> = scoresByRound.getOrElse(roundIndex) { emptyList() }
            round.mapIndexed { courtIndex: Int, match: AmericanoScheduleGenerator.MatchSetup ->
                val score: Pair<Int, Int>? = roundScores.getOrNull(courtIndex)
                if (score != null) {
                    TournamentMatchRecord(
                        playerA1Index = match.playerA1,
                        playerA2Index = match.playerA2,
                        playerB1Index = match.playerB1,
                        playerB2Index = match.playerB2,
                        scoreA = score.first,
                        scoreB = score.second,
                        isScoreSet = true,
                    )
                } else {
                    TournamentMatchRecord(
                        playerA1Index = match.playerA1,
                        playerA2Index = match.playerA2,
                        playerB1Index = match.playerB1,
                        playerB2Index = match.playerB2,
                        scoreA = 0,
                        scoreB = 0,
                        isScoreSet = false,
                    )
                }
            }
        }.flatten()
    }

    private companion object {
        val GOLDEN_SCHEDULE_4_PLAYERS: String = """
            players=4
            round=1: 0,3|1,2
            round=2: 0,1|2,3
            round=3: 0,2|3,1
        """.trimIndent()

        val GOLDEN_SCHEDULE_8_PLAYERS: String = """
            players=8
            round=1: 0,7|1,6 2,5|3,4
            round=2: 0,1|2,7 3,6|4,5
            round=3: 0,2|5,6 3,1|4,7
            round=4: 0,3|4,2 5,1|6,7
            round=5: 0,4|7,1 5,3|6,2
            round=6: 0,5|6,4 7,3|1,2
            round=7: 0,6|2,3 7,5|1,4
        """.trimIndent()

        val GOLDEN_SCHEDULE_12_PLAYERS: String = """
            players=12
            round=1: 0,11|1,10 2,9|3,8 4,7|5,6
            round=2: 0,1|4,9 2,11|6,7 3,10|5,8
            round=3: 0,2|4,11 3,1|7,8 5,10|6,9
            round=4: 0,3|5,1 4,2|7,10 6,11|8,9
            round=5: 0,4|8,11 5,3|6,2 7,1|9,10
            round=6: 0,5|8,2 6,4|9,1 7,3|10,11
            round=7: 0,6|9,3 7,5|8,4 10,2|11,1
            round=8: 0,7|8,6 9,5|1,2 10,4|11,3
            round=9: 0,8|9,7 10,6|11,5 1,4|2,3
            round=10: 0,9|3,4 10,8|1,6 11,7|2,5
            round=11: 0,10|2,7 11,9|4,5 1,8|3,6
        """.trimIndent()

        val GOLDEN_STANDINGS_8_PLAYERS_PARTIAL: String = """
            P0: points=10, played=2, wins=1
            P1: points=6, played=2, wins=0
            P2: points=4, played=1, wins=0
            P3: points=0, played=0, wins=0
            P4: points=0, played=0, wins=0
            P5: points=0, played=0, wins=0
            P6: points=2, played=1, wins=0
            P7: points=10, played=2, wins=1
        """.trimIndent()

        val GOLDEN_VERIFICATION_4_PLAYERS: String = """
            allOk=true
            player=0
              vs1: partner=1/1 opponent=2/2
              vs2: partner=1/1 opponent=2/2
              vs3: partner=1/1 opponent=2/2
            player=1
              vs0: partner=1/1 opponent=2/2
              vs2: partner=1/1 opponent=2/2
              vs3: partner=1/1 opponent=2/2
            player=2
              vs0: partner=1/1 opponent=2/2
              vs1: partner=1/1 opponent=2/2
              vs3: partner=1/1 opponent=2/2
            player=3
              vs0: partner=1/1 opponent=2/2
              vs1: partner=1/1 opponent=2/2
              vs2: partner=1/1 opponent=2/2
        """.trimIndent()

        val GOLDEN_MATCH_VALIDITY_AUDIT: String = """
            title=Golden Cup
            maxCombined=8
            allValid=false
            round=1
              court=1 A=Alice & Bob B=Carol & Dave score=6:2 entered=true valid=true issues=[]
              court=2 A=Alice & Alice B=Bob & Carol score=-1:10 entered=true valid=false issues=[DUPLICATE_PLAYERS_ON_COURT,SAME_PLAYER_TWICE_ON_TEAM_A,NEGATIVE_SCORE,COMBINED_SCORE_EXCEEDS_LIMIT]
            round=2
              court=1 A=Alice & Carol B=Bob & Dave score=0:0 entered=false valid=true issues=[]
        """.trimIndent()

        val GOLDEN_SHARE_TEXT: String = """
            Saturday Americano
            Americano · Padlecano

            STANDINGS
            #  Player  Pts  Games  Wins
            1  Alice    12      2     2
            2  Bob      10      2     1
            3  Carol     8      2     1
            4  Dave      6      2     0

            MATCHES
            Round 1 of 2
              Court 1: Alice & Bob vs Carol & Dave — 6 : 2
            Round 2 of 2
              Court 1: Alice & Carol vs Bob & Dave — 6 : 4
        """.trimIndent()
    }
}
