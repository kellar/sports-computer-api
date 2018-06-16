package com.blakekellar.sportscomputer

import com.blakekellar.sportscomputer.config.ComputerConfiguration
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@SpringBootApplication
@EnableConfigurationProperties(ComputerConfiguration::class)
class SportscomputerApplication

fun main(args: Array<String>) {
    runApplication<SportscomputerApplication>(*args)
}
