package com.niftysoft.k8s.server;

import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;

import java.util.List;

public class GossipServerHandler extends ByteToMessageDecoder {

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        if (in.readableBytes() < 2) {
            return;
        }

        final int magic1 = in.getUnsignedByte(in.readerIndex());
        final int magic2 = in.getUnsignedByte(in.readerIndex() + 1);

        if (isHeartbeat(magic1, magic2)) {
            switchToHeartbeat(ctx);
        } else if (isSync(magic1, magic2)) {
            switchToSync(ctx);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }

    public void switchToHeartbeat(ChannelHandlerContext ctx) {
        ChannelPipeline p = ctx.pipeline();
        p.addLast("handler", new HeartbeatHandler());
        p.remove(this);
    }

    public void switchToSync(ChannelHandlerContext ctx) {
        ChannelPipeline p = ctx.pipeline();
        p.addLast("decoder", new ObjectEncoder());
        p.addLast("encoder", new ObjectDecoder(ClassResolvers.cacheDisabled(null)));
        p.addLast("handler", new SyncServerHandler());
    }

    public boolean isSync(int magic1, int magic2) {
        return magic1 == 'S' && magic2 == 'Y';
    }

    public boolean isHeartbeat(int magic1, int magic2) {
        return magic1 == 'H' && magic2 == 'B';
    }
}
