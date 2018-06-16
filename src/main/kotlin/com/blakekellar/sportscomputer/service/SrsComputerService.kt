package com.blakekellar.sportscomputer.service

import com.blakekellar.sportscomputer.model.GameResult
import com.blakekellar.sportscomputer.model.TeamRank
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface SrsComputerService {
    fun computeSrs(gameResults: Flux<GameResult>): Mono<MutableList<TeamRank>>
}