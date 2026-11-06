-- AI玄学命理测算平台 - 数据库性能优化脚本
-- 添加复合索引以优化常见查询模式

USE divination_pc_db;

-- ============================================================
-- 1. 测算记录表优化 - 增加复合索引
-- ============================================================

-- 优化：user_id + create_time 复合索引，用于分页列表和当日统计
-- 这将显著提高 getTodayCount() 和 listRecords() 的性能
ALTER TABLE `divination_record`
ADD INDEX `idx_user_create_time` (`user_id`, `create_time`);

-- 可选：如果按type查询较多，可增加类型索引
-- ALTER TABLE `divination_record` ADD INDEX `idx_type` (`type`);

-- ============================================================
-- 2. 用户表优化 - 已存在良好索引（username唯一索引）
-- ============================================================

-- 如果经常按token_version查询注销用户，可考虑添加索引
-- ALTER TABLE `user` ADD INDEX `idx_token_version` (`token_version`);

-- ============================================================
-- 3. 卡密表优化
-- ============================================================

-- card_key表已有uk_card_key唯一索引
-- 如果需要按用户ID查询兑换记录，可添加以下索引
-- ALTER TABLE `card_key` ADD INDEX `idx_user_id` (`user_id`);

-- ============================================================
-- 4. 其他建议表索引
-- ============================================================

-- 如创建favorite_record表（用于收藏功能），建议添加如下索引：
/*
CREATE TABLE IF NOT EXISTS `favorite_record` (
  `id` bigint NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `record_id` bigint NOT NULL '关联的测算记录ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_favorite` (`user_id`, `record_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 为收藏表的查询优化索引
ALTER TABLE `favorite_record` ADD INDEX `idx_favorite_user` (`user_id`);
*/

-- ============================================================
-- 5. 视图检查
-- ============================================================

-- 验证当前索引状态
SELECT
    TABLE_NAME,
    INDEX_NAME,
    COLUMN_NAME,
    SEQ_IN_INDEX
FROM information_schema.statistics
WHERE TABLE_SCHEMA = 'divination_pc_db'
ORDER BY TABLE_NAME, INDEX_NAME, SEQ_IN_INDEX;

-- ============================================================
-- 6. 配置说明
-- ============================================================
-- 请在应用部署后执行此脚本
-- 建议在低峰期执行大表索引操作
-- 索引创建期间可能会短暂锁定表（InnoDB为在线操作）
-- 生产环境请先在测试环境验证
