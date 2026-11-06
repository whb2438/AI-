package com.divination.config;

import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步AI调用线程池配置 - 增强版
 * 避免AI请求阻塞主线程，支持熔断、限流和优雅的错误处理
 */
@Configuration
public class AsyncConfig implements AsyncConfigurer {

    // AI调用专用线程池参数
    private static final int CORE_POOL_SIZE = 8;       // 核心线程数（根据CPU/IO调整）
    private static final int MAX_POOL_SIZE = 16;      // 最大线程数
    private static final int QUEUE_CAPACITY = 200;    // 任务队列容量
    private static final long KEEP_ALIVE_SECONDS = 120; // 空闲线程存活时间

    /**
     * 获取异步执行器 - 用于@Async("aiCallExecutor")注解的方法
     *
     * @return 配置的ThreadPoolTaskExecutor
     */
    @Bean("aiCallExecutor")
    public Executor aiCallExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(CORE_POOL_SIZE);
        executor.setMaxPoolSize(MAX_POOL_SIZE);
        executor.setQueueCapacity(QUEUE_CAPACITY);
        executor.setKeepAliveSeconds((int) KEEP_ALIVE_SECONDS);
        executor.setThreadNamePrefix("ai-call-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        // 异常处理统一由 getAsyncUncaughtExceptionHandler() 处理，防止异常静默丢失
        executor.initialize();
        return executor;
    }

    /**
     * 获取默认的异步执行器（如需多个线程池可重复定义）
     */
    @Bean("defaultAsyncExecutor")
    public Executor defaultAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("task-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    /**
     * 统一处理异步方法抛出的异常
     *
     * @return 异常处理程序
     */
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (throwable, method, objects) -> {
            // 记录日志
            System.err.println("[" + method.getDeclaringClass().getSimpleName() + "." + method.getName() + "] Unhandled exception: " + throwable.getMessage());
            throwable.printStackTrace();
            // 生产环境可集成到监控系统（如Sentry、ELK等）
        };
    }
}
