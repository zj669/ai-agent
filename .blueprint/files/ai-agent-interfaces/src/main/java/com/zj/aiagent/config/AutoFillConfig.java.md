## Metadata
- file: `.blueprint/files/ai-agent-interfaces/src/main/java/com/zj/aiagent/config/AutoFillConfig.java.md`
- version: `1.0`
- status: 正常
- updated_at: 2026-02-14
- owner: blueprint-team

## 状态机
- 状态集合: `正常` / `待修改` / `修改中` / `修改完成`
- 允许流转: `正常 -> 待修改 -> 修改中 -> 修改完成 -> 正常`
- 允许回退: `修改中 -> 待修改`、`修改完成 -> 修改中`

## 1) 整体文件职责
- 主题: AutoFillConfig.java
- 实现 MyBatis-Plus `MetaObjectHandler`，统一填充 `createBy/updateBy/createTime/updateTime` 审计字段，避免各 Mapper 手工写入。

## 2) 核心方法
- `insertFill(MetaObject metaObject)`
- `updateFill(MetaObject metaObject)`

## 3) 具体方法
### 3.1 insertFill(MetaObject metaObject)
- 函数签名: `insertFill(MetaObject metaObject) -> void`
- 入参: `metaObject` MyBatis 元对象
- 出参: 无
- 功能含义: 新增时从 `UserContext` 获取用户并填充创建/更新时间与创建/更新人。
- 链路作用: 持久化新增 -> MyBatis 自动填充 -> 基础审计字段一致化。

## 4) 变更记录
- 2026-02-15: 基于源码回填自动填充职责与 insert/update 双路径契约。

## 5) Temp缓存区
当前状态为 `正常`，本区留空。
