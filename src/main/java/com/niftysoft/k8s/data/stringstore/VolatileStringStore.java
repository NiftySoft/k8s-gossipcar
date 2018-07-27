package com.niftysoft.k8s.data.stringstore;

import com.niftysoft.k8s.data.HashUtil;
import com.niftysoft.k8s.data.VersionedString;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
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
    Map<Long, VersionedString> internalMap;

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
     * This function is used to ensure that the same hash function is used in all places.
     */
    static final Function<String, Long> hasher = HashUtil::hash;

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

    public int size() { return internalMap.size(); }
    public boolean isEmpty() { return size() == 0; }

    public Optional<Long> getVersion(String key) {
        long hash = hasher.apply(key);
        if (!internalMap.containsKey(hash))
            return Optional.empty();

        return Optional.of(internalMap.get(hash).getVersion());
    }
}
