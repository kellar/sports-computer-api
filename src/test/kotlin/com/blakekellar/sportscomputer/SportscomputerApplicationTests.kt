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
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Flux
import java.nio.file.Files
import java.nio.file.Paths
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
    fun computesSrsForLargeRequest() {
        val gameResults: MutableList<GameResult> = mutableListOf()

        // construct a set of game results
        for (i in 0..10000) {
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
    }

    @Test
    @Ignore // TODO:  Fails with org.apache.commons.math3.linear.SingularMatrixException: matrix is singular
    fun computesSrsForSmallRequest() {
        val gameResults: MutableList<GameResult> = mutableListOf()

        // construct a set of game results
        for (i in 0..30) {
            // construct two teams "teamM" and "teamN" with random scores
            val team1 = randomBetween(0, 9)
            val team2 = randomBetween(0, 9, team1)
            val team1score = randomBetween(0.0, 100.0)
            val team2score = randomBetween(0.0, 100.0)
            gameResults.add(gameResultFactory("team" + team1, team1score, "team" + team2, team2score))
        }

        logger.info("request=${gameResults}")

        var postBody: Flux<GameResult> = Flux.empty()
        gameResults.forEach { gameResult ->
            postBody = postBody.concatWith(Flux.just(gameResult))
        }

        val response = this.webClient.post().uri("/gameresults").body(postBody).exchange()
        response.expectStatus().isOk
        response.returnResult<String>().responseBody.subscribe({

            logger.info("response=${response.returnResult<String>()}")
        })
        Thread.sleep(1000)
    }

    @Test
    @Ignore // TODO:  Fails with org.apache.commons.math3.linear.SingularMatrixException: matrix is singular
    fun computesSrsKnownResult() {

        //https://codeandfootball.wordpress.com/2011/04/12/issues-with-the-simple-ranking-system/

        val gameResults: MutableList<GameResult> = mutableListOf()
        gameResults.add(gameResultFactory("A", 27.0, "B", 13.0))
        gameResults.add(gameResultFactory("A", 15.0, "C", 3.0))
        gameResults.add(gameResultFactory("A", 7.0, "D", 19.0))
        gameResults.add(gameResultFactory("A", 35.0, "E", 20.0))

        gameResults.add(gameResultFactory("B", 13.0, "A", 27.0))
        gameResults.add(gameResultFactory("B", 24.0, "E", 27.0))
        gameResults.add(gameResultFactory("B", 9.0, "F", 31.0))
        gameResults.add(gameResultFactory("B", 18.0, "D", 20.0))

        gameResults.add(gameResultFactory("C", 21.0, "D", 17.0))
        gameResults.add(gameResultFactory("C", 3.0, "C", 15.0))
        gameResults.add(gameResultFactory("C", 10.0, "E", 30.0))
        gameResults.add(gameResultFactory("C", 12.0, "F", 15.0))

        gameResults.add(gameResultFactory("D", 17.0, "C", 21.0))
        gameResults.add(gameResultFactory("D", 7.0, "F", 17.0))
        gameResults.add(gameResultFactory("D", 19.0, "A", 7.0))
        gameResults.add(gameResultFactory("D", 20.0, "B", 18.0))

        gameResults.add(gameResultFactory("E", 30.0, "F", 41.0))
        gameResults.add(gameResultFactory("E", 21.0, "B", 24.0))
        gameResults.add(gameResultFactory("E", 30.0, "C", 10.0))
        gameResults.add(gameResultFactory("E", 20.0, "A", 35.0))

        gameResults.add(gameResultFactory("F", 41.0, "E", 30.0))
        gameResults.add(gameResultFactory("F", 17.0, "D", 7.0))
        gameResults.add(gameResultFactory("F", 31.0, "B", 9.0))
        gameResults.add(gameResultFactory("F", 15.0, "C", 12.0))

        logger.info("request=${gameResults}")

        var postBody: Flux<GameResult> = Flux.empty()
        gameResults.forEach { gameResult ->
            postBody = postBody.concatWith(Flux.just(gameResult))
        }

        val response = this.webClient.post().uri("/gameresults").body(postBody).exchange()
        response.expectStatus().isOk

        /*
        Expected:
        A 2.93
        B -7.19
        C -6.19
        D -0.82
        E -3.07
        F 7.18
         */

        logger.info("response=${response.returnResult<String>()}")
    }

    @Test
    fun foo() {
        val path = Paths.get(javaClass.classLoader.getResource("2017.csv")!!.toURI())
        val gameResults: MutableList<GameResult> = mutableListOf()
        val lines = Files.lines(path)

        var count = 0
        lines.forEach { line ->
            count++

            if (count < 201) {
                // TODO: RFC4180 parser e.g. jackson
                var teamScoreTeamScore = line.split(',')
                val gameResult = gameResultFactory(teamScoreTeamScore[0], teamScoreTeamScore[1].toDouble(), teamScoreTeamScore[2], teamScoreTeamScore[3].toDouble())
                logger.info("Adding " + gameResult)
                gameResults.add(gameResult)

            }
        }

        logger.info("request=${gameResults}")

        var postBody: Flux<GameResult> = Flux.empty()
        gameResults.forEach { gameResult ->
            postBody = postBody.concatWith(Flux.just(gameResult))
        }

this.webClient.post().uri("/gameresults").body(postBody).exchange().returnResult(String::class.java).responseBody.subscribe({ a -> println("a="+a)})
    }

}

