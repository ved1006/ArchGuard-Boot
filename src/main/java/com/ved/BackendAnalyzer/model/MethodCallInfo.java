package com.ved.BackendAnalyzer.model;

public class MethodCallInfo {

    private final String callerClass;
    private final String calledObject;
    private final String methodName;

    public MethodCallInfo(String callerClass, String calledObject, String methodName) {
        this.callerClass = callerClass;
        this.calledObject = calledObject;
        this.methodName = methodName;
    }

    public String getCallerClass() {
        return callerClass;
    }

    public String getCalledObject() {
        return calledObject;
    }

    public String getMethodName() {
        return methodName;
    }
}
