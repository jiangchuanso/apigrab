package com.gitee.apigrab.admin.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

/**
 * Request body for the Groovy transform script debug endpoint.
 */
@Getter
@Setter
public class DebugScriptRequest {
    private String script;
    private List<Map<String, Object>> sampleData;
}
