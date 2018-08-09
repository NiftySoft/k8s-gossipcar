package com.niftysoft.k8s.server;

import com.niftysoft.k8s.data.stringstore.VolatileStringStore;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class GossipServerInitializerTest {

    @Test
    public void testGossipServerInitializerAddsGossipServerHandlerToPipeline() {
        VolatileStringStore vss = mock(VolatileStringStore.class);

        EmbeddedChannel chan = new EmbeddedChannel(new GossipServerInitializer(vss, null));

        assertThat(chan.pipeline().removeLast()).hasSameClassAs(new GossipServerHandler(vss, null));
    }
}