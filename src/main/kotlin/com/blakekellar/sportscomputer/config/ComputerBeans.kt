package com.blakekellar.sportscomputer.config

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class ComputerBeans(@Autowired private val computerConfiguration: ComputerConfiguration) {

    @Bean
    fun webClient(): WebClient {
        return WebClient.builder().baseUrl(computerConfiguration.mlbApiBaseUri).build()
    }
}

