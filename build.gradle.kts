import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
    id("org.springframework.boot") version "2.7.14"
    id("io.spring.dependency-management") version "1.1.2"
    id("org.jlleitschuh.gradle.ktlint") version "11.5.1"
    kotlin("jvm") version "1.8.22"
    kotlin("plugin.spring") version "1.8.22"
    application
}

group = "com.steelhouse"
java.sourceCompatibility = JavaVersion.VERSION_11

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

val artifactoryUser: String by project
val artifactoryPassword: String by project

repositories {
    mavenCentral()
    maven {
        setUrl("https://steelhouse.jfrog.io/steelhouse/releases")
        credentials {
            username = artifactoryUser
            password = artifactoryPassword
        }
    }
    maven {
        setUrl("https://steelhouse.jfrog.io/steelhouse/snapshots")
        credentials {
            username = artifactoryUser
            password = artifactoryPassword
        }
    }
}

val jasyptVersion = "2.1.2"
val steelhouseDomainPostgresVersion = "0.54"
val swaggerVersion = "2.8.0"

dependencies {
    implementation("com.github.ulisesbocchio:jasypt-spring-boot-starter:$jasyptVersion")

    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-jetty")
    implementation("org.springframework.boot:spring-boot-starter-web") {
        exclude(group = "org.springframework.boot", module = "spring-boot-starter-tomcat")
    }

    implementation("org.postgresql:postgresql:42.2.27")
    implementation("com.github.ben-manes.caffeine:caffeine:2.8.1")
    implementation("io.micrometer:micrometer-registry-prometheus")
    implementation("com.steelhouse:domain-postgresql:$steelhouseDomainPostgresVersion") {
        exclude(group = "javax.validation", module = "validation-api")
    }
//    implementation("io.springfox:springfox-swagger2:$swaggerVersion")
//    implementation("io.springfox:springfox-swagger-ui:$swaggerVersion")

    testImplementation("org.junit.jupiter:junit-jupiter:5.8.1")
    testImplementation("io.mockk:mockk:1.9.3")
}

springBoot {
    buildInfo {
        properties {
            name = "Smart Pixel Config Service"
        }
    }
    mainClass.set("com.steelhouse.smartpixelconfigservice.ApplicationKt")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs += "-Xjsr305=strict"
        jvmTarget = "11"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}

tasks {
    register<Zip>("fatZip") {
        dependsOn("jar", "bootJar")
        from("src/main/resources", "build/libs")
        include("*")
        into("/")
    }

    "assemble" {
        dependsOn("fatZip")
    }

    getByName<BootJar>("bootJar") {
        launchScript()
        archiveFileName.set("${archiveBaseName.get()}.${archiveExtension.get()}")
    }

    wrapper {
        gradleVersion = "7.4.2"
        distributionType = Wrapper.DistributionType.ALL
    }
}
