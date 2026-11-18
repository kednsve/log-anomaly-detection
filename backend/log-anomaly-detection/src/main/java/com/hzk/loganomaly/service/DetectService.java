package com.hzk.loganomaly.service;

import com.hzk.loganomaly.dto.DetectResultDTO;

public interface DetectService {
    DetectResultDTO detect(Long taskId);
}
