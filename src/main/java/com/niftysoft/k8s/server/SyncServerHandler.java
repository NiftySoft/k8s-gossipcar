package com.niftysoft.k8s.server;

import com.niftysoft.k8s.data.stringstore.VolatileStringStore;
import io.netty.channel.*;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/** @author K. Alex Mills */
@ChannelHandler.Sharable
public class SyncServerHandler extends SimpleChannelInboundHandler<VolatileStringStore> {
  private static final InternalLogger log =
      InternalLoggerFactory.getInstance(SyncServerHandler.class);

  private final VolatileStringStore myStore;

  public SyncServerHandler(VolatileStringStore store) {
    this.myStore = store;
  }

  @Override
  public void channelActive(ChannelHandlerContext ctx) {
    log.debug("Remote sync initiated by peer. Receiving store.");
  }

  @Override
  public void channelRead0(ChannelHandlerContext ctx, final VolatileStringStore otherStore) {
    log.debug("Store received.");

    log.debug("Writing store to remote peer.");
    ctx.executor().execute(() -> myStore.mergeAllFresher(otherStore));

    ChannelFuture future = ctx.write(myStore);
    future.addListener(ChannelFutureListener.CLOSE);
  }

  public void channelReadComplete(ChannelHandlerContext ctx) {
    ctx.flush();
  }

  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    cause.printStackTrace();
    ctx.close();
  }
}
