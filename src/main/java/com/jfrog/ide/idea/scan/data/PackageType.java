package com.jfrog.ide.idea.scan.data;

public enum PackageType {
    PYPI("pypi"),
    NPM("npm"),
    YARN("yarn"),
    MAVEN("maven"),
    GRADLE("gradle"),
    GO("go");

    private final String name;

    PackageType(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

}
