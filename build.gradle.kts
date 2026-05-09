plugins {
    java
    id("org.springframework.boot") version "4.0.6" apply false
}

allprojects {
    group = "com.lumen1024"
    version = "0.0.1-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "java")

    java {
        toolchain {
            languageVersion = JavaLanguageVersion.of(25)
        }
    }

    dependencies {
        implementation(platform("org.springframework.boot:spring-boot-dependencies:4.0.6"))
        implementation(platform("org.springframework.cloud:spring-cloud-dependencies:2025.0.6"))
        compileOnly(platform("org.springframework.boot:spring-boot-dependencies:4.0.6"))
        annotationProcessor(platform("org.springframework.boot:spring-boot-dependencies:4.0.6"))
        compileOnly("org.projectlombok:lombok")
        annotationProcessor("org.projectlombok:lombok")
        testImplementation("org.springframework.boot:spring-boot-starter-test")
        testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }
}