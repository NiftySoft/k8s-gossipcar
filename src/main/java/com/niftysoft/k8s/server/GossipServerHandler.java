package com.niftysoft.k8s.server;

import com.niftysoft.k8s.data.stringstore.VolatileStringStore;
import com.niftysoft.k8s.data.stringstore.VolatileStringStoreDecoder;
import com.niftysoft.k8s.data.stringstore.VolatileStringStoreEncoder;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

public class GossipServerHandler extends ByteToMessageDecoder {

    private final VolatileStringStore myStore;

    public GossipServerHandler(VolatileStringStore store) {
        this.myStore = store;
    }

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
        p.addLast("decoder", new VolatileStringStoreDecoder());
        p.addLast("encoder", new VolatileStringStoreEncoder());
        p.addLast("handler", new SyncServerHandler(myStore));
        p.remove(this);
    }

    public boolean isSync(int magic1, int magic2) {
        return magic1 == 'S' && magic2 == 'Y';
    }

    public boolean isHeartbeat(int magic1, int magic2) {
        return magic1 == 'H' && magic2 == 'B';
    }
}
