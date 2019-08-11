plugins {
    `java-library`
}

repositories {
    jcenter()
}

tasks.test {
    useJUnitPlatform {
        includeEngines("jqwik","jupiter")
    }
    include("de/gtrefs/pbtc/**/*.class")
}

dependencies {
    testImplementation("net.jqwik:jqwik:1.1.3")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.4.2")
    testImplementation("org.assertj:assertj-core:3.12.2")
    testImplementation("org.testcontainers:testcontainers:1.11.2")
    testImplementation("io.rest-assured:rest-assured:4.0.0")
    testImplementation("org.testcontainers:toxiproxy:1.11.3")
    testImplementation("org.testcontainers:kafka:1.11.3")
}
