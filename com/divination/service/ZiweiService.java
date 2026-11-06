package com.divination.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.divination.entity.ZiweiRecord;

/**
 * 紫微斗数测算服务 — 基于独立排盘引擎（divination-ziwei-engine）+ AI 解读
 */
public interface ZiweiService extends IService<ZiweiRecord> {

    /**
     * 创建紫微斗数测算：排盘 → 存命盘数据 → AI 解读 → 积分计费
     *
     * @param userId       用户ID
     * @param birthDate    公历出生日期 yyyy-MM-dd
     * @param birthTimeZhi 出生时辰地支（子丑寅卯辰巳午未申酉戌亥）
     * @param gender       性别（男/女）
     * @param name         姓名（可选，用于解读称呼）
     * @return 测算记录（含命盘JSON与AI解读结果）
     */
    ZiweiRecord createZiweiDivination(Long userId, String birthDate, String birthTimeZhi,
                                      String gender, String name);

    /**
     * 分页查询用户的紫微测算记录
     */
    IPage<ZiweiRecord> listRecords(Long userId, int page, int size);

    /**
     * 获取单条记录（校验归属）
     */
    ZiweiRecord getRecord(Long userId, Long id);
}
