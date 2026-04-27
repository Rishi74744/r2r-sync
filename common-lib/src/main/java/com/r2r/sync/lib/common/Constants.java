package com.r2r.sync.lib.common;

/**
 * Constants defines the system-wide constant values used across all modules.
 */
public class Constants {
    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_FAILURE = "FAILURE";
    public static final String STATUS_RETRY = "RETRY";
    public static final String STATUS_RECONCILE = "RECONCILE";
    public static final String STATUS_MANUAL_CHECK = "MANUAL_CHECK";

    public static final String SCENARIO_SUCCESS = "success";
    public static final String SCENARIO_FAILURE = "failure";
    public static final String SCENARIO_TIMEOUT = "timeout";
    public static final String SCENARIO_RATE_LIMIT = "rate_limit";

    public static final String ACTION_SYNC_SUCCESS = "SYNC_SUCCESS";
    public static final String ACTION_SYNC_RETRY = "SYNC_RETRY";
    public static final String ACTION_SYNC_FAILED = "SYNC_FAILED";
    public static final String ACTION_SYNC_EXHAUSTED = "SYNC_EXHAUSTED_RECONCILE";
    public static final String ACTION_MANUAL_REQUIRED = "MANUAL_CHECK_REQUIRED";

    public static final int DEFAULT_MAX_RETRIES = 3;
    public static final int DEFAULT_MAX_RECONCILE_ATTEMPTS = 3;
    public static final long DEFAULT_INITIAL_BACKOFF_MS = 1000;
    public static final int DEFAULT_BACKPRESSURE_LIMIT = 10;
    public static final int DEFAULT_CIRCUIT_THRESHOLD = 3;
}
