package com.niftysoft.k8s.client;

import com.niftysoft.k8s.data.stringstore.VolatileStringStore;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class SyncClientWriteHandler extends ChannelInboundHandlerAdapter {

    private final VolatileStringStore myStore;

    public SyncClientWriteHandler(VolatileStringStore store) {
        this.myStore = store;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        ctx.write('S');
        ctx.write('Y');

        synchronized(myStore) { // Obtain write lock.
            ctx.writeAndFlush(myStore);
        }
    }
}
