package io.github.istiorouteexplorer.model.kubernetes;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class EndpointConditionDto {
    private Boolean ready;
    private Boolean serving;
    private Boolean terminating;
}
