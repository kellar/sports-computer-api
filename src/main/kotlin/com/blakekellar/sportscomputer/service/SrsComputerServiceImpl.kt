package com.blakekellar.sportscomputer.service

import com.blakekellar.sportscomputer.model.GameResult
import com.blakekellar.sportscomputer.model.TeamRank
import mu.KLogging
import org.apache.commons.math3.linear.ArrayRealVector
import org.apache.commons.math3.linear.LUDecomposition
import org.apache.commons.math3.linear.MatrixUtils
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Service
class SrsComputerServiceImpl : SrsComputerService {

    companion object : KLogging()

    override fun computeSrs(gameResults: Flux<GameResult>): Mono<MutableList<TeamRank>> {

        return gameResults.collectList().flatMap { resultsList ->

            if (resultsList.isEmpty()) {
                throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Received ${resultsList.size} teamScores, at least 1 required.")
            }

            // set of all teams
            val teams: MutableSet<String> = mutableSetOf()

            // sum of all point margins by team
            val totalPointMargin: MutableMap<String, Double> = mutableMapOf()

            // number of games played by team
            val totalGamesPlayed: MutableMap<String, Double> = mutableMapOf()

            // set of opponents played by team
            val opponents: MutableMap<String, Set<String>> = mutableMapOf()

            resultsList.forEach { gameResult ->

                if (gameResult.teamScores.size != 2) {
                    throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Received ${gameResult.teamScores.size} gameResult teamScores, exactly 2 required.")
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
                opponents.put(firstTeamScore.team, opponents[firstTeamScore.team]!!.plus(secondTeamScore.team))
                opponents.put(secondTeamScore.team, opponents[secondTeamScore.team]!!.plus(firstTeamScore.team))
            }

            logger.debug("teams=${teams}")
            logger.debug("totalPointMargin=${totalPointMargin}")
            logger.debug("totalGamesPlayed=${totalGamesPlayed}")
            logger.debug("opponents=${opponents}")

            val constantsArray: MutableList<Double> = mutableListOf()
            teams.forEach { team ->
                constantsArray.add(-totalPointMargin[team]!! / totalGamesPlayed[team]!!)
            }
            val constants = ArrayRealVector(constantsArray.toDoubleArray(), false)

            var matrixData: Array<DoubleArray> = arrayOf()
            teams.forEach { team ->
                val singleTeamArray: MutableList<Double> = mutableListOf()
                teams.forEach { opponent ->
                    when {
                        team == opponent -> singleTeamArray.add(-1.0) // Mij <- -1
                        opponents[team]!!.contains(opponent) -> // scale by number of games
                            singleTeamArray.add(1.0 / totalGamesPlayed[team]!!)
                        else -> // 0 if teams did not play
                            singleTeamArray.add(0.0)
                    }
                }
                matrixData = matrixData.plus(singleTeamArray.toDoubleArray())
            }

            val coefficients = MatrixUtils.createRealMatrix(matrixData)

            logger.debug("coefficients=$coefficients")
            logger.debug("constants=$constants")

            val solver = LUDecomposition(coefficients).solver
            val solution = solver.solve(constants)

            val result: MutableList<TeamRank> = mutableListOf()

            var i = 0
            teams.forEach { team ->
                result.add(TeamRank(team, solution.getEntry(i)))
                i++
            }

            Mono.just(result)
        }
    }
}