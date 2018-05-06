package com.blakekellar.sportscomputer.handler

import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import reactor.core.publisher.Mono

interface GameResultsHandler {
    fun post(request: ServerRequest): Mono<ServerResponse>
}