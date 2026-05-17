# ai-agent-shared 模块蓝图

## 模块职责

公共基础层，提供跨模块复用的响应体、用户上下文、Redis Key 常量、基础枚举接口和安全工具。当前源码没有早期文档中提到的 `shared/design/*` 设计模式目录；后续不要继续把这些目录写成当前事实。

## 当前关键文件

| 文件/包 | 职责 |
|---------|------|
| `shared/constants/RedisKeyConstants.java` | Redis Key 常量统一管理 |
| `shared/context/UserContext.java` | 线程级用户上下文（ThreadLocal） |
| `shared/response/Response.java` | 统一 API 响应体 |
| `shared/response/PageResult.java` | 分页响应体 |
| `shared/util/XssFilterUtil.java` | XSS 过滤工具 |
| `shared/model/enums/IBaseEnum.java` | 枚举基础接口 |

## 上下游依赖

- 上游：无（底层共享模块）
- 下游：被 domain、application、infrastructure、interfaces 依赖
