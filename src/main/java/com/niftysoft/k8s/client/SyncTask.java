package com.niftysoft.k8s.client;

import com.niftysoft.k8s.data.Config;
import com.niftysoft.k8s.data.stringstore.VolatileStringStore;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.BadClientSilencer;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;

/** @author K. Alex Mills */
public class SyncTask implements Runnable {
  private static final InternalLogger log =
      InternalLoggerFactory.getInstance(BadClientSilencer.class);

  private final int port;
  private final String hostname;
  private final VolatileStringStore myStore;
  private final InetAddress podIp;

  public SyncTask(Config config, VolatileStringStore store) {

    this.myStore = store;
    this.hostname = config.serviceDnsName;
    this.port = config.peerPort;
    InetAddress assignMeToPodIp = null;
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

  public static List<ChannelHandler> buildPipeline(VolatileStringStore store) {
    return Arrays.asList(
        new VolatileStringStore.VolatileStringStoreDecoder(),
        new VolatileStringStore.VolatileStringStoreEncoder(),
        new SyncClientHandler(store));
  }

  @Override
  public void run() {
    try {
      InetAddress host = lookupRandomPeer(hostname);
      log.debug("Syncing with " + host);

      if (host == null) return; // No friends. :-(

      Bootstrap b = new Bootstrap();
      b.channel(NioSocketChannel.class);
      b.option(ChannelOption.SO_KEEPALIVE, true);
      b.handler(
          new ChannelInitializer<SocketChannel>() {
            protected void initChannel(SocketChannel ch) throws Exception {
              ch.pipeline().addLast(buildPipeline(myStore).toArray(new ChannelHandler[0]));
            }
          });
      ChannelFuture f = b.connect(host, port).sync();

      f.channel().closeFuture().sync();
    } catch (UnknownHostException e) {
      e.printStackTrace();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  private InetAddress lookupRandomPeer(String host) throws UnknownHostException {
    InetAddress[] peers;
    try {
      peers = InetAddress.getAllByName(host);
      if (peers.length == 0) {
        return null;
      }
      peers = findAndRemoveOwnAddress(peers);
      return peers[(int) (Math.random() * peers.length)];
    } catch (UnknownHostException e) {
      throw new UnknownHostException("Error, unknown host: " + host);
    }
  }

  private InetAddress[] findAndRemoveOwnAddress(InetAddress[] peers) {
    if (podIp == null) return peers;

    boolean found = false;
    for (int i = 0; i < peers.length; ++i) {
      if (peers[i].equals(podIp)) {
        found = true;
      }
      if (found && i < peers.length - 1) peers[i] = peers[i + 1];
    }
    if (!found) return peers;

    return Arrays.copyOf(peers, peers.length - 1);
  }
}
