package com.divination.service.impl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.divination.common.exception.BusinessException;
import com.divination.common.result.ResultCode;
import com.divination.common.util.CardKeyUtil;
import com.divination.entity.CardKey;
import com.divination.entity.User;
import com.divination.mapper.CardKeyMapper;
import com.divination.service.CardService;
import com.divination.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
@Service
public class CardKeyServiceImpl extends ServiceImpl<CardKeyMapper, CardKey> implements CardService {
    private final CardKeyUtil cardKeyUtil;
    private final UserService userService;
    private com.divination.mapper.UserMapper userMapper;
    public CardKeyServiceImpl(CardKeyUtil cardKeyUtil, UserService userService) {
        this.cardKeyUtil = cardKeyUtil;
        this.userService = userService;
    }
    @Autowired
    public void setUserMapper(com.divination.mapper.UserMapper userMapper) {
        this.userMapper = userMapper;
    }
    @Override
    @Transactional
    public List<String> generateCardKeys(int count, int integral, String batchNo) {
        List<String> keys = new ArrayList<>(count);
        List<CardKey> cardKeys = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            String key = cardKeyUtil.generateCardKey();
            keys.add(key);
            CardKey cardKey = new CardKey();
            cardKey.setCardKey(key);
            cardKey.setIntegral(integral);
            cardKey.setBatchNo(batchNo);
            cardKey.setStatus(0);
            cardKeys.add(cardKey);
        }
        this.saveBatch(cardKeys, Math.min(count, 500));
        return keys;
    }
    @Override
    @Transactional
    public int exchangeCardKey(String cardKey, Long userId) {
        // Use SELECT ... FOR UPDATE to prevent concurrent redemption (race condition fix for BUG-002)
        CardKey cardKeyEntity = this.baseMapper.selectForUpdate(cardKey);
        if (cardKeyEntity == null) {
            throw new BusinessException(ResultCode.CARD_KEY_NOT_FOUND);
        }
        if (cardKeyEntity.getStatus() == 1) {
            throw new BusinessException(ResultCode.CARD_KEY_USED);
        }
        if (cardKeyEntity.getStatus() == 2) {
            throw new BusinessException(ResultCode.CARD_KEY_INVALID);
        }
        cardKeyEntity.setStatus(1);
        cardKeyEntity.setUserId(userId);
        cardKeyEntity.setExchangeTime(LocalDateTime.now());
        this.updateById(cardKeyEntity);
        // Bug fix (BUG-003): use atomic SQL to avoid TOCTOU race
        userMapper.addIntegralAtomically(userId, cardKeyEntity.getIntegral());
        // Return new integral for frontend (fixes BUG-003)
        User user = userService.getUserById(userId);
        return user.getIntegral();
    }
    @Override
    public IPage<CardKey> listCardKeys(int page, int size, String batchNo, Integer status) {
        Page<CardKey> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<CardKey> wrapper = new LambdaQueryWrapper<>();
        if (batchNo != null && !batchNo.isEmpty()) {
            wrapper.eq(CardKey::getBatchNo, batchNo);
        }
        if (status != null) {
            wrapper.eq(CardKey::getStatus, status);
        }
        wrapper.orderByDesc(CardKey::getCreateTime);
        return this.page(pageParam, wrapper);
    }
    @Override
    @Transactional
    public void invalidateCardKey(Long id) {
        CardKey cardKey = this.getById(id);
        if (cardKey != null) {
            cardKey.setStatus(2);
            this.updateById(cardKey);
        }
    }
    @Override
    @Transactional
    public void deleteCardKey(Long id) {
        this.removeById(id);
    }
    @Override
    public long countTodayExchanges() {
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LambdaQueryWrapper<CardKey> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CardKey::getStatus, 1)
               .ge(CardKey::getExchangeTime, startOfDay);
        return this.count(wrapper);
    }
}
