package com.niftysoft.k8s.client;

import com.niftysoft.k8s.data.LifetimeStats;
import com.niftysoft.k8s.data.VolatileByteStore;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/** @author kalexmills */
@ChannelHandler.Sharable
public class SyncInitiateHandler extends SimpleChannelInboundHandler {
  private static final InternalLogger log =
      InternalLoggerFactory.getInstance(SyncInitiateHandler.class);

  private final VolatileByteStore myStore;

  public SyncInitiateHandler(VolatileByteStore store) {
    this.myStore = store;
  }

  @Override
  public void channelActive(ChannelHandlerContext ctx) {
    log.debug("Initiating sync.");

    log.debug("Writing local store during sync initiation.");
    ctx.writeAndFlush(myStore);
  }

  @Override
  public void channelRead0(ChannelHandlerContext ctx, final Object msg) {
    assert (msg.getClass().equals(VolatileByteStore.class));
    log.debug("Received remote store during client-initiated sync.");

    final VolatileByteStore otherStore = (VolatileByteStore) msg;
    ctx.executor().execute(() -> {
        myStore.mergeAllFresher(otherStore);
        LifetimeStats.SUCCESSFUL_OUTGOING_SYNCS.increment();
    });
  }

  public void channelReadComplete(ChannelHandlerContext ctx) {
    ctx.close();
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    cause.printStackTrace();
    ctx.close();
  }
}
