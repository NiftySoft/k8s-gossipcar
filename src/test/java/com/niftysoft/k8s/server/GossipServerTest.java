package com.niftysoft.k8s.server;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.niftysoft.k8s.client.SyncInitiateTaskInitializer;
import com.niftysoft.k8s.data.Config;
import com.niftysoft.k8s.data.stringstore.VolatileStringStore;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.EventExecutorGroup;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class GossipServerTest {

  @Rule public final EnvironmentVariables environmentVariables = new EnvironmentVariables();

  @Before
  public void before() {
    environmentVariables.clear("PEER_PORT");
    environmentVariables.clear("CLIENT_PORT");
    environmentVariables.clear("SERVICE_DNS_NAME");
  }

  @Test(timeout = 30000)
  public void testGossipServerStartsWithoutException() throws Exception {
    Config config = constructTestConfig();
    final GossipServer server = new GossipServer(config);

    Runnable run = constructGossipRunnable(server);

    Thread t = new Thread(run);
    try {
      t.start();

      do {
        Thread.sleep(10);
      } while (!server.isStarted());
    } finally {
      t.interrupt();
      t.join();
    }
  }

  @Test
  public void testGossipServerMainRespondsToRemoteSync() throws Exception {
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

      VolatileStringStore vss = new VolatileStringStore();
      vss.put("key", "value");

      EventExecutorGroup clientSyncGroup = new DefaultEventExecutorGroup(1);
      EventLoopGroup group = new NioEventLoopGroup(1);
      Bootstrap b = new Bootstrap();
      b.group(group);
      b.channel(NioSocketChannel.class);
      b.option(ChannelOption.SO_KEEPALIVE, true);
      b.handler(new SyncInitiateTaskInitializer(vss));

      b.connect("localhost", testConfig.peerPort).sync().channel().closeFuture().sync();
    } finally {
      t.interrupt();
      t.join();
    }
  }

  @Test
  public void testGossipServerMainRespondsToHttpClient() throws Exception {
    Config testConfig = constructTestConfig();
    testConfig.serviceDnsName = "example.com"; // Ensure the server doesn't end up talking to itself.
    testConfig.clientPort = 12345;
    GossipServer server = new GossipServer(testConfig);
    Runnable run = constructGossipRunnable(server);

    Thread t = new Thread(run);
    try {
      t.start();

      do {
        Thread.sleep(10);
      } while (!server.isStarted());


      HttpResponse<String> resp = Unirest.put("http://localhost:12345/map?k=123")
              .body("Hello there.")
              .asString();

      assertThat(resp.getBody()).isEmpty();

      resp = Unirest.get("http://localhost:12345/map?k=123")
              .asString();

      assertThat(resp.getBody()).isEqualTo("123=Hello there.\n");

    } finally {
      t.interrupt();
      t.join();
    }
  }

  private Config constructTestConfig() {
    Config config = new Config();
    config.clientPort = 12345;
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
