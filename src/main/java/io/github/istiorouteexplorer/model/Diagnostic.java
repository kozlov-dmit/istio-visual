package io.github.istiorouteexplorer.model;

public class Diagnostic {
    public enum Severity {
        INFO,
        WARNING,
        ERROR
    }
    private final Severity severity;
    private final String code;
    private final String message;
    private final String suggestion;

    public Diagnostic(Severity severity, String code, String message, String suggestion) {
        this.severity = severity;
        this.code = code;
        this.message = message;
        this.suggestion = suggestion;
    }
}
