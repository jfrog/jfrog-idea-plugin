plugins {
    id 'java'
}

repositories {
    jcenter()
}

dependencies {
    implementation 'a1:b1:c1'
    implementation group: "a2", name: "b2", version: "c2"
    implementation 'a3:b3:c3',
            group: 'd', name: 'e', version: 'f'
    implementation("a4:b4:c4") {
        exclude module: "d"
    }
    compile project(":project")
    compile fileTree(dir: "${gradle.gradleHomeDir}/lib/plugins", include: '**/*.jar')
}