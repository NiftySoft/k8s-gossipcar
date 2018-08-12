package com.niftysoft.k8s.client;

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
public class SyncInitiateHandlerTest {

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
  public void testSyncTaskMergesLocalStoreWithRemote() {
    final VolatileByteStore remoteStore = new VolatileByteStore();
    remoteStore.put("key", "value".getBytes());

    EmbeddedChannel channel = constructTestStack(localStore);

    channel.writeInbound(remoteStore);

    verify(localStore).mergeAllFresher(storeCaptor.capture());

    VolatileByteStore storeMerged = storeCaptor.getValue();

    assertThat(storeMerged.get("key")).isEqualTo("value".getBytes());
    assertThat(storeMerged.getVersion("key")).isEqualTo(Optional.of(0L));
  }

  @Test
  public void testSyncClientInitiatesSyncProtocol() {
    final VolatileByteStore store = new VolatileByteStore();

    EmbeddedChannel channel = constructTestStack(store);

    assertThat(channel.outboundMessages().size()).isEqualTo(1);

    ByteBuf buf = channel.readOutbound();
  }

  @Test
  public void testSyncClientWritesLocalStringStore() {
    final VolatileByteStore testStore = new VolatileByteStore();

    testStore.put("test", "string".getBytes());

    EmbeddedChannel channel = constructTestStack(testStore);

    assertThat(channel.outboundMessages().size()).isEqualTo(1);

    EmbeddedChannel decoderChan =
        new EmbeddedChannel(new VolatileByteStore.VolatileByteStoreDecoder());

    decoderChan.writeInbound((ByteBuf) channel.readOutbound());

    Object obj = decoderChan.readInbound();

    assertThat(obj).hasSameClassAs(testStore);

    assertThat(((VolatileByteStore) obj).get("test")).isEqualTo("string".getBytes());
  }

  @Test
  public void testSyncTaskPrintsExceptionAndClosesStream() {
    final VolatileByteStore testStore = new VolatileByteStore();

    EmbeddedChannel channel = constructTestStack(testStore);

    channel.pipeline().fireChannelRead("bad request");

    assertThat(channel.isOpen()).isFalse();
    assertThat(errContent.toString()).isNotBlank();
  }

  private EmbeddedChannel constructTestStack(VolatileByteStore store) {
    return new EmbeddedChannel(
        new VolatileByteStore.VolatileByteStoreDecoder(),
        new VolatileByteStore.VolatileByteStoreEncoder(),
        new SyncInitiateHandler(store));
  }
}
