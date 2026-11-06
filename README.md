# AI玄学命理测算平台 — 技术文档

> 版本：v1.0.0  
> 日期：2026-06-15  
> 作者：项目自动分析

---

## 目录

1. [项目概述](#1-项目概述)
2. [技术栈](#2-技术栈)
3. [系统架构](#3-系统架构)
4. [数据库设计](#4-数据库设计)
5. [接口文档](#5-接口文档)
6. [核心业务逻辑](#6-核心业务逻辑)
7. [安全机制](#7-安全机制)
8. [前后端交互](#8-前后端交互)
9. [AI集成](#9-ai集成)
10. [部署说明](#10-部署说明)

---

## 1. 项目概述

**AI玄学命理测算平台**是一个基于 Spring Boot + Vue 3 的 Web 应用，为用户提供在线玄学命理测算服务。核心功能包括：

- **AI命理测算**：用户输入生辰八字等信息，调用 DeepSeek AI 生成命理分析报告
- **AI自由对话**：基于测算记录的追问对话，以及自由对话功能
- **签到系统**：每日签到获得积分，连续签到奖励递增
- **每日运势**：六爻占卜每日运势，支持运势趋势图
- **积分商城**：积分兑换卡密、增加每日次数等虚拟商品
- **收藏系统**：收藏感兴趣的测算记录
- **通知系统**：站内信推送，支持管理员广播
- **用户反馈**：用户提交反馈，管理员回复处理
- **用户等级**：根据累计消耗积分自动升级（新手→入门→熟练→大师→宗师）
- **卡密系统**：管理员批量生成卡密，用户兑换积分
- **后台管理**：用户管理、卡密管理、数据统计、商品管理

### 1.1 项目结构

```
AI玄学命理测算平台/
├── divination-backend/          # Spring Boot 后端
│   ├── src/main/java/com/divination/
│   │   ├── controller/          # REST 控制器（4个）
│   │   ├── service/             # 业务逻辑层
│   │   │   └── impl/            # 服务实现
│   │   ├── mapper/              # MyBatis-Plus Mapper
│   │   ├── entity/              # 数据实体（16个）
│   │   ├── config/              # 配置类（5个）
│   │   ├── common/              # 公共模块
│   │   │   ├── constant/        # 常量
│   │   │   ├── exception/       # 异常处理
│   │   │   ├── result/          # 统一响应
│   │   │   └── util/            # 工具类
│   │   └── DivinationApplication.java
│   └── src/main/resources/
│       └── application.yml
├── divination-frontend/         # Vue 3 前端
│   ├── src/
│   │   ├── api/                 # API 请求封装（11个模块）
│   │   ├── components/          # 公共组件
│   │   ├── router/              # 路由配置
│   │   ├── store/               # Pinia 状态管理
│   │   ├── utils/               # 工具函数
│   │   └── views/               # 页面视图
│   ├── package.json
│   └── vite.config.js
└── pom.xml (backend)
```

---

## 2. 技术栈

### 2.1 后端

| 技术 | 版本 | 用途 |
|------|------|------|
| Java | 17 | 运行环境 |
| Spring Boot | 3.2.5 | 应用框架 |
| Spring Security | 6.x | 安全认证 |
| MyBatis-Plus | 3.5.6 | ORM 框架 |
| MySQL | 8.x | 数据库 |
| JWT (jjwt) | 0.12.5 | Token 认证 |
| BCrypt | — | 密码加密 |
| DeepSeek API | — | AI 大模型接口 |
| RestTemplate | — | HTTP 客户端 |
| Hutool | 5.8.26 | 工具库 |
| FastJSON2 | 2.0.47 | JSON 解析 |
| SpringDoc OpenAPI | 2.5.0 | API 文档 |
| Lombok | 1.18.34 | 代码简化 |

### 2.2 前端

| 技术 | 版本 | 用途 |
|------|------|------|
| Vue | 3.4.21 | 前端框架 |
| Vue Router | 4.3.0 | 路由管理 |
| Pinia | 2.1.7 | 状态管理 |
| Axios | 1.6.8 | HTTP 请求 |
| Element Plus | 2.6.3 | UI 组件库 |
| ECharts | 6.1.0 | 数据可视化 |
| Vite | 5.2.8 | 构建工具 |

---

## 3. 系统架构

### 3.1 架构分层

```
┌─────────────────────────────────────────────────────────────────┐
│                        浏览器 (Vue 3)                           │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐          │
│  │ 路由守卫 │ │ Pinia Store│ │ Axios  │ │ 页面组件 │          │
│  └────┬─────┘ └────┬─────┘ └────┬─────┘ └────┬─────┘          │
└───────┼────────────┼─────────────┼─────────────┼────────────────┘
        │ HTTP/JSON  │             │             │
        │ (Bearer Token)          │             │
        ▼                     ▼             ▼
┌─────────────────────────────────────────────────────────────────┐
│                     Spring Boot 3.2.5                            │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │  Controller 层                                           │   │
│  │  UserController | DivinationController | CardController   │   │
│  │  AdminController                                         │   │
│  └─────────────────────┬────────────────────────────────────┘   │
│  ┌─────────────────────▼────────────────────────────────────┐   │
│  │  Filter 层                                               │   │
│  │  JwtAuthenticationFilter (JWT验证 + 权限校验)             │   │
│  └─────────────────────┬────────────────────────────────────┘   │
│  ┌─────────────────────▼────────────────────────────────────┐   │
│  │  Service 层                                              │   │
│  │  UserService | DivinationRecordService | AiChatService   │   │
│  │  CardService | AdminService                              │   │
│  └─────────────────────┬────────────────────────────────────┘   │
│  ┌─────────────────────▼────────────────────────────────────┐   │
│  │  Mapper 层 (MyBatis-Plus)                                 │   │
│  │  UserMapper | RecordMapper | CardKeyMapper | ...         │   │
│  └─────────────────────┬────────────────────────────────────┘   │
│  ┌─────────────────────▼────────────────────────────────────┐   │
│  │  外部集成                                                │   │
│  │  AiRequestUtil → RestTemplate → DeepSeek API             │   │
│  └──────────────────────────────────────────────────────────┘   │
└────────────────────────────────────────┬────────────────────────┘
                                         │ JDBC
                                         ▼
                                ┌────────────────┐
                                │   MySQL 8.0    │
                                │ divination_pc_db│
                                └────────────────┘
```

### 3.2 请求处理流程

```
前端请求
  │
  ├─ 携带 Authorization: Bearer <token> 头
  │
  ▼
JwtAuthenticationFilter
  ├─ 解析 JWT Token
  ├─ 提取 userId、username、role
  ├─ 校验 tokenVersion 防踢
  └─ 设置 SecurityContext + request attributes
  │
  ▼
SecurityConfig 鉴权
  ├─ /api/user/register, /login → permitAll
  ├─ /api/admin/** → authenticated
  └─ 其他 → authenticated
  │
  ▼
Controller → Service → Mapper → MySQL
  │
  ▼
统一返回 Result<T> 格式
```

---

## 4. 数据库设计

### 4.1 实体关系图

```
user (1) ──┬── (N) divination_record
           ├── (N) sign_in_record
           ├── (N) fortune_calendar
           ├── (N) push_notification
           ├── (N) favorite_record ──→ divination_record
           ├── (N) notification
           ├── (N) user_feedback
           ├── (N) integral_order
           ├── (N) card_key (作为 userId 持有者)
           └── (1) user_level
```

### 4.2 数据表清单

#### 4.2.1 `user` — 用户表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT AUTO | 主键 |
| username | VARCHAR | 登录用户名（唯一） |
| password | VARCHAR | BCrypt 加密密码 |
| nickname | VARCHAR | 昵称 |
| integral | INT | 积分余额 |
| role | VARCHAR | USER / ADMIN |
| status | INT | 1=正常, 0=禁用 |
| register_time | DATETIME | 注册时间 |
| last_login_time | DATETIME | 最后登录时间 |
| login_ip | VARCHAR | 最后登录IP |
| login_count | INT | 登录次数 |
| token_version | INT | Token 版本（用于踢人） |
| real_name | VARCHAR | 真实姓名 |
| gender | VARCHAR | 男/女 |
| birth_date | VARCHAR | 公历生日 |
| birth_time | VARCHAR | 出生时辰 |

#### 4.2.2 `divination_record` — 测算记录表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT AUTO | 主键 |
| user_id | BIGINT | 用户ID |
| type | VARCHAR | 测算类型 |
| input_info | VARCHAR(2000) | 用户输入信息 |
| result | TEXT | AI 生成结果 |
| cost_integral | INT | 消耗积分 |
| create_time | DATETIME | 创建时间 |

#### 4.2.3 `card_key` — 卡密表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT AUTO | 主键 |
| card_key | VARCHAR(32) | 卡密字符串 |
| integral | INT | 卡密价值（积分） |
| batch_no | VARCHAR | 批次号 |
| status | INT | 0=未兑换, 1=已兑换, 2=已作废 |
| user_id | BIGINT | 兑换用户ID |
| exchange_time | DATETIME | 兑换时间 |
| create_time | DATETIME | 创建时间 |

#### 4.2.4 `sign_in_record` — 签到记录表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT AUTO | 主键 |
| user_id | BIGINT | 用户ID |
| sign_in_date | DATE | 签到日期 |
| consecutive_days | INT | 连续签到天数 |
| reward_integral | INT | 奖励积分 |
| create_time | DATETIME | 创建时间 |

#### 4.2.5 `fortune_calendar` — 每日运势表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT AUTO | 主键 |
| user_id | BIGINT | 用户ID |
| fortune_date | DATE | 运势日期 |
| overall_score | INT | 综合评分 (1-10) |
| career_score | INT | 事业评分 |
| wealth_score | INT | 财运评分 |
| love_score | INT | 感情评分 |
| health_score | INT | 健康评分 |
| fortune_text | TEXT | 运势文字描述 |
| lucky_color | VARCHAR | 幸运颜色 |
| lucky_number | VARCHAR | 幸运数字 |
| lucky_direction | VARCHAR | 幸运方向 |
| create_time | DATETIME | 创建时间 |

#### 4.2.6 `push_notification` — 推送通知设置表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT AUTO | 主键 |
| user_id | BIGINT | 用户ID |
| enable_fortune | INT | 是否开启运势推送 |
| enable_promotion | INT | 是否开启推广推送 |
| enabled_at | DATETIME | 开启时间 |
| updated_at | DATETIME | 更新时间 |

#### 4.2.7 `favorite_record` — 收藏记录表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT AUTO | 主键 |
| user_id | BIGINT | 用户ID |
| record_id | BIGINT | 关联测算记录ID |
| create_time | DATETIME | 收藏时间 |

#### 4.2.8 `notification` — 站内通知表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT AUTO | 主键 |
| user_id | BIGINT | 用户ID |
| title | VARCHAR | 通知标题 |
| content | TEXT | 通知内容 |
| type | VARCHAR | 类型 (PUSH/PROMOTION 等) |
| is_read | INT | 0=未读, 1=已读 |
| create_time | DATETIME | 创建时间 |

#### 4.2.9 `user_feedback` — 用户反馈表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT AUTO | 主键 |
| user_id | BIGINT | 用户ID |
| feedback_type | VARCHAR | 反馈类型 |
| content | TEXT | 反馈内容 |
| contact | VARCHAR | 联系方式 |
| status | INT | 0=待处理, 1=已回复 |
| reply | TEXT | 管理员回复 |
| create_time | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |

#### 4.2.10 `integral_shop` — 积分商城商品表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT AUTO | 主键 |
| item_name | VARCHAR | 商品名称 |
| item_desc | TEXT | 商品描述 |
| cost_integral | INT | 所需积分 |
| item_type | VARCHAR | 类型 (CARD_KEY/EXTRA_LIMIT) |
| item_value | VARCHAR | 商品值 |
| stock | INT | 库存 (-1=无限) |
| enabled | INT | 1=上架, 0=下架 |
| sort_order | INT | 排序 |
| create_time | DATETIME | 创建时间 |

#### 4.2.11 `integral_order` — 积分兑换订单表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT AUTO | 主键 |
| user_id | BIGINT | 用户ID |
| shop_item_id | BIGINT | 关联商品ID |
| cost_integral | INT | 消耗积分 |
| status | INT | 订单状态 |
| remark | VARCHAR | 备注（卡密值） |
| create_time | DATETIME | 创建时间 |

#### 4.2.12 `user_level` — 用户等级表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT AUTO | 主键 |
| user_id | BIGINT | 用户ID |
| level | VARCHAR | 等级 (新手/入门/熟练/大师/宗师) |
| level_points | INT | 经验值 |
| total_divination_count | INT | 总测算次数 |
| updated_at | DATETIME | 更新时间 |

#### 4.2.13 `ai_chat_session` — AI 会话表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT AUTO | 主键 |
| user_id | BIGINT | 用户ID |
| record_id | BIGINT | 关联测算记录ID（可空） |
| title | VARCHAR | 会话标题 |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |

#### 4.2.14 `ai_chat_message` — AI 对话消息表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT AUTO | 主键 |
| session_id | BIGINT | 关联会话ID |
| role | VARCHAR | user / assistant |
| content | TEXT | 消息内容 |
| create_time | DATETIME | 创建时间 |

#### 4.2.15 `share_poster` — 分享海报表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT AUTO | 主键 |
| 含 share_url 等字段 | | 分享链接 |

---

## 5. 接口文档

> 基础 URL: `http://localhost:8080/api`  
> 所有接口返回格式: `{ "code": 200, "msg": "...", "data": ... }`  
> 除注册/登录外，所有接口需在请求头携带: `Authorization: Bearer <token>`

---

### 5.1 认证接口 (`/api/user`)

#### 5.1.1 用户注册
- **URL**: `POST /api/user/register`
- **权限**: 公开
- **请求体**:
  ```json
  {
    "username": "string (2-50字符)",
    "password": "string (大小写字母+数字, 6-20位)",
    "confirmPassword": "string"
  }
  ```
- **响应**:
  ```json
  {
    "code": 200,
    "msg": "注册成功",
    "data": {
      "token": "eyJhbGc...",
      "userId": 1,
      "username": "string",
      "nickname": "string",
      "integral": 50,
      "role": "USER"
    }
  }
  ```
- **逻辑**: 创建用户 → BCrypt加密密码 → 赠送50积分 → 生成JWT Token

#### 5.1.2 用户登录
- **URL**: `POST /api/user/login`
- **权限**: 公开
- **请求体**:
  ```json
  { "username": "string", "password": "string" }
  ```
- **响应**: 同注册响应
- **逻辑**: 验证用户名密码 → 检查账号状态 → 更新登录信息 → 生成JWT Token

#### 5.1.3 获取用户信息
- **URL**: `GET /api/user/info`
- **权限**: 登录
- **响应**:
  ```json
  {
    "code": 200,
    "data": {
      "userId": 1,
      "username": "string",
      "nickname": "string",
      "integral": 50,
      "role": "USER",
      "realName": "string",
      "gender": "男/女",
      "birthDate": "string",
      "birthTime": "string",
      "registerTime": "datetime"
    }
  }
  ```

#### 5.1.4 退出登录
- **URL**: `POST /api/user/logout`
- **权限**: 登录

#### 5.1.5 获取测算记录
- **URL**: `GET /api/user/records?page=1&size=10`
- **权限**: 登录
- **响应**: 分页的测算记录列表

---

### 5.2 测算接口 (`/api/divination`)

#### 5.2.1 创建测算
- **URL**: `POST /api/divination/create`
- **权限**: 登录
- **请求体**:
  ```json
  {
    "type": "string (测算类型)",
    "inputInfo": "string (用户信息)"
  }
  ```
- **响应**: 创建成功的 `DivinationRecord` 对象（含 AI 生成的 result）
- **逻辑**:
  1. 检查今日测算次数是否超过上限（30次）
  2. 调用 DeepSeek AI 生成测算结果
  3. 原子扣减积分（10积分/次）
  4. 保存测算记录
  5. 更新用户等级

#### 5.2.2 删除测算记录
- **URL**: `DELETE /api/divination/delete/{id}`
- **权限**: 登录（仅本人可删除）

#### 5.2.3 AI 自由对话（旧版）
- **URL**: `POST /api/divination/ai/chat`
- **权限**: 登录
- **请求体**:
  ```json
  {
    "message": "string",
    "history": [{"role": "ai/user", "content": "..."}]
  }
  ```
- **响应**:
  ```json
  { "code": 200, "data": { "reply": "string" } }
  ```
- **逻辑**: 将用户个人信息 + 八字注入 systemPrompt → 调用 AI

#### 5.2.4 AI 会话管理（新版）

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/divination/chat/session` | POST | 基于测算记录创建会话 |
| `/api/divination/chat/session/{id}` | GET | 获取会话详情 |
| `/api/divination/chat/session/{id}/messages` | GET | 获取会话消息列表 |
| `/api/divination/chat/session/{id}/message` | POST | 发送消息（AI 回复） |
| `/api/divination/chat/session/standalone` | POST | 创建自由对话会话 |

---

### 5.3 卡密接口 (`/api/card`)

#### 5.3.1 兑换卡密
- **URL**: `POST /api/card/exchange`
- **权限**: 登录
- **请求体**:
  ```json
  { "cardKey": "string (32位卡密)" }
  ```
- **响应**:
  ```json
  {
    "code": 200,
    "msg": "兑换成功，积分已到账",
    "data": { "integral": 150 }
  }
  ```
- **逻辑**: 查找卡密 → 检查状态 → 标记已兑换 → 原子增加积分

---

### 5.4 用户功能接口 (`/api/user` 续)

#### 5.4.1 签到
| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/user/signin` | POST | 每日签到（获积分奖励） |
| `/api/user/signin/status` | GET | 签到状态 |

- 连续签到奖励：1天=5分, 3天=10分, 7天=15分
- 签到后自动生成当日六爻运势

#### 5.4.2 每日运势
| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/user/fortune/calendar?year=2026&month=6` | GET | 获取月度运势 |
| `/api/user/fortune/generate` | POST | 手动生成今日运势 |

- 运势数据：综合/事业/财运/感情/健康 评分 + 幸运属性
- 六爻占卜通过 AI 生成，JSON 格式解析

#### 5.4.3 推送设置
| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/user/push/settings` | GET | 获取推送设置 |
| `/api/user/push/settings` | PUT | 更新推送设置 |

- 设置项：`enable_fortune`（运势推送）、`enable_promotion`（推广推送）

#### 5.4.4 收藏系统
| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/user/favorite/{recordId}` | POST | 收藏 |
| `/api/user/favorite/{recordId}` | DELETE | 取消收藏 |
| `/api/user/favorites` | GET | 获取收藏列表 |
| `/api/user/favorite/check/{recordId}` | GET | 检查是否已收藏 |

#### 5.4.5 通知系统
| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/user/notifications?page=1&size=20` | GET | 获取通知列表 |
| `/api/user/notification/{id}/read` | POST | 标记已读 |
| `/api/user/notifications/read-all` | POST | 全部已读 |

#### 5.4.6 反馈系统
| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/user/feedback` | POST | 提交反馈 |
| `/api/user/feedback/list` | GET | 我的反馈列表 |

#### 5.4.7 积分商城
| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/user/shop?enabled=1` | GET | 商品列表 |
| `/api/user/shop/redeem/{itemId}` | POST | 兑换商品 |

- 商品类型：
  - `CARD_KEY`：兑换卡密 → 自动创建卡密订单
  - `EXTRA_LIMIT`：增加每日次数 → 返还等量积分

#### 5.4.8 用户等级
| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/user/level` | GET | 获取用户等级数据 |

- 等级阈值：50分→入门, 200分→熟练, 500分→大师, 1000分→宗师

#### 5.4.9 测算趋势
| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/user/records/trend?days=30` | GET | 测算趋势（含类型细分） |

#### 5.4.10 背包系统
| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/user/backpack` | GET | 我的背包（未使用的卡密） |
| `/api/user/backpack/use/{id}` | POST | 使用卡密 |

#### 5.4.11 用户资料
| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/user/profile` | GET | 获取用户资料 |
| `/api/user/profile` | PUT | 更新用户资料 |

---

### 5.5 后台管理接口 (`/api/admin`)

> ⚠️ 所有 `/api/admin/**` 接口需要 `ROLE_ADMIN` 权限

#### 5.5.1 用户管理
| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/admin/user/list?page=1&size=10&username=&nickname=&status=` | GET | 用户列表 |
| `/api/admin/user/update` | PUT | 更新用户（昵称/积分/角色/状态） |
| `/api/admin/user/delete/{id}` | DELETE | 删除用户 |
| `/api/admin/user/reset-password` | PUT | 重置密码 |

- 禁用/重置密码操作会递增 `tokenVersion`，强制用户重新登录

#### 5.5.2 卡密管理
| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/admin/card/generate` | POST | 批量生成卡密 |
| `/api/admin/card/list?page=1&size=10&batchNo=&status=` | GET | 卡密列表 |
| `/api/admin/card/invalid/{id}` | PUT | 作废卡密 |
| `/api/admin/card/delete/{id}` | DELETE | 删除卡密 |

- 卡密格式：`XXXX-XXXX-XXXX-XXXX-XXXX-XXXX-XXXX-XXXX`（4×8字母数字，用 `-` 分隔）

#### 5.5.3 数据统计
| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/admin/statistics` | GET | 统计概览 |
| `/api/admin/stat/trend?days=30` | GET | 趋势数据 |

- 统计项：总用户数、今日新增用户、今日测算次数、总消耗积分、今日卡密兑换
- 趋势数据：测算趋势（按日期/按类型）、签到趋势

#### 5.5.4 通知管理
| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/admin/notification/list` | GET | 通知列表 |
| `/api/admin/notification/broadcast` | POST | 广播通知 |

#### 5.5.5 反馈管理
| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/admin/feedback/list` | GET | 反馈列表 |
| `/api/admin/feedback/reply` | PUT | 回复反馈 |

#### 5.5.6 商品管理
| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/admin/shop/item` | POST | 新增商品 |
| `/api/admin/shop/item/{id}` | PUT | 更新商品 |

---

## 6. 核心业务逻辑

### 6.1 测算流程

```
用户提交测算请求
    │
    ├─ ① 检查今日次数上限 (30次/天)
    │
    ├─ ② 调用 DeepSeek AI 生成测算结果
    │     └─ system: "你是资深传统玄学命理师..."
    │     └─ user: "测算类型：X \n 用户信息：Y"
    │     └─ fallback: AI 不可用时返回模拟结果
    │
    ├─ ③ 原子扣减积分 (10积分/次)
    │     SQL: UPDATE user SET integral = integral - 10
    │            WHERE id = ? AND integral >= 10
    │     → 防止并发扣成负数
    │
    ├─ ④ 保存测算记录
    │
    └─ ⑤ 更新用户等级
```

### 6.2 八字计算引擎 (`DivinationBaziUtil`)

纯前端算法实现，无需外部依赖：

```
公历日期 → 天干地支（四柱）

年柱：(年份-4) % 10 天干, (年份-4) % 12 地支
月柱：年干定月干（五虎遁诀），月支固定（正月寅）
日柱：基姆拉尔森公式变体，60甲子循环
时柱：日干定时干（五鼠遁诀），时辰区间划分
```

支持中文时辰输入（子时、丑时...），自动转换为对应小时区间。

### 6.3 积分体系

| 行为 | 积分变化 | 说明 |
|------|----------|------|
| 注册赠送 | +50 | 首次注册 |
| 签到（1天） | +5 | 首次签到 |
| 签到（3天） | +10 | 连续3天 |
| 签到（7天） | +15 | 连续7天 |
| 单次测算 | -10 | 每日最多30次 |
| 卡密兑换 | +卡密值 | 管理员设置 |
| 商城兑换 | -商城价 | 商品类型决定行为 |

**并发安全**：所有积分操作使用原子 SQL：
```sql
-- 扣减（余额不足时返回0）
UPDATE user SET integral = integral - #{amount}
WHERE id = #{userId} AND integral >= #{amount}

-- 增加（不限制上限）
UPDATE user SET integral = integral + #{amount}
WHERE id = #{userId}

-- 调整（防止负数）
UPDATE user SET integral = integral + #{amount}
WHERE id = #{userId} AND integral + #{amount} >= 0
```

### 6.4 卡密系统

- **生成**：管理员通过后台批量生成，格式 `XXXXXXXX-XXXXXXXX-XXXXXXXX-XXXXXXXX-XXXXXXXX-XXXXXXXX-XXXXXXXX-XXXXXXXX`
- **兑换**：用户输入卡密 → 校验状态 → 标记已兑换 → 原子增加积分
- **防并发**：使用 `SELECT ... FOR UPDATE` 行锁防止重复兑换
- **状态**：0=未兑换 → 1=已兑换，2=已作废

### 6.5 签到 + 六爻运势

```
用户签到
  │
  ├─ 检查今日是否已签到
  ├─ 计算连续天数
  ├─ 计算奖励积分
  ├─ 记录签到
  ├─ 原子增加积分
  └─ 生成六爻占卜
        │
        ├─ 调用 DeepSeek AI（JSON 格式输出）
        ├─ 解析卦名、爻辞、五项评分、运势文字、幸运属性
        ├─ 解析失败 → 随机降级
        └─ 存入 fortune_calendar 表（upsert）
```

### 6.6 用户等级

```
等级点 = 累计消耗积分

0     → 新手
50    → 入门
200   → 熟练
500   → 大师
1000  → 宗师

每次测算消耗积分 → 自动累加经验值 → 自动升级
```

### 6.7 Token 版本控制（踢人机制）

```
用户登录 → Token 中包含 tokenVersion（= user.tokenVersion）

管理员禁用用户 / 重置密码：
  user.tokenVersion += 1

下次请求时：
  JwtAuthenticationFilter 对比 Token 中 version 与 DB 中 version
  → 不匹配 → 静默跳过认证 → 前端收到 401 → 跳转登录页
```

---

## 7. 安全机制

### 7.1 JWT 认证

- **算法**：HMAC-SHA (HS256)
- **有效期**：86400000ms = 24小时
- **存储位置**：前端 `localStorage`，请求头 `Authorization: Bearer <token>`
- **Token 内容**：`userId`, `username`, `role`, `tokenVersion`

### 7.2 密码安全

- 存储：BCrypt 哈希
- 校验：`PasswordEncoder.matches()`
- 传输：HTTPS（生产环境建议）

### 7.3 权限控制

```
SecurityConfig 规则：
  permitAll:   /api/user/register, /api/user/login, /swagger-ui/**, /actuator/**
  authenticated: /api/admin/**, 所有其他接口
  admin-only:  Controller 中 checkAdmin() 方法验证 ROLE_ADMIN

CORS: 允许指定域名跨域（application.yml 配置）
```

### 7.4 并发安全

| 场景 | 方案 |
|------|------|
| 积分扣减 | 原子 SQL (`WHERE integral >= amount`) |
| 积分增加 | 原子 SQL |
| 卡密兑换 | `SELECT ... FOR UPDATE` 行锁 |
| 每日测算上限 | 计数查询 + 业务层检查 |

### 7.5 输入校验

- 后端：Spring Validation (`@NotBlank`, `@Pattern`, `@Size`, `@Min`, `@Max`)
- 前端：Element Plus 表单验证

### 7.6 全局异常处理

```
BusinessException     → 400 Bad Request
IllegalArgumentException → 400 Bad Request
BindException         → 400 Bad Request
MethodArgumentNotValidException → 400 Bad Request
AccessDeniedException → 403 Forbidden
Exception             → 500 Internal Server Error
```

---

## 8. 前后端交互

### 8.1 连接建立

```
前端 Axios 实例 (src/utils/request.js)
  │
  ├─ baseURL: 'http://localhost:8080/api'
  ├─ timeout: 60000ms (60秒，因 AI 调用可能较慢)
  ├─ Content-Type: 'application/json; charset=utf-8'
  │
  ├─ 请求拦截器：
  │     从 Pinia (localStorage) 读取 token
  │     添加到 Authorization 请求头
  │
  ├─ 响应拦截器：
  │     code === 200 → 返回 data
  │     code !== 200 → ElMessage 提示错误
  │     401 → 清除登录状态，跳转 /login
  │     403 → 提示权限不足
  │     500 → 提示服务器异常
  │     网络错误 → 提示网络错误
  │
  ▼
后端 Spring MVC
  │
  ├─ JwtAuthenticationFilter 解析 Token
  ├─ SecurityConfig 权限检查
  ├─ Controller 接收请求
  ├─ 业务处理 → 返回 Result<T>
  │
  └─ 前端响应拦截器处理 Result.code
```

### 8.2 前端 API 层

```
src/api/
├── user.js          → /api/user/*      认证、资料、记录
├── divination.js    → /api/divination/* 测算、AI 会话
├── card.js          → /api/card/*      卡密兑换
├── admin.js         → /api/admin/*     后台管理
├── ai.js            → /api/divination/ai/chat  AI 对话
├── signin.js        → /api/user/signin/* 签到
├── fortune.js       → /api/user/fortune/*  运势
├── favorite.js      → /api/user/favorite/* 收藏
├── notification.js  → /api/user/notification/* 通知
├── feedback.js      → /api/user/feedback/* 反馈
├── shop.js          → /api/user/shop/*   积分商城
└── profile.js       → /api/user/profile  用户资料
└── backpack.js      → /api/user/backpack 背包
```

### 8.3 前端状态管理 (Pinia)

```
store/user.js:
  token       → localStorage token
  userId      → localStorage userId
  username    → localStorage username
  nickname    → localStorage nickname
  integral    → localStorage integral
  role        → localStorage role

  setLoginData(data) → 写入 localStorage
  logout()           → 清除 localStorage
  isLoggedIn()       → token 是否为空
  isAdmin()          → role === 'ADMIN'
```

### 8.4 前端路由守卫

```
router.beforeEach:
  1. /login, /register → 直接放行
  2. 未登录 → 跳转 /login
  3. /admin/** 且非 ADMIN → 提示 + 跳转 /home
  4. 其他 → 放行
```

### 8.5 数据流图

```
用户操作 (Vue 组件)
  │
  ├─ 调用 API 函数 (src/api/*.js)
  │     │
  │     └─ axios.post/get/put/delete('http://localhost:8080/api/...', data)
  │           │
  │           ├─ 请求拦截器添加 Bearer Token
  │           │
  │           ▼
  │     Spring Boot (localhost:8080)
  │           │
  │           ├─ JwtAuthenticationFilter
  │           ├─ Controller
  │           ├─ Service
  │           ├─ Mapper → MySQL
  │           └─ 返回 Result<T> (JSON)
  │
  ▼
响应拦截器处理
  ├─ code === 200 → 返回 data
  └─ code !== 200 → ElMessage.error()
  │
  ▼
Vue 组件更新 (响应式)
```

### 8.6 前端页面路由

| 路径 | 页面 | 说明 |
|------|------|------|
| `/login` | Login.vue | 登录 |
| `/register` | Register.vue | 注册 |
| `/home` | Home.vue | 首页 |
| `/divination/type` | TypeSelect.vue | 选择测算类型 |
| `/divination/form` | InfoForm.vue | 填写信息 |
| `/divination/result` | Result.vue | 查看结果 |
| `/personal` | Personal/Index.vue | 个人中心首页 |
| `/personal/record` | RecordList.vue | 记录列表 |
| `/personal/signin` | SignIn.vue | 签到 |
| `/personal/fortune` | FortuneCalendar.vue | 运势日历 |
| `/personal/favorites` | Favorites.vue | 我的收藏 |
| `/personal/shop` | Shop.vue | 积分商城 |
| `/personal/trend` | Trend.vue | 趋势分析 |
| `/personal/notifications` | Notifications.vue | 通知 |
| `/personal/feedback` | Feedback.vue | 反馈 |
| `/personal/backpack` | Backpack.vue | 背包 |
| `/personal/ai-chat` | AiChat.vue | AI 对话 |
| `/personal/profile` | Profile.vue | 个人资料 |
| `/personal/exchange` | CardExchange.vue | 卡密兑换 |
| `/admin/user` | UserManage.vue | 用户管理 |
| `/admin/card` | CardManage.vue | 卡密管理 |
| `/admin/generate` | CardGenerate.vue | 卡密生成 |
| `/admin/stat` | DataStat.vue | 数据统计 |

### 8.7 前端组件

| 组件 | 说明 |
|------|------|
| `Layout.vue` | 主布局（侧边栏 + 顶部导航） |
| `SkeletonLoader.vue` | 骨架屏加载 |
| `TypewriterText.vue` | 打字机效果（AI 文字逐字展示） |
| `CSSBarChart.vue` | CSS 纯样式柱状图 |
| `EChartsTrend.vue` | ECharts 趋势图表 |

---

## 9. AI 集成

### 9.1 配置 (`application.yml`)

```yaml
deepseek:
  api-url: https://api.deepseek.com/v1/chat/completions
  api-key: ${DEEPSEEK_API_KEY:}
  model: deepseek-chat
  timeout: 60000

divination:
  ai-prompt: "你是资深传统玄学命理师，根据用户提供的信息进行专业解读，文风古风典雅，内容详实易懂"
```

### 9.2 调用方式

**工具类**: `AiRequestUtil`

| 方法 | 说明 |
|------|------|
| `callAi(systemPrompt, userPrompt)` | 单次调用（用于测算生成） |
| `callAiChat(systemPrompt, history)` | 带历史对话（用于追问） |

**请求格式**:
```json
{
  "model": "deepseek-chat",
  "messages": [
    { "role": "system", "content": "你是资深传统玄学命理师..." },
    { "role": "user", "content": "测算类型：八字 \n 用户信息：..." }
  ],
  "temperature": 0.7,
  "max_tokens": 2000
}
```

**响应解析**:
```
{
  "choices": [
    { "message": { "role": "assistant", "content": "测算结果..." } }
  ]
}
```

### 9.3 AI 降级策略

```
正常流程: 调用 DeepSeek API
    │
    └─ 异常/fallback:
         ├─ 测算: 返回模拟结果（"您的命格独特，运势起伏有致..."）
         ├─ 六爻占卜: 解析 JSON 失败 → 随机降级（随机评分 + 默认文案）
         ├─ AI 对话: 返回 "AI服务暂时不可用，请稍后重试。"
```

### 9.4 Prompt 增强

所有 AI 调用会自动注入用户个人信息：

```
【用户个人信息】
姓名：XXX
性别：男/女
公历生日：1995-06-15
生辰八字：乙亥年 壬午月 甲子日 庚午时
```

八字由 `DivinationBaziUtil.toBazi()` 实时计算。

### 9.5 AI 配置类

```
AiConfig:          存储 API URL / Key / Model / Prompt
RestTemplateConfig: 配置 RestTemplate (禁用时间戳序列化)
```

---

## 10. 部署说明

### 10.1 后端启动

```bash
cd divination-backend
# 设置环境变量
export DB_PASSWORD="your_mysql_password"
export DEEPSEEK_API_KEY="your_deepseek_api_key"
export JWT_SECRET="your_jwt_secret"

# 编译运行
mvn spring-boot:run
# 或使用打包后的 jar
mvn package -DskipTests
java -jar target/divination-backend-1.0.0.jar
```

**端口**: `8080`  
**数据库**: `mysql://127.0.0.1:3306/divination_pc_db`

### 10.2 前端启动

```bash
cd divination-frontend
npm install
npm run dev
# 默认端口: 5173
```

### 10.3 生产环境配置

| 配置项 | 建议 |
|--------|------|
| 数据库密码 | 使用环境变量 `DB_PASSWORD` |
| API Key | 使用环境变量 `DEEPSEEK_API_KEY` |
| JWT Secret | 使用强随机字符串 |
| CORS | 修改 `divination.allowed-origins` 为生产域名 |
| HTTPS | 使用 Nginx 反向代理 |
| 前端 baseURL | 修改 `src/utils/request.js` 的 `baseURL` |
| Token 有效期 | 生产建议缩短为 `jwt.expire-time: 3600000` (1小时) |
| 日志级别 | 改为 `info` 或 `warn` |

### 10.4 API 文档

启动后可通过 Swagger UI 查看: `http://localhost:8080/swagger-ui/index.html`

---

## 附录

### A. 环境变量清单

| 变量 | 默认值 | 说明 |
|------|--------|------|
| `DB_PASSWORD` | `000000` | 数据库密码 |
| `DEEPSEEK_API_KEY` | 硬编码Key | DeepSeek API 密钥 |
| `JWT_SECRET` | 硬编码密钥 | JWT 签名密钥 |

### B. 业务配置项 (`divination.*`)

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `divination.register-give-integral` | 50 | 注册赠送积分 |
| `divination.once-cost-integral` | 10 | 单次测算消耗积分 |
| `divination.daily-limit-count` | 30 | 每日测算上限 |
| `divination.ai-prompt` | 系统提示词 | AI 角色设定 |
| `divination.allowed-origins` | 逗号分隔列表 | CORS 允许的来源 |

### C. 常量 (`JwtConstants`)

| 常量 | 值 | 说明 |
|------|-----|------|
| `HEADER` | `Authorization` | Token 请求头名 |
| `PREFIX` | `Bearer ` | Token 前缀 |
| `CLAIM_USER_ID` | `userId` | 用户ID字段 |
| `CLAIM_USERNAME` | `username` | 用户名字段 |
| `CLAIM_ROLE` | `role` | 角色字段 |
| `CLAIM_TOKEN_VERSION` | `tokenVersion` | Token版本字段 |

### D. 错误码 (`ResultCode`)

| 常量 | 码值 | 说明 |
|------|------|------|
| `SUCCESS` | 200 | 成功 |
| `ERROR` | 500 | 服务器异常 |
| `UNAUTHORIZED` | 401 | 未登录/Token过期 |
| `FORBIDDEN` | 403 | 权限不足 |
| `USER_DISABLED` | 400 | 账号已禁用 |
| `INTEGRAL_INSUFFICIENT` | 400 | 积分不足 |
| `DAILY_LIMIT_EXCEEDED` | 400 | 当日测算次数已达上限 |
| `CARD_KEY_NOT_FOUND` | 400 | 卡密不存在 |
| `CARD_KEY_USED` | 400 | 卡密已被兑换 |
| `CARD_KEY_INVALID` | 400 | 卡密已作废 |
| `USERNAME_EXISTS` | 400 | 账号已存在 |
| `INVALID_CREDENTIALS` | 401 | 账号或密码错误 |
| `RECORD_NOT_FOUND` | 400 | 记录不存在 |
| `USER_NOT_FOUND` | 400 | 用户不存在 |
