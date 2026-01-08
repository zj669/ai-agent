graph TD
    %% 用户交互层
    subgraph User_Layer [用户交互层 User Interaction]
        User[开发者/业务人员] -->|提交 YAML 配置| API_Gateway[API 网关]
        User -->|触发任务请求| API_Gateway
        Output_View[结果可视化/日志]
    end

    %% 定义层
    subgraph Definition_Layer [定义层 Definition Layer]
        YAML_File(agent-topology.yaml)
        Schema_Validator[JSON Schema 校验器]
        YAML_Parser[YAML 解析器 (Jackson/SnakeYAML)]
    end

    %% 核心引擎层 (Spring Boot)
    subgraph Core_Engine [核心引擎层 Core Engine Layer]
        direction TB
        
        %% 启动时/配置加载组件
        subgraph Config_Processor [配置处理]
            Bean_Registrar[动态 Bean 注册机<br/>(BeanDefinitionRegistryPostProcessor)]
            Factory[Agent 运行时工厂<br/>(Runtime Factory)]
        end
        
        %% 运行时调度组件
        subgraph Runtime_Scheduler [DAG 调度与执行]
            DAG_Engine[DAG 调度器] -->|拓扑排序 & 循环检测| Task_Queue[就绪任务队列]
            Task_Queue -->|分发| Virtual_Executor[虚拟线程执行器<br/>(Java 21 Virtual Threads)]
            
            Workflow_Context[工作流上下文<br/>(WorkflowContext / ConcurrentHashMap)]
            State_Monitor[状态监控 & 汇聚]
        end
    end

    %% 基础设施层 (Spring AI)
    subgraph Infra_Layer [基础设施层 Infrastructure Layer]
        Chat_Gateway[模型网关 (ChatClient)]
        MCP_Bus[工具总线 (MCP Client)]
        Memory_Store[记忆存储 (Redis/VectorDB)]
    end

    %% 进化预留层
    subgraph Evolution_Layer [进化预留层 Evolutionary Layer]
        Yaml_Writer[YAML 生成服务]
        Sandbox[Agent Gym 沙箱<br/>(Testcontainers/Docker)]
    end

    %% 外部依赖
    subgraph External [外部环境]
        LLM_Provider[大模型 API<br/>(OpenAI/Claude)]
        External_Tools[外部工具 API<br/>(Search/Database)]
    end

    %% 关系连线
    API_Gateway --> YAML_Parser
    YAML_Parser -->|解析对象| Bean_Registrar
    YAML_Parser -->|拓扑结构| DAG_Engine
    
    Bean_Registrar -->|注册通用能力| Chat_Gateway
    Bean_Registrar -->|注册工具 Bean| MCP_Bus
    
    DAG_Engine -->|获取状态| Workflow_Context
    Virtual_Executor -->|创建实例| Factory
    Factory -->|引用 Bean| Chat_Gateway
    Factory -->|引用 Bean| MCP_Bus
    
    Virtual_Executor -->|读写| Workflow_Context
    Workflow_Context -->|持久化| Memory_Store
    
    Chat_Gateway --> LLM_Provider
    MCP_Bus --> External_Tools
    
    %% 进化回路
    State_Monitor -.->|触发进化| Yaml_Writer
    Yaml_Writer -->|新代码测试| Sandbox
    Sandbox -.->|验证通过| YAML_File