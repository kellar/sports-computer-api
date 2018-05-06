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

    // TODO: Break this down into methods
    override fun computeSrs(gameResults: Flux<GameResult>): Mono<MutableList<TeamRank>> {

        return gameResults.collectList().flatMap { resultsList ->

            if (resultsList.isEmpty()) {
               throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Received ${resultsList.size} teamScores, at least 1 required.")
            }

            // Average point margin by team is the sum of all margins (team score minus opponent score) divided by number of games.

            val teams: MutableSet<String> = mutableSetOf()
            val totalPointMargin: MutableMap<String, Double> = mutableMapOf()
            val totalGamesPlayed: MutableMap<String, Double> = mutableMapOf()
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
                constantsArray.add(-totalPointMargin[team]!! / totalGamesPlayed[team]!!)
            }
            val constants = ArrayRealVector(constantsArray.toDoubleArray(), false)

            var matrixData: Array<DoubleArray> = arrayOf()
            teams.forEach { team ->
                val singleTeamArray: MutableList<Double> = mutableListOf()
                teams.forEach { opponent ->
                    if (team == opponent) {
                        singleTeamArray.add(0.0)
                    } else if (opponents[team]!!.contains(opponent)) {
                        singleTeamArray.add(1.0 / opponents[team]!!.size)
                    } else {
                        singleTeamArray.add(0.0)
                    }
                }
                matrixData = matrixData.plus(singleTeamArray.toDoubleArray())
            }

            val coefficients = MatrixUtils.createRealMatrix(matrixData)
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