package com.blakekellar.sportscomputer

import com.blakekellar.sportscomputer.model.GameResult
import com.blakekellar.sportscomputer.model.TeamScore
import mu.KLogging
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.body
import org.springframework.test.web.reactive.server.returnResult
import reactor.core.publisher.Flux
import java.util.*

@RunWith(SpringRunner::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SportscomputerApplicationTests {

    companion object : KLogging()

    @Autowired private lateinit var webClient: WebTestClient

    @Test
    fun emptyPostBodyIsBadRequest() {
        this.webClient.post().uri("/gameresults").exchange().expectStatus().isBadRequest
    }

    @Test
    fun threeScoresPerGameIsBadRequest() {
        var postBody: Flux<GameResult> = Flux.empty()
        val teamScores: MutableList<TeamScore> = mutableListOf()
        teamScores.add(TeamScore("team1", 42.0))
        teamScores.add(TeamScore("team2", 42.0))
        teamScores.add(TeamScore("team3", 42.0))
        postBody = postBody.concatWith(Flux.just(GameResult(teamScores)))
        this.webClient.post().uri("/gameresults").body(postBody).exchange().expectStatus().isBadRequest
    }

    @Test
    fun oneScorePerGameIsBadRequest() {
        var postBody: Flux<GameResult> = Flux.empty()
        val teamScores: MutableList<TeamScore> = mutableListOf()
        teamScores.add(TeamScore("team1", 42.0))
        postBody = postBody.concatWith(Flux.just(GameResult(teamScores)))
        this.webClient.post().uri("/gameresults").body(postBody).exchange().expectStatus().isBadRequest
    }

    fun gameResultFluxFactory(team1: String, team1score: Double, team2: String, team2score: Double): Flux<GameResult> {
        return Flux.just(gameResultFactory(team1, team1score, team2, team2score))
    }

    fun gameResultFactory(team1: String, team1score: Double, team2: String, team2score: Double): GameResult {
        val teamScores: MutableList<TeamScore> = mutableListOf()
        teamScores.add(TeamScore(team1, team1score))
        teamScores.add(TeamScore(team2, team2score))
        return GameResult(teamScores)
    }

    @Test
    fun computesSrsForValidRequest() {
        var postBody: Flux<GameResult> = Flux.empty()
        postBody = postBody.concatWith(gameResultFluxFactory("team1", 1.0, "team2", 2.0))
        postBody = postBody.concatWith(gameResultFluxFactory("team1", 1.0, "team2", 3.0))
        this.webClient.post().uri("/gameresults").body(postBody).exchange().expectStatus().isOk.expectBody().json("[{\"team\":\"team1\",\"rank\":-1.5},{\"team\":\"team2\",\"rank\":1.5}]\n")
    }

    val random = Random()
    fun randomBetween(from: Double, to: Double): Double {
        val randZeroOne = random.nextDouble()
        val randFromTo = randZeroOne * (to - from) + from
        return randFromTo
    }

    fun randomBetween(from: Int, to: Int, notEqualTo: Int? = null): Int {
        var randFromTo: Int = random.nextInt(to - from) + from
        if (notEqualTo == null) {
            return randFromTo
        } else {
            while (randFromTo == notEqualTo) {
                randFromTo = random.nextInt(to - from) + from
            }
            return randFromTo
        }
    }

    @Test
    @Ignore // TODO:  Fails with org.apache.commons.math3.linear.SingularMatrixException: matrix is singular
    fun computesSrsForLargerRequest() {
        val gameResults: MutableList<GameResult> = mutableListOf()

        // construct a set of game results
        for (i in 0..100) {
            // construct two teams "teamM" and "teamN" with random scores
            val team1 = randomBetween(0, 9)
            val team2 = randomBetween(0, 9, team1)
            val team1score = randomBetween(0.0, 100.0)
            val team2score = randomBetween(0.0, 100.0)
            gameResults.add(gameResultFactory("team" + team1, team1score, "team" + team2, team2score))
        }

        var postBody: Flux<GameResult> = Flux.empty()
        gameResults.forEach { gameResult ->
            postBody = postBody.concatWith(Flux.just(gameResult))
        }

        val response = this.webClient.post().uri("/gameresults").body(postBody).exchange()
        response.expectStatus().isOk
        logger.info("request=${gameResults} response=${response.returnResult<String>()}")
    }
}
