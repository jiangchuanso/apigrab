package com.gitee.apigrab.persistence.entity;

import com.gitee.apigrab.persistence.entity.enums.PaginationMode;
import com.gitee.apigrab.persistence.entity.enums.ParamLocation;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * Pagination configuration (persisted as JSON in APIGRAB_API_CONFIG.page_config).
 */
@Getter
@Setter
public class PageConfig implements Serializable {
    private static final long serialVersionUID = 1L;

    private PaginationMode mode;
    private ParamLocation paramLocation;
    private String pageParam;
    private String sizeParam;
    private String offsetParam;
    private String limitParam;
    private String cursorParam;
    private String cursorPath;
    private Integer pageSize;
    private Integer maxPages;
    private Integer totalPages;
    private Boolean stopByBatchSize;
    private Integer requestIntervalMs;
    private String initialCursor;
}
