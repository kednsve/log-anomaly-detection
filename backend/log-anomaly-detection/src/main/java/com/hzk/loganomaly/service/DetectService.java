package com.hzk.loganomaly.service;

public interface DetectService {
    void claimRuleDetect(Long taskId);

    void claimModelDetect(Long taskId);

    void claimDetectAll(Long taskId);

    void runRuleAsync(Long taskId);

    void runModelAsync(Long taskId);

    void runAllAsync(Long taskId);

}
