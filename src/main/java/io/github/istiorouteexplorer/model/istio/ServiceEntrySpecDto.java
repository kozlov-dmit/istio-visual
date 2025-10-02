package io.github.istiorouteexplorer.model.istio;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO holding the specification details of an Istio ServiceEntry.
 */
@Data
@NoArgsConstructor(force = true)
public class ServiceEntrySpecDto {

    private List<String> addresses;
    private List<WorkloadEntrySpecDto> endpoints;
    private List<String> exportTo;
    private List<String> hosts;
    private List<PortDto> ports;
    private ServiceEntryResolution resolution;
    private List<String> subjectAltNames;
    private WorkLoadSelectorDto workloadSelector;

}
