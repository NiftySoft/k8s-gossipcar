package com.niftysoft.k8s;

import com.niftysoft.k8s.client.SyncInitiateTask;
import com.niftysoft.k8s.client.SyncInitiateTaskInitializer;
import com.niftysoft.k8s.data.stringstore.VolatileStringStore;
import com.niftysoft.k8s.server.GossipServer;
import com.niftysoft.k8s.server.GossipServerInitializer;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.DefaultChannelPromise;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.EventExecutorGroup;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SyncClientSyncServerIntegrationTest {

    private VolatileStringStore clientStore;
    private VolatileStringStore serverStore;
    private EmbeddedChannel clientChan;
    private EmbeddedChannel serverChan;

    private static EventExecutorGroup serverGroup = new DefaultEventExecutorGroup(1);
    private static EventExecutorGroup clientGroup = new DefaultEventExecutorGroup(1);

    @Before
    public void before() {
        clientStore = new VolatileStringStore();
        serverStore = new VolatileStringStore();
    }

    private void connect() {
        clientChan =
                new EmbeddedChannel(new SyncInitiateTaskInitializer(clientStore, serverGroup));
        serverChan =
                new EmbeddedChannel(new GossipServerInitializer(serverStore, clientGroup));
    }

    @Test
    public void testSyncOfEmptyStoreIsSuccessful() throws Exception {
        connect();

        pumpMessagesRoundTrip();

        assertChannelsEmpty();
    }

    @Test
    public void testClientSyncSendsToServer() throws Exception {
        clientStore.put("key", "value");

        connect();

        pumpMessagesRoundTrip();

        assertChannelsEmpty();

        assertThat(serverStore.get("key")).isEqualTo("value");
    }

    @Test
    public void testServerSyncSendsToClient() throws Exception {
        serverStore.put("key", "value");
        connect();
        pumpMessagesRoundTrip();
        assertChannelsEmpty();

        assertThat(clientStore.get("key")).isEqualTo("value");
    }

    @Test
    public void testClientValuesOverwritesServerValue() throws Exception {
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
    public void testServerValuesOverwritesClientValue() throws Exception {
        serverStore.put("key", "value0");
        serverStore.put("key", "value1");
        serverStore.put("key", "value2");

        clientStore.put("key", "server-value");

        connect();
        pumpMessagesRoundTrip();
        assertChannelsEmpty();

        assertThat(serverStore.get("key")).isEqualTo("value2");
    }

    private void pumpMessagesRoundTrip() throws Exception {
        clientChan.flush();
        Thread.sleep(200);

        assertThat(clientChan.outboundMessages()).isNotEmpty();

        while (!clientChan.outboundMessages().isEmpty())
            serverChan.writeOneInbound(clientChan.readOutbound());

        serverChan.flush();
        Thread.sleep(200);

        assertThat(serverChan.outboundMessages()).isNotEmpty();

        while (!serverChan.outboundMessages().isEmpty())
            clientChan.writeOneInbound(serverChan.readOutbound()).awaitUninterruptibly();

        clientChan.flush();
        serverChan.flush();
        Thread.sleep(200);
    }

    private void assertChannelsEmpty() {
        assertThat(serverChan.outboundMessages()).isEmpty();
        assertThat(serverChan.inboundMessages()).isEmpty();
        assertThat(clientChan.outboundMessages()).isEmpty();
        assertThat(clientChan.inboundMessages()).isEmpty();
    }
}
