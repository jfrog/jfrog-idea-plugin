val spi: Configuration by configurations.creating

dependencies {
    implementation(project(":project"))
    compile("a:b:c")
    testCompile("d", "e", "f")
}

// Just a smoke test that using this option does not lead to any exception
tasks {
    named<JavaCompile>("compileJava") {
        options.compilerArgs = listOf("-Xlint:unchecked")
    }
}
