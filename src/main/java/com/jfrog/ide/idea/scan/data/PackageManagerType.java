package com.jfrog.ide.idea.scan.data;

public enum PackageManagerType {
    PYPI("pypi"),
    NPM("npm"),
    YARN("yarn"),
    MAVEN("maven"),
    GRADLE("gradle"),
    GO("go");

    private final String name;

    PackageManagerType(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

}
