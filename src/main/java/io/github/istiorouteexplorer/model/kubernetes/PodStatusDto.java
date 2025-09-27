package io.github.istiorouteexplorer.model.kubernetes;
import java.util.Objects;
import lombok.NoArgsConstructor;

/**
 * DTO capturing status information for a Kubernetes Pod.
 */
@NoArgsConstructor(force = true)
public class PodStatusDto {

    private String phase;

    public PodStatusDto(String phase) {
        this.phase = phase;
    }

    public String phase() {
        return phase;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PodStatusDto that)) {
            return false;
        }
        return Objects.equals(phase, that.phase);
    }

    @Override
    public int hashCode() {
        return Objects.hash(phase);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("PodStatusDto{");
        builder.append("phase=").append(phase);
        builder.append('}');
        return builder.toString();
    }
}
