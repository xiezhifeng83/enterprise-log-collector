# 日志查看系统使用说明

## 概述

本系统提供了完整的日志查看和分析功能，包括：
- 日志查询和过滤
- 事务追踪
- 统计分析
- 实时日志展示

## 系统架构

### 后端 API

#### 1. 日志查询 API (`/api/v1/logs`)

**查询日志**
```http
GET /api/v1/logs?serverId={serverId}&startTime={startTime}&endTime={endTime}&errorOnly={boolean}&page={page}&size={size}

参数说明:
- serverId: 服务器ID (可选)
- startTime: 开始时间 (ISO 8601 格式: 2024-10-24T00:00:00)
- endTime: 结束时间
- errorOnly: 是否仅显示错误日志 (true/false)
- page: 页码 (从0开始)
- size: 每页记录数 (默认20)

返回示例:
{
  "content": [
    {
      "id": 1,
      "serverId": 1,
      "fileName": "app.log",
      "logPath": "/var/log/app.log",
      "logType": "application",
      "content": "2024-10-24 10:00:00 INFO  Application started",
      "originalTimestamp": "2024-10-24 10:00:00",
      "collectionTimestamp": "2024-10-24 10:01:00",
      "transactionId": "TXN-12345",
      "parsed": true,
      "errorIndicator": false
    }
  ],
  "totalElements": 100,
  "totalPages": 5,
  "number": 0,
  "size": 20
}
```

**获取单条日志**
```http
GET /api/v1/logs/{id}

返回单条日志详情
```

**按事务查询日志**
```http
GET /api/v1/logs/transaction/{transactionId}

返回指定事务的所有日志
```

**查询错误日志**
```http
GET /api/v1/logs/errors?startTime={startTime}&endTime={endTime}

返回时间范围内的所有错误日志
```

**获取日志统计**
```http
GET /api/v1/logs/statistics?startTime={startTime}&endTime={endTime}

返回示例:
{
  "totalLogs": 10000,
  "errorLogs": 150,
  "parsedLogs": 9500,
  "unparsedLogs": 500,
  "errorRate": 0.015
}
```

#### 2. 事务查询 API (`/api/v1/transactions`)

**查询事务**
```http
GET /api/v1/transactions?status={status}&startTime={startTime}&endTime={endTime}&page={page}&size={size}

参数说明:
- status: 事务状态 (SUCCESS, FAILED, TIMEOUT, IN_PROGRESS)
- startTime: 开始时间
- endTime: 结束时间
- page: 页码
- size: 每页记录数

返回示例:
{
  "content": [
    {
      "transactionId": "TXN-12345",
      "status": "SUCCESS",
      "startTime": "2024-10-24 10:00:00",
      "endTime": "2024-10-24 10:00:05",
      "durationMs": 5000,
      "sourceSystem": "frontend",
      "targetSystem": "backend",
      "errorMessage": null,
      "retryCount": 0
    }
  ],
  "totalElements": 50,
  "totalPages": 3,
  "number": 0,
  "size": 20
}
```

**获取事务详情**
```http
GET /api/v1/transactions/{transactionId}
```

**获取事务关联的日志**
```http
GET /api/v1/transactions/{transactionId}/logs

返回该事务的所有日志记录（按时间顺序）
```

### 前端界面

访问地址: `http://localhost:8080/` 或 `http://localhost:8080/index.html`

#### 功能说明

**1. 日志查询标签页**
- 支持按服务器ID、时间范围筛选
- 支持仅显示错误日志
- 分页显示结果
- 点击"详情"查看完整日志内容
- 点击"事务"跳转到事务追踪页面

**2. 事务追踪标签页**
- 按状态筛选（成功/失败/超时/进行中）
- 按时间范围查询
- 查看事务关联的所有日志
- 分页显示

**3. 统计分析标签页**
- 显示总日志数、错误日志数
- 显示已解析/未解析日志数量
- 计算错误率
- 按时间范围统计

## 启动说明

### 开发环境启动（无认证）

在开发环境下，系统禁用了所有安全认证，便于测试：

```bash
# 激活dev配置
java -jar log-collector.jar --spring.profiles.active=dev

# 或使用 Maven
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

访问: http://localhost:8080/

### 生产环境启动（需要认证）

生产环境需要配置 Keycloak OAuth2 认证：

```bash
# 激活prod配置
java -jar log-collector.jar --spring.profiles.active=prod
```

需要提供有效的 JWT Token 才能访问 API。

## 配置说明

### 安全配置 (SecurityConfig.java)

**开发环境** (`@Profile("dev")`)
- 禁用所有认证
- 允许所有请求

**生产环境** (`@Profile("!dev")`)
- 启用 OAuth2 JWT 认证
- 基于角色的访问控制 (RBAC)
- 静态资源公开访问
- API 需要认证

### CORS 配置

已配置允许所有来源的跨域请求，支持：
- GET, POST, PUT, DELETE, OPTIONS 方法
- 所有请求头
- 凭证传递

## 数据库表结构

### log_entries (日志条目表)
- id: 主键
- server_id: 服务器ID
- file_name: 文件名
- log_path: 日志文件路径
- log_type: 日志类型
- content: 日志内容
- original_timestamp: 原始时间戳
- collection_timestamp: 收集时间戳
- transaction_id: 事务ID
- parsed: 是否已解析
- error_indicator: 是否为错误日志

### transactions (事务表)
- transaction_id: 事务ID (主键)
- status: 状态 (SUCCESS, FAILED, TIMEOUT, IN_PROGRESS)
- start_time: 开始时间
- end_time: 结束时间
- duration_ms: 耗时(毫秒)
- source_system: 源系统
- target_system: 目标系统
- error_message: 错误消息
- retry_count: 重试次数

## 技术栈

### 后端
- Java 17
- Spring Boot 3.2+
- Spring Security (OAuth2 + JWT)
- Spring Data JPA
- MySQL
- Elasticsearch (全文搜索)

### 前端
- 纯 HTML + CSS + JavaScript
- 无需额外依赖
- 响应式设计
- 现代化 UI

## 常见问题

### 1. 无法访问前端页面

确保：
- 应用已启动
- 使用开发环境配置 (`--spring.profiles.active=dev`)
- 访问正确的端口 (默认8080)

### 2. API 返回 401 Unauthorized

如果在生产环境：
- 确认是否配置了 Keycloak
- 检查 JWT Token 是否有效
- 确认用户角色是否正确

建议先使用开发环境测试。

### 3. 日志查询返回空结果

确认：
- 数据库中是否有日志数据
- 时间范围是否正确
- 服务器ID是否存在

### 4. 前端页面显示异常

- 检查浏览器控制台错误
- 确认 API 端点是否正确
- 检查 CORS 配置

## 后续扩展

可以进一步添加的功能：
1. Elasticsearch 全文搜索集成
2. 实时日志流（WebSocket）
3. 日志导出功能（CSV, Excel）
4. 高级图表和可视化
5. 告警规则管理界面
6. 日志归档管理界面

## 联系方式

如有问题，请查看项目文档或联系开发团队。
