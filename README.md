# Enterprise Log Collection and Analysis System

一个基于 Spring Boot 的企业级日志收集与分析系统，支持从多台远程服务器自动收集日志，并提供实时分析、告警和可视化功能。

## 📋 目录

- [功能特性](#功能特性)
- [技术架构](#技术架构)
- [快速开始](#快速开始)
- [系统架构](#系统架构)
- [API 文档](#api-文档)
- [配置说明](#配置说明)
- [开发指南](#开发指南)
- [部署说明](#部署说明)

## ✨ 功能特性

### 核心功能

- **🔄 自动化日志收集**
  - 支持多台 Linux 服务器并行日志收集
  - SSH 协议安全连接（JSch）
  - 增量收集，避免重复读取
  - 连接池管理，提高效率
  - 自动重试机制（指数退避）

- **📊 实时数据处理**
  - Kafka 消息队列实现异步处理
  - Avro 序列化，保证数据一致性
  - 批量发送，优化网络传输
  - LZ4 压缩，降低存储成本

- **🔐 安全性**
  - OAuth2 认证（Keycloak）
  - 基于角色的访问控制（RBAC）
  - 密码 AES-256 加密存储
  - 完整的审计日志

- **💾 多层存储**
  - MySQL：热数据（近期日志）
  - Redis：缓存层（位置追踪）
  - Elasticsearch：温数据（可搜索）
  - MinIO：冷数据（长期归档）

- **📈 监控与可视化**
  - Prometheus + Grafana 监控仪表板
  - 自定义健康检查
  - 实时指标收集
  - 分布式追踪（Correlation ID）

## 🏗 技术架构

### 后端技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| Java | 17 (LTS) | 编程语言 |
| Spring Boot | 3.2.0 | 应用框架 |
| Spring Cloud | 2023.0.0 | 微服务框架 |
| Apache Kafka | 3.6.0 | 消息队列 |
| MySQL | 8.0 | 关系数据库 |
| Redis | 7.2 | 缓存 |
| Elasticsearch | 8.11.0 | 搜索引擎 |
| MinIO | Latest | 对象存储 |
| JSch | 0.2.16 | SSH 客户端 |

### 中间件服务

| 服务 | 版本 | 端口 | 说明 |
|------|------|------|------|
| MySQL | 8.0 | 3306 | 主数据库 |
| Redis | 7.2 | 6379 | 缓存和位置追踪 |
| Kafka | 3.6.0 | 9092 | 消息队列 |
| Zookeeper | 3.9.1 | 2181 | Kafka 协调器 |
| Elasticsearch | 8.11.0 | 9200, 9300 | 日志搜索 |
| MinIO | Latest | 9000, 9001 | 对象存储 |
| Keycloak | 23.0.0 | 8180 | 认证授权 |
| Prometheus | Latest | 9090 | 指标收集 |
| Grafana | Latest | 3000 | 可视化 |

## 🚀 快速开始

### 前置要求

- Java 17+
- Maven 3.8+
- Docker & Docker Compose
- Git

### 本地开发

1. **克隆项目**
```bash
git clone https://github.com/yourusername/log_collector_dev.git
cd log_collector_dev
```

2. **启动基础设施服务**
```bash
cd docker
docker-compose up -d
```

等待所有服务启动（约 2-3 分钟），检查状态：
```bash
docker ps
```

3. **编译项目**
```bash
mvn clean compile
```

4. **运行测试**
```bash
mvn test
```

5. **启动应用**
```bash
mvn spring-boot:run
```

或者打包后运行：
```bash
mvn package -DskipTests
java -jar target/enterprise-log-collector-1.0.0-SNAPSHOT.jar
```

6. **访问服务**

- 应用 API: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html
- Actuator: http://localhost:8080/actuator
- Grafana: http://localhost:3000 (admin/admin)
- Prometheus: http://localhost:9090
- Keycloak: http://localhost:8180 (admin/admin)
- MinIO Console: http://localhost:9001 (minioadmin/minioadmin)

## 🏛 系统架构

### 架构图

```
┌─────────────────┐
│   Web Client    │
└────────┬────────┘
         │ HTTPS (OAuth2)
         ▼
┌─────────────────────────────────────────────────────┐
│              Spring Boot Application                │
│  ┌──────────────────────────────────────────────┐  │
│  │          REST API Controllers                 │  │
│  │   (Server Management, Query, Export)          │  │
│  └────────────────┬─────────────────────────────┘  │
│                   │                                 │
│  ┌────────────────▼─────────────────────────────┐  │
│  │         Business Services                     │  │
│  │  - ServerConfigurationService                 │  │
│  │  - LogCollectionService                       │  │
│  │  - LogParsingService                          │  │
│  └────────────────┬─────────────────────────────┘  │
│                   │                                 │
│  ┌────────────────▼─────────────────────────────┐  │
│  │      Infrastructure Components               │  │
│  │  - SSHConnectionManager (Connection Pool)     │  │
│  │  - CollectionPositionTracker (Redis)          │  │
│  │  - RawLogKafkaProducer (Async)                │  │
│  └────────────────┬─────────────────────────────┘  │
└───────────────────┼─────────────────────────────────┘
                    │
         ┌──────────┼──────────┐
         │          │          │
         ▼          ▼          ▼
    ┌────────┐ ┌────────┐ ┌────────┐
    │  SSH   │ │ Kafka  │ │ Redis  │
    │ Servers│ │ Cluster│ │ Cache  │
    └────────┘ └───┬────┘ └────────┘
                   │
          ┌────────┼────────┐
          ▼        ▼        ▼
      ┌──────┐ ┌──────┐ ┌──────┐
      │ MySQL│ │  ES  │ │MinIO │
      │  DB  │ │Search│ │Store │
      └──────┘ └──────┘ └──────┘
```

### 数据流

1. **日志收集流程**
   - 定时调度器触发收集任务
   - SSH 连接到远程服务器
   - 根据 Redis 中的位置增量读取日志
   - 发送原始日志到 Kafka 队列
   - 更新位置到 Redis

2. **日志处理流程**
   - Kafka Consumer 消费原始日志
   - 解析日志内容（时间戳、级别、内容）
   - 识别交易流和错误模式
   - 存储到 MySQL（热数据）
   - 索引到 Elasticsearch（搜索）
   - 归档到 MinIO（冷数据）

## 📡 API 文档

### 服务器管理

#### 创建服务器配置
```http
POST /v1/servers
Authorization: Bearer {token}
Content-Type: application/json

{
  "hostname": "app-server-01.example.com",
  "port": 22,
  "username": "loguser",
  "password": "securepass",
  "logFilePaths": [
    "/var/log/app/application.log",
    "/var/log/app/error.log"
  ],
  "collectionInterval": 30000,
  "batchSize": 500
}
```

响应：
```json
{
  "id": 1,
  "hostname": "app-server-01.example.com",
  "port": 22,
  "username": "loguser",
  "logFilePaths": [
    "/var/log/app/application.log",
    "/var/log/app/error.log"
  ],
  "collectionInterval": 30000,
  "batchSize": 500,
  "status": "DISCONNECTED",
  "createdAt": "2025-10-19T19:00:00",
  "updatedAt": "2025-10-19T19:00:00"
}
```

#### 获取所有服务器
```http
GET /v1/servers
Authorization: Bearer {token}
```

#### 测试连接
```http
POST /v1/servers/test-connection
Authorization: Bearer {token}
Content-Type: application/json

{
  "hostname": "app-server-01.example.com",
  "port": 22,
  "username": "loguser",
  "password": "securepass"
}
```

### 角色权限

| 端点 | ADMIN | OPERATOR | VIEWER |
|------|-------|----------|--------|
| POST /v1/servers | ✅ | ❌ | ❌ |
| GET /v1/servers | ✅ | ✅ | ✅ |
| PUT /v1/servers/{id} | ✅ | ❌ | ❌ |
| DELETE /v1/servers/{id} | ✅ | ❌ | ❌ |
| POST /v1/servers/test-connection | ✅ | ✅ | ❌ |

## ⚙️ 配置说明

### 应用配置

主配置文件：`src/main/resources/application.yml`

```yaml
# 服务器配置
server:
  port: 8080

# 数据源配置
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/log_monitor
    username: loguser
    password: logpass

  # Redis 配置
  redis:
    host: localhost
    port: 6379

  # Kafka 配置
  kafka:
    bootstrap-servers: localhost:9092

# 日志收集配置
log-collection:
  scheduler:
    interval: 30000  # 调度间隔（毫秒）
  ssh:
    timeout: 60000   # SSH 超时（毫秒）
  kafka:
    raw-logs-topic: raw-logs
```

### 环境配置

开发环境：`application-dev.yml`
生产环境：`application-prod.yml`

### 环境变量

```bash
# 数据库
DATABASE_URL=jdbc:mysql://mysql:3306/log_monitor
DATABASE_USERNAME=loguser
DATABASE_PASSWORD=logpass

# 加密密钥
ENCRYPTION_SECRET_KEY=your-32-character-secret-key

# OAuth2
OAUTH2_ISSUER_URI=http://keycloak:8180/realms/log-monitor
```

## 🛠 开发指南

### 项目结构

```
log_collector_dev/
├── src/
│   ├── main/
│   │   ├── java/com/logcollector/
│   │   │   ├── api/              # REST API层
│   │   │   │   ├── controller/   # 控制器
│   │   │   │   ├── dto/          # 数据传输对象
│   │   │   │   ├── service/      # API服务
│   │   │   │   └── exception/    # 异常处理
│   │   │   ├── collector/        # 日志收集层
│   │   │   │   ├── ssh/          # SSH连接管理
│   │   │   │   ├── kafka/        # Kafka生产者
│   │   │   │   ├── position/     # 位置追踪
│   │   │   │   ├── scheduler/    # 调度器
│   │   │   │   ├── service/      # 收集服务
│   │   │   │   └── retry/        # 重试逻辑
│   │   │   ├── domain/           # 领域层
│   │   │   │   ├── model/        # JPA实体
│   │   │   │   └── repository/   # 数据仓库
│   │   │   ├── config/           # 配置层
│   │   │   │   ├── database/     # 数据库配置
│   │   │   │   ├── kafka/        # Kafka配置
│   │   │   │   ├── security/     # 安全配置
│   │   │   │   └── logging/      # 日志配置
│   │   │   └── health/           # 健康检查
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── application-dev.yml
│   │       ├── application-prod.yml
│   │       ├── logback-spring.xml
│   │       ├── avro/             # Avro schemas
│   │       └── db/migration/     # Flyway迁移
│   └── test/
│       └── java/com/logcollector/
│           ├── collector/        # 单元测试
│           ├── contract/         # 契约测试
│           └── integration/      # 集成测试
├── docker/
│   └── docker-compose.yml
├── scripts/
│   ├── setup-keycloak.sh
│   └── generate-test-data.sh
├── pom.xml
└── README.md
```

### 代码规范

- 遵循 Java 编码规范
- 使用 Lombok 减少样板代码
- 所有 public 方法需要 JavaDoc
- 测试覆盖率要求 ≥ 80%

### 运行测试

```bash
# 单元测试
mvn test

# 集成测试
mvn verify -P integration-tests

# 测试覆盖率报告
mvn clean test jacoco:report
# 查看报告: target/site/jacoco/index.html
```

### 添加新功能

1. 在 `.specify/features/` 下创建特性规格
2. 使用 TDD 方式开发：先写测试
3. 实现功能代码
4. 运行测试确保通过
5. 提交代码并创建 Pull Request

## 🚢 部署说明

### Docker 部署

1. **构建镜像**
```bash
mvn clean package -DskipTests
docker build -t log-collector:latest .
```

2. **启动所有服务**
```bash
docker-compose up -d
```

3. **查看日志**
```bash
docker-compose logs -f log-collector-app
```

### Kubernetes 部署

```bash
# 应用配置
kubectl apply -f k8s/configmap.yaml
kubectl apply -f k8s/secrets.yaml

# 部署应用
kubectl apply -f k8s/deployment.yaml
kubectl apply -f k8s/service.yaml

# 检查状态
kubectl get pods
kubectl logs -f <pod-name>
```

### 生产环境配置

1. **数据库优化**
   - 启用分区表（按月）
   - 配置主从复制
   - 定期备份

2. **Kafka 优化**
   - 增加分区数
   - 配置副本因子 ≥ 3
   - 启用压缩

3. **监控告警**
   - 配置 Prometheus 告警规则
   - 设置 Grafana 仪表板
   - 集成钉钉/企业微信告警

## 📊 性能指标

### 系统容量

- **并发服务器数**: 100+
- **日志处理速率**: 10,000 条/秒
- **存储容量**: TB 级
- **查询响应时间**: < 100ms (P99)

### 资源要求

| 组件 | CPU | 内存 | 磁盘 |
|------|-----|------|------|
| 应用服务 | 2 核 | 4 GB | 10 GB |
| MySQL | 4 核 | 8 GB | 500 GB |
| Elasticsearch | 4 核 | 16 GB | 1 TB |
| Kafka | 4 核 | 8 GB | 500 GB |
| Redis | 2 核 | 4 GB | 20 GB |

## 🔧 故障排查

### 常见问题

1. **SSH 连接失败**
   - 检查网络连通性
   - 验证用户名密码
   - 确认 SSH 端口开放

2. **Kafka 连接超时**
   - 检查 Kafka 服务状态
   - 验证网络配置
   - 查看防火墙规则

3. **内存溢出**
   - 调整 JVM 参数：`-Xmx4g -Xms2g`
   - 减小批次大小
   - 启用 G1GC

### 日志分析

```bash
# 应用日志
tail -f logs/application.log

# 收集日志
tail -f logs/collection.log

# 错误日志
grep ERROR logs/application.log
```

## 🤝 贡献指南

欢迎贡献！请遵循以下步骤：

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启 Pull Request

## 📄 许可证

本项目采用 MIT 许可证 - 详见 [LICENSE](LICENSE) 文件

## 👥 联系方式

- 项目维护者: Your Name
- Email: your.email@example.com
- 项目链接: https://github.com/yourusername/log_collector_dev

## 🙏 致谢

- Spring Boot 团队
- Apache Kafka 社区
- Elasticsearch 团队
- 所有贡献者

---

⭐ 如果这个项目对你有帮助，请给一个 Star！
