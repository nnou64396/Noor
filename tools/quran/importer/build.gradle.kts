import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
    application
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

application {
    mainClass.set("com.noor.quran.importer.MainKt")
}

dependencies {
    implementation(project(":tools:quran:common"))
    implementation(libs.sqlite.jdbc)
    implementation(libs.org.json)
    testImplementation(libs.junit)
}

val generateQuranDb = tasks.register<JavaExec>("generateQuranDb") {
    group = "quran"
    description = "Builds the validated prebuilt quran_reader.db asset from the Tanzil sources + exported Room schema"
    dependsOn(":app:kspDebugKotlin")

    val sourcesDir = rootProject.layout.projectDirectory.dir("tools/quran/importer/_sources")
    val schemaFile = rootProject.layout.projectDirectory
        .dir("app/schemas/dz.techvision.noor.data.local.db.quran.QuranReaderDatabase/1.json")
    val assetsDir = rootProject.layout.projectDirectory.dir("app/src/main/assets/quran")

    inputs.dir(sourcesDir)
    inputs.file(schemaFile)
    outputs.dir(assetsDir)

    classpath = sourceSets.getByName("main").runtimeClasspath
    mainClass.set("com.noor.quran.importer.MainKt")

    systemProperty("noor.sourcesDir", sourcesDir.asFile.absolutePath)
    systemProperty("noor.schemaJson", schemaFile.asFile.absolutePath)
    systemProperty("noor.assetsDir", assetsDir.asFile.absolutePath)
}