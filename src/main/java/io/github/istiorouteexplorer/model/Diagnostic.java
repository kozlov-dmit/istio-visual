package io.github.istiorouteexplorer.model;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class Diagnostic {
    public enum Severity {
        INFO,
        WARNING,
        ERROR
    }
    private Severity severity;
    private String code;
    private String message;
    private String suggestion;

    public Diagnostic(Severity severity, String code, String message, String suggestion) {
        this.severity = severity;
        this.code = code;
        this.message = message;
        this.suggestion = suggestion;
    }
}
