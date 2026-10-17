package com.gitee.apigrab.admin.dto.request;

import com.gitee.apigrab.persistence.entity.DetailConfig;
import com.gitee.apigrab.persistence.entity.PageConfig;
import com.gitee.apigrab.persistence.entity.enums.BodyType;
import com.gitee.apigrab.persistence.entity.enums.EnableStatus;
import com.gitee.apigrab.persistence.entity.enums.HttpMethod;
import com.gitee.apigrab.persistence.entity.enums.RespType;
import com.gitee.apigrab.persistence.entity.enums.TransformType;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

/**
 * Writable fields for creating/updating or trial-testing an API config.
 * Mirrors the writable fields of {@code ApiConfigEntity} (excluding server-managed
 * id/sampleData/createTime/updateTime and the read-only derived auth fields) so
 * BeanUtils.copyProperties maps directly.
 */
@Getter
@Setter
public class ApiConfigRequest {
    private String name;
    private String url;
    private HttpMethod method;
    private Map<String, String> headers;
    private Map<String, String> queryParams;
    private BodyType bodyType;
    private String bodyContent;
    /** 连接超时（秒）。空或非正值时后端按默认 10 秒生效。 */
    private Integer connectTimeout;
    /** 读取超时（秒）。空或非正值时后端按默认 60 秒生效。 */
    private Integer readTimeout;
    /** 关联的认证配置 id（空=无认证）。 */
    private Long authConfigId;
    private RespType respType;
    private String dataPath;
    private Integer pageEnabled;
    private PageConfig pageConfig;
    /** 详情补全配置（列表→详情两跳采集）；为空表示未启用。 */
    private DetailConfig detailConfig;
    private TransformType transformType;
    private String convertScript;
    private Integer enableMultilines;
    private EnableStatus status;
    private String remark;
}
