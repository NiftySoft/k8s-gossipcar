package com.niftysoft.k8s.data;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Static value-class for storing the stats collected by this node during its lifetime.
 */
public abstract class LifetimeStats {
    public static final LocalDateTime START_TIME = LocalDateTime.now();

    public static final AtomicLong SUCCESSFUL_OUTGOING_SYNCS = new AtomicLong(0L);
    public static final AtomicLong SUCCESSFUL_INCOMING_SYNCS = new AtomicLong(0L);

    public static final AtomicLong SUCCESSFUL_HTTP_REQUESTS = new AtomicLong(0L);
    public static final AtomicLong FAILED_HTTP_REQUESTS = new AtomicLong(0L);
}
