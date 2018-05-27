package com.blakekellar.sportscomputer

import com.blakekellar.sportscomputer.model.GameResult
import com.blakekellar.sportscomputer.model.TeamScore
import mu.KLogging
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.body
import org.springframework.test.web.reactive.server.returnResult
import reactor.core.publisher.Flux
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*

@RunWith(SpringRunner::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SportscomputerApplicationTests {

    companion object : KLogging()

    @Autowired
    private lateinit var webClient: WebTestClient

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
        this.webClient.post().uri("/gameresults").body(postBody).exchange().expectStatus().isOk.expectBody().json("[{\"team\":\"team1\",\"rank\":-1.0},{\"team\":\"team2\",\"rank\":1.0}]\n")
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
    }

    @Test
    fun computesSrsKnownResult() {

        //https://codeandfootball.wordpress.com/2011/04/12/issues-with-the-simple-ranking-system/

        val gameResults: MutableList<GameResult> = mutableListOf()
        gameResults.add(gameResultFactory("A", 27.0, "B", 13.0))
        gameResults.add(gameResultFactory("A", 15.0, "C", 3.0))
        gameResults.add(gameResultFactory("A", 7.0, "D", 19.0))
        gameResults.add(gameResultFactory("A", 35.0, "E", 20.0))
        gameResults.add(gameResultFactory("B", 24.0, "E", 21.0))
        gameResults.add(gameResultFactory("B", 9.0, "F", 31.0))
        gameResults.add(gameResultFactory("B", 18.0, "D", 20.0))
        gameResults.add(gameResultFactory("C", 21.0, "D", 17.0))
        gameResults.add(gameResultFactory("C", 10.0, "E", 30.0))
        gameResults.add(gameResultFactory("C", 12.0, "F", 15.0))
        gameResults.add(gameResultFactory("D", 7.0, "F", 17.0))
        gameResults.add(gameResultFactory("E", 30.0, "F", 41.0))

        logger.info("request=${gameResults}")

        var postBody: Flux<GameResult> = Flux.empty()
        gameResults.forEach { gameResult ->
            postBody = postBody.concatWith(Flux.just(gameResult))
        }

        val response = this.webClient.post().uri("/gameresults").body(postBody).exchange()
        response.expectStatus().is5xxServerError

        /*
        Matrix is singular. Better methods produce:

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
    fun mlb2017fromFile() {
        val path = Paths.get(javaClass.classLoader.getResource("2017.csv")!!.toURI())
        val gameResults: MutableList<GameResult> = mutableListOf()
        val lines = Files.lines(path)

        lines.forEach { line ->
            val teamScoreTeamScore = line.split(',') // TODO: RFC4180 parser e.g. jackson
            val gameResult = gameResultFactory(teamScoreTeamScore[0], teamScoreTeamScore[1].toDouble(), teamScoreTeamScore[2], teamScoreTeamScore[3].toDouble())
            gameResults.add(gameResult)
        }

        logger.info("request=${gameResults}")

        var postBody: Flux<GameResult> = Flux.empty()
        gameResults.forEach { gameResult ->
            postBody = postBody.concatWith(Flux.just(gameResult))
        }

        val response = this.webClient.post().uri("/gameresults").body(postBody).exchange()
        response.expectStatus().isOk
        response.expectBody().json("[{\"team\":\"San Francisco Giants\",\"rank\":-0.8529051285945617},{\"team\":\"Arizona D'Backs\",\"rank\":0.87803953390723},{\"team\":\"Chicago Cubs\",\"rank\":0.5954896415957698},{\"team\":\"St. Louis Cardinals\",\"rank\":0.33847549359069234},{\"team\":\"New York Yankees\",\"rank\":1.1741323733785292},{\"team\":\"Tampa Bay Rays\",\"rank\":-0.060398396751359676},{\"team\":\"Toronto Blue Jays\",\"rank\":-0.5590433832012243},{\"team\":\"Baltimore Orioles\",\"rank\":-0.5931607016920941},{\"team\":\"Pittsburgh Pirates\",\"rank\":-0.39424808364508007},{\"team\":\"Boston Red Sox\",\"rank\":0.6638624318556164},{\"team\":\"Philadelphia Phillies\",\"rank\":-0.5639959712135538},{\"team\":\"Cincinnati Reds\",\"rank\":-0.7042627706703027},{\"team\":\"Seattle Mariners\",\"rank\":-0.13820175932169734},{\"team\":\"Houston Astros\",\"rank\":1.1115971734543253},{\"team\":\"San Diego Padres\",\"rank\":-1.3104103980917012},{\"team\":\"Los Angeles Dodgers\",\"rank\":1.2283185772851044},{\"team\":\"Colorado Rockies\",\"rank\":0.38038294652275223},{\"team\":\"Milwaukee Brewers\",\"rank\":0.21384639710901335},{\"team\":\"Kansas City Royals\",\"rank\":-0.5396219482347184},{\"team\":\"Minnesota Twins\",\"rank\":0.1458353252515807},{\"team\":\"Atlanta Braves\",\"rank\":-0.5530935529010194},{\"team\":\"New York Mets\",\"rank\":-0.7817243764802849},{\"team\":\"Los Angeles Angels\",\"rank\":0.008104594241261892},{\"team\":\"Oakland Athletics\",\"rank\":-0.544540949966589},{\"team\":\"Cleveland Indians\",\"rank\":1.4940409772230945},{\"team\":\"Texas Rangers\",\"rank\":-0.11789987800031361},{\"team\":\"Miami Marlins\",\"rank\":-0.27396076157310584},{\"team\":\"Washington Nationals\",\"rank\":0.8857835330871605},{\"team\":\"Detroit Tigers\",\"rank\":-0.9735650376656129},{\"team\":\"Chicago White Sox\",\"rank\":-0.691419407449411}]\n")
    }

    @Test
    // TODO: Stub (Moco) MLB API
    fun mlb2017fromApi() {
        val response = this.webClient.get().uri("/mlb/2017/srs").exchange()
        response.expectStatus().isOk
        response.expectBody().json("[{\"team\":\"Cleveland Indians\",\"rank\":1.5602942526545245},{\"team\":\"New York Yankees\",\"rank\":1.2156233964319583},{\"team\":\"Houston Astros\",\"rank\":1.204660978849869},{\"team\":\"Los Angeles Dodgers\",\"rank\":1.158634922327715},{\"team\":\"Arizona Diamondbacks\",\"rank\":0.9389721701446376},{\"team\":\"Washington Nationals\",\"rank\":0.8965363059328156},{\"team\":\"Chicago Cubs\",\"rank\":0.7751881142939342},{\"team\":\"Boston Red Sox\",\"rank\":0.7200271904698983},{\"team\":\"Colorado Rockies\",\"rank\":0.4031395895966099},{\"team\":\"St. Louis Cardinals\",\"rank\":0.34052987882269026},{\"team\":\"Milwaukee Brewers\",\"rank\":0.21605682853663186},{\"team\":\"Minnesota Twins\",\"rank\":0.1732873270297123},{\"team\":\"Los Angeles Angels\",\"rank\":0.009689584691483392},{\"team\":\"Tampa Bay Rays\",\"rank\":-0.05733655538978248},{\"team\":\"Texas Rangers\",\"rank\":-0.11587500428773971},{\"team\":\"Seattle Mariners\",\"rank\":-0.13604965047864603},{\"team\":\"Miami Marlins\",\"rank\":-0.2719370454673652},{\"team\":\"Pittsburgh Pirates\",\"rank\":-0.39219369841308194},{\"team\":\"Kansas City Royals\",\"rank\":-0.5376054763427656},{\"team\":\"Oakland Athletics\",\"rank\":-0.5425160762540148},{\"team\":\"Toronto Blue Jays\",\"rank\":-0.5498463415987557},{\"team\":\"Atlanta Braves\",\"rank\":-0.5510321975299965},{\"team\":\"Philadelphia Phillies\",\"rank\":-0.5616464703393914},{\"team\":\"Baltimore Orioles\",\"rank\":-0.5961802771603336},{\"team\":\"Chicago White Sox\",\"rank\":-0.6883130945300585},{\"team\":\"Cincinnati Reds\",\"rank\":-0.7018142946190437},{\"team\":\"New York Mets\",\"rank\":-0.7794648985236733},{\"team\":\"San Francisco Giants\",\"rank\":-0.8508878117885151},{\"team\":\"Detroit Tigers\",\"rank\":-0.9715485657736599},{\"team\":\"San Diego Padres\",\"rank\":-1.3083930812856541}]\n")
    }
}

