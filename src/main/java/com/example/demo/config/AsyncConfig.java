package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Pool dedicado ao consumo de eventos, para que a entrega de webhooks nunca bloqueie
 * a thread que atende a requisicao HTTP.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    public static final String WEBHOOK_EXECUTOR = "webhookTaskExecutor";

    @Bean(WEBHOOK_EXECUTOR)
    public ThreadPoolTaskExecutor webhookTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("webhook-");
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(100);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(10);
        executor.initialize();
        return executor;
    }
}
