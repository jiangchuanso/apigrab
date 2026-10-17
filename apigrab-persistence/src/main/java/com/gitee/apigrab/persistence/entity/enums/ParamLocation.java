package com.gitee.apigrab.persistence.entity.enums;

public enum ParamLocation {
    /** 查询串：未以占位符出现的分页参数追加到 URL 查询串。 */
    QUERY,
    /** 请求体：未以占位符出现的分页参数写入 JSON/FORM/XML 请求体。 */
    BODY,
    /** 路径：以 URL 中的 {@code ${参数名}} 占位符替换；未占位时退化为查询参数。 */
    PATH
}
