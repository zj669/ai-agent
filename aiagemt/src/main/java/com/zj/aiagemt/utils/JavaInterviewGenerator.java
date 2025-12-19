package com.zj.aiagemt.utils;

import javax.swing.*;
import java.util.*;
import java.util.stream.Collectors;


public class JavaInterviewGenerator {
    
    /**
     * 技术栈配置 - 使用Map存储分类及其对应的技术点
     */
    private static final Map<String, List<String>> TECH_STACKS = new HashMap<String, List<String>>() {{
        put("核心语言与平台", Arrays.asList("Java SE (8/11/17)", "Jakarta EE (Java EE)", "JVM"));
        put("构建工具", Arrays.asList("Maven", "Gradle", "Ant"));
        put("Web框架", Arrays.asList("Spring Boot", "Spring MVC", "Spring WebFlux", "Jakarta EE", "Micronaut", "Quarkus", "Play Framework", "Struts (Legacy)"));
        put("数据库与ORM", Arrays.asList("Hibernate", "MyBatis", "JPA", "Spring Data JDBC", "HikariCP", "C3P0", "Flyway", "Liquibase"));
        put("测试框架", Arrays.asList("JUnit 5", "TestNG", "Mockito", "PowerMock", "AssertJ", "Selenium", "Cucumber"));
        put("微服务与云原生", Arrays.asList("Spring Cloud", "Netflix OSS (Eureka, Zuul)", "Consul", "gRPC", "Apache Thrift", "Kubernetes Client", "OpenFeign", "Resilience4j"));
        put("安全框架", Arrays.asList("Spring Security", "Apache Shiro", "JWT", "OAuth2", "Keycloak", "Bouncy Castle"));
        put("消息队列", Arrays.asList("Kafka", "RabbitMQ", "ActiveMQ", "JMS", "Apache Pulsar", "Redis Pub/Sub"));
        put("缓存技术", Arrays.asList("Redis", "Ehcache", "Caffeine", "Hazelcast", "Memcached", "Spring Cache"));
        put("日志框架", Arrays.asList("Log4j2", "Logback", "SLF4J", "Tinylog"));
        put("监控与运维", Arrays.asList("Prometheus", "Grafana", "Micrometer", "ELK Stack", "New Relic", "Jaeger", "Zipkin"));
        put("模板引擎", Arrays.asList("Thymeleaf", "FreeMarker", "Velocity", "JSP/JSTL"));
        put("REST与API工具", Arrays.asList("Swagger/OpenAPI", "Spring HATEOAS", "Jersey", "RESTEasy", "Retrofit"));
        put("序列化", Arrays.asList("Jackson", "Gson", "Protobuf", "Avro"));
        put("CI/CD工具", Arrays.asList("Jenkins", "GitLab CI", "GitHub Actions", "Docker", "Kubernetes"));
        put("大数据处理", Arrays.asList("Hadoop", "Spark", "Flink", "Cassandra", "Elasticsearch"));
        put("版本控制", Arrays.asList("Git", "SVN"));
        put("工具库", Arrays.asList("Apache Commons", "Guava", "Lombok", "MapStruct", "JSch", "POI"));
        put("AI", Arrays.asList("Spring AI", "Google A2A", "MCP（模型上下文协议）", "RAG（检索增强生成）", "Agent（智能代理）", "聊天会话内存", "工具执行框架", "提示填充", "向量化", "语义检索", "向量数据库（Milvus/Chroma/Redis）", "Embedding模型（OpenAI/Ollama）", "客户端-服务器架构", "工具调用标准化", "扩展能力", "Agentic RAG", "文档加载", "企业文档问答", "复杂工作流", "智能客服系统", "AI幻觉（Hallucination）", "自然语言语义搜索"));
        put("其他", Arrays.asList("JUnit Pioneer", "Dubbo", "R2DBC", "WebSocket"));
    }};
    
