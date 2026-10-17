package com.gitee.apigrab.admin.init;

import com.gitee.apigrab.persistence.dao.SystemUserDao;
import com.gitee.apigrab.persistence.entity.SystemUserEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SystemUserInitializerTest {

    @Mock private SystemUserDao systemUserDao;
    @InjectMocks private SystemUserInitializer initializer;

    @Test
    void createsDefaultAdminWhenEmpty() {
        when(systemUserDao.list(any())).thenReturn(Collections.emptyList());

        initializer.run(null);

        verify(systemUserDao).insert(any(SystemUserEntity.class));
    }

    @Test
    void doesNothingWhenUsersExist() {
        SystemUserEntity existing = new SystemUserEntity();
        existing.setUsername("someone");
        when(systemUserDao.list(any())).thenReturn(Collections.singletonList(existing));

        initializer.run(null);

        verify(systemUserDao, never()).insert(any());
    }
}
