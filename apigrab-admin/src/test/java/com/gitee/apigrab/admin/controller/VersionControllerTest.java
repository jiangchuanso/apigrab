package com.gitee.apigrab.admin.controller;

import com.gitee.apigrab.admin.dto.VersionResponse;
import com.gitee.apigrab.common.model.ResultEntity;
import com.gitee.apigrab.common.util.PomVerUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VersionControllerTest {

    private VersionController controller = new VersionController();

    @Test
    void testVersion() {
        ResultEntity<VersionResponse> result = controller.version();

        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertNotNull(result.getData().getVersion());
        assertFalse(result.getData().getVersion().trim().isEmpty());
        assertEquals(PomVerUtils.getCachedProjectVersion(), result.getData().getVersion());
    }
}
