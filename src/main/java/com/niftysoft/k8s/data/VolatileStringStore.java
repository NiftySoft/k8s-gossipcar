package com.niftysoft.k8s.data;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Streamlined map implementing a small subset of the Map interface. Maps Strings to Strings. Each value
 * is internally versioned. When put is called on a value, its version is incremented. When loadIfFresher is
 * called, only values with versions larger than the current version are updated.
 */
public class VolatileStringStore implements Serializable {

    private static final long serialVersionUID = -5261239982290750103L;

    private static VolatileStringStore SINGLETON;

    /**
     *  Use a long keys internally to avoid sending unnecessary strings over the wire (KeySet).
     */
    private Map<Long, VersionedString> internalMap;

    private long timestamp;

    private VolatileStringStore() {
        internalMap = new HashMap<>();
    }

    /**
     * @return VolatileStringStore singleton instance of the VolatileStringStore on this node.
     */
    public static VolatileStringStore getInstance() {
        synchronized (SINGLETON) {
            if (SINGLETON == null) SINGLETON = new VolatileStringStore();
        }
        return SINGLETON;
    }

    /**
     * Loads all entries from the other string store which have a more up-to-date version.
     *
     * @param other
     */
    public synchronized void loadIfFresher(VolatileStringStore other) {
        for (Map.Entry<Long, VersionedString> entry : other.internalMap.entrySet()) {
            long key = entry.getKey();
            this.internalMap.merge(key, entry.getValue(), (VersionedString oldValue, VersionedString value) ->
                    (value.version > oldValue.version) ? value : oldValue);
        }
    }

    /**
     * This function is used to ensure that the same hash function is used in all places.
     */
    private static final Function<String, Long> hasher = HashUtil::hash;

    /**
     * @param key String key
     * @return String value associated with the given key.
     */
    public String get(String key) {
        return internalMap.get(hasher.apply(key)).value;
    }

    /**
     * Put should only be called when a client updates the value. When receiving a payload from a peer,
     * use loadIfFresher().
     *
     * @param key String key to use to retrieve value in future.
     * @param value String value to associate with key.
     * @return String the value previously stored.
     */
    public synchronized String put(String key, String value) {
        return internalMap.merge(hasher.apply(key), new VersionedString(value), (oldValue, newValue) -> {
            newValue.version = oldValue.version + 1;
            return newValue;
        }).value;
    }
}
