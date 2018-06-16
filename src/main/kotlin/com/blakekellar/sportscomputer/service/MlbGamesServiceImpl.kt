package com.blakekellar.sportscomputer.service

import com.blakekellar.sportscomputer.model.MlbGames
import mu.KLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

@Service
class MlbGamesServiceImpl(
        @Autowired private val webClient: WebClient) : MlbGamesService {

    companion object : KLogging()

    override fun getMlbGames(season: Int): Mono<MlbGames> {
        return webClient
                .get()
                .uri({ uriBuilder ->
                    uriBuilder.queryParam("sportId", 1)
                            .queryParam("season", season)
                            .queryParam("startDate", season.toString() + "-01-01")
                            .queryParam("endDate", season.toString() + "-12-31")
                            .build()
                })
                .retrieve()
                .bodyToMono(MlbGames::class.java)
    }
}