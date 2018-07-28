package com.niftysoft.k8s.server;

import com.niftysoft.k8s.data.stringstore.VolatileStringStore;
import com.niftysoft.k8s.data.stringstore.VolatileStringStoreDecoder;
import com.niftysoft.k8s.data.stringstore.VolatileStringStoreEncoder;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class GossipServerHandlerTest {

    @Test
    public void testGossipServerHandlerSwitchesToHeartbeatHandler() {
        VolatileStringStore localStore = new VolatileStringStore();
        EmbeddedChannel chan = new EmbeddedChannel(new GossipServerHandler(localStore));

        assertThat(chan.writeInbound(ByteBufUtil.writeAscii(ByteBufAllocator.DEFAULT, "HB"))).isTrue();
        assertThat(chan.pipeline().get("handler")).hasSameClassAs(new HeartbeatHandler());
    }


    @Test
    public void testGossipServerHandlerSwitchesToSyncHandlersAndSetsEncoderAndDecoder() {
        VolatileStringStore localStore = new VolatileStringStore();
        EmbeddedChannel chan = new EmbeddedChannel(new GossipServerHandler(localStore));

        chan.writeInbound(ByteBufUtil.writeAscii(ByteBufAllocator.DEFAULT, "SY"));
        assertThat(chan.pipeline().get("handler")).hasSameClassAs(new SyncServerHandler(null));
        assertThat(chan.pipeline().get("encoder")).hasSameClassAs(new VolatileStringStoreEncoder());
        assertThat(chan.pipeline().get("decoder")).hasSameClassAs(new VolatileStringStoreDecoder());
    }

    @Test
    public void testGossipServerHandlerRespondsToHeartBeat() {
        VolatileStringStore localStore = new VolatileStringStore();
        EmbeddedChannel chan = new EmbeddedChannel(new GossipServerHandler(localStore));

        assertThat(chan.writeInbound(ByteBufUtil.writeAscii(ByteBufAllocator.DEFAULT, "HB"))).isTrue();

        assertThat(chan.inboundMessages().size()).isGreaterThanOrEqualTo(1);
    }

    @Test
    public void testGossipServerHandlerRespondsToSyncRequest() {
        VolatileStringStore localStore = new VolatileStringStore();

        EmbeddedChannel chan = new EmbeddedChannel(new GossipServerHandler(localStore));

        chan.writeInbound(ByteBufUtil.writeAscii(ByteBufAllocator.DEFAULT, "SY"));
        chan.writeInbound(localStore);

        assertThat(chan.outboundMessages().size()).isGreaterThanOrEqualTo(1);
    }
}