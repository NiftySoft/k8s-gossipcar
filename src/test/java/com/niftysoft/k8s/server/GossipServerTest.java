package com.niftysoft.k8s.server;

import com.niftysoft.k8s.data.Config;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;

import static org.junit.Assert.fail;

public class GossipServerTest {

    @Rule
    public final EnvironmentVariables environmentVariables
            = new EnvironmentVariables();

    @Before
    public void before() {
        environmentVariables.clear("PEER_PORT");
        environmentVariables.clear("CLIENT_PORT");
        environmentVariables.clear("SERVICE_DNS_NAME");
    }

    @Test(timeout = 30000)
    public void testGossipServerStartsWithoutException() throws Exception {
        Config config = new Config();
        config.serviceDnsName = "localhost";
        final GossipServer server = new GossipServer(config);

        Runnable run = () -> {
            try {
                server.run();
            } catch (Exception e) {
                if (e.getClass().equals(InterruptedException.class)) return;

                e.printStackTrace();
                fail("Run encountered an exception");
            }
        };

        Thread t = new Thread(run);
        t.start();

        Thread.sleep(15000);

        t.interrupt();

        Thread.sleep(2000);
    }

    @Test
    public void testGossipServerStartsFromMainWithoutException() throws Exception {
        environmentVariables.set("PEER_PORT", "1336");
        environmentVariables.set("CLIENT_PORT", "8080");
        environmentVariables.set("SERVICE_DNS_NAME", "localhost");

        Runnable run = () -> {
            try {
                GossipServer.main(new String[0]);
            } catch (Exception e) {
                if (e.getClass().equals(InterruptedException.class)) return;

                e.printStackTrace();
                fail("Run encountered an exception");
            }
        };

        Thread t = new Thread(run);
        t.start();

        Thread.sleep(15000);

        t.interrupt();

        Thread.sleep(2000);
    }
}
