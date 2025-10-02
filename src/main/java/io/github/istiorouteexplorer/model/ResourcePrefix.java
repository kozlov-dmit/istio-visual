package io.github.istiorouteexplorer.model;

/**
 * Prefixes for all resources
 */
public enum ResourcePrefix {

    SERVICE("service:"),
    SERVICE_ENTRY("serviceentry:"),
    POD("pod:"),
    DEPLOYMENT("deployment:"),
    UNKNOWN("unknown:")
    ;
    private final String prefix;
    ResourcePrefix(String prefix) {
        this.prefix = prefix;
    }
    public String getPrefix() {
        return prefix;
    }
}
