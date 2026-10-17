package com.gitee.apigrab.admin.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

/**
 * Request body for previewing a field mapping draft against sample data.
 */
@Getter
@Setter
public class PreviewMappingRequest {
    private List<FieldMappingInput> mappings;
    private List<Map<String, Object>> sampleData;
}
