package com.steelhouse.smartpixelconfigservice

import com.steelhouse.postgresql.publicschema.AdvertiserSmartPxVariables
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.runApplication

@SpringBootApplication
@EntityScan(
    basePackageClasses = [
        AdvertiserSmartPxVariables::class
    ]
)
class Application
fun main(args: Array<String>) {
    runApplication<Application>(*args)
}
