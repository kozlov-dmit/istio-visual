package io.github.istiorouteexplorer.model.istio;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * DTO describing workload attributes contained in an Istio WorkloadEntry specification.
 */
@Data
@NoArgsConstructor(force = true)
public class WorkloadEntrySpecDto {

    private String address;
    private Map<String, String> labels;
    private String locality;
    private String network;
    private Map<String, Long> ports;
    private String serviceAccount;
    private Long weight;

}
