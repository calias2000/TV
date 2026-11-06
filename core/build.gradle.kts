// :core is a plain Kotlin (JVM) library. No Android plugin here, so Android imports
// simply do not compile in this module. That is the guardrail that keeps logic portable.
plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
