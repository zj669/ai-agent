# HTTPS 配置指南

**文档版本**: v1.0
**更新日期**: 2025-02-10
**适用环境**: 生产环境 (Profile: prod)

---

## 一、为什么需要 HTTPS

### 1.1 安全风险

在 HTTP 环境下,用户登录时密码以明文形式传输,存在以下风险:
- **中间人攻击**: 攻击者可以截获并查看密码
- **会话劫持**: Token 可能被窃取
- **数据篡改**: 请求和响应可能被修改

### 1.2 HTTPS 的保护

HTTPS 通过 TLS/SSL 加密,提供:
- **加密传输**: 密码和 Token 加密传输
- **身份验证**: 确认服务器身份
- **数据完整性**: 防止数据被篡改

---

## 二、HTTPS 配置步骤

### 2.1 获取 SSL 证书

#### 方案 1: Let's Encrypt (免费,推荐)

```bash
# 安装 Certbot
sudo apt-get update
sudo apt-get install certbot

# 获取证书 (需要域名)
sudo certbot certonly --standalone -d yourdomain.com

# 证书位置
# /etc/letsencrypt/live/yourdomain.com/fullchain.pem
# /etc/letsencrypt/live/yourdomain.com/privkey.pem
```

#### 方案 2: 自签名证书 (仅用于测试)

```bash
# 生成自签名证书
keytool -genkeypair \
  -alias server \
  -keyalg RSA \
  -keysize 2048 \
  -storetype PKCS12 \
  -keystore server.p12 \
  -validity 3650 \
  -storepass changeit

# 输入信息
# CN (Common Name): yourdomain.com
# OU (Organizational Unit): IT Department
# O (Organization): Your Company
# L (Locality): Beijing
# ST (State): Beijing
# C (Country): CN
```

#### 方案 3: 商业证书 (生产环境推荐)

从证书颁发机构 (CA) 购买:
- DigiCert
- GlobalSign
- Comodo

### 2.2 转换证书格式 (如果需要)

```bash
# PEM 转 PKCS12
openssl pkcs12 -export \
  -in fullchain.pem \
  -inkey privkey.pem \
  -out server.p12 \
  -name server \
  -passout pass:your_password
```

### 2.3 放置证书文件

```bash
# 创建证书目录
mkdir -p ai-agent-interfaces/src/main/resources/keystore

# 复制证书
cp server.p12 ai-agent-interfaces/src/main/resources/keystore/

# 设置权限 (仅所有者可读)
chmod 600 ai-agent-interfaces/src/main/resources/keystore/server.p12
```

### 2.4 配置环境变量

```bash
# 编辑环境变量文件
vi /etc/environment

# 添加以下内容
export SSL_KEYSTORE_PASSWORD=your_secure_password
export JWT_SECRET=your_jwt_secret_key_at_least_32_characters
export DB_HOST=your_db_host
export DB_PORT=3306
export DB_NAME=ai_agent
export DB_USERNAME=your_db_user
export DB_PASSWORD=your_db_password
export REDIS_HOST=your_redis_host
export REDIS_PORT=6379
export REDIS_PASSWORD=your_redis_password
export MAIL_HOST=smtp.example.com
export MAIL_PORT=587
export MAIL_USERNAME=your_email@example.com
export MAIL_PASSWORD=your_email_password
export MILVUS_HOST=your_milvus_host
export MILVUS_PORT=19530
export MINIO_ENDPOINT=http://your_minio_host:9000
export MINIO_ACCESS_KEY=your_minio_access_key
export MINIO_SECRET_KEY=your_minio_secret_key
export MINIO_BUCKET=ai-agent

# 重新加载环境变量
source /etc/environment
```

### 2.5 启动应用

```bash
# 使用生产环境配置启动
java -jar ai-agent-interfaces.jar --spring.profiles.active=prod

# 或使用 Maven
mvn spring-boot:run -pl ai-agent-interfaces -Dspring-boot.run.profiles=prod
```

---

## 三、验证 HTTPS 配置

### 3.1 检查 HTTPS 访问

```bash
# 访问 HTTPS 端口
curl -k https://yourdomain.com:8443/actuator/health

# 预期响应
{"status":"UP"}
```

### 3.2 检查 HTTP 重定向

```bash
# 访问 HTTP 端口
curl -I http://yourdomain.com:8080/actuator/health

# 预期响应 (301 重定向)
HTTP/1.1 301 Moved Permanently
Location: https://yourdomain.com:8443/actuator/health
```

### 3.3 检查 HSTS 头

```bash
# 检查响应头
curl -I https://yourdomain.com:8443/actuator/health

# 预期包含
Strict-Transport-Security: max-age=31536000; includeSubDomains; preload
```

### 3.4 检查 Cookie 安全属性

```bash
# 登录并检查 Cookie
curl -X POST https://yourdomain.com:8443/client/user/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"Test@1234"}' \
  -c cookies.txt

# 检查 cookies.txt
cat cookies.txt

# 预期包含
# Secure; HttpOnly; SameSite=Strict
```

