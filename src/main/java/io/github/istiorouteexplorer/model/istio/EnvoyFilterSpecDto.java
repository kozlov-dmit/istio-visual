package io.github.istiorouteexplorer.model.istio;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class EnvoyFilterSpecDto {

    private WorkLoadSelectorDto workloadSelector;

}
