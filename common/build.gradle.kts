dependencies {
    implementation("org.springframework:spring-web")
    implementation("org.springframework.security:spring-security-core")
    implementation("io.jsonwebtoken:jjwt-api:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")
    compileOnly("jakarta.servlet:jakarta.servlet-api")
}
