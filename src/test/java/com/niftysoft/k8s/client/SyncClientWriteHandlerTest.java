package com.niftysoft.k8s.client;

import com.niftysoft.k8s.data.stringstore.VolatileStringStore;
import com.niftysoft.k8s.data.stringstore.VolatileStringStoreDecoder;
import com.niftysoft.k8s.data.stringstore.VolatileStringStoreEncoder;
import io.netty.buffer.ByteBuf;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SyncClientWriteHandlerTest {

    @Test
    public void testSyncTaskInitiatesSyncProtocol() {
        final VolatileStringStore store = new VolatileStringStore();

        EmbeddedChannel channel = constructTestStack(store);

        assertThat(channel.outboundMessages().size()).isEqualTo(3);

        char magic1 = channel.readOutbound();
        char magic2 = channel.readOutbound();

        assertThat(magic1).isEqualTo('S');
        assertThat(magic2).isEqualTo('Y');
    }

    @Test
    public void testSyncTaskWritesLocalStringStore() {
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

    private EmbeddedChannel constructTestStack(VolatileStringStore store) {
        return new EmbeddedChannel(
                new VolatileStringStoreDecoder(),
                new VolatileStringStoreEncoder(),
                new SyncClientWriteHandler(store),
                new SyncClientReadHandler(store));
    }
}
