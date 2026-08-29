package com.rex.config;

import java.util.concurrent.Executor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Executor for telemetry writes.
 *
 * <p>Bounded deliberately. An unbounded queue would let a burst of evaluations accumulate work
 * faster than it drains and eventually exhaust memory, so the caller runs the task itself once the
 * queue is full. That slows the burst rather than losing it.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

  @Bean("telemetryExecutor")
  public Executor telemetryExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(2);
    executor.setMaxPoolSize(4);
    executor.setQueueCapacity(500);
    executor.setThreadNamePrefix("telemetry-");
    executor.setRejectedExecutionHandler(
        new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
    executor.initialize();
    return executor;
  }
}
