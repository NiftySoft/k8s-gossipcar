package com.niftysoft.k8s.client;

import com.niftysoft.k8s.data.stringstore.VolatileStringStore;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class SyncClientReadHandler extends ChannelInboundHandlerAdapter {

    private final VolatileStringStore myStore;

    public SyncClientReadHandler(VolatileStringStore store) {
        this.myStore = store;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        assert(msg.getClass().equals(VolatileStringStore.class));

        VolatileStringStore otherStore = (VolatileStringStore)msg;

        myStore.mergeAllFresher(otherStore);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}
