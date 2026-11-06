# AI玄学命理测算平台 - 缓存配置
# spring.cache.cache-names=divinationRecords,userDailyLimit,userProfile,userInfo

# 缓存策略（根据Caffeine或Redis配置调整）

# divinationRecords: 用户测算记录列表
# - TTL: 5分钟 (刷新频率)
# - Max Size: 100 entries (每个用户的列表)
# - 适合场景：分页查询，变化不频繁

# userDailyLimit: 用户每日限额统计
# - TTL: 24小时 (每天重置)
# - Max Size: 所有活跃用户数
# - 适合场景：getTodayCount()高频读取

# userProfile: 用户基本信息
# - TTL: 30分钟
# - Max Size: 所有用户数
# - 适合场景：用户信息频繁读取

# userInfo: 用户完整资料（含生日、VIP状态等）
# - TTL: 1小时
# - Max Size: 所有用户数
# - 适合场景：权限检查、个性化推荐

# 应用示例：
# @Cacheable("userDailyLimit", key = "#userId")
# public int getTodayCount(Long userId) { ... }

# @CacheEvict(value = "userDailyLimit", key = "#userId")
# @Transactional
# public void createDivination(...) { ... }

# 注意：在分布式环境下，建议使用Redis代替本地Caffeine缓存
# spring.cache.type=redis
# spring.redis.host=${REDIS_HOST:localhost}
# spring.redis.port=${REDIS_PORT:6379}
# spring.redis.timeout=5000ms
