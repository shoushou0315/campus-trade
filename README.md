# 🎓 campus-trade — 校园二手交易平台

**基于 Java 21 + Spring Boot 的校园二手交易系统** —— 注册认证、商品发布浏览、多条件搜索、购物车、下单与订单状态流转。重点打磨**安全认证、缓存高可用、事务一致性**三项后端工程能力。

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4-green)]()
[![Java](https://img.shields.io/badge/Java-21-orange)]()
[![Spring Security](https://img.shields.io/badge/Security-JWT-blue)]()
[![Redis](https://img.shields.io/badge/Redis-red)]()
[![MySQL](https://img.shields.io/badge/MySQL-8-blue)]()

---

## ✨ 核心亮点

- 🔐 **双 Token + Redis 白名单**：access(15min) + refresh(7d)，登出即失效、改密吊销、单设备踢下线——解决纯 JWT 无法撤销的安全隐患
- 🛡 **缓存高可用**：防穿透（空值标记）、防击穿（SETNX 互斥锁）、防雪崩（TTL 抖动）、版本号 O(1) 失效；Redis 故障自动降级直查 DB
- 📦 **事务一致性**：下单 `@Transactional`（订单+明细+清购物车原子操作）+ 状态机校验流转
- 🗃 **MyBatis 动态 SQL**：多条件分页搜索，表名白名单 + 参数化绑定防注入
- 🧪 **35 个 JUnit 测试**全通过（认证/缓存/下单/权限全链路）

---

## 🚀 快速开始

### 前置依赖

| 组件 | 说明 |
|---|---|
| JDK 21 | |
| MySQL 8 | 库 `campus_trade`（`init_ddl.sql` 建表） |
| Redis | 缓存 + Token 白名单 |

### 启动

```bash
# 1. 配置 application.yml（MySQL/Redis 连接）
# 2. 执行 init_ddl.sql 建库建表（或启动时自动初始化）
mvn spring-boot:run
# API 文档: http://localhost:8080/swagger-ui.html
```

### 测试数据

- 用户：`zhangsan` / `lisi` / `admin`
- 商品：iPhone 14、MacBook M1、考研书、教材、小风扇、哑铃

---

## 🏗 架构总览

```
浏览器 / API 客户端
   │
   ├─ POST /api/auth/register|login → 签发 access + refresh → Redis 白名单
   ├─ GET  /api/products|{id}      → Redis 旁路缓存（防击穿/穿透/雪崩）
   ├─ POST /api/orders              → @Transactional 下单
   └─ ... 带 access token 访问受保护接口
        │
        ▼
Spring Security (无状态)
   └─ JwtAuthFilter
       ├─ 解析 access token（验签 + type + 过期）
       └─ 比对 Redis token:access:{userId}（登出/踢下线后自动失效）
        │
        ▼
Controller → Service → MyBatis Mapper → MySQL
                │
                └─ CacheService（Redis 通用缓存封装）
                    ├─ readThrough: 防穿透 + 防击穿（SETNX 锁）
                    ├─ put: TTL 随机抖动防雪崩
                    └─ bumpVersion: 版本号 O(1) 失效
```

---

## 🧩 模块说明

### 1. 安全认证（P2 + P7 加固）

| 能力 | 实现 |
|---|---|
| 注册/登录 | BCrypt 加密 + 双 Token 签发 |
| 认证链路 | `JwtAuthFilter` 验签 + Redis 白名单比对 |
| 刷新 | `POST /api/auth/refresh`，refresh token 轮换 |
| 登出 | `POST /api/auth/logout`，Redis 双 token 立即删除 |
| 单设备登录 | 新登录覆盖 Redis access，旧设备被踢 |
| 权限 | USER / ADMIN 三级控制，`ROLE_` 前缀 |
| 防刷 | 注册 5次/60s、登录 10次/60s 限流（可配置开关） |

**双 Token 解决的核心问题**：纯 JWT 签发后无法撤销，token 被窃取即永久有效。改造后——

```
登录 → Redis: token:access:{userId}=xxx, token:refresh:{userId}=yyy
请求 → 校验 access 与 Redis 一致？一致放行，不一致 401
登出 → 删 Redis → 旧 token 全部失效
刷新 → 校验 refresh 白名单 → 轮换发新双 token
```

### 2. 缓存（P7 加固）

`CacheService.readThrough()` 统一封装四种防护：

| 问题 | 场景 | 方案 |
|---|---|---|
| 穿透 | 恶意刷不存在的 id | loader 返回 null → 缓存空值标记（TTL 5min） |
| 击穿 | 热点 key 过期瞬间 | Redis SETNX 互斥锁，抢锁线程重建，其余自旋重试 |
| 雪崩 | 大量 key 同时过期 | TTL = 基础值 ± 10% 随机抖动 |
| 列表失效 | 商品变更清缓存 | 版本号 `product:list:ver` 自增（O(1)），替代通配删除 |

**降级**：所有 Redis 操作 try-catch，Redis 宕机自动直查 DB，主流程不中断。

### 3. 商品模块

- 分类树（一次性查全量 + 内存构建）
- 多条件搜索（关键字/分类/校区/价格区间/成色 + 排序分页）
- 商品详情（缓存 + 浏览量自增）
- 发布/编辑/上下架/删除（仅卖家本人，鉴权校验）

### 4. 购物车 & 订单

- 加购自动合并数量（`UNIQUE(user_id, product_id)`）
- 下单 `@Transactional`：校验商品 → 计算金额 → 插订单+明细 → 清购物车
- 状态机：待接单(1) → 已接单(2) → 已完成(3)，取消(0) 各步骤校验角色与前置状态
- 订单明细做商品快照（标题/价格/图片），不受后续修改影响

---

## 🔌 接口清单

### 公开
| 方法 | 路径 | 说明 |
|---|---|---|
| POST | /api/auth/register | 注册（限流） |
| POST | /api/auth/login | 登录（限流） |
| POST | /api/auth/refresh | 刷新 access |
| GET | /api/categories | 分类树 |
| GET | /api/products | 商品搜索 |
| GET | /api/products/{id} | 商品详情 |

### 需登录
| 方法 | 路径 | 说明 |
|---|---|---|
| POST | /api/auth/logout | 登出 |
| GET | /api/user/info | 用户信息 |
| POST/PUT/DELETE | /api/products[/{id}] | 商品管理（仅卖家） |
| GET/POST/PUT/DELETE | /api/cart[/{id}] | 购物车 |
| POST/GET | /api/orders[/sold] | 下单 / 我买的 / 我卖的 |
| GET | /api/orders/{id} | 订单详情 |
| PUT | /api/orders/{id}/status | 状态流转 |

---

## 📁 项目结构

```
src/main/java/com/campus/trade/
├── controller/        # REST 接口层
├── service/           # 业务逻辑（impl/）
├── mapper/            # MyBatis 数据访问
├── entity/            # 数据库实体
├── dto/  vo/          # 请求/响应对象
├── security/          # JwtUtil / JwtAuthFilter
├── common/            # 统一响应/异常/限流注解
└── config/            # Security / Redis / Web 配置
```

---

## 🧪 测试

| 测试类 | 数量 | 覆盖 |
|---|---|---|
| AuthControllerTest | 9 | 注册/登录/刷新/登出/单设备/白名单 |
| ProductControllerTest | 7 | 分类/搜索/详情/发布/缓存 |
| ProductCacheTest | 3 | 缓存命中/空值标记/版本号 |
| OrderControllerTest | 9 | 购物车→下单→接单→完成 |
| UserMapperTest | 4 | MyBatis CRUD |
| CampusTradeApplicationTest | 3 | 统一响应 |

**35/35 全通过**

---

## ⚠️ 免责声明

个人学习项目，数据与逻辑为演示用途。
