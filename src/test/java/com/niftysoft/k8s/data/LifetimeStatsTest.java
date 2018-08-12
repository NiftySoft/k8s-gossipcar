package com.niftysoft.k8s.data;

import org.junit.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

public class LifetimeStatsTest {

    @Test
    public void testLifetimeStatsStartTimeIsNotNull() {
        assertThat(LifetimeStats.START_TIME).isNotNull();
    }

    @Test
    public void testLifetimeStatsStartTimeIsInPast() {
        assertThat(LifetimeStats.START_TIME).isBeforeOrEqualTo(LocalDateTime.now());
    }

    @Test
    public void testLifetimeStatsAddersAreNotNull() {
        assertThat(LifetimeStats.SUCCESSFUL_INCOMING_SYNCS).isNotNull();
        assertThat(LifetimeStats.SUCCESSFUL_OUTGOING_SYNCS).isNotNull();
        assertThat(LifetimeStats.SUCCESSFUL_HTTP_REQUESTS).isNotNull();
        assertThat(LifetimeStats.FAILED_HTTP_REQUESTS).isNotNull();
    }
}
