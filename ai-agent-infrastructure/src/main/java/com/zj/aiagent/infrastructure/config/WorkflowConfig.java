package com.zj.aiagent.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 工作流引擎基础设施配置
 * 配置线程池和分布式锁
 */
@Configuration
public class WorkflowConfig {

    @Value("${workflow.executor.core-pool-size:20}")
    private int corePoolSize;

    @Value("${workflow.executor.max-pool-size:100}")
    private int maxPoolSize;

    @Value("${workflow.executor.queue-capacity:200}")
    private int queueCapacity;

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Value("${spring.data.redis.password:}")
    private String redisPassword;

    /**
     * 节点执行专用线程池（IO 密集型）
     * 避免阻塞 ForkJoinPool.commonPool
     */
    @Bean("nodeExecutorThreadPool")
    public Executor nodeExecutorThreadPool() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("wf-exec-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }

}
