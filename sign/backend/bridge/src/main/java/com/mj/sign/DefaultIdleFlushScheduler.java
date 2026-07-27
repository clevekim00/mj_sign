package com.mj.sign;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
public class DefaultIdleFlushScheduler implements IdleFlushScheduler {

    private final ScheduledExecutorService idleFlushExecutor;

    public DefaultIdleFlushScheduler(@Qualifier("idleFlushExecutor") ScheduledExecutorService idleFlushExecutor) {
        this.idleFlushExecutor = idleFlushExecutor;
    }

    @Override
    public void schedule(String sessionId, long scheduleToken, Duration delay, Runnable task) {
        idleFlushExecutor.schedule(task, delay.toMillis(), TimeUnit.MILLISECONDS);
    }
}
