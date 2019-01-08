plugins {
    base
    kotlin("jvm") version "1.3.10" apply false
}

allprojects {

    group = "com.networknt.scheduler"

    version = "1.0"

    repositories {
        mavenLocal() // mavenLocal must be added first.
        jcenter()
    }
}

dependencies {
    // Make the root project archives configuration depend on every subproject
    subprojects.forEach {
        archives(it)
    }
}
