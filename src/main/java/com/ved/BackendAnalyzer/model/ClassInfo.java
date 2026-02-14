package com.ved.BackendAnalyzer.model;

public class ClassInfo {

    public enum Type {
        CONTROLLER,
        SERVICE,
        REPOSITORY,
        ENTITY,
        OTHER
    }

    private final String className;
    private final String packageName;
    private final Type type;

    public ClassInfo(String className, String packageName, Type type) {
        this.className = className;
        this.packageName = packageName;
        this.type = type;
    }

    public String getClassName() {
        return className;
    }

    public String getPackageName() {
        return packageName;
    }

    public Type getType() {
        return type;
    }
}
