package io.github.istiorouteexplorer.model.istio;

public enum ServiceEntryLocation {
    MESH_EXTERNAL(0),
    MESH_INTERNAL(1);
    private final int value;
    ServiceEntryLocation(int value) {
        this.value = value;
    }
}
