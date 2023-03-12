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
            'j:k:l'
    implementation("m:n:o") {
        exclude module: "p"
    }
    implementation(
            [group: 'net.lingala.zip4j', name: 'zip4j', version: '2.3.0'],
            ['org.codehaus.groovy:groovy-all:3.0.5']
    )
    compile fileTree(dir: "${gradle.gradleHomeDir}/lib/plugins", include: '**/*.jar')
}