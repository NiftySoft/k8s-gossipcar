package com.niftysoft.k8s.server;

import com.niftysoft.k8s.data.VolatileByteStore;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;

public class SyncServerInitializer extends ChannelInitializer<Channel> {

    private VolatileByteStore vss;
    public SyncServerInitializer(VolatileByteStore vss) {
        this.vss = vss;
    }

    @Override
    public void initChannel(Channel ch) throws Exception {
        ch.pipeline().addLast("decoder", new VolatileByteStore.VolatileByteStoreDecoder())
                     .addLast("encoder", new VolatileByteStore.VolatileByteStoreEncoder())
                     .addLast("handler", new SyncServerHandler(vss));
    }

}
