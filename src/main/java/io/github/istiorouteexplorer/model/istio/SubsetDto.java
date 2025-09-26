package io.github.istiorouteexplorer.model.istio;

public final class SubsetDto {

    public SubsetDto() {
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof SubsetDto;
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "SubsetDto{}";
    }
}
