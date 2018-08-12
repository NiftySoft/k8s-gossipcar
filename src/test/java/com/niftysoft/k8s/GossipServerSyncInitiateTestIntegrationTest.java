package com.niftysoft.k8s;

import com.niftysoft.k8s.client.SyncInitiateTask;
import com.niftysoft.k8s.data.Config;
import com.niftysoft.k8s.data.VolatileByteStore;
import com.niftysoft.k8s.server.GossipServer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class GossipServerSyncInitiateTestIntegrationTest {

    @Test
    public void testGossipServerRespondsToRemoteSyncInitiate() throws Exception {
        Config testConfig = constructTestConfig();
        testConfig.serviceDnsName = "example.com"; // Ensure the server doesn't end up talking to itself.
        GossipServer server = new GossipServer(testConfig);
        Runnable run = constructGossipRunnable(server);

        Thread t = new Thread(run);
        try {
            t.start();

            do {
                Thread.sleep(10);
            } while (!server.isStarted());

            // Send "key" -> "value" to server.
            VolatileByteStore vss = new VolatileByteStore();
            vss.put("key", "value".getBytes());

            EventLoopGroup group = new NioEventLoopGroup(1);
            testConfig.serviceDnsName = "localhost";
            SyncInitiateTask initiateSync = new SyncInitiateTask(testConfig, vss, group);

            Thread t2 = new Thread(initiateSync);
            t2.start();
            t2.join();
            // Create a new sync task with an empty store.
            initiateSync = new SyncInitiateTask(testConfig, new VolatileByteStore(), group);

            t2 = new Thread(initiateSync);
            t2.start();
            t2.join();

            group.shutdownGracefully();

            // If the value was persisted to the server, it should be loaded again after a second sync.
            assertThat(vss.get("key")).isEqualTo("value".getBytes());
        } finally {
            t.interrupt();
            t.join();
        }
    }

    private Config constructTestConfig() {
        Config config = new Config();
        config.clientPort = 8080;
        config.serviceDnsName = "localhost";
        return config;
    }

    private Runnable constructGossipRunnable(GossipServer server) {
        return () -> {
            try {
                server.run();
            } catch (Exception e) {
                if (e.getClass().equals(InterruptedException.class)) return;

                e.printStackTrace();
                fail("Run encountered an exception");
            }
        };
    }
}
