package com.niftysoft.k8s.client;

import com.niftysoft.k8s.data.Config;
import com.niftysoft.k8s.data.stringstore.VolatileStringStore;
import com.niftysoft.k8s.util.PeerUtil;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.BadClientSilencer;
import io.netty.util.concurrent.EventExecutorGroup;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;

/** @author K. Alex Mills */
public class SyncInitiateTask implements Runnable {
  private static final InternalLogger log =
      InternalLoggerFactory.getInstance(BadClientSilencer.class);

  private final int port;
  private final String hostname;
  private final VolatileStringStore myStore;
  private final EventExecutorGroup syncGroup;
  private final InetAddress podIp;

  public SyncInitiateTask(Config config, VolatileStringStore store, EventExecutorGroup syncGroup) {
    this.syncGroup = syncGroup;
    this.myStore = store;
    this.hostname = config.serviceDnsName;
    this.port = config.peerPort;
    InetAddress assignMeToPodIp;
    try {
      assignMeToPodIp = InetAddress.getByName(config.podIp);
    } catch (UnknownHostException e) {
      System.err.println(
          "Caught unknown host exception for configured podIp.\n"
              + "While not fatal, it means this node might try and sync with itself, which is inefficient.");
      assignMeToPodIp = null;
    }
    podIp = assignMeToPodIp;
  }

  @Override
  public void run() {
    try {
      InetAddress host = PeerUtil.lookupRandomPeer(hostname, podIp);
      log.debug("Syncing with " + host);

      if (host == null) return; // No friends. :-(

      // TODO: MUST set group here!
      Bootstrap b = new Bootstrap();
      EventLoopGroup workerGroup = new NioEventLoopGroup();
      b.channel(NioSocketChannel.class);
      b.group(workerGroup);
      b.option(ChannelOption.SO_KEEPALIVE, true);
      b.handler(new SyncInitiateTaskInitializer(myStore, syncGroup));

      ChannelFuture f = b.connect(host, port).sync();

      f.channel().closeFuture().sync();
    } catch (UnknownHostException e) {
      e.printStackTrace();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }
}
