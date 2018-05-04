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

        return gameResults.collectList().flatMap { resultsList ->

            // Average point margin by team is the sum of all margins (team score minus opponent score) divided by number of games.

            val teams: MutableSet<String> = mutableSetOf()
            val totalPointMargin: MutableMap<String, Double> = mutableMapOf()
            val totalGamesPlayed: MutableMap<String, Double> = mutableMapOf()
            val opponents: MutableMap<String, Set<String>> = mutableMapOf()

            resultsList.forEach { gameResult ->

                if (gameResult.teamScores.size != 2) {
                    throw UnsupportedOperationException("Received ${gameResult.teamScores.size} gameResult teamScores, exactly 2 required.")
                }

                gameResult.teamScores.forEach {
                    teams.add(it.team)
                    totalGamesPlayed.putIfAbsent(it.team, 0.0)
                    totalGamesPlayed[it.team] = totalGamesPlayed[it.team]!!.inc()
                    totalPointMargin.putIfAbsent(it.team, 0.0)
                    opponents.putIfAbsent(it.team, setOf())
                }

                val firstTeamScore = gameResult.teamScores.first()
                val secondTeamScore = gameResult.teamScores.last()
                val margin = firstTeamScore.score - secondTeamScore.score
                totalPointMargin[firstTeamScore.team] = totalPointMargin[firstTeamScore.team]!!.plus(margin)
                totalPointMargin[secondTeamScore.team] = totalPointMargin[secondTeamScore.team]!!.minus(margin)
                opponents.put(firstTeamScore.team, opponents.get(firstTeamScore.team)!!.plus(secondTeamScore.team))
                opponents.put(secondTeamScore.team, opponents[secondTeamScore.team]!!.plus(firstTeamScore.team))
            }

            logger.info("teams=${teams}")
            logger.info("totalPointMargin=${totalPointMargin}")
            logger.info("totalGamesPlayed=${totalGamesPlayed}")
            logger.info("opponents=${opponents}")

            // initial state of solution (LHS) is average point margin
            // initial state of coefficient matrix is
            //  -1 for a team vs itself
            //  0 if team has not played other team
            //  1 divided by number of team's opponents

/*
SRS (n=number of teams, M = team margin, S_n = SRS for team S)
S_1 = M_1 + (1/n) * (S_2 + .. + S_n)
S_2 = M_2 + (1/n) * (S_1 + .. + S_n)
...
S_n = M_n + (1/n) * (S_1 + .. + S_n-1)
 */

            val constantsArray: MutableList<Double> = mutableListOf()
            teams.forEach { team ->
                constantsArray.add(totalPointMargin[team]!! / totalGamesPlayed[team]!!)
            }
            val constants = ArrayRealVector(constantsArray.toDoubleArray(), false)

            
            var matrixData: Array<DoubleArray> = emptyArray()
            teams.forEach { team ->
                //  TODO: LIST
                var singleTeamArray: DoubleArray = doubleArrayOf()
                teams.forEach { opponent ->
                    if (team == opponent) {
                        singleTeamArray = singleTeamArray.plus(0.0)
                    } else if (opponents[team]!!.contains(opponent)) {
                        singleTeamArray = singleTeamArray.plus(1.0 / opponents[team]!!.size)
                    } else {
                        singleTeamArray = singleTeamArray.plus(0.0)
                    }
                }
                matrixData = matrixData.plus(singleTeamArray)
            }

            val coefficients = MatrixUtils.createRealMatrix(matrixData)
            val solver = LUDecomposition(coefficients).solver
            val solution = solver.solve(constants)
            Mono.just(solution)
        }
    }
}