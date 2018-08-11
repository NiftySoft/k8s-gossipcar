package com.niftysoft.k8s.server;

import com.niftysoft.k8s.data.stringstore.VolatileStringStore;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;

public class GossipServerInitializer extends ChannelInitializer<Channel> {

    private VolatileStringStore vss;
    public GossipServerInitializer(VolatileStringStore vss) {
        this.vss = vss;
    }

    @Override
    public void initChannel(Channel ch) throws Exception {
        ch.pipeline().addLast("decoder", new VolatileStringStore.VolatileStringStoreDecoder())
                     .addLast("encoder", new VolatileStringStore.VolatileStringStoreEncoder())
                     .addLast("handler", new SyncServerHandler(vss));
    }

}
