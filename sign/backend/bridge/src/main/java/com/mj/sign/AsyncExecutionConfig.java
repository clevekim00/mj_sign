package com.mj.sign;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Configuration
public class AsyncExecutionConfig {

    @Bean(name = "inferenceExecutor")
    public Executor inferenceExecutor(
            @Value("${sign.async.core-pool-size:2}") int corePoolSize,
            @Value("${sign.async.max-pool-size:4}") int maxPoolSize,
            @Value("${sign.async.queue-capacity:16}") int queueCapacity
    ) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("sign-inference-");
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.initialize();
        return executor;
    }

    @Bean(name = "idleFlushExecutor", destroyMethod = "shutdown")
    public ScheduledExecutorService idleFlushExecutor(
            @Value("${sign.window.flush-threads:1}") int flushThreads
    ) {
        return Executors.newScheduledThreadPool(flushThreads);
    }
}
