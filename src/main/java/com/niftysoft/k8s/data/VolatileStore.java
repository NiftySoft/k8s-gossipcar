package com.niftysoft.k8s.data;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class VolatileStore implements Map<String, String> {

    private static VolatileStore SINGLETON;

    /**
     *  Use a Map<Long, Object> internally to avoid sending strings over the wire.
     */
    private Map<Long, String> internalMap;

    private VolatileStore() {
        internalMap = new HashMap<>();
    }

    /**
     * @return VolatileStore singleton instance of the VolatileStore on this node.
     */
    public static VolatileStore getInstance() {
        synchronized (SINGLETON) {
            if (SINGLETON == null) SINGLETON = new VolatileStore();
        }
        return SINGLETON;
    }

    /**
     * This hasher is used to ensure that the same hash function is used in all places.
     */
    private static final Function<String, Long> hasher = HashUtil::hash;

    public String getValue(String key) {
        return internalMap.get(hasher.apply(key));
    }

    public void putValue(String key, String value) {
        internalMap.put(hasher.apply(key), value);
    }

}
