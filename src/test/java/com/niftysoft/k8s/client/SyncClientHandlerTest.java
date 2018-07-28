package com.niftysoft.k8s.client;

import com.niftysoft.k8s.data.stringstore.VolatileStringStore;
import com.niftysoft.k8s.data.stringstore.VolatileStringStoreDecoder;
import com.niftysoft.k8s.data.stringstore.VolatileStringStoreEncoder;
import io.netty.buffer.ByteBuf;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class SyncClientHandlerTest {

    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;

    private VolatileStringStore localStore = mock(VolatileStringStore.class);

    @Captor
    private ArgumentCaptor<VolatileStringStore> storeCaptor;

    @Before
    public void setUpStreams() {
        System.setOut(new PrintStream(outContent));
        System.setErr(new PrintStream(errContent));
    }

    @After
    public void restoreStreams() {
        System.setOut(originalOut);
        System.setErr(originalErr);
    }


    @Test
    public void testSyncTaskMergesLocalStoreWithRemote() {
        final VolatileStringStore remoteStore = new VolatileStringStore();
        remoteStore.put("key", "value");

        EmbeddedChannel channel = constructTestStack(localStore);

        channel.writeInbound(remoteStore);

        verify(localStore).mergeAllFresher(storeCaptor.capture());

        VolatileStringStore storeMerged = storeCaptor.getValue();

        assertThat(storeMerged.get("key")).isEqualTo("value");
        assertThat(storeMerged.getVersion("key")).isEqualTo(Optional.of(0L));
    }

    @Test
    public void testSyncClientInitiatesSyncProtocol() {
        final VolatileStringStore store = new VolatileStringStore();

        EmbeddedChannel channel = constructTestStack(store);

        assertThat(channel.outboundMessages().size()).isEqualTo(3);

        char magic1 = channel.readOutbound();
        char magic2 = channel.readOutbound();

        assertThat(magic1).isEqualTo('S');
        assertThat(magic2).isEqualTo('Y');
    }

    @Test
    public void testSyncClientWritesLocalStringStore() {
        final VolatileStringStore testStore = new VolatileStringStore();

        testStore.put("test", "string");

        EmbeddedChannel channel = constructTestStack(testStore);

        assertThat(channel.outboundMessages().size()).isEqualTo(3);

        channel.readOutbound(); // Discard the magic bytes for this test.
        channel.readOutbound();

        EmbeddedChannel decoderChan = new EmbeddedChannel(new VolatileStringStoreDecoder());

        decoderChan.writeInbound((ByteBuf)channel.readOutbound());

        Object obj = decoderChan.readInbound();

        assertThat(obj).hasSameClassAs(testStore);

        assertThat(((VolatileStringStore) obj).get("test")).isEqualTo("string");
    }

    @Test
    public void testSyncTaskPrintsExceptionAndClosesStream() {
        final VolatileStringStore testStore = new VolatileStringStore();

        EmbeddedChannel channel = constructTestStack(testStore);

        channel.pipeline().fireChannelRead("bad request");

        assertThat(channel.isOpen()).isFalse();
        assertThat(errContent.toString()).isNotBlank();

    }

    private EmbeddedChannel constructTestStack(VolatileStringStore store) {
        return new EmbeddedChannel(
                new VolatileStringStoreDecoder(),
                new VolatileStringStoreEncoder(),
                new SyncClientHandler(store));
    }
}
