package com.divination.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.divination.entity.DivinationRecord;

public interface DivinationRecordService extends IService<DivinationRecord> {
    DivinationRecord createDivination(String type, String inputInfo, int costIntegral, Long userId);
    IPage<DivinationRecord> listRecords(Long userId, int page, int size);
    DivinationRecord getRecord(Long id);
    void deleteRecord(Long id);
    int getTodayCount(Long userId);
    boolean hasEnoughIntegral(Long userId, int cost);
    void deductIntegral(Long userId, int cost);
}