    /**
     * 业务场景配置
     */
    private static final List<String> BUSINESS_SCENARIOS = Arrays.asList(
            // 传统互联网
            "音视频流媒体", "短视频平台", "直播平台", "内容社区", "知识付费平台", "社交媒体", "即时通讯",

            // 游戏娱乐
            "手机游戏", "PC游戏", "H5游戏", "云游戏", "游戏社区", "电竞平台", "游戏直播",

            // 电商零售
            "B2C电商平台", "B2B电商平台", "跨境电商", "社交电商", "直播电商", "二手交易平台", "奢侈品电商",

            // 本地生活服务
            "外卖配送", "出行打车", "民宿酒店", "旅游出行", "家政服务", "美业服务", "维修服务", "宠物服务",

            // 共享经济
            "共享单车", "共享汽车", "共享充电宝", "共享办公", "共享存储", "技能共享", "时间共享",

            // 金融科技
            "数字银行", "移动支付", "数字钱包", "保险科技", "消费金融", "供应链金融", "量化交易", "风控系统", "征信系统",

            // 医疗健康
            "互联网医院", "在线问诊", "医药电商", "健康管理", "医疗AI诊断", "基因检测", "智能穿戴设备",

            // 企业服务
            "CRM系统", "ERP系统", "HRM系统", "财务管理", "供应链管理", "客服系统", "营销自动化", "数据分析平台",

            // 大数据与AI
            "推荐算法平台", "搜索引擎", "数据分析平台", "机器学习平台", "计算机视觉", "自然语言处理", "语音识别",

            // 在线教育
            "K12在线教育", "职业技能培训", "语言学习", "MOOC平台", "企业培训", "编程教育", "艺术教育",

            // 物流供应链
            "快递物流", "同城配送", "跨境物流", "冷链物流", "智能仓储", "供应链管理", "货运平台",

            // 新兴技术
            "Web3.0区块链", "NFT交易平台", "去中心化金融DeFi", "元宇宙平台", "数字藏品", "加密货币交易",

            // AI生成内容
            "AIGC内容生成", "AI绘画平台", "AI写作助手", "AI代码生成", "AI视频制作", "AI音乐创作",

            // 安全与风控
            "网络安全监控", "身份认证系统", "反欺诈系统", "数据安全平台", "隐私保护", "威胁情报",

            // 广告营销
            "程序化广告", "精准营销", "社交媒体营销", "内容营销平台", "affiliate营销", "网红经济平台",

            // 智慧城市
            "智慧交通", "智慧政务", "智慧园区", "智慧社区", "智慧环保", "智慧能源",

            // 工业互联网
            "工业4.0", "智能制造", "设备监控", "预测性维护", "数字化工厂", "产业链协同",

            // 农业科技
            "智慧农业", "农业大数据", "农产品溯源", "农业金融", "农机共享", "智能温室",

            // 房产科技
            "房产交易平台", "租房平台", "房产金融", "智慧物业", "房产大数据分析", "VR看房"
    );
    
    /**
     * 随机数生成器
     */
    private static final Random RANDOM = new Random();
    
    /**
     * 面试配置类 - 封装选中的技术栈和业务场景
     */
    public static class InterviewConfig {
        private final List<String> selectedTechCategories;
        private final String selectedBusinessScenario;
        private final Map<String, List<String>> techDetails;
        
        public InterviewConfig(List<String> techCategories, String businessScenario) {
            this.selectedTechCategories = new ArrayList<>(techCategories);
            this.selectedBusinessScenario = businessScenario;
            this.techDetails = new HashMap<>();
            
            // 获取选中技术栈的详细信息
            techCategories.forEach(category -> {
                if (TECH_STACKS.containsKey(category)) {
                    this.techDetails.put(category, TECH_STACKS.get(category));
                }
            });
        }
        
        // Getters
        public List<String> getSelectedTechCategories() {
            return new ArrayList<>(selectedTechCategories);
        }
        
        public String getSelectedBusinessScenario() {
            return selectedBusinessScenario;
        }
        
        public Map<String, List<String>> getTechDetails() {
            return new HashMap<>(techDetails);
        }
        
        @Override
        public String toString() {
            return String.format("技术栈: %s, 业务场景: %s", 
                String.join(" & ", selectedTechCategories), 
                selectedBusinessScenario);
        }
    }
    
    /**
     * 随机选择技术栈和业务场景
     * 
     * @return InterviewConfig 包含选中的技术栈和业务场景的配置对象
     */
    public static InterviewConfig randomSelect() {
        // 随机选择2个技术栈分类
        List<String> techCategories = new ArrayList<>(TECH_STACKS.keySet());
        Collections.shuffle(techCategories, RANDOM);
        List<String> selectedTechCategories = techCategories.subList(0, 2);

        // 加权：30%概率选AI
        if (RANDOM.nextDouble() < 0.3) {
            selectedTechCategories.remove(selectedTechCategories.size()-1);
            selectedTechCategories.add("AI");
        }
        
        // 随机选择1个业务场景
        String selectedBusinessScenario = BUSINESS_SCENARIOS.get(RANDOM.nextInt(BUSINESS_SCENARIOS.size()));
        
        return new InterviewConfig(selectedTechCategories, selectedBusinessScenario);
    }
    
