package com.ga.bankdesk.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * Defines 2 background thread pools.
 * notificationExecutor: 3 workers, queue of 100, CallerRunsPolicy (the sender runs the job itself when the queue is full, so no email is lost)
 * virusTotalExecutor: 1 worker, queue of 50, AbortPolicy (rejects when full). One worker keeps VirusTotal calls under the free-tier limit.
 */

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "notificationExecutor")
    public ThreadPoolTaskExecutor notificationExecutor() {
        ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
        //3 workers send emails at the same time
        ex.setCorePoolSize(3);
        //fixed size
        ex.setMaxPoolSize(3);
        //up to 100 emails wait their turn
        ex.setQueueCapacity(100);
        ex.setThreadNamePrefix("notification-");
        ex.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        ex.setWaitForTasksToCompleteOnShutdown(true);
        ex.setAwaitTerminationSeconds(20);
        return ex;
    }

    @Bean(name = "virusTotalExecutor")
    public ThreadPoolTaskExecutor virusTotalExecutor() {
        ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
        ex.setCorePoolSize(1);
        ex.setMaxPoolSize(1);
        ex.setQueueCapacity(50);
        ex.setThreadNamePrefix("virustotal-");
        ex.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        return ex;
    }
}
