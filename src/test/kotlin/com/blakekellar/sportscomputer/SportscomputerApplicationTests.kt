package com.blakekellar.sportscomputer

import com.blakekellar.sportscomputer.model.GameResult
import com.blakekellar.sportscomputer.model.TeamScore
import com.fasterxml.jackson.databind.MappingIterator
import com.fasterxml.jackson.dataformat.csv.CsvMapper
import com.fasterxml.jackson.dataformat.csv.CsvSchema
import com.github.dreamhead.moco.Moco.*
import com.github.dreamhead.moco.RequestMatcher
import com.github.dreamhead.moco.Runner.runner
import mu.KLogging
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.body
import reactor.core.publisher.Flux
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*

@RunWith(SpringRunner::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(locations = ["classpath:application-integration-test.properties"])
class SportscomputerApplicationTests {

    companion object : KLogging()

    @Autowired
    private lateinit var webClient: WebTestClient
    private lateinit var runner: com.github.dreamhead.moco.Runner

    @Before
    fun startMlbApiMock() {
        val server = httpServer(12306)
        server.get(RequestMatcher.ANY_REQUEST_MATCHER).response(with(file("src/test/resources/mlbapi2017.json")), header("Content-Type", "application/json"))
        runner = runner(server)
        runner.start()
    }

    @After
    fun stopMlbApiMock() {
        runner.stop()
    }

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

        var postBody: Flux<GameResult> = Flux.empty()
        gameResults.forEach { gameResult ->
            postBody = postBody.concatWith(Flux.just(gameResult))
        }

        val response = this.webClient.post().uri("/gameresults").body(postBody).exchange()
        response.expectStatus().is5xxServerError

        /*
        TODO: Matrix is singular. Better methods produce:

        A 2.93
        B -7.19
        C -6.19
        D -0.82
        E -3.07
        F 7.18
         */
    }

    @Test
    fun mlb2017fromFile() {

        val mapper = CsvMapper()
        val path = Paths.get(javaClass.classLoader.getResource("2017.api.regular.csv")!!.toURI())
        val schema = CsvSchema.emptySchema().withHeader()
        val iterator: MappingIterator<Map<String, String>> = mapper.readerFor(Map::class.java).with(schema).readValues(Files.newBufferedReader(path))

        val gameResults: MutableList<GameResult> = mutableListOf()

        while (iterator.hasNext()) {
            val rowAsMap = iterator.next()
            val gameResult = gameResultFactory(rowAsMap.get("team1name")!!,
                    rowAsMap.get("team1score").toString().toDouble(),
                    rowAsMap.get("team2name")!!,
                    rowAsMap.get("team2score").toString().toDouble())
            gameResults.add(gameResult)
        }

        var postBody: Flux<GameResult> = Flux.empty()
        gameResults.forEach { gameResult ->
            postBody = postBody.concatWith(Flux.just(gameResult))
        }

        val response = this.webClient.post().uri("/gameresults").body(postBody).exchange()
        response.expectStatus().isOk
        response.expectBody().json("[{\"team\":\"Cleveland Indians\",\"rank\":1.5602942526545245},{\"team\":\"New York Yankees\",\"rank\":1.215623396431958},{\"team\":\"Houston Astros\",\"rank\":1.2046609788498697},{\"team\":\"Los Angeles Dodgers\",\"rank\":1.158634922327715},{\"team\":\"Arizona Diamondbacks\",\"rank\":0.9389721701446375},{\"team\":\"Washington Nationals\",\"rank\":0.8965363059328156},{\"team\":\"Chicago Cubs\",\"rank\":0.7751881142939341},{\"team\":\"Boston Red Sox\",\"rank\":0.7200271904698983},{\"team\":\"Colorado Rockies\",\"rank\":0.4031395895966099},{\"team\":\"St. Louis Cardinals\",\"rank\":0.3405298788226905},{\"team\":\"Milwaukee Brewers\",\"rank\":0.21605682853663194},{\"team\":\"Minnesota Twins\",\"rank\":0.1732873270297124},{\"team\":\"Los Angeles Angels\",\"rank\":0.009689584691483389},{\"team\":\"Tampa Bay Rays\",\"rank\":-0.05733655538978249},{\"team\":\"Texas Rangers\",\"rank\":-0.11587500428773972},{\"team\":\"Seattle Mariners\",\"rank\":-0.13604965047864598},{\"team\":\"Miami Marlins\",\"rank\":-0.27193704546736513},{\"team\":\"Pittsburgh Pirates\",\"rank\":-0.39219369841308194},{\"team\":\"Kansas City Royals\",\"rank\":-0.537605476342766},{\"team\":\"Oakland Athletics\",\"rank\":-0.5425160762540149},{\"team\":\"Toronto Blue Jays\",\"rank\":-0.5498463415987559},{\"team\":\"Atlanta Braves\",\"rank\":-0.5510321975299965},{\"team\":\"Philadelphia Phillies\",\"rank\":-0.5616464703393915},{\"team\":\"Baltimore Orioles\",\"rank\":-0.5961802771603335},{\"team\":\"Chicago White Sox\",\"rank\":-0.6883130945300586},{\"team\":\"Cincinnati Reds\",\"rank\":-0.701814294619044},{\"team\":\"New York Mets\",\"rank\":-0.7794648985236734},{\"team\":\"San Francisco Giants\",\"rank\":-0.8508878117885149},{\"team\":\"Detroit Tigers\",\"rank\":-0.97154856577366},{\"team\":\"San Diego Padres\",\"rank\":-1.3083930812856543}]")
    }

    @Test
    fun mlb2017fromApi() {
        val response = this.webClient.get().uri("/mlb/2017/srs").exchange()
        response.expectStatus().isOk
        response.expectBody().json("[{\"team\":\"Cleveland Indians\",\"rank\":1.5602942526545245},{\"team\":\"New York Yankees\",\"rank\":1.2156233964319583},{\"team\":\"Houston Astros\",\"rank\":1.204660978849869},{\"team\":\"Los Angeles Dodgers\",\"rank\":1.158634922327715},{\"team\":\"Arizona Diamondbacks\",\"rank\":0.9389721701446376},{\"team\":\"Washington Nationals\",\"rank\":0.8965363059328156},{\"team\":\"Chicago Cubs\",\"rank\":0.7751881142939342},{\"team\":\"Boston Red Sox\",\"rank\":0.7200271904698983},{\"team\":\"Colorado Rockies\",\"rank\":0.4031395895966099},{\"team\":\"St. Louis Cardinals\",\"rank\":0.34052987882269026},{\"team\":\"Milwaukee Brewers\",\"rank\":0.21605682853663186},{\"team\":\"Minnesota Twins\",\"rank\":0.1732873270297123},{\"team\":\"Los Angeles Angels\",\"rank\":0.009689584691483392},{\"team\":\"Tampa Bay Rays\",\"rank\":-0.05733655538978248},{\"team\":\"Texas Rangers\",\"rank\":-0.11587500428773971},{\"team\":\"Seattle Mariners\",\"rank\":-0.13604965047864603},{\"team\":\"Miami Marlins\",\"rank\":-0.2719370454673652},{\"team\":\"Pittsburgh Pirates\",\"rank\":-0.39219369841308194},{\"team\":\"Kansas City Royals\",\"rank\":-0.5376054763427656},{\"team\":\"Oakland Athletics\",\"rank\":-0.5425160762540148},{\"team\":\"Toronto Blue Jays\",\"rank\":-0.5498463415987557},{\"team\":\"Atlanta Braves\",\"rank\":-0.5510321975299965},{\"team\":\"Philadelphia Phillies\",\"rank\":-0.5616464703393914},{\"team\":\"Baltimore Orioles\",\"rank\":-0.5961802771603336},{\"team\":\"Chicago White Sox\",\"rank\":-0.6883130945300585},{\"team\":\"Cincinnati Reds\",\"rank\":-0.7018142946190437},{\"team\":\"New York Mets\",\"rank\":-0.7794648985236733},{\"team\":\"San Francisco Giants\",\"rank\":-0.8508878117885151},{\"team\":\"Detroit Tigers\",\"rank\":-0.9715485657736599},{\"team\":\"San Diego Padres\",\"rank\":-1.3083930812856541}]\n")
    }
}

