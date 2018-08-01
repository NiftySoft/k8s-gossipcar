package com.niftysoft.k8s.server;

import com.niftysoft.k8s.data.stringstore.VolatileStringStore;
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

  private VolatileStringStore localStore = mock(VolatileStringStore.class);

  @Captor private ArgumentCaptor<VolatileStringStore> storeCaptor;

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
    final VolatileStringStore remoteStore = new VolatileStringStore();
    remoteStore.put("key", "come from away");

    EmbeddedChannel channel = constructTestStack(localStore);

    channel.writeInbound(remoteStore);

    verify(localStore).mergeAllFresher(storeCaptor.capture());

    VolatileStringStore storeMerged = storeCaptor.getValue();

    assertThat(storeMerged.get("key")).isEqualTo("come from away");
    assertThat(storeMerged.getVersion("key")).isEqualTo(Optional.of(0L));
  }

  @Test
  public void testSyncServerRespondsWithLocalStore() {
    final VolatileStringStore localStore = new VolatileStringStore();
    final VolatileStringStore remoteStore = new VolatileStringStore();

    localStore.put("key", "look what I have");

    EmbeddedChannel channel = constructTestStack(localStore);

    channel.writeInbound(remoteStore);

    assertThat(channel.outboundMessages().size()).isEqualTo(1);

    EmbeddedChannel decoderChan =
        new EmbeddedChannel(new VolatileStringStore.VolatileStringStoreDecoder());

    decoderChan.writeInbound((ByteBuf) channel.readOutbound());
    Object obj = decoderChan.readInbound();

    assertThat(obj).hasSameClassAs(remoteStore);
    assertThat(((VolatileStringStore) obj).get("key")).isEqualTo("look what I have");
  }

  private EmbeddedChannel constructTestStack(VolatileStringStore store) {
    return new EmbeddedChannel(
        new VolatileStringStore.VolatileStringStoreDecoder(),
        new VolatileStringStore.VolatileStringStoreEncoder(),
        new SyncServerHandler(store));
  }
}
