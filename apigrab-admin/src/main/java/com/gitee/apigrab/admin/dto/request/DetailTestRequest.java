package com.gitee.apigrab.admin.dto.request;

import lombok.Getter;
import lombok.Setter;

/**
 * 详情补全试跑请求：继承完整接口请求字段（供临时/未保存配置试跑时解析父接口的认证与超时），
 * 额外携带详情配置与一个样例键值（design C11）。
 */
@Getter
@Setter
public class DetailTestRequest extends ApiConfigRequest {
    /** 用于构造详情请求的样例键值。 */
    private String sampleKey;
}
