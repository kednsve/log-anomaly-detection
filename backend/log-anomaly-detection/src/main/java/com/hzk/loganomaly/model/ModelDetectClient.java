package com.hzk.loganomaly.model;

import com.hzk.loganomaly.common.exception.BusinessException;
import com.hzk.loganomaly.config.ModelDetectConfig;
import com.hzk.loganomaly.dto.ModelDetectResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Slf4j
@Component
public class ModelDetectClient {

    private final RestClient restClient;
    private final RestClient onlineClient;

    public ModelDetectClient(ModelDetectConfig config) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(config.getConnectTimeoutMs());
        factory.setReadTimeout(config.getReadTimeoutMs());
        this.restClient = RestClient
                .builder()
                .baseUrl(config.getBaseUrl())
                .requestFactory(factory)
                .build();
        SimpleClientHttpRequestFactory onlineFactory = new SimpleClientHttpRequestFactory();
        onlineFactory.setConnectTimeout(1000);
        onlineFactory.setReadTimeout(2000);
        this.onlineClient = RestClient.builder()
                                      .baseUrl(config.getBaseUrl())
                                      .requestFactory(onlineFactory)
                                      .build();
    }

    /**
     * 检测 Python 服务是否在线
     */
    public void checkOnline() {
        try {
            onlineClient.get().uri("/online").retrieve().toBodilessEntity();
        } catch (Exception e) {
            log.warn("模型检测服务离线：{}", e.getMessage());
            throw new BusinessException(503, "模型检测服务离线");
        }
    }

    public ModelDetectResponse detect(Long taskId) {
        checkOnline();
        ModelDetectResponse response;
        try {
            response = restClient.post()
                                 .uri("/detect")
                                 .contentType(MediaType.APPLICATION_JSON)
                                 .body(Map.of("taskId", taskId))
                                 .retrieve()
                                 .body(ModelDetectResponse.class);
        } catch (Exception e) {
            log.error("模型调用失败：taskId = {}", taskId, e);
            throw new RuntimeException("调用Python模型检测服务失败", e);
        }
        if (response == null) {
            throw new IllegalStateException("模型服务返回空响应");
        }
        log.info(
                "模型服务检测完成：taskId={}，IP {} 个，异常 {} 个，耗时 {} ms",
                taskId, response.getIpCount(), response.getAbnormalIpCount(), response.getElapsedMs()
        );
        return response;
    }
}
