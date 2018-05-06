package com.blakekellar.sportscomputer

import com.blakekellar.sportscomputer.model.GameResult
import com.blakekellar.sportscomputer.model.TeamScore
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.body
import reactor.core.publisher.Flux

@RunWith(SpringRunner::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SportscomputerApplicationTests {

    @Autowired private lateinit var webClient: WebTestClient

    @Test
    fun emptyPostBodyIsBadRequest() {
        this.webClient.post().uri("/gameresults").exchange().expectStatus().isBadRequest
    }

    @Test
    fun threeScoresPerGameIsBadRequest() {
        var postBody: Flux<GameResult> = Flux.empty()
        val teamScores: MutableList<TeamScore> = mutableListOf()
        teamScores.add(TeamScore("team1",42.0))
        teamScores.add(TeamScore("team2",42.0))
        teamScores.add(TeamScore("team3",42.0))
        postBody = postBody.concatWith(Flux.just(GameResult(teamScores)))
        this.webClient.post().uri("/gameresults").body(postBody).exchange().expectStatus().isBadRequest
    }

    @Test
    fun oneScorePerGameIsBadRequest() {
        var postBody: Flux<GameResult> = Flux.empty()
        val teamScores: MutableList<TeamScore> = mutableListOf()
        teamScores.add(TeamScore("team1",42.0))
        postBody = postBody.concatWith(Flux.just(GameResult(teamScores)))
        this.webClient.post().uri("/gameresults").body(postBody).exchange().expectStatus().isBadRequest
    }

    @Test
    fun computesSrsForValidRequest() {
        var postBody: Flux<GameResult> = Flux.empty()

        val teamScores1: MutableList<TeamScore> = mutableListOf()
        teamScores1.add(TeamScore("team1",1.0))
        teamScores1.add(TeamScore("team2",2.0))

        val teamScores2: MutableList<TeamScore> = mutableListOf()
        teamScores2.add(TeamScore("team1",1.0))
        teamScores2.add(TeamScore("team2",3.0))

        postBody = postBody.concatWith(Flux.just(GameResult(teamScores1)))
        postBody = postBody.concatWith(Flux.just(GameResult(teamScores2)))
        this.webClient.post().uri("/gameresults").body(postBody).exchange().expectStatus().isOk.expectBody().json("[{\"team\":\"team1\",\"rank\":-1.5},{\"team\":\"team2\",\"rank\":1.5}]\n")
    }

}
