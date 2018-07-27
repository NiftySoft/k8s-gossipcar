package com.niftysoft.k8s.client;

import com.niftysoft.k8s.data.VolatileStringStore;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.nio.charset.Charset;

public class SyncClientHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        VolatileStringStore myStore = VolatileStringStore.getInstance();

        ctx.write("SY".getBytes(Charset.forName("UTF-8")));
        synchronized(myStore) { // Obtain write lock.
            ctx.writeAndFlush(myStore);
        }
    }
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (!msg.getClass().equals(VolatileStringStore.class)) {
            ctx.write("NO");
            ctx.close();
        }

        VolatileStringStore otherStore = (VolatileStringStore)msg;
        VolatileStringStore myStore = VolatileStringStore.getInstance();

        myStore.loadIfFresher(otherStore);
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
