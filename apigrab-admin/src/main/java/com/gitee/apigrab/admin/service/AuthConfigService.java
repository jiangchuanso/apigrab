package com.gitee.apigrab.admin.service;

import cn.hutool.core.util.StrUtil;
import com.gitee.apigrab.common.exception.CommonException;
import com.gitee.apigrab.common.exception.ResponseErrorCode;
import com.gitee.apigrab.core.auth.TokenTestResult;
import com.gitee.apigrab.core.auth.impl.OAuth2Handler;
import com.gitee.apigrab.core.auth.impl.PasswordTokenAuthHandler;
import com.gitee.apigrab.persistence.dao.AuthConfigDao;
import com.gitee.apigrab.persistence.entity.AuthConfig;
import com.gitee.apigrab.persistence.entity.AuthConfigEntity;
import com.gitee.apigrab.persistence.entity.enums.AuthType;
import com.gitee.apigrab.persistence.entity.enums.EnableStatus;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
public class AuthConfigService {

    /** 与列定义 `rc_auth_config.name VARCHAR(100)` 对应。 */
    private static final int NAME_MAX_LENGTH = 100;

    @Resource
    private AuthConfigDao authConfigDao;

    @Resource
    private OAuth2Handler oauth2Handler;

    @Resource
    private PasswordTokenAuthHandler passwordTokenAuthHandler;

    public AuthConfigEntity getById(Long id) {
        AuthConfigEntity entity = authConfigDao.getById(id);
        if (entity == null) {
            throw new CommonException(ResponseErrorCode.NOT_FOUND, "认证配置不存在");
        }
        return entity;
    }

    public List<AuthConfigEntity> list(AuthConfigEntity param) {
        return authConfigDao.list(param);
    }

    public void create(AuthConfigEntity entity) {
        entity.setName(requireUniqueName(entity.getName(), null));
        requireReusableType(entity.getAuthType());
        if (entity.getStatus() == null) {
            entity.setStatus(EnableStatus.ENABLED);
        }
        authConfigDao.insert(entity);
    }

    public void update(AuthConfigEntity entity) {
        entity.setName(requireUniqueName(entity.getName(), entity.getId()));
        requireReusableType(entity.getAuthType());
        authConfigDao.updateById(entity);
    }

    /**
     * 删除前检查引用：被任一接口引用时禁止删除（design D6）。
     * 引用数由 {@code countReferences} 聚合 rc_api_config.auth_config_id 得到。
     */
    public void delete(Long id) {
        if (authConfigDao.countReferences(id) > 0) {
            throw new CommonException(ResponseErrorCode.BAD_REQUEST, "该认证配置已被接口引用，无法删除");
        }
        authConfigDao.deleteById(id);
    }

    /**
     * 名称归一与唯一性校验，返回 trim 后的名称（design D2）。
     *
     * <p>判重依赖 {@code getByName} 的 SQL 等值查询，**不在 Java 层比较字符串**：
     * 表使用 MySQL 默认（大小写不敏感）排序规则，与唯一索引判定一致。
     *
     * @param selfId 更新时传入自身 id 以排除自己；新增时传 {@code null}
     */
    private String requireUniqueName(String rawName, Long selfId) {
        String name = StrUtil.trimToNull(rawName);
        if (name == null) {
            throw new CommonException(ResponseErrorCode.BAD_REQUEST, "认证配置名称不能为空");
        }
        if (name.length() > NAME_MAX_LENGTH) {
            throw new CommonException(ResponseErrorCode.BAD_REQUEST,
                "认证配置名称长度不能超过 " + NAME_MAX_LENGTH + " 个字符");
        }
        AuthConfigEntity existing = authConfigDao.getByName(name);
        if (existing != null && (selfId == null || !selfId.equals(existing.getId()))) {
            throw new CommonException(ResponseErrorCode.BAD_REQUEST, "认证配置名称已存在");
        }
        return name;
    }

    /** 可复用的认证配置必须有具体认证类型，且不得为 NONE（design D1）。 */
    private void requireReusableType(AuthType authType) {
        if (authType == null || authType == AuthType.NONE) {
            throw new CommonException(ResponseErrorCode.BAD_REQUEST, "认证类型不能为空，且不能为无认证（NONE）");
        }
    }

    /**
     * 获取 Token 测试（design D18 / 本特性 D2）：仅 OAuth2 与账号密码换 Token 可用。
     * 使用传入的（可为未保存的）配置实时换取 Token，返回**原始 Token 响应**与提取结果，不缓存。
     * 响应已取得但非 JSON、或账号密码换 Token 未设置提取路径时，不视为失败：
     * 仍携带原始响应返回，供前端展示结构并让用户点选路径。
     */
    public TokenTestResult testToken(AuthConfigEntity entity) {
        AuthType type = entity == null ? null : entity.getAuthType();
        AuthConfig auth = entity == null ? null : entity.getAuthConfig();
        try {
            if (type == AuthType.OAUTH2) {
                return oauth2Handler.fetchTokenForTest(auth);
            }
            if (type == AuthType.PASSWORD_TOKEN) {
                return passwordTokenAuthHandler.fetchTokenForTest(auth);
            }
        } catch (CommonException e) {
            throw e;
        } catch (Exception e) {
            throw new CommonException(ResponseErrorCode.BAD_REQUEST, "获取 Token 失败: " + e.getMessage());
        }
        throw new CommonException(ResponseErrorCode.BAD_REQUEST, "仅 OAuth2 与账号密码换 Token 支持获取 Token 测试");
    }
}
