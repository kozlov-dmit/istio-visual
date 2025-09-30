package io.github.istiorouteexplorer.model.istio;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class EnvoyFilterSpecDto {

    private WorkLoadSelectorDto workloadSelector;

    public EnvoyFilterSpecDto(WorkLoadSelectorDto workloadSelector) {
        this.workloadSelector = workloadSelector;
    }

    public WorkLoadSelectorDto getWorkloadSelector() {
        return workloadSelector;
    }

    public void setWorkloadSelector(WorkLoadSelectorDto workloadSelector) {
        this.workloadSelector = workloadSelector;
    }

}
