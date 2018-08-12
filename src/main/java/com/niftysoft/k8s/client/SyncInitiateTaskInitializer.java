package com.niftysoft.k8s.client;

import com.niftysoft.k8s.data.VolatileByteStore;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;

public class SyncInitiateTaskInitializer extends ChannelInitializer<Channel> {

    private VolatileByteStore vss;

    public SyncInitiateTaskInitializer(VolatileByteStore vss) {
        this.vss = vss;
    }

    @Override
    protected void initChannel(Channel ch) {
        ch.pipeline()
            .addLast(new VolatileByteStore.VolatileByteStoreDecoder())
            .addLast(new VolatileByteStore.VolatileByteStoreEncoder())
            .addLast(new SyncInitiateHandler(vss));
    }
}
