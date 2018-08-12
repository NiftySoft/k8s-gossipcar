package com.niftysoft.k8s.data.stringstore;

import io.netty.buffer.ByteBuf;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Before;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class VolatileByteStoreEncoderTest {

  private EmbeddedChannel channel;
  private VolatileByteStore vss;

  @Before
  public void before() {
    channel = new EmbeddedChannel(new VolatileByteStore.VolatileByteStoreEncoder());
    vss = new VolatileByteStore();
  }

  @Test
  public void testEncodesEmptyMap() {
    ByteBuf buf = writeToOutboundAndReadOutboundByteBuf();

    assertSize(buf, 0);
    assertThat(buf.readableBytes()).isEqualTo(0);
  }

  @Test
  public void testEncodesSingletonMap() {
    vss.put("key", "value".getBytes());

    ByteBuf buf = writeToOutboundAndReadOutboundByteBuf();

    assertSize(buf, 1);
    assertEntry(buf, "key", "value", 0L);
    assertThat(buf.readableBytes()).isEqualTo(0);
  }

  @Test
  public void testEncodesMultiKeyMap() {
    // For loops increment version number artificially.
    writeEntry("alpha", "beta", 5L);
    writeEntry("gamma", "slamma", 17L);
    writeEntry("imma", "mamma", 1L);
    writeEntry("excelsior", "marvel", 0L);
    writeEntry("try it", "not really", 10L);

    ByteBuf buf = writeToOutboundAndReadOutboundByteBuf();

    assertSize(buf, 5);
    Set<Entry> entries = new HashSet<>();
    for (int i = 0; i < 5; ++i) entries.add(obtainEntry(buf));
    assertThat(buf.readableBytes()).isEqualTo(0);

    assertThat(entries).contains(new Entry("alpha", "beta", 5L));
    assertThat(entries).contains(new Entry("gamma", "slamma", 17L));
    assertThat(entries).contains(new Entry("excelsior", "marvel", 0L));
    assertThat(entries).contains(new Entry("imma", "mamma", 1L));
    assertThat(entries).contains(new Entry("try it", "not really", 10L));
  }

  private ByteBuf writeToOutboundAndReadOutboundByteBuf() {
    assertThat(channel.writeOutbound(vss)).isTrue();
    assertThat(channel.outboundMessages().size()).isEqualTo(1);

    return channel.readOutbound();
  }

  private void writeEntry(String key, String value, long version) {
    for (int i = 0; i < version + 1; ++i) vss.put(key, value.getBytes());
  }

  private void assertSize(ByteBuf buf, int size) {
    assertThat(buf.readInt()).isEqualTo(size);
  }

  private Entry obtainEntry(ByteBuf buf) {
    long keyCode = buf.readLong();
    long version = buf.readLong();

    byte[] byteArr = new byte[buf.readInt()];
    buf.readBytes(byteArr);
    String value = new String(byteArr);

    return new Entry(keyCode, value, version);
  }

  private void assertEntry(ByteBuf buf, String key, String value, long version) {
    assertThat(buf.readLong()).isEqualTo(VolatileByteStore.getHasher().apply(key));
    assertThat(buf.readLong()).isEqualTo(version);

    byte[] byteArr = new byte[buf.readInt()];
    buf.readBytes(byteArr);
    assertThat(new String(byteArr)).isEqualTo(value);
  }

  private static class Entry {
    public long keyCode;
    public String value;
    public long version;

    public Entry(String key, String value, long version) {
      this(VolatileByteStore.getHasher().apply(key), value, version);
    }

    public Entry(long keyCode, String value, long version) {
      this.keyCode = keyCode;
      this.value = value;
      this.version = version;
    }

    public boolean equals(Object o) {
      if (o == null) return false;
      if (o == this) return true;
      if (o.getClass() != this.getClass()) return false;

      Entry other = (Entry) o;
      return other.keyCode == this.keyCode
          && other.value.equals(this.value)
          && other.version == this.version;
    }
  }
}
