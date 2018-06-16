package com.blakekellar.sportscomputer.handler

import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import reactor.core.publisher.Mono

interface MlbHandler {
    fun getSeasonSrs(request: ServerRequest): Mono<ServerResponse>
}
