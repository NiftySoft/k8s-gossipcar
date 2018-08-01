package com.niftysoft.k8s.server;

import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

/** @author K. Alex Mills */
public class HeartbeatHandler extends ChannelInboundHandlerAdapter {
  @Override
  public void channelActive(ChannelHandlerContext ctx) {
    // This five-byte string has the advantage of being the same in both big-endian and
    // little-endian encodings.
    // It is also really cute.
    ChannelFuture f = ctx.writeAndFlush(ByteBufUtil.writeAscii(ByteBufAllocator.DEFAULT, "bb bb"));
    f.addListener(ChannelFutureListener.CLOSE);
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    cause.printStackTrace();
    ctx.close();
  }
}
