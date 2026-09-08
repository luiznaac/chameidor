package dev.agner.chameidor.application

import org.springframework.boot.runApplication
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration

@Configuration
@ComponentScan(basePackages = ["dev.agner.chameidor"])
class Boot

fun main() {
    runApplication<Boot>()
}