    /**
     * 指定技术栈分类选择业务场景
     * 
     * @param techCategories 指定的技术栈分类列表
     * @return InterviewConfig 配置对象
     * @throws IllegalArgumentException 如果技术栈分类无效或数量不为2
     */
    public static InterviewConfig selectWithTechCategories(List<String> techCategories) {
        if (techCategories == null || techCategories.size() != 2) {
            throw new IllegalArgumentException("必须指定2个技术栈分类");
        }
        
        // 验证技术栈分类是否有效
        for (String category : techCategories) {
            if (!TECH_STACKS.containsKey(category)) {
                throw new IllegalArgumentException("无效的技术栈分类: " + category);
            }
        }
        
        // 随机选择业务场景
        String selectedBusinessScenario = BUSINESS_SCENARIOS.get(RANDOM.nextInt(BUSINESS_SCENARIOS.size()));
        
        return new InterviewConfig(techCategories, selectedBusinessScenario);
    }
    
    /**
     * 指定业务场景选择技术栈
     * 
     * @param businessScenario 指定的业务场景
     * @return InterviewConfig 配置对象
     * @throws IllegalArgumentException 如果业务场景无效
     */
    public static InterviewConfig selectWithBusinessScenario(String businessScenario) {
        if (!BUSINESS_SCENARIOS.contains(businessScenario)) {
            throw new IllegalArgumentException("无效的业务场景: " + businessScenario);
        }
        
        // 随机选择2个技术栈分类
        List<String> techCategories = new ArrayList<>(TECH_STACKS.keySet());
        Collections.shuffle(techCategories, RANDOM);
        List<String> selectedTechCategories = techCategories.subList(0, 2);
        
        return new InterviewConfig(selectedTechCategories, businessScenario);
    }
    
    /**
     * 完全指定配置
     * 
     * @param techCategories 指定的技术栈分类列表
     * @param businessScenario 指定的业务场景
     * @return InterviewConfig 配置对象
     * @throws IllegalArgumentException 如果参数无效
     */
    public static InterviewConfig selectWithFullConfig(List<String> techCategories, String businessScenario) {
        if (techCategories == null || techCategories.size() != 2) {
            throw new IllegalArgumentException("必须指定2个技术栈分类");
        }
        
        if (!BUSINESS_SCENARIOS.contains(businessScenario)) {
            throw new IllegalArgumentException("无效的业务场景: " + businessScenario);
        }
        
        // 验证技术栈分类是否有效
        for (String category : techCategories) {
            if (!TECH_STACKS.containsKey(category)) {
                throw new IllegalArgumentException("无效的技术栈分类: " + category);
            }
        }
        
        return new InterviewConfig(techCategories, businessScenario);
    }
    
    /**
     * 生成面试文章提示词
     * 
     * @param config 面试配置对象
     * @return String 完整的提示词文本
     */
    public static String generatePrompt(InterviewConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("配置对象不能为null");
        }
        
        StringBuilder prompt = new StringBuilder();
        
        // 构建技术栈详情
        List<String> techDetails = config.getTechDetails().entrySet().stream()
            .map(entry -> entry.getKey() + ": " + String.join(", ", entry.getValue()))
            .collect(Collectors.toList());
        
        prompt.append("# Java面试场景文章生成\n\n");
        prompt.append("## 角色设定\n");
        prompt.append("你是一个专业的技术内容生成助手，擅长创作互联网大厂Java面试场景的技术文章。\n\n");
        
        prompt.append("## 核心任务\n");
        prompt.append("根据以下指定的技术栈和业务场景，生成一篇以面试对话形式展现的Java技术文章，包含面试过程和详细的技术解析。\n\n");
        
        prompt.append("## 本次指定内容\n\n");
        prompt.append("### 🛠️ 技术栈重点 (深度挖掘以下2个领域,专注于其中3-5个技术栈即可)\n");
        techDetails.forEach(detail -> prompt.append(detail).append("\n"));
        prompt.append("\n");
        
        prompt.append("### 🏢 业务场景背景\n");
        prompt.append(config.getSelectedBusinessScenario()).append("\n\n");
        
        prompt.append("## 文章结构要求\n\n");
        prompt.append("### 1. 基础框架\n");
        prompt.append("- **场景背景**: 互联网大厂Java开发工程师面试\n");
        prompt.append("- **主要角色**: \n");
        prompt.append("  - 严肃专业的面试官（技术专家）\n");
        prompt.append("  - 搞笑但技术基础不扎实的程序员\"小润龙\"\n");
        prompt.append("- **面试流程**: 3轮提问，每轮3-5个问题，循序渐进\n\n");
        
