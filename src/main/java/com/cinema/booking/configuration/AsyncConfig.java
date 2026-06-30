package com.cinema.booking.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);        // Số thread mặc định luôn chạy
        executor.setMaxPoolSize(20);       // Số thread tối đa khi hàng đợi đầy
        executor.setQueueCapacity(100);    // Hàng đợi chứa các task chờ thực thi
        executor.setThreadNamePrefix("AsyncEmail-");
        executor.initialize();
        return executor;
    }
}
