package com.gitee.apigrab.admin.service;

import com.gitee.apigrab.persistence.dao.ApiConfigDao;
import com.gitee.apigrab.persistence.dao.CollectTaskDao;
import com.gitee.apigrab.persistence.dao.DatasourceConfigDao;
import com.gitee.apigrab.persistence.dao.ExecutionLogDao;
import com.gitee.apigrab.persistence.entity.ApiConfigEntity;
import com.gitee.apigrab.persistence.entity.CollectTaskEntity;
import com.gitee.apigrab.persistence.entity.DatasourceConfigEntity;
import com.gitee.apigrab.persistence.entity.ExecutionLogEntity;
import com.gitee.apigrab.persistence.entity.enums.ExecutionStatus;
import com.gitee.apigrab.persistence.entity.enums.TaskStatus;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DashboardService {

    @Resource
    private ApiConfigDao apiConfigDao;

    @Resource
    private DatasourceConfigDao datasourceConfigDao;

    @Resource
    private CollectTaskDao collectTaskDao;

    @Resource
    private ExecutionLogDao executionLogDao;

    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();

        // 接口配置数 / 数据源数
        stats.put("apiConfigCount", apiConfigDao.list(new ApiConfigEntity()).size());
        stats.put("datasourceCount", datasourceConfigDao.list(new DatasourceConfigEntity()).size());

        // 任务总数 / 运行中任务数
        List<CollectTaskEntity> allTasks = collectTaskDao.list(new CollectTaskEntity());
        stats.put("taskCount", allTasks.size());
        CollectTaskEntity runningTaskParam = new CollectTaskEntity();
        runningTaskParam.setStatus(TaskStatus.RUNNING);
        stats.put("runningTaskCount", collectTaskDao.list(runningTaskParam).size());

        // 执行统计（按状态分布）
        List<ExecutionLogEntity> logs = executionLogDao.list(new ExecutionLogEntity());
        int total = logs.size();
        int success = (int) logs.stream().filter(l -> l.getStatus() == ExecutionStatus.SUCCESS).count();
        int running = (int) logs.stream().filter(l -> l.getStatus() == ExecutionStatus.RUNNING).count();
        int failed = (int) logs.stream().filter(l -> l.getStatus() == ExecutionStatus.FAILED).count();
        int partial = (int) logs.stream().filter(l -> l.getStatus() == ExecutionStatus.PARTIAL).count();
        stats.put("totalExecutions", total);
        stats.put("successCount", success);
        stats.put("runningCount", running);
        stats.put("failedCount", failed);
        stats.put("partialCount", partial);

        // 总入库记录数
        int totalRecords = logs.stream()
            .filter(l -> l.getTotalRecords() != null)
            .mapToInt(ExecutionLogEntity::getTotalRecords)
            .sum();
        stats.put("totalRecords", totalRecords);

        // 成功率：仅按已结束的执行（成功+失败+部分成功）计算，排除运行中；与饼图口径一致
        int finished = success + failed + partial;
        double successRate = finished == 0 ? 0.0 : Math.round((double) success / finished * 1000) / 10.0;
        stats.put("successRate", successRate);

        return stats;
    }
}
