package com.ved.BackendAnalyzer.model;

public class Issue {

    public enum Severity {
        HIGH,
        MEDIUM,
        LOW
    }

    private final String ruleId;
    private final Severity severity;
    private final String message;
    private final String location;

    public Issue(String ruleId, Severity severity, String message, String location) {
        this.ruleId = ruleId;
        this.severity = severity;
        this.message = message;
        this.location = location;
    }

    public String getRuleId() {
        return ruleId;
    }

    public Severity getSeverity() {
        return severity;
    }

    public String getMessage() {
        return message;
    }

    public String getLocation() {
        return location;
    }
}
