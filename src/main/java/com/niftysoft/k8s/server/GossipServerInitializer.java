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
        // TODO: Enable sending object larger than 1 MB
        ch.pipeline().addLast(new GossipServerHandler(vss));
    }

}
