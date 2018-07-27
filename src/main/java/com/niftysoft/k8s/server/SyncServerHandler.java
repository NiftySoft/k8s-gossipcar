package com.niftysoft.k8s.server;

import com.niftysoft.k8s.data.VolatileStringStore;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class SyncServerHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (!msg.getClass().equals(VolatileStringStore.class)) {
            ctx.write("NO");
            ctx.close();
        }

        VolatileStringStore otherStore = (VolatileStringStore)msg;
        VolatileStringStore myStore = VolatileStringStore.getInstance();

        myStore.loadIfFresher(otherStore);

        ctx.write(myStore);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}
