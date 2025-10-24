# MySQL 数据库设置说明

## 错误信息
```
Access denied for user 'loguser'@'localhost' (using password: YES)
```

## 解决方案

### 方法1: 使用MySQL命令行工具

1. 打开命令提示符或PowerShell
2. 登录MySQL（使用root用户）:
```bash
mysql -u root -p
```

3. 输入MySQL root密码后，执行以下SQL命令:
```sql
-- 创建数据库
CREATE DATABASE IF NOT EXISTS logdb CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 创建用户并授权
CREATE USER IF NOT EXISTS 'loguser'@'localhost' IDENTIFIED BY 'logpass';
GRANT ALL PRIVILEGES ON logdb.* TO 'loguser'@'localhost';
FLUSH PRIVILEGES;

-- 验证
SHOW DATABASES;
SELECT user, host FROM mysql.user WHERE user='loguser';

-- 退出
EXIT;
```

### 方法2: 使用MySQL Workbench (图形界面)

1. 打开MySQL Workbench
2. 连接到你的MySQL服务器
3. 点击"Server" -> "Users and Privileges"
4. 点击"Add Account"创建新用户:
   - Login Name: `loguser`
   - Limit to Hosts Matching: `localhost`
   - Password: `logpass`
5. 切换到"Schema Privileges"标签页
6. 点击"Add Entry"，选择"Selected schema"，输入`logdb`
7. 勾选所有权限或选择"SELECT ALL"
8. 点击"Apply"

然后创建数据库:
1. 点击工具栏的"Create a new schema"图标
2. 名称: `logdb`
3. Character Set: `utf8mb4`
4. Collation: `utf8mb4_unicode_ci`
5. 点击"Apply"

### 方法3: 使用SQL脚本文件

已经创建了SQL脚本文件: `setup_database.sql`

执行命令:
```bash
mysql -u root -p < D:\GitHub\claude-demo\log_collector_dev\setup_database.sql
```

## 验证配置

执行以下命令测试连接:
```bash
mysql -u loguser -plogpass -e "SHOW DATABASES;"
```

应该能看到 `logdb` 数据库。

## 应用配置

应用使用的数据库配置（在 `application-dev.yml`）:
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/logdb?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
    username: loguser
    password: logpass
```

## 完成后

创建数据库和用户后，重新运行应用:
```bash
cd D:\GitHub\claude-demo\log_collector_dev
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

或直接运行:
```bash
start_app.bat
```

## 数据库表

应用启动后，Flyway会自动创建以下表:
- `log_entries` - 日志条目
- `transactions` - 事务记录
- `transaction_flow_nodes` - 事务流节点
- `server_configurations` - 服务器配置
- `alert_rules` - 告警规则
- `alert_instances` - 告警实例
- `archive_jobs` - 归档任务
- `user_accounts` - 用户账户
