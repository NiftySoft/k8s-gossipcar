package com.niftysoft.k8s;

import com.niftysoft.k8s.client.SyncInitiateTaskInitializer;
import com.niftysoft.k8s.data.VolatileByteStore;
import com.niftysoft.k8s.server.SyncServerInitializer;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SyncClientSyncServerIntegrationTest {

    private VolatileByteStore clientStore;
    private VolatileByteStore serverStore;
    private EmbeddedChannel clientChan;
    private EmbeddedChannel serverChan;

    @Before
    public void before() {
        clientStore = new VolatileByteStore();
        serverStore = new VolatileByteStore();
    }

    private void connect() {
        clientChan =
                new EmbeddedChannel(new SyncInitiateTaskInitializer(clientStore));
        serverChan =
                new EmbeddedChannel(new SyncServerInitializer(serverStore));
    }

    @Test
    public void testSyncOfEmptyStoreIsSuccessful() throws Exception {
        connect();

        pumpMessagesRoundTrip();

        assertChannelsEmpty();
    }

    @Test
    public void testClientSyncSendsToServer() throws Exception {
        clientStore.put("key", "value".getBytes());

        connect();

        pumpMessagesRoundTrip();

        assertChannelsEmpty();

        assertThat(serverStore.get("key")).isEqualTo("value".getBytes());
    }

    @Test
    public void testServerSyncSendsToClient() throws Exception {
        serverStore.put("key", "value".getBytes());
        connect();
        pumpMessagesRoundTrip();
        assertChannelsEmpty();

        assertThat(clientStore.get("key")).isEqualTo("value".getBytes());
    }

    @Test
    public void testClientValuesOverwritesServerValue() throws Exception {
        clientStore.put("key", "value0".getBytes());
        clientStore.put("key", "value1".getBytes());
        clientStore.put("key", "value2".getBytes());

        serverStore.put("key", "server-value".getBytes());

        connect();
        pumpMessagesRoundTrip();
        assertChannelsEmpty();

        assertThat(serverStore.get("key")).isEqualTo("value2".getBytes());
    }

    @Test
    public void testServerValuesOverwritesClientValue() throws Exception {
        serverStore.put("key", "value0".getBytes());
        serverStore.put("key", "value1".getBytes());
        serverStore.put("key", "value2".getBytes());

        clientStore.put("key", "server-value".getBytes());

        connect();
        pumpMessagesRoundTrip();
        assertChannelsEmpty();

        assertThat(clientStore.get("key")).isEqualTo("value2".getBytes());
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
