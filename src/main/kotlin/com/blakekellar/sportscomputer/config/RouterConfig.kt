package com.blakekellar.sportscomputer.config

import com.blakekellar.sportscomputer.handler.GameResultsHandler
import com.blakekellar.sportscomputer.handler.MlbHandler
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.server.HandlerFunction
import org.springframework.web.reactive.function.server.RequestPredicates
import org.springframework.web.reactive.function.server.RouterFunction
import org.springframework.web.reactive.function.server.RouterFunctions


@Configuration
class RouterConfig {

    @Autowired
    lateinit var gameResultsHandler: GameResultsHandler

    @Autowired
    lateinit var mlbHandler: MlbHandler

    @Bean
    fun routerFunction(): RouterFunction<*> {
        return RouterFunctions
                .route(RequestPredicates.POST("/gameresults"), HandlerFunction { request -> gameResultsHandler.post(request) })
                .andRoute(RequestPredicates.GET("/mlb/{season}/srs"), HandlerFunction { request -> mlbHandler.getSeasonSrs(request) })
    }
}
