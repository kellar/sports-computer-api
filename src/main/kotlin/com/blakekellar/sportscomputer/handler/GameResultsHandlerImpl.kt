package com.blakekellar.sportscomputer.handler

import com.blakekellar.sportscomputer.model.GameResult
import com.blakekellar.sportscomputer.service.SrsComputerService
import mu.KLogging
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.body
import reactor.core.publisher.Mono

@Component
class GameResultsHandlerImpl(private val srsComputerService: SrsComputerService) : GameResultsHandler {

    companion object : KLogging()

    override fun post(request: ServerRequest): Mono<ServerResponse> {
        val gameResults = request.bodyToFlux(GameResult::class.java)
        val result = srsComputerService.computeSrs(gameResults)
        return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(result)
    }
}