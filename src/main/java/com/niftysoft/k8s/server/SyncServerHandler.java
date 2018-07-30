package com.niftysoft.k8s.server;

import com.niftysoft.k8s.data.stringstore.VolatileStringStore;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.BadClientSilencer;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * @author K. Alex Mills
 */
public class SyncServerHandler extends ChannelInboundHandlerAdapter {
    private static final InternalLogger log = InternalLoggerFactory.getInstance(BadClientSilencer.class);

    private final VolatileStringStore myStore;

    public SyncServerHandler(VolatileStringStore store) {
        this.myStore = store;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        assert(msg.getClass().equals(VolatileStringStore.class));
        log.debug("Remote sync initiated by peer. Receiving store.");

        VolatileStringStore otherStore = (VolatileStringStore)msg;

        myStore.mergeAllFresher(otherStore);

        synchronized(myStore) {
            log.debug("Writing store to remote.");
            ctx.writeAndFlush(myStore);
        }
        ctx.close();
    }

    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}
