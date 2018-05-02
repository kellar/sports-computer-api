package com.blakekellar.sportscomputer

import mu.KLogging
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

data class GameResult(val results: List<TeamScore>)
data class TeamScore(val team: String, val score: Int)

interface GameResultsHandler {
    fun post(request: ServerRequest): Mono<ServerResponse>
}

@Component
class GameResultsHandlerImpl(private val srsComputer: SrsComputer) : GameResultsHandler {
    override fun post(request: ServerRequest): Mono<ServerResponse> {
        val gameResults = request.bodyToFlux(GameResult::class.java)
        return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(srsComputer.computeSrs(gameResults), String::class.java)
    }
}

interface SrsComputer {
    fun computeSrs(gameResults: Flux<GameResult>): Mono<String>
}

@Service
class SrsComputerImpl : SrsComputer {

    companion object : KLogging()

    override fun computeSrs(gameResults: Flux<GameResult>): Mono<String> {

        gameResults.subscribe({ foo -> logger.info("got=" + foo) })

        return gameResults.collectList().flatMap { resultsList ->
            Mono.just(resultsList.toString())
        }
    }
}