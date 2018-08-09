package com.niftysoft.k8s.client;

import com.niftysoft.k8s.data.stringstore.VolatileStringStore;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.util.concurrent.EventExecutorGroup;

public class SyncInitiateTaskInitializer extends ChannelInitializer<Channel> {

    private VolatileStringStore vss;
    private EventExecutorGroup syncGroup;

    public SyncInitiateTaskInitializer(VolatileStringStore vss, EventExecutorGroup syncGroup) {
        this.vss = vss;
        this.syncGroup = syncGroup;
    }

    @Override
    protected void initChannel(Channel ch) {
        ch.pipeline()
            .addLast(new VolatileStringStore.VolatileStringStoreDecoder())
            .addLast(new VolatileStringStore.VolatileStringStoreEncoder())
            .addLast(syncGroup, new SyncInitiateHandler(vss));
    }
}
