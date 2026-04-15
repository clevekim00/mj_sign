package com.mj.sign;

import java.time.Duration;

public interface IdleFlushScheduler {
    void schedule(String sessionId, long scheduleToken, Duration delay, Runnable task);
}
