package com.gitee.apigrab.core.collector;

public class DataWriteResult {
    private int successCount;
    private int failCount;

    public DataWriteResult() {}

    public DataWriteResult(int successCount, int failCount) {
        this.successCount = successCount;
        this.failCount = failCount;
    }

    public int getSuccessCount() { return successCount; }
    public void setSuccessCount(int successCount) { this.successCount = successCount; }
    public int getFailCount() { return failCount; }
    public void setFailCount(int failCount) { this.failCount = failCount; }
}
