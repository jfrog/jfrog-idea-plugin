package com.jfrog.ide.idea.scan.data;

public enum PackageManagerType {
    PYPI("pypi", "python"),
    NPM("npm", "js"),
    YARN("yarn", "js"),
    MAVEN("maven", "java"),
    GRADLE("gradle", "java"),
    GO("go", "go");

    private final String name;
    private final String ProgramingLanguage;

    PackageManagerType(String name, String ProgramingLanguage) {
        this.name = name;
        this.ProgramingLanguage = ProgramingLanguage;
    }

    public String getName() {
        return this.name;
    }

    public String getProgramingLanguage() {
        return ProgramingLanguage;
    }
}
