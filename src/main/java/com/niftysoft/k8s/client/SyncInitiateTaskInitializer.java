package com.niftysoft.k8s.client;

import com.niftysoft.k8s.data.stringstore.VolatileStringStore;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;

public class SyncInitiateTaskInitializer extends ChannelInitializer<Channel> {

    private VolatileStringStore vss;

    public SyncInitiateTaskInitializer(VolatileStringStore vss) {
        this.vss = vss;
    }

    @Override
    protected void initChannel(Channel ch) {
        ch.pipeline()
            .addLast(new VolatileStringStore.VolatileStringStoreDecoder())
            .addLast(new VolatileStringStore.VolatileStringStoreEncoder())
            .addLast(new SyncInitiateHandler(vss));
    }
}
