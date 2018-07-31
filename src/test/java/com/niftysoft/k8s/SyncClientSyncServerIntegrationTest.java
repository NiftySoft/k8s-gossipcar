package com.niftysoft.k8s;

import com.niftysoft.k8s.client.SyncTask;
import com.niftysoft.k8s.data.stringstore.VolatileStringStore;
import com.niftysoft.k8s.server.GossipServer;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SyncClientSyncServerIntegrationTest {

    private VolatileStringStore clientStore;
    private VolatileStringStore serverStore;
    private EmbeddedChannel clientChan;
    private EmbeddedChannel serverChan;

    @Before
    public void before() {
        clientStore = new VolatileStringStore();
        serverStore = new VolatileStringStore();
    }

    private void connect() {
        clientChan = new EmbeddedChannel(SyncTask.buildPipeline(clientStore).toArray(new ChannelHandler[0]));
        serverChan = new EmbeddedChannel(GossipServer.buildSyncPipeline(serverStore).toArray(new ChannelHandler[0]));
    }

    @Test
    public void testSyncOfEmptyStoreIsSuccessful() {
        connect();

        pumpMessagesRoundTrip();

        assertChannelsEmpty();
    }

    @Test
    public void testClientSyncSendsToServer() {
        clientStore.put("key","value");

        connect();

        pumpMessagesRoundTrip();

        assertChannelsEmpty();

        assertThat(serverStore.get("key")).isEqualTo("value");
    }

    @Test
    public void testServerSyncSendsToClient() {
        serverStore.put("key", "value");
        connect();
        pumpMessagesRoundTrip();
        assertChannelsEmpty();

        assertThat(clientStore.get("key")).isEqualTo("value");
    }

    @Test
    public void testClientValuesOverwritesServerValue() {
        clientStore.put("key", "value0");
        clientStore.put("key", "value1");
        clientStore.put("key", "value2");

        serverStore.put("key", "server-value");

        connect();
        pumpMessagesRoundTrip();
        assertChannelsEmpty();

        assertThat(serverStore.get("key")).isEqualTo("value2");
    }

    @Test
    public void testServerValuesOverwritesClientValue() {
        serverStore.put("key", "value0");
        serverStore.put("key", "value1");
        serverStore.put("key", "value2");

        clientStore.put("key", "server-value");

        connect();
        pumpMessagesRoundTrip();
        assertChannelsEmpty();

        assertThat(serverStore.get("key")).isEqualTo("value2");
    }

    private void pumpMessagesRoundTrip() {

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
    }

    private void assertChannelsEmpty() {
        assertThat(serverChan.outboundMessages()).isEmpty();
        assertThat(serverChan.inboundMessages()).isEmpty();
        assertThat(clientChan.outboundMessages()).isEmpty();
        assertThat(clientChan.inboundMessages()).isEmpty();
    }
}
