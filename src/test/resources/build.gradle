plugins {
    id 'java'
}

repositories {
    jcenter()
}

dependencies {
    implementation 'a:b:c'
    implementation group: "a", name: "b", version: "c"
    implementation 'a:b:c',
            group: 'd', name: 'e', version: 'f'
    implementation("a:b:c") {
        exclude module: "d"
    }
    compile project(":project")
    compile fileTree(dir: "${gradle.gradleHomeDir}/lib/plugins", include: '**/*.jar')
}