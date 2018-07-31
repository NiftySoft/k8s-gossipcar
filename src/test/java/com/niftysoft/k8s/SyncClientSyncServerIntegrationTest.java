package com.niftysoft.k8s;

import com.niftysoft.k8s.client.SyncTask;
import com.niftysoft.k8s.data.stringstore.VolatileStringStore;
import com.niftysoft.k8s.server.GossipServer;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SyncClientSyncServerIntegrationTest {

    @Test
    public void testSyncOfEmptyStoreIsSuccessful() {
        VolatileStringStore clientStore = new VolatileStringStore();
        VolatileStringStore serverStore = new VolatileStringStore();

        EmbeddedChannel clientChan =
                new EmbeddedChannel(SyncTask.buildPipeline(clientStore).toArray(new ChannelHandler[0]));
        EmbeddedChannel serverChan =
                new EmbeddedChannel(GossipServer.buildSyncPipeline(serverStore).toArray(new ChannelHandler[0]));

        clientChan.flush();
        assertThat(clientChan.outboundMessages()).isNotEmpty();

        while(!clientChan.outboundMessages().isEmpty())
            serverChan.writeInbound((ByteBuf)clientChan.readOutbound());

        serverChan.flush();
        assertThat(serverChan.outboundMessages()).isNotEmpty();

        while(!serverChan.outboundMessages().isEmpty())
            clientChan.writeInbound((ByteBuf)serverChan.readOutbound());

        clientChan.flush();
        serverChan.flush();

        assertThat(serverChan.outboundMessages()).isEmpty();
        assertThat(serverChan.inboundMessages()).isEmpty();
        assertThat(clientChan.outboundMessages()).isEmpty();
        assertThat(clientChan.inboundMessages()).isEmpty();
    }
}
