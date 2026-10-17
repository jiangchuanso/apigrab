package com.gitee.apigrab.admin.init;

import com.gitee.apigrab.common.util.BCryptUtils;
import com.gitee.apigrab.persistence.dao.SystemUserDao;
import com.gitee.apigrab.persistence.entity.SystemUserEntity;
import com.gitee.apigrab.persistence.entity.enums.EnableStatus;
import com.gitee.apigrab.persistence.entity.enums.UserRole;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

/**
 * Seeds a default administrator (admin/admin123) when the user table is empty.
 */
@Component
public class SystemUserInitializer implements ApplicationRunner {

    @Resource
    private SystemUserDao systemUserDao;

    @Override
    public void run(ApplicationArguments args) {
        List<SystemUserEntity> existing = systemUserDao.list(new SystemUserEntity());
        if (existing != null && !existing.isEmpty()) {
            return;
        }
        SystemUserEntity admin = new SystemUserEntity();
        admin.setUsername("admin");
        admin.setPassword(BCryptUtils.hashPassword("123456"));
        admin.setRealName("管理员");
        admin.setEmail("");
        admin.setAddress("");
        admin.setRole(UserRole.ADMIN);
        admin.setStatus(EnableStatus.ENABLED);
        systemUserDao.insert(admin);
    }
}
