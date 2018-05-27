package com.blakekellar.sportscomputer.handler

import com.blakekellar.sportscomputer.model.DetailedState
import com.blakekellar.sportscomputer.model.GameResult
import com.blakekellar.sportscomputer.model.SeriesDescription
import com.blakekellar.sportscomputer.model.TeamScore
import com.blakekellar.sportscomputer.service.MlbGamesService
import com.blakekellar.sportscomputer.service.SrsComputerService
import mu.KLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.body
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Component
class MlbHandlerImpl : MlbHandler {

    companion object : KLogging()

    @Autowired
    lateinit var mlbGamesService: MlbGamesService

    @Autowired
    lateinit var srsComputerService: SrsComputerService

    override fun getSeasonSrs(request: ServerRequest): Mono<ServerResponse> {
        val season = request.pathVariable("season").toInt()
        val mlbGamesMono = mlbGamesService.getMlbGames(season)
        val gameResultsFlux = mlbGamesMono.flatMapMany { mlbGames ->
            var gameResults: Flux<GameResult> = Flux.empty()
            mlbGames.dates.forEach { date ->
                date.games.forEach { game ->
                    val homeScore = game.teams.home.score
                    val homeTeam = game.teams.home.team.name
                    val awayScore = game.teams.away.score
                    val awayTeam = game.teams.away.team.name

                    if (homeScore != null &&
                            awayScore != null &&
                            (game.seriesDescription == SeriesDescription.REGULAR) &&
                            (game.status.detailedState == DetailedState.FINAL ||
                                    game.status.detailedState == DetailedState.COMPLETEDEARLY)) {
                        val teamScores: MutableList<TeamScore> = mutableListOf()
                        teamScores.add(TeamScore(homeTeam, homeScore.toDouble()))
                        teamScores.add(TeamScore(awayTeam, awayScore.toDouble()))
                        gameResults = gameResults.mergeWith(Mono.just(GameResult(teamScores)))
                    }
                }
            }
            gameResults
        }
        val srs = srsComputerService.computeSrs(gameResultsFlux)
        return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(srs)
    }
}
