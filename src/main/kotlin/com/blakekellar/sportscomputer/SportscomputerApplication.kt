package com.blakekellar.sportscomputer

import mu.KLogging
import org.apache.commons.math3.linear.ArrayRealVector
import org.apache.commons.math3.linear.LUDecomposition
import org.apache.commons.math3.linear.MatrixUtils
import org.apache.commons.math3.linear.RealVector
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.server.*
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@SpringBootApplication
class SportscomputerApplication

fun main(args: Array<String>) {
    runApplication<SportscomputerApplication>(*args)
}

@Configuration
class RouterConfig {

    @Autowired
    lateinit var gameResultsHandler: GameResultsHandler

    @Bean
    fun routerFunction(): RouterFunction<*> {
        return RouterFunctions.route(
                RequestPredicates.POST("/gameresults"),
                HandlerFunction { request ->
                    gameResultsHandler.post(request)
                })
    }
}

data class GameResult(val teamScores: List<TeamScore>)
data class TeamScore(val team: String, val score: Double)

interface GameResultsHandler {
    fun post(request: ServerRequest): Mono<ServerResponse>
}

@Component
class GameResultsHandlerImpl(private val srsComputer: SrsComputer) : GameResultsHandler {
    override fun post(request: ServerRequest): Mono<ServerResponse> {
        val gameResults = request.bodyToFlux(GameResult::class.java)
        val result = srsComputer.computeSrs(gameResults)
        return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(result, RealVector::class.java)
    }
}

interface SrsComputer {
    fun computeSrs(gameResults: Flux<GameResult>): Mono<RealVector>
}

@Service
class SrsComputerImpl : SrsComputer {

    companion object : KLogging()

    override fun computeSrs(gameResults: Flux<GameResult>): Mono<RealVector> {

        /*
        Average point margin by team is the sum of all margins (team score minus opponent score) divided by number of games.

        SRS (n=number of teams, M = team margin, S_n = SRS for team S)
        S_1 = M_1 + (1/n) * (S_2 + .. + S_n)
        S_2 = M_2 + (1/n) * (S_1 + .. + S_n)
        ...
        S_n = M_n + (1/n) * (S_1 + .. + S_n-1)

        1. Compute margin
        2. Solve system of equations
         */

        return gameResults.collectList().flatMap { resultsList ->

            val teams: MutableSet<String> = mutableSetOf()
            val totalPointMargin: MutableMap<String, Double> = mutableMapOf()
            val totalGamesPlayed: MutableMap<String, Double> = mutableMapOf()

            resultsList.forEach { gameResult ->

                if (gameResult.teamScores.size != 2) {
                    throw UnsupportedOperationException("Received ${gameResult.teamScores.size} gameResult teamScores, exactly 2 required.")
                }

                gameResult.teamScores.forEach {
                    teams.add(it.team)
                    totalGamesPlayed.putIfAbsent(it.team, 0.0)
                    totalGamesPlayed[it.team] = totalGamesPlayed[it.team]!!.inc()
                    totalPointMargin.putIfAbsent(it.team, 0.0)
                }

                val firstTeamScore = gameResult.teamScores.first()
                val secondTeamScore = gameResult.teamScores.last()
                val margin = firstTeamScore.score - secondTeamScore.score
                totalPointMargin[firstTeamScore.team] = totalPointMargin[firstTeamScore.team]!!.plus(margin)
                totalPointMargin[secondTeamScore.team] = totalPointMargin[secondTeamScore.team]!!.minus(margin)
            }

            logger.info("teams=${teams}")
            logger.info("totalPointMargin=${totalPointMargin}")
            logger.info("totalGamesPlayed=${totalGamesPlayed}")

            val matrixData = arrayOf(doubleArrayOf(1.0, 2.0, 3.0), doubleArrayOf(2.0, 5.0, 3.0), doubleArrayOf(2.0, -5.0, 4.0))
            val coefficients = MatrixUtils.createRealMatrix(matrixData)
            val solver = LUDecomposition(coefficients).solver
            val constants = ArrayRealVector(doubleArrayOf(1.0, -2.0, 1.0), false)
            val solution = solver.solve(constants)
            Mono.just(solution)
        }
    }
}