# ai-agent-shared 模块蓝图

## 模块职责
公共基础层，提供跨模块复用的设计模式、工具类、常量和统一响应体。不依赖任何框架（无 Spring、无 MyBatis），是整个项目的最底层依赖。

## 关键文件

| 文件/包 | 职责 |
|---------|------|
| `shared/design/dag/` | DAG 有向无环图执行框架（DagNode、DagContext、ConditionalDagNode） |
| `shared/design/workflow/` | 工作流状态机（WorkflowState、WorkflowNode、StateReducer、ControlSignal） |
| `shared/design/ruletree/` | 规则树/策略路由模式（AbstractStrategyRouter、StrategyHandler） |
| `shared/design/strategymode/` | 策略模式工厂（DefaultStrategyFactory） |
| `shared/design/bizlogic/` | 业务逻辑委托模式（AbstractBizLogicDelegate、BizLogicProcessor） |
| `shared/constants/RedisKeyConstants.java` | Redis Key 常量统一管理 |
| `shared/context/UserContext.java` | 线程级用户上下文（ThreadLocal） |
| `shared/response/Response.java` | 统一 API 响应体 |
| `shared/response/PageResult.java` | 分页响应体 |
| `shared/util/XssFilterUtil.java` | XSS 过滤工具 |
| `shared/model/enums/IBaseEnum.java` | 枚举基础接口 |

## 上下游依赖
- 上游：无（最底层）
- 下游：被 domain、application、infrastructure、interfaces 全部依赖
