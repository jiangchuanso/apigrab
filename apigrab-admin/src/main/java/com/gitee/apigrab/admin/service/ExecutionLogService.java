package com.gitee.apigrab.admin.service;

import com.gitee.apigrab.common.exception.CommonException;
import com.gitee.apigrab.common.exception.ResponseErrorCode;
import com.gitee.apigrab.persistence.dao.ExecutionLogDao;
import com.gitee.apigrab.persistence.dao.ExecutionStageLogDao;
import com.gitee.apigrab.persistence.entity.ExecutionLogEntity;
import com.gitee.apigrab.persistence.entity.ExecutionStageLogEntity;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
public class ExecutionLogService {

    @Resource
    private ExecutionLogDao executionLogDao;

    @Resource
    private ExecutionStageLogDao executionStageLogDao;

    public ExecutionLogEntity getById(Long id) {
        ExecutionLogEntity entity = executionLogDao.getById(id);
        if (entity == null) {
            throw new CommonException(ResponseErrorCode.NOT_FOUND, "执行记录不存在");
        }
        return entity;
    }

    public List<ExecutionLogEntity> list(ExecutionLogEntity param) {
        return executionLogDao.list(param);
    }

    public List<ExecutionStageLogEntity> listStages(Long executionId) {
        return executionStageLogDao.getByExecutionId(executionId);
    }
}
