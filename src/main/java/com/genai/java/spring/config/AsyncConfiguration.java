package com.genai.java.spring.config;

import io.micrometer.context.ContextSnapshotFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;

@Configuration
public class AsyncConfiguration {

    @Bean
    public TaskDecorator tracingTaskDecorator() {
        return (runnable) -> ContextSnapshotFactory.builder().build().captureAll().wrap(runnable);
    }

    @Bean("traceableAsyncExecutor")
    public Executor traceableAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        int availableProcessors = Runtime.getRuntime().availableProcessors();
        executor.setCorePoolSize(availableProcessors);
        executor.setMaxPoolSize(availableProcessors);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("traceableThreadPoolExecutor-");
        executor.setTaskDecorator(tracingTaskDecorator());
        executor.initialize();
        return executor;
    }

    @Bean("traceableWatchLoopExecutor")
    public Executor traceableWatchLoopExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(1);
        executor.setThreadNamePrefix("traceableWatchLoopExecutor-");
        executor.setTaskDecorator(tracingTaskDecorator());
        executor.initialize();
        return executor;
    }

    @Bean("traceableScheduledExecutorService")
    public ScheduledExecutorService traceableScheduledExecutorService() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("traceableDebounceExecutor-");
        scheduler.setTaskDecorator(tracingTaskDecorator());
        scheduler.initialize();
        return scheduler.getScheduledExecutor();
    }


}
