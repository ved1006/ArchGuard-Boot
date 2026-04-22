package com.ved.BackendAnalyzer.model;

public class MethodCallInfo {

    private final String callerClass;
    private final String callerPackage;
    private final String calledObject;
    private final String methodName;
    private final int lineNumber;

    public MethodCallInfo(String callerClass, String callerPackage, String calledObject, String methodName, int lineNumber) {
        this.callerClass = callerClass;
        this.callerPackage = callerPackage;
        this.calledObject = calledObject;
        this.methodName = methodName;
        this.lineNumber = lineNumber;
    }

    public String getCallerClass() {
        return callerClass;
    }

    public String getCallerPackage() {
        return callerPackage;
    }

    public String getCalledObject() {
        return calledObject;
    }

    public String getMethodName() {
        return methodName;
    }

    public int getLineNumber() {
        return lineNumber;
    }
}
