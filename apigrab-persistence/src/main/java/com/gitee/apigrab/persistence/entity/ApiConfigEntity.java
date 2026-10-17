package com.gitee.apigrab.persistence.entity;

import com.gitee.apigrab.persistence.entity.enums.HttpMethod;
import com.gitee.apigrab.persistence.entity.enums.TransformType;
import com.gitee.apigrab.persistence.entity.enums.AuthType;
import com.gitee.apigrab.persistence.entity.enums.BodyType;
import com.gitee.apigrab.persistence.entity.enums.EnableStatus;
import com.gitee.apigrab.persistence.entity.enums.RespType;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;

@Getter
@Setter
public class ApiConfigEntity implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private String name;
    private String url;
    private HttpMethod method;
    private Map<String, String> headers;
    private Map<String, String> queryParams;
    private BodyType bodyType;
    private String bodyContent;
    /** 连接超时（秒）。空或非正值时按默认 10 秒生效。 */
    private Integer connectTimeout;
    /** 读取超时（秒）。空或非正值时按默认 60 秒生效。 */
    private Integer readTimeout;
    /** 关联的认证配置 id（空=无认证）。持久化列 APIGRAB_API_CONFIG.auth_config_id。 */
    private Long authConfigId;
    /** 只读派生：由关联 APIGRAB_AUTH_CONFIG 填充，供认证链按类型分派使用。 */
    private AuthType authType;
    /** 只读派生：由关联 APIGRAB_AUTH_CONFIG 填充。 */
    private AuthConfig authConfig;
    /** 只读派生：关联认证配置名称，供列表展示。 */
    private String authConfigName;
    private RespType respType;
    private String dataPath;
    private Integer pageEnabled;
    private PageConfig pageConfig;
    /** 详情补全配置（列表→详情两跳采集）；为空/未启用时行为与现状一致。持久化列 APIGRAB_API_CONFIG.detail_config。 */
    private DetailConfig detailConfig;
    private TransformType transformType;
    private String convertScript;
    private Integer enableMultilines;
    private Map<String, Object> sampleData;
    private EnableStatus status;
    private String remark;
    private Date createTime;
    private Date updateTime;
    /** 只读派生：引用本接口配置的任务数（列表查询时聚合，非持久化列）。 */
    private Integer refCount;
}
