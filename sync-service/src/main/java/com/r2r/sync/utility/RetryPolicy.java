package com.r2r.sync.utility;

import java.util.Random;

/**
 * RetryPolicy calculates exponential backoff with jitter using provided initial delay.
 */
public class RetryPolicy {
    private final Random random = new Random();

    /**
     * Calculates the next retry timestamp.
     */
    public long nextRetryTime(int retryCount, long initialBackoffMs) {
        long backoff = (long) (initialBackoffMs * Math.pow(2, retryCount));
        long jitter = random.nextInt(100);
        return System.currentTimeMillis() + backoff + jitter;
    }
}
