package com.niftysoft.k8s.server;

import com.niftysoft.k8s.data.stringstore.VolatileByteStore;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class SyncServerHandlerTest {

  private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
  private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
  private final PrintStream originalOut = System.out;
  private final PrintStream originalErr = System.err;

  private VolatileByteStore localStore = mock(VolatileByteStore.class);

  @Captor private ArgumentCaptor<VolatileByteStore> storeCaptor;

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
  public void testSyncServerMergesLocalStoreWithRemote() {
    final VolatileByteStore remoteStore = new VolatileByteStore();
    remoteStore.put("key", "come from away".getBytes());

    EmbeddedChannel channel = constructTestStack(localStore);

    channel.writeInbound(remoteStore);

    verify(localStore).mergeAllFresher(storeCaptor.capture());

    VolatileByteStore storeMerged = storeCaptor.getValue();

    assertThat(storeMerged.get("key")).isEqualTo("come from away".getBytes());
    assertThat(storeMerged.getVersion("key")).isEqualTo(Optional.of(0L));
  }

  @Test
  public void testSyncServerRespondsWithLocalStore() {
    final VolatileByteStore localStore = new VolatileByteStore();
    final VolatileByteStore remoteStore = new VolatileByteStore();

    localStore.put("key", "look what I have".getBytes());

    EmbeddedChannel channel = constructTestStack(localStore);

    channel.writeInbound(remoteStore);

    assertThat(channel.outboundMessages().size()).isEqualTo(1);

    EmbeddedChannel decoderChan =
        new EmbeddedChannel(new VolatileByteStore.VolatileByteStoreDecoder());

    decoderChan.writeInbound((ByteBuf) channel.readOutbound());
    Object obj = decoderChan.readInbound();

    assertThat(obj).hasSameClassAs(remoteStore);
    assertThat(((VolatileByteStore) obj).get("key")).isEqualTo("look what I have".getBytes());
  }

  private EmbeddedChannel constructTestStack(VolatileByteStore store) {
    return new EmbeddedChannel(
        new VolatileByteStore.VolatileByteStoreDecoder(),
        new VolatileByteStore.VolatileByteStoreEncoder(),
        new SyncServerHandler(store));
  }
}
