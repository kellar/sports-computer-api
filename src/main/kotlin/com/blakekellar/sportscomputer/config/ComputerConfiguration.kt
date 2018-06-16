package com.blakekellar.sportscomputer.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.PropertySource

@Configuration
@PropertySource("classpath:application.properties")
@ConfigurationProperties(prefix = "computer")
data class ComputerConfiguration(var mlbApiBaseUri: String = "")
