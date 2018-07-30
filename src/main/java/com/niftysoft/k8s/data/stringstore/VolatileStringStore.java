package com.niftysoft.k8s.data.stringstore;

import com.niftysoft.k8s.data.HashUtil;
import com.niftysoft.k8s.data.VersionedString;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.handler.codec.ReplayingDecoder;

import java.io.Serializable;
import java.util.*;
import java.util.function.Function;

/**
 * Streamlined map implementing a small subset of the Map interface. Maps Strings to Strings. Each value
 * is internally versioned. When put is called on a value, its version is incremented. When mergeAllFresher is
 * called, only values with versions larger than the current version are updated.
 */
public class VolatileStringStore implements Serializable {

    private static final long serialVersionUID = -5261239982290750103L;

    /**
     *  Use a long keys internally to avoid sending unnecessary strings over the wire (KeySet). Package private to
     *  enable
     */
    private Map<Long, VersionedString> internalMap;

    /**
     * This function is used to ensure that the same hash function is used in all places.
     */
    private static final Function<String, Long> hasher = HashUtil::hash;

    public static Function<String,Long> getHasher() { return hasher; }

    public VolatileStringStore() {
        internalMap = new HashMap<>();
    }

    /**
     * Loads all entries from the other string store which have a more up-to-date version.
     *
     * @param other
     */
    public synchronized void mergeAllFresher(VolatileStringStore other) {
        for (Map.Entry<Long, VersionedString> entry : other.internalMap.entrySet()) {
            long key = entry.getKey();
            this.internalMap.merge(key, entry.getValue(), (VersionedString oldValue, VersionedString value) ->
                    (value.getVersion() > oldValue.getVersion()) ? value : oldValue);
        }
    }

    /**
     * @param key String key
     * @return String value associated with the given key.
     */
    public String get(String key) {
        VersionedString str = internalMap.get(hasher.apply(key));
        return (str == null) ? null : str.getValue();
    }

    /**
     * Put should only be called when a client updates the value. When receiving a payload from a peer,
     * use mergeAllFresher().
     *
     * @param key String key to use to retrieve value in future.
     * @param value String value to associate with key.
     * @return String the value previously stored.
     */
    public synchronized String put(String key, String value) {
        return internalMap.merge(hasher.apply(key), new VersionedString(value), (oldValue, newValue) -> {
            newValue.setVersion(oldValue.getVersion() + 1);
            return newValue;
        }).getValue();
    }

    /**
     * @param key String
     * @return true iff this map contains a value associated with key.
     */
    public boolean containsKey(String key) { return internalMap.containsKey(hasher.apply(key)); }

    /**
     * @return int the number of entries managed by this store.
     */
    public int size() { return internalMap.size(); }

    /**
     * @return this.size() == 0;
     */
    public boolean isEmpty() { return size() == 0; }

    /**
     * @param key String
     * @return Optional<Long> the current version of the value associated with key
     */
    public Optional<Long> getVersion(String key) {
        long hash = hasher.apply(key);
        if (!internalMap.containsKey(hash))
            return Optional.empty();

        return Optional.of(internalMap.get(hash).getVersion());
    }

    public static class VolatileStringStoreDecoder extends ReplayingDecoder<VolatileStringStore> {
        @Override
        protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
            VolatileStringStore result = new VolatileStringStore();
            int numEntries = in.readInt();
            for (int i = 0; i < numEntries; ++i) {
                long key = in.readLong();
                long verstrVersion = in.readLong();

                byte[] stringBuffer = new byte[in.readInt()];
                in.readBytes(stringBuffer);
                String verstrValue = new String(stringBuffer, "UTF-8");

                VersionedString verstr = new VersionedString();
                verstr.setVersion(verstrVersion);
                verstr.setValue(verstrValue);

                result.internalMap.put(key, verstr);
            }
            out.add(result);
        }
    }

    public static class VolatileStringStoreEncoder extends MessageToByteEncoder<VolatileStringStore> {
        @Override
        protected void encode(ChannelHandlerContext ctx, VolatileStringStore in, ByteBuf out) throws Exception {
            Set<Map.Entry<Long, VersionedString>> entrySet = in.internalMap.entrySet();
            out.writeInt(entrySet.size());
            for (Map.Entry<Long, VersionedString> entry : entrySet) {
                out.writeLong(entry.getKey());

                VersionedString verstr = entry.getValue();
                out.writeLong(verstr.getVersion());

                int nBytes = ByteBufUtil.utf8Bytes(verstr.getValue());
                out.writeInt(nBytes);
                ByteBufUtil.reserveAndWriteUtf8(out, verstr.getValue(), nBytes);
            }
        }
    }
}
