plugins {
    id("org.springframework.boot")
}

dependencies {
    implementation(platform("org.springframework.cloud:spring-cloud-dependencies:2025.0.0"))
    implementation(project(":common"))
    implementation("org.springframework.cloud:spring-cloud-starter-gateway")
}
