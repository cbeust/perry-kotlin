
import com.beust.kobalt.plugin.application.*
import com.beust.kobalt.plugin.java.javaCompiler
import com.beust.kobalt.plugin.kotlin.kotlinCompiler
import com.beust.kobalt.plugin.packaging.assemble
import com.beust.kobalt.plugin.publish.bintray
import com.beust.kobalt.project

object Version {
    val main = "0.0.1"
    val kotlin = "1.2.10"
    val dropWizard = "1.2.4"
}

val p = project {
    name = "koolaid"
    group = "com.beust"
    artifactId = name
    version = Version.main

    dependencies {
        compile(
                "org.jetbrains.kotlin:kotlin-stdlib:${Version.kotlin}",
                "org.jetbrains.kotlin:kotlin-reflect:${Version.kotlin}",
                "io.dropwizard:dropwizard-core:${Version.dropWizard}",
                "io.dropwizard:dropwizard-assets:${Version.dropWizard}",
                "com.google.inject:guice:4.2.0",
                "com.hubspot.dropwizard:dropwizard-guice:1.0.6.0",
                "postgresql:postgresql:9.1-901-1.jdbc4",
                "org.jetbrains.exposed:exposed:0.12.1",
                "mysql:mysql-connector-java:8.0.15"
                )
    }

    dependenciesTest {
        compile("org.testng:testng:6.14.3")
    }

    assemble {
        jar {
            fatJar = true
            name = projectName + ".jar"
            manifest {
                attributes("Main-Class", "com.beust.kobalt.wrapper.Main")
            }
        }
    }

    bintray {
        publish = true
    }

    javaCompiler {
        args("-source", "1.7", "-target", "1.7")
    }

    kotlinCompiler {
        args("-no-stdlib")
    }

    application {
        mainClass = "com.beust.koolaid.MainKt"
        jvmArgs("-Ddw.server.applicationConnectors[0].port=")
        args("server", "config.yml")
    }
}
