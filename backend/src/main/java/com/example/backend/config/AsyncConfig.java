package com.example.backend.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync  
@Slf4j
public class AsyncConfig {

    @Bean(name = "documentProcessingExecutor")
    public Executor documentProcessingExecutor() {
        log.info("🔵 Creating Document Processing Thread Pool");
        
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("DocProcessor-");
        executor.initialize();
        
        log.info("✅ Thread Pool ready - CoreSize: 5, MaxSize: 10");
        return executor;
    }
}