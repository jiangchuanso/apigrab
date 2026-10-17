package com.gitee.apigrab.core.auth;

import com.gitee.apigrab.persistence.entity.ApiConfigEntity;
import com.gitee.apigrab.persistence.entity.enums.AuthType;
import okhttp3.Headers;

public interface AuthHandler {
    boolean supports(AuthType authType);

    /**
     * Auth headers only. Kept for backward compatibility; handlers that need to place
     * material into query/body override {@link #contribute(ApiConfigEntity)} instead.
     */
    Headers processAuth(ApiConfigEntity config);

    /**
     * Full auth contribution (headers + query params + body fields). Defaults to the
     * header-only result so existing handlers need no change.
     */
    default AuthContribution contribute(ApiConfigEntity config) {
        return AuthContribution.headersOnly(processAuth(config));
    }
}
