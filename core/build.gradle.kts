// :core is a plain Kotlin (JVM) library. No Android plugin here, so Android imports
// simply do not compile in this module. That is the guardrail that keeps logic portable.
plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    // Only the XmlPullParser interface. Not packaged: Android provides it at runtime.
    compileOnly(libs.xmlpull)
    testImplementation(libs.kxml2)
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
