package com.niftysoft.k8s.server;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.nio.charset.Charset;

public class HeartbeatHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        // This five-byte string has the advantage of being the same in both big-endian and little-endian encodings.
        // It is also really cute.
        ctx.write(ctx.alloc().buffer(5).writeBytes("bb bb".getBytes(Charset.forName("UTF-8"))));
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }

}
