package com.gitee.apigrab.admin.controller;

import com.gitee.apigrab.common.model.ResultEntity;
import com.gitee.apigrab.core.datasource.DriverCatalog;
import com.gitee.apigrab.persistence.entity.enums.DbType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据源表单元数据：数据库类型清单由 {@link DbType} 枚举驱动、外部驱动包清单由
 * {@link DriverCatalog} 扫描 drivers 目录得到，避免前后端各维护一份硬编码清单。
 */
@RestController
@RequestMapping("/api/datasource-meta")
public class DatasourceMetaController {

    @Resource
    private DriverCatalog driverCatalog;

    @GetMapping("/db-types")
    public ResultEntity<List<Map<String, Object>>> dbTypes() {
        List<Map<String, Object>> types = new ArrayList<>();
        for (DbType type : DbType.values()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("value", type.name());
            item.put("label", type.getDisplayName());
            item.put("driverClass", type.getDriverClass());
            item.put("urlPrefix", type.getUrlPrefix());
            item.put("urlSample", type.getUrlSample());
            item.put("defaultPort", type.getDefaultPort());
            item.put("externalDriver", type.isExternalDriver());
            item.put("driverDirectory", type.getDriverDirectory());
            types.add(item);
        }
        return ResultEntity.success(types);
    }

    @GetMapping("/drivers")
    public ResultEntity<List<Map<String, Object>>> drivers() {
        return ResultEntity.success(driverCatalog.list());
    }

    @PostMapping("/drivers/refresh")
    public ResultEntity<Void> refreshDrivers() {
        driverCatalog.refresh();
        return ResultEntity.success();
    }
}
