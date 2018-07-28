package com.niftysoft.k8s.server;

import com.niftysoft.k8s.data.stringstore.VolatileStringStore;
import com.niftysoft.k8s.data.stringstore.VolatileStringStoreDecoder;
import com.niftysoft.k8s.data.stringstore.VolatileStringStoreEncoder;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class GossipServerHandlerTest {

    VolatileStringStore vss = mock(VolatileStringStore.class);

    @Test
    public void testGossipServerHandlerSwitchesToSyncHandlersAndSetsEncoderAndDecoder() {
        EmbeddedChannel chan = new EmbeddedChannel(new GossipServerHandler(vss));

        chan.writeInbound(ByteBufUtil.writeAscii(ByteBufAllocator.DEFAULT, "SY"));
        assertThat(chan.pipeline().get("handler")).hasSameClassAs(new SyncServerHandler(null));
        assertThat(chan.pipeline().get("encoder")).hasSameClassAs(new VolatileStringStoreEncoder());
        assertThat(chan.pipeline().get("decoder")).hasSameClassAs(new VolatileStringStoreDecoder());
    }

    @Test
    public void testGossipServerHandlerRespondsToHeartBeat() {
        EmbeddedChannel chan = new EmbeddedChannel(new GossipServerHandler(vss));

        chan.writeInbound(ByteBufUtil.writeAscii(ByteBufAllocator.DEFAULT, "HB"));

        assertThat(chan.outboundMessages().size()).isGreaterThanOrEqualTo(1);
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