plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

rootProject.name = "battleship-admin"

include("core")
include("persistence")
include("cli")
include("gui")