        prompt.append("### 2. 对话风格要求\n\n");
        prompt.append("#### 面试官特征：\n");
        prompt.append("- 严肃专业，逻辑清晰\n");
        prompt.append("- 问题具有层次性和递进性\n");
        prompt.append("- 对正确答案给予肯定和引导\n");
        prompt.append("- 对模糊回答追根问底\n");
        prompt.append("- 最终给出面试结果反馈\n\n");
        
        prompt.append("#### 小润龙特征：\n");
        prompt.append("- 对基础问题能够回答\n");
        prompt.append("- 遇到复杂问题开始含糊其辞\n");
        prompt.append("- 偶尔有搞笑的回答或比喻\n");
        prompt.append("- 技术理解有偏差但有一定基础\n");
        prompt.append("- 表现出紧张但努力的状态\n\n");
        
        prompt.append("### 3. 问题设计原则\n");
        prompt.append("- **循序渐进**: 从基础概念到实际应用再到性能优化\n");
        prompt.append("- **业务结合**: 每个技术问题都要结合").append(config.getSelectedBusinessScenario()).append("业务场景\n");
        prompt.append("- **深度挖掘**: 专注于").append(String.join("和", config.getSelectedTechCategories())).append("的深入探讨\n");
        prompt.append("- **实战导向**: 问题要贴近实际开发中的真实挑战\n\n");
        
        prompt.append("### 4. 文章输出格式\n\n");
        prompt.append("#### 标准Markdown结构：\n");
        prompt.append("```markdown\n");
        prompt.append("# 【文章标题】");
        
        prompt.append("## 📋 面试背景\n");
        prompt.append("[简要介绍面试场景、公司背景、岗位要求]\n\n");
        
        prompt.append("## 🎭 面试实录\n\n");
        prompt.append("### 第一轮：基础概念考查\n");
        prompt.append("[3-5个基础问题的对话]\n\n");
        
        prompt.append("### 第二轮：实际应用场景\n");
        prompt.append("[3-5个应用层面问题的对话]\n\n");
        
        prompt.append("### 第三轮：性能优化与架构设计\n");
        prompt.append("[3-5个高级问题的对话]\n\n");
        
        prompt.append("### 面试结果\n");
        prompt.append("[面试官的最终反馈]\n\n");
        
        prompt.append("## 📚 技术知识点详解\n\n");
        prompt.append("### [知识点1]\n");
        prompt.append("[详细的技术解析，包含代码示例、架构图等]\n\n");
        
        prompt.append("### [知识点2]\n");
        prompt.append("[详细的技术解析，包含最佳实践、注意事项等]\n\n");
        
        prompt.append("### [知识点N]\n");
        prompt.append("[详细的技术解析，适合小白学习]\n\n");
        
        prompt.append("## 💡 总结与建议\n");
        prompt.append("[针对面试中出现的问题，给出学习建议和技术成长路径]\n");
        prompt.append("```\n\n");
        
        prompt.append("### 5. 内容质量要求\n");
        prompt.append("- **技术准确性**: 确保所有技术内容准确无误\n");
        prompt.append("- **实用性**: 提供可落地的技术方案和代码示例\n");
        prompt.append("- **教育性**: 详解部分要让技术小白也能理解\n");
        prompt.append("- **趣味性**: 通过对话形式增加阅读趣味性\n");
        prompt.append("- **完整性**: 从面试到知识点解析的完整闭环\n\n");
        
        prompt.append("### 6. 特殊要求\n");
        prompt.append("- 在开始写文章之前， 可以使用sequential-thinking梳理文章书写脉络和相关知识点\n");
        prompt.append("- 每个技术问题都要有对应的详细解析\n");
        prompt.append("- 代码示例要完整且可执行, 必要时使用context7工具去搜索相关技术文档\n");
//        prompt.append("- 业务场景要真实可信， 可以使用zhipuWebSearch搜索网上相关场景，并以此为参考\n");
        prompt.append("- 避免过于宽泛，专注深度挖掘\n");
        prompt.append("- 文章内容使用markdown格式书写\n\n");
        
        prompt.append("## 输出指令\n");
        prompt.append("请严格按照上述要求，基于指定的技术栈\"").append(String.join("和", config.getSelectedTechCategories()));
        prompt.append("\"以及业务场景\"").append(config.getSelectedBusinessScenario());
        prompt.append("\"，生成一篇高质量的Java面试技术文章，确保内容的专业性、实用性和可读性。\n");
        prompt.append("，不要阐述其他信息，请直接提供；文章标题（需要含带技术点）、文章内容、文章标签（多个用英文逗号隔开）、文章简述（100字），并发布到csdn");

