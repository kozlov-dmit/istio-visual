package io.github.istiorouteexplorer.model.istio;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
public class HttpMatchRequestDto {

    private StringMatchDto authority;
    private List<String> gateways = new ArrayList<>();
    private Map<String, StringMatchDto> headers = new LinkedHashMap<>();
    private Boolean ignoreUriCase;
    private StringMatchDto method;
    private String name;
    private Long port;
    private Map<String, StringMatchDto> queryParams = new LinkedHashMap<>();
    private StringMatchDto scheme;
    private Map<String, String> sourceLabels = new LinkedHashMap<>();
    private String sourceNamespace;
    private String statPrefix;
    private StringMatchDto uri;
    private Map<String, StringMatchDto> withoutHeaders = new LinkedHashMap<>();

}
