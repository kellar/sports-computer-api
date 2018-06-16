package com.blakekellar.sportscomputer.service

import com.blakekellar.sportscomputer.model.MlbGames
import reactor.core.publisher.Mono

interface MlbGamesService {
    fun getMlbGames(season : Int): Mono<MlbGames>
}