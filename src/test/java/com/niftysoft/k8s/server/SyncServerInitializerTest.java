package com.niftysoft.k8s.server;

import com.niftysoft.k8s.data.VolatileByteStore;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class SyncServerInitializerTest {

    @Test
    public void testGossipServerInitializerAddsSyncServerHandlerToPipeline() {
        VolatileByteStore vss = mock(VolatileByteStore.class);

        EmbeddedChannel chan = new EmbeddedChannel(new SyncServerInitializer(vss));

        assertThat(chan.pipeline().removeLast()).hasSameClassAs(new SyncServerHandler(vss));
    }
}
