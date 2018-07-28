package com.niftysoft.k8s.client;

import com.niftysoft.k8s.data.stringstore.VolatileStringStore;
import com.niftysoft.k8s.data.stringstore.VolatileStringStoreDecoder;
import com.niftysoft.k8s.data.stringstore.VolatileStringStoreEncoder;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class SyncClientReadHandlerTest {

    private VolatileStringStore localStore = mock(VolatileStringStore.class);

    @Captor
    private ArgumentCaptor<VolatileStringStore> storeCaptor;

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

    private EmbeddedChannel constructTestStack(VolatileStringStore store) {
        return new EmbeddedChannel(
                new VolatileStringStoreDecoder(),
                new VolatileStringStoreEncoder(),
                new SyncClientWriteHandler(store),
                new SyncClientReadHandler(store));
    }
}
