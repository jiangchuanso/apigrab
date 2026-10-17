package com.gitee.apigrab.core.alarm;

import lombok.extern.slf4j.Slf4j;
import com.gitee.apigrab.persistence.dao.AlarmConfigDao;
import com.gitee.apigrab.persistence.entity.AlarmConfigEntity;
import org.apache.commons.lang3.text.StrSubstitutor;
import org.apache.commons.lang3.StringEscapeUtils;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class UnifyAlarmOpsService {
    private static final Long ALARM_CONFIG_ID = 1L;

    private final RestTemplate restTemplate;

    @Resource
    private AlarmConfigDao alarmConfigDao;

    public UnifyAlarmOpsService() {
        this.restTemplate = new RestTemplate(new AlarmHttpRequestFactory());
    }

    public AlarmConfigEntity getConfig() {
        return alarmConfigDao.getById(ALARM_CONFIG_ID);
    }

    public void saveConfig(AlarmConfigEntity config) {
        config.setId(ALARM_CONFIG_ID);
        alarmConfigDao.replace(config);
    }

    public void testAlarm(AlarmConfigEntity config, Map<String, String> dataModel) {
        dataModel.put("accessTime", cn.hutool.core.date.DateUtil.now());
        sendAlarm(config, dataModel);
    }

    public void triggerAlarm(Map<String, String> dataModel) {
        AlarmConfigEntity config = alarmConfigDao.getById(ALARM_CONFIG_ID);
        if (config == null || config.getStatus() == null || config.getStatus().getCode() != 1) {
            log.debug("Alarm is disabled, skip");
            return;
        }
        try {
            sendAlarm(config, dataModel);
        } catch (Exception e) {
            log.error("Send alarm message failed", e);
        }
    }

    private void sendAlarm(AlarmConfigEntity config, Map<String, String> dataModel) {
        StrSubstitutor substitutor = new StrSubstitutor(escapeJson(dataModel));
        String body = substitutor.replace(config.getInputTemplate());

        HttpHeaders headers = new HttpHeaders();
        String contentType = config.getContentType();
        if (contentType != null) {
            headers.setContentType(MediaType.parseMediaType(contentType.replace(";", "") + "; charset=UTF-8"));
        } else {
            headers.setContentType(MediaType.APPLICATION_JSON);
        }

        HttpEntity<String> entity = new HttpEntity<>(body, headers);

        ResponseEntity<String> response = restTemplate.exchange(
            config.getEndpoint(), HttpMethod.POST, entity, String.class);

        if (response.getStatusCodeValue() != HttpStatus.OK.value()) {
            log.warn("Alarm response error: {} {}", response.getStatusCode(), response.getBody());
        }
    }

    private Map<String, String> escapeJson(Map<String, String> data) {
        Map<String, String> escaped = new HashMap<>();
        data.forEach((k, v) -> escaped.put(k, StringEscapeUtils.escapeJson(v)));
        return escaped;
    }
}