        return prompt.toString();
    }
    
    /**
     * 获取所有可用的技术栈分类
     * 
     * @return Set<String> 技术栈分类集合
     */
    public static Set<String> getAllTechCategories() {
        return new HashSet<>(TECH_STACKS.keySet());
    }
    
    /**
     * 获取所有可用的业务场景
     * 
     * @return List<String> 业务场景列表
     */
    public static List<String> getAllBusinessScenarios() {
        return new ArrayList<>(BUSINESS_SCENARIOS);
    }
    
    /**
     * 获取指定技术栈分类的详细技术点
     * 
     * @param category 技术栈分类名称
     * @return List<String> 技术点列表，如果分类不存在返回空列表
     */
    public static List<String> getTechDetailsByCategory(String category) {
        return TECH_STACKS.getOrDefault(category, new ArrayList<>());
    }
    
    /**
     * 一键生成 - 随机选择配置并生成提示词
     * 
     * @return String 完整的提示词文本
     */
    public static String oneClickGenerate() {
        InterviewConfig config = randomSelect();
        return generatePrompt(config);
    }
    
    /**
     * 打印配置信息的辅助方法
     * 
     * @param config 面试配置对象
     */
    public static void printConfigInfo(InterviewConfig config) {
        System.out.println("=== 面试配置信息 ===");
        System.out.println("业务场景: " + config.getSelectedBusinessScenario());
        System.out.println("技术栈分类: " + String.join(" & ", config.getSelectedTechCategories()));
        System.out.println("\n技术栈详情:");
        config.getTechDetails().forEach((category, techs) -> {
            System.out.println("  " + category + ": " + String.join(", ", techs));
        });
        System.out.println("==================\n");
    }
    
    /**
     * 主方法 - 演示工具类的使用
     */
    public static void main(String[] args) {
        System.out.println("🚀 Java面试文章生成器工具类演示\n");
        
        // 演示1: 随机生成
        System.out.println("【演示1】随机生成配置:");
        InterviewConfig randomConfig = randomSelect();
        printConfigInfo(randomConfig);
        
        // 演示2: 指定技术栈分类
        System.out.println("【演示2】指定技术栈分类:");
        try {
            InterviewConfig techConfig = selectWithTechCategories(Arrays.asList("Spring Boot", "Redis"));
            printConfigInfo(techConfig);
        } catch (IllegalArgumentException e) {
            System.out.println("错误: " + e.getMessage());
            // 使用有效的技术栈分类
            InterviewConfig validTechConfig = selectWithTechCategories(Arrays.asList("Web框架", "缓存技术"));
            printConfigInfo(validTechConfig);
        }
        
        // 演示3: 指定业务场景
        System.out.println("【演示3】指定业务场景:");
        InterviewConfig businessConfig = selectWithBusinessScenario("电商");
        printConfigInfo(businessConfig);
        
        // 演示4: 完全指定配置
        System.out.println("【演示4】完全指定配置:");
        InterviewConfig fullConfig = selectWithFullConfig(Arrays.asList("微服务与云原生", "消息队列"), "支付金融");
        printConfigInfo(fullConfig);
        
        // 演示5: 生成提示词
        System.out.println("【演示5】生成提示词（部分展示）:");
        String prompt = generatePrompt(fullConfig);
        String[] lines = prompt.split("\n");
        for (int i = 0; i < Math.min(10, lines.length); i++) {
            System.out.println(lines[i]);
        }
        System.out.println("...(省略剩余内容)\n");
        
        // 演示6: 一键生成
        System.out.println("【演示6】一键生成:");
        String oneClickPrompt = oneClickGenerate();
        String[] oneClickLines = oneClickPrompt.split("\n");
        System.out.println("生成的提示词长度: " + oneClickPrompt.length() + " 字符");
        System.out.println("前5行内容:");
        for (int i = 0; i < Math.min(5, oneClickLines.length); i++) {
            System.out.println(oneClickLines[i]);
        }
        
        // 演示7: 查看所有可用选项
        System.out.println("\n【可用选项统计】");
        System.out.println("技术栈分类总数: " + getAllTechCategories().size());
        System.out.println("业务场景总数: " + getAllBusinessScenarios().size());
        System.out.println("理论组合总数: " + (getAllTechCategories().size() * (getAllTechCategories().size() - 1) / 2 * getAllBusinessScenarios().size()));
    }
}