---

## 四、Nginx 反向代理 (可选)

如果使用 Nginx 作为反向代理,可以在 Nginx 层处理 HTTPS。

### 4.1 Nginx 配置

```nginx
server {
    listen 80;
    server_name yourdomain.com;

    # HTTP 重定向到 HTTPS
    return 301 https://$server_name$request_uri;
}

server {
    listen 443 ssl http2;
    server_name yourdomain.com;

    # SSL 证书配置
    ssl_certificate /etc/letsencrypt/live/yourdomain.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/yourdomain.com/privkey.pem;

    # SSL 协议和加密套件
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;
    ssl_prefer_server_ciphers on;

    # HSTS
    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains; preload" always;

    # 其他安全头
    add_header X-Frame-Options "SAMEORIGIN" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header X-XSS-Protection "1; mode=block" always;

    # 反向代理到 Spring Boot
    location / {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

### 4.2 Spring Boot 配置 (使用 Nginx 时)

```yaml
# application-prod.yml
server:
  port: 8080
  # 不需要配置 SSL (由 Nginx 处理)
  forward-headers-strategy: native  # 信任 X-Forwarded-* 头
```

---

## 五、证书续期

### 5.1 Let's Encrypt 自动续期

```bash
# 测试续期
sudo certbot renew --dry-run

# 设置自动续期 (Cron Job)
sudo crontab -e

# 添加以下行 (每天凌晨 2 点检查续期)
0 2 * * * certbot renew --quiet --post-hook "systemctl restart ai-agent"
```

### 5.2 商业证书续期

- 证书到期前 30 天,CA 会发送提醒邮件
- 重新购买或续期证书
- 替换旧证书文件
- 重启应用

---

## 六、常见问题

### 6.1 证书不受信任

**问题**: 浏览器提示 "您的连接不是私密连接"

**原因**:
- 使用自签名证书
- 证书域名与访问域名不匹配
- 证书已过期

**解决方案**:
- 生产环境使用 Let's Encrypt 或商业证书
- 确保证书 CN 与域名一致
- 检查证书有效期

### 6.2 HTTP 未重定向到 HTTPS

**问题**: 访问 HTTP 端口没有自动跳转

**原因**:
- `HttpsConfig` 未生效 (Profile 不是 prod)
- Tomcat 配置错误

**解决方案**:
```bash
# 检查 Profile
java -jar ai-agent-interfaces.jar --spring.profiles.active=prod

# 检查日志
tail -f /var/log/ai-agent/application.log | grep "HttpsConfig"
```

### 6.3 证书密码错误

**问题**: 启动时报错 "Keystore was tampered with, or password was incorrect"

**原因**:
- 环境变量 `SSL_KEYSTORE_PASSWORD` 未设置
- 密码错误

**解决方案**:
```bash
# 检查环境变量
echo $SSL_KEYSTORE_PASSWORD

# 重新设置
export SSL_KEYSTORE_PASSWORD=your_password
```

### 6.4 端口被占用

**问题**: 启动时报错 "Port 8443 is already in use"

**原因**:
- 端口被其他进程占用

**解决方案**:
```bash
# 查找占用端口的进程
lsof -i :8443

# 杀死进程
kill -9 <PID>

# 或修改端口
# application-prod.yml
server:
  port: 9443
```

---

## 七、安全检查清单

### 7.1 部署前检查

- [ ] SSL 证书有效且未过期
- [ ] 证书密码已设置为环境变量
- [ ] JWT Secret 已设置为环境变量 (至少 32 字符)
- [ ] 数据库密码已设置为环境变量
- [ ] Redis 密码已设置为环境变量
- [ ] 邮件服务密码已设置为环境变量
- [ ] MinIO 密钥已设置为环境变量
- [ ] 认证调试模式已关闭 (`auth.debug.enabled: false`)

### 7.2 部署后检查

- [ ] HTTPS 访问正常
- [ ] HTTP 自动重定向到 HTTPS
- [ ] HSTS 头正确设置
- [ ] Cookie 包含 Secure 和 HttpOnly 属性
- [ ] 登录功能正常
- [ ] Token 验证正常
- [ ] 日志中无敏感信息泄露

---

## 八、参考资料

- [Let's Encrypt 官方文档](https://letsencrypt.org/docs/)
- [Spring Boot HTTPS 配置](https://docs.spring.io/spring-boot/docs/current/reference/html/howto.html#howto.webserver.configure-ssl)
- [OWASP Transport Layer Protection](https://cheatsheetseries.owasp.org/cheatsheets/Transport_Layer_Protection_Cheat_Sheet.html)
- [Mozilla SSL Configuration Generator](https://ssl-config.mozilla.org/)

---

**文档维护**: backend-developer-2
**最后更新**: 2025-02-10
