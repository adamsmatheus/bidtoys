package com.leilao.backend

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class LeilaoApplication

fun main(args: Array<String>) {
    runApplication<LeilaoApplication>(*args)
}
