package com.gitee.apigrab.core.collector;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CollectResult {
    private int totalRecords;
    private int successCount;
    private int failCount;
    private int pageCount;
    private long costMs;

    public CollectResult() {}

    public CollectResult(int totalRecords, int successCount, int failCount, int pageCount) {
        this.totalRecords = totalRecords;
        this.successCount = successCount;
        this.failCount = failCount;
        this.pageCount = pageCount;
    }
}
