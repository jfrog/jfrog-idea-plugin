plugins {
    id 'java'
}

repositories {
    jcenter()
}

dependencies {
    implementation 'a:b:c'
    implementation group: "d", name: "e", version: "f"
    implementation 'g:h:i',
            group: 'j', name: 'k', version: 'l'
    implementation("m:n:o") {
        exclude module: "p"
    }
    compile project(":project")
    compile fileTree(dir: "${gradle.gradleHomeDir}/lib/plugins", include: '**/*.jar')
}