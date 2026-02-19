# 用户认证接口文档

## 基础信息

- **Base URL**: `/client/user`
- **认证方式**: JWT Bearer Token (除公开接口外)
- **Content-Type**: `application/json`

---

## 公开接口 (无需认证)

### 1. 发送邮箱验证码

**接口**: `POST /client/user/email/sendCode`

**描述**: 发送6位数字验证码到指定邮箱,用于注册或重置密码。

**请求参数**:
```json
{
  "email": "user@example.com"
}
```

**字段说明**:
| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| email | string | 是 | 邮箱地址,需符合邮箱格式 |

**成功响应** (200):
```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

**错误响应**:
| HTTP状态码 | 错误码 | 说明 |
|-----------|--------|------|
| 400 | INVALID_EMAIL | 邮箱格式不正确 |
| 429 | RATE_LIMITED | 操作过于频繁,请稍后再试 (1分钟内只能发送1次) |
| 500 | EMAIL_SEND_FAILED | 邮件发送失败 |

**业务规则**:
- 验证码有效期: 5分钟
- 限流规则: 同一邮箱1分钟内只能发送1次
- 验证码为6位数字,范围 100000-999999

---

### 2. 邮箱注册

**接口**: `POST /client/user/email/register`

**描述**: 使用邮箱验证码注册新用户,注册成功后自动登录并返回 Token。

**请求参数**:
```json
{
  "email": "user@example.com",
  "code": "123456",
  "password": "SecurePass123",
  "username": "张三"
}
```

**字段说明**:
| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| email | string | 是 | 邮箱地址 |
| code | string | 是 | 6位验证码 |
| password | string | 是 | 密码,至少8位 |
| username | string | 否 | 用户名,不填则使用邮箱前缀 |

**成功响应** (200):
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "expireIn": 604800,
    "user": {
      "id": 1,
      "username": "张三",
      "email": "user@example.com",
      "phone": null,
      "avatarUrl": null,
      "status": 1,
      "createdAt": "2025-01-15T10:30:00"
    }
  }
}
```

**字段说明**:
| 字段 | 类型 | 说明 |
|------|------|------|
| token | string | JWT Token,用于后续请求认证 |
| expireIn | number | Token 有效期(秒),默认7天 |
| user.id | number | 用户ID |
| user.username | string | 用户名 |
| user.email | string | 邮箱 |
| user.status | number | 状态: 0-禁用, 1-正常 |

**错误响应**:
| HTTP状态码 | 错误码 | 说明 |
|-----------|--------|------|
| 400 | INVALID_VERIFICATION_CODE | 验证码无效或已过期 |
| 400 | EMAIL_ALREADY_REGISTERED | 该邮箱已被注册 |
| 400 | WEAK_PASSWORD | 密码强度不足,至少需要8位 |

**业务规则**:
- 验证码使用后立即销毁,不可重复使用
- 密码使用 BCrypt 加密存储
- 用户名默认为邮箱 @ 前的部分

---

### 3. 用户登录

**接口**: `POST /client/user/login`

**描述**: 使用邮箱和密码登录,返回 JWT Token。

**请求参数**:
```json
{
  "email": "user@example.com",
  "password": "SecurePass123"
}
```

**字段说明**:
| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| email | string | 是 | 邮箱地址 |
| password | string | 是 | 密码 |

**成功响应** (200):
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "expireIn": 604800,
    "user": {
      "id": 1,
      "username": "张三",
      "email": "user@example.com",
      "phone": "13800138000",
      "avatarUrl": "https://example.com/avatar.jpg",
      "status": 1,
      "createdAt": "2025-01-15T10:30:00"
    }
  }
}
```

**错误响应**:
| HTTP状态码 | 错误码 | 说明 |
|-----------|--------|------|
| 401 | INVALID_CREDENTIALS | 用户名或密码错误 |
| 403 | USER_DISABLED | 用户已被禁用 |
| 429 | TOO_MANY_LOGIN_ATTEMPTS | 登录失败次数过多,账号已被锁定15分钟 |

**业务规则**:
- 登录失败限流: 同一邮箱15分钟内最多失败5次,超过后锁定15分钟
- 登录成功后记录最后登录IP和时间
- Token 有效期7天

---

### 4. 重置密码

**接口**: `POST /client/user/resetPassword`

**描述**: 使用邮箱验证码重置密码。

**请求参数**:
```json
{
  "email": "user@example.com",
  "code": "123456",
  "newPassword": "NewSecurePass123",
  "confirmPassword": "NewSecurePass123"
}
```

**字段说明**:
| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| email | string | 是 | 邮箱地址 |
| code | string | 是 | 6位验证码 |
| newPassword | string | 是 | 新密码,至少8位 |
| confirmPassword | string | 是 | 确认密码,需与新密码一致 |

**成功响应** (200):
```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

**错误响应**:
| HTTP状态码 | 错误码 | 说明 |
|-----------|--------|------|
| 400 | INVALID_VERIFICATION_CODE | 验证码无效或已过期 |
| 400 | PASSWORD_MISMATCH | 两次输入的密码不一致 |
| 400 | WEAK_PASSWORD | 密码强度不足,至少需要8位 |
| 404 | USER_NOT_FOUND | 用户不存在 |

**业务规则**:
- 验证码使用后立即销毁
- 密码重置后,所有已登录的 Token 不会自动失效 (建议前端提示用户重新登录)

---

