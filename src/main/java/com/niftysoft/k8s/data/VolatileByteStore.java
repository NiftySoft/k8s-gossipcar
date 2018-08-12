package com.niftysoft.k8s.data;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.handler.codec.ReplayingDecoder;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Streamlined map implementing a small subset of the Map interface. Maps Strings to Strings. Each
 * value is internally versioned. When put is called on a value, its version is incremented. When
 * mergeAllFresher is called, only values with versions larger than the current version are updated.
 *
 * @author kalexmills
 */
public class VolatileByteStore {
  /** This function is used to ensure that the same hash function is used in all places. */
  private static final Function<String, Long> hasher = HashUtil::hash;
  /**
   * Use a long keys internally to avoid sending unnecessary strings over the wire (KeySet). Package
   * private to enable
   */
  private Map<Long, VersionedByteArr> internalMap;

  public VolatileByteStore() {
    internalMap = new ConcurrentHashMap<>();
  }

  public static Function<String, Long> getHasher() {
    return hasher;
  }

  /**
   * Loads all entries from the other string store which have a more up-to-date version.
   *
   * @param other
   */
  public void mergeAllFresher(VolatileByteStore other) {
    for (Map.Entry<Long, VersionedByteArr> entry : other.internalMap.entrySet()) {
      long key = entry.getKey();
      this.internalMap.merge(
          key,
          entry.getValue(),
          (VersionedByteArr oldValue, VersionedByteArr value) ->
              (value.getVersion() > oldValue.getVersion()) ? value : oldValue);
    }
  }

  /**
   * @param key String key
   * @return String value associated with the given key.
   */
  public byte[] get(String key) {
    VersionedByteArr vArr = internalMap.get(hasher.apply(key));
    return (vArr == null) ? null : vArr.getValue();
  }

  /**
   * Put should only be called when a client updates the value. When receiving a payload from a
   * peer, use mergeAllFresher().
   *
   * @param key String key to use to retrieve value in future.
   * @param value byte[] value to associate with key.
   * @return byte[] the value previously stored.
   */
  public byte[] put(String key, byte[] value) {
    return internalMap
        .merge(
            hasher.apply(key),
            new VersionedByteArr(value),
            (oldValue, newValue) -> {
              newValue.setVersion(oldValue.getVersion() + 1);
              return newValue;
            })
        .getValue();
  }

  /**
   * @param key String
   * @return true iff this map contains a value associated with key.
   */
  public boolean containsKey(String key) {
    return internalMap.containsKey(hasher.apply(key));
  }

  /** @return int the number of entries managed by this store. */
  public int size() {
    return internalMap.size();
  }

  /** @return this.size() == 0; */
  public boolean isEmpty() {
    return size() == 0;
  }

  /**
   * @param key String
   * @return Optional<Long> the current version of the value associated with key
   */
  public Optional<Long> getVersion(String key) {
    long hash = hasher.apply(key);
    if (!internalMap.containsKey(hash)) return Optional.empty();

    return Optional.of(internalMap.get(hash).getVersion());
  }

  public static class VolatileByteStoreDecoder extends ReplayingDecoder<VolatileByteStore> {
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out)
        throws Exception {
      VolatileByteStore result = new VolatileByteStore();
      int numEntries = in.readInt();
      for (int i = 0; i < numEntries; ++i) {
        long key = in.readLong();
        long vArrVersion = in.readLong();

        byte[] bytes = new byte[in.readInt()];
        in.readBytes(bytes);

        VersionedByteArr vArr = new VersionedByteArr();
        vArr.setVersion(vArrVersion);
        vArr.setValue(bytes);

        result.internalMap.put(key, vArr);
      }
      out.add(result);
    }
  }

  public static class VolatileByteStoreEncoder extends MessageToByteEncoder<VolatileByteStore> {
    @Override
    protected void encode(ChannelHandlerContext ctx, VolatileByteStore in, ByteBuf out) {
      Set<Map.Entry<Long, VersionedByteArr>> entrySet = in.internalMap.entrySet();
      out.writeInt(entrySet.size());
      for (Map.Entry<Long, VersionedByteArr> entry : entrySet) {
        out.writeLong(entry.getKey());

        VersionedByteArr vArr = entry.getValue();
        out.writeLong(vArr.getVersion());

        byte[] bytes = vArr.getValue();
        out.writeInt(bytes.length);
        out.writeBytes(bytes);
      }
    }
  }
}
