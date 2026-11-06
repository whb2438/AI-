package com.divination.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.divination.entity.CardKey;

import java.util.List;

public interface CardService extends IService<CardKey> {
    List<String> generateCardKeys(int count, int integral, String batchNo);
    int exchangeCardKey(String cardKey, Long userId);
    IPage<CardKey> listCardKeys(int page, int size, String batchNo, Integer status);
    void invalidateCardKey(Long id);
    void deleteCardKey(Long id);
    long countTodayExchanges();
}