## 受保护接口 (需要认证)

**认证方式**: 在请求头中添加 `Authorization: Bearer {token}`

### 5. 获取当前用户信息

**接口**: `GET /client/user/info`

**描述**: 获取当前登录用户的详细信息。

**请求头**:
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**成功响应** (200):
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "username": "张三",
    "email": "user@example.com",
    "phone": "13800138000",
    "avatarUrl": "https://example.com/avatar.jpg",
    "status": 1,
    "createdAt": "2025-01-15T10:30:00"
  }
}
```

**错误响应**:
| HTTP状态码 | 错误码 | 说明 |
|-----------|--------|------|
| 401 | UNAUTHORIZED | Token 无效或已过期 |
| 404 | USER_NOT_FOUND | 用户不存在 |

---

### 6. 修改用户信息

**接口**: `POST /client/user/profile`

**描述**: 修改当前用户的个人信息 (用户名、头像、手机号)。

**请求头**:
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**请求参数**:
```json
{
  "username": "李四",
  "avatarUrl": "https://example.com/new-avatar.jpg",
  "phone": "13900139000"
}
```

**字段说明**:
| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| username | string | 否 | 用户名 |
| avatarUrl | string | 否 | 头像URL |
| phone | string | 否 | 手机号 |

**成功响应** (200):
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "username": "李四",
    "email": "user@example.com",
    "phone": "13900139000",
    "avatarUrl": "https://example.com/new-avatar.jpg",
    "status": 1,
    "createdAt": "2025-01-15T10:30:00"
  }
}
```

**错误响应**:
| HTTP状态码 | 错误码 | 说明 |
|-----------|--------|------|
| 401 | UNAUTHORIZED | Token 无效或已过期 |
| 404 | USER_NOT_FOUND | 用户不存在 |

**业务规则**:
- 不支持修改邮箱 (邮箱作为唯一标识)
- 不支持修改密码 (需使用重置密码接口)

---

### 7. 用户登出

**接口**: `POST /client/user/logout`

**描述**: 用户登出,将当前 Token 加入黑名单。

**请求头**:
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**成功响应** (200):
```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

**错误响应**:
| HTTP状态码 | 错误码 | 说明 |
|-----------|--------|------|
| 401 | UNAUTHORIZED | Token 无效或已过期 |

**业务规则**:
- Token 加入 Redis 黑名单,有效期为 Token 剩余有效期
- 登出后该 Token 无法再次使用

---

## 错误码汇总

| 错误码 | HTTP状态码 | 说明 |
|--------|-----------|------|
| RATE_LIMITED | 429 | 操作过于频繁,请稍后再试 |
| TOO_MANY_LOGIN_ATTEMPTS | 429 | 登录失败次数过多,账号已被锁定15分钟 |
| EMAIL_SEND_FAILED | 500 | 邮件发送失败 |
| INVALID_VERIFICATION_CODE | 400 | 验证码无效或已过期 |
| EMAIL_ALREADY_REGISTERED | 400 | 该邮箱已被注册 |
| INVALID_CREDENTIALS | 401 | 用户名或密码错误 |
| USER_DISABLED | 403 | 用户已被禁用 |
| WEAK_PASSWORD | 400 | 密码强度不足,至少需要8位 |
| PASSWORD_MISMATCH | 400 | 两次输入的密码不一致 |
| USER_NOT_FOUND | 404 | 用户不存在 |
| UNAUTHORIZED | 401 | Token 无效或已过期 |

---

## 安全说明

### 1. 密码安全
- 密码使用 BCrypt 加密存储,不可逆
- 最小长度要求: 8位
- 建议包含大小写字母、数字和特殊字符

### 2. Token 安全
- Token 使用 HMAC-SHA256 签名
- 有效期: 7天 (604800秒)
- 登出后 Token 加入黑名单,无法再次使用
- Token 格式: `Bearer {jwt_token}`

### 3. 限流保护
- 邮件发送: 1次/分钟
- 登录失败: 5次/15分钟

### 4. IP 记录
- 系统记录每次登录的 IP 地址
- 支持代理场景 (X-Forwarded-For, X-Real-IP)

---

## 调用示例

### cURL 示例

**登录**:
```bash
curl -X POST http://localhost:8080/client/user/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "SecurePass123"
  }'
```

**获取用户信息**:
```bash
curl -X GET http://localhost:8080/client/user/info \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

### JavaScript 示例

```javascript
// 登录
const login = async (email, password) => {
  const response = await fetch('http://localhost:8080/client/user/login', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ email, password }),
  });
  const data = await response.json();
  if (data.code === 200) {
    localStorage.setItem('token', data.data.token);
    return data.data.user;
  }
  throw new Error(data.message);
};

// 获取用户信息
const getUserInfo = async () => {
  const token = localStorage.getItem('token');
  const response = await fetch('http://localhost:8080/client/user/info', {
    method: 'GET',
    headers: {
      'Authorization': `Bearer ${token}`,
    },
  });
  const data = await response.json();
  if (data.code === 200) {
    return data.data;
  }
  throw new Error(data.message);
};
```

---

## 更新日志

### v1.1.0 (2025-02-10)
- 新增登录失败限流 (5次/15分钟)
- 优化 Token 验证性能 (先查黑名单再验签)
- 完善接口文档

### v1.0.0 (2025-01-15)
- 初始版本
- 支持邮箱注册、登录、登出
- 支持密码重置
- JWT Token 认证
