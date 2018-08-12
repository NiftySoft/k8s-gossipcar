package com.niftysoft.k8s.client;

import com.niftysoft.k8s.data.Config;
import com.niftysoft.k8s.data.stringstore.VolatileByteStore;
import com.niftysoft.k8s.util.PeerUtil;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;

/** @author kalexmills */
public class SyncInitiateTask implements Runnable {
  private static final InternalLogger log =
      InternalLoggerFactory.getInstance(SyncInitiateTask.class);

  private final int port;
  private final String hostname;
  private final VolatileByteStore myStore;
  private final InetAddress podIp;

  private final EventLoopGroup group;

  public SyncInitiateTask(Config config, VolatileByteStore store, EventLoopGroup group) {
    this.group = group;
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

      Bootstrap b = new Bootstrap();
      b.channel(NioSocketChannel.class)
       .group(group)
       .handler(new SyncInitiateTaskInitializer(myStore));

      b.connect(host, port);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
