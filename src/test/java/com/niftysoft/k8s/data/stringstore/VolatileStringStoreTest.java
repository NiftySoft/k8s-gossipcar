package com.niftysoft.k8s.data.stringstore;

import com.niftysoft.k8s.data.HashUtil;
import com.niftysoft.k8s.data.stringstore.VolatileStringStore;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class VolatileStringStoreTest {

    @Test
    public void testGetNonExistingKeyReturnsNull() {
        VolatileStringStore store = new VolatileStringStore();

        assertThat(store.get("non-existent-key")).isNull();
    }

    @Test
    public void testGetReturnsLastKeyPut() {
        VolatileStringStore store = new VolatileStringStore();

        store.put("key", "value");

        assertThat(store.get("key")).isEqualTo("value");
    }

    @Test
    public void testPutOverwritesLastKeyPut() {
        VolatileStringStore store = new VolatileStringStore();

        store.put("key", "value");
        store.put("key", "a totally different value");

        assertThat(store.get("key")).isEqualTo("a totally different value");
    }

    @Test
    public void testLoadIfFresherOverwritesStaleData() {
        VolatileStringStore store1 = new VolatileStringStore();
        VolatileStringStore store2 = new VolatileStringStore();

        store1.put("val", "0");
        store1.put("val", "1");
        store1.put("val", "2");

        store2.put("val", "dead dove");

        store2.mergeAllFresher(store1);

        assertThat(store2.get("val")).isEqualTo("2");
    }

    @Test
    public void testLoadIfFresherWritesNewData() {
        VolatileStringStore store1 = new VolatileStringStore();
        VolatileStringStore store2 = new VolatileStringStore();

        store1.put("val", "0");

        store2.mergeAllFresher(store1);

        assertThat(store2.get("val")).isEqualTo("0");
    }

    @Test
    public void testLoadIfFresherIgnoresStaleData() {
        VolatileStringStore store1 = new VolatileStringStore();
        VolatileStringStore store2 = new VolatileStringStore();

        store1.put("val", "0");
        store1.put("val", "1");
        store1.put("val", "2");

        store2.put("val", "0");

        store1.mergeAllFresher(store2);

        assertThat(store1.get("val")).isEqualTo("2");
    }

    @Test
    public void testLoadIfFresherSyncIsIdempotent() {
        VolatileStringStore store1 = new VolatileStringStore();
        VolatileStringStore store2 = new VolatileStringStore();

        store1.put("val", "0");
        store1.put("val", "1");
        store1.put("val", "2");

        store2.put("val", "dead dove");

        store1.mergeAllFresher(store2);
        store2.mergeAllFresher(store1);

        assertThat(store2.get("val")).isEqualTo("2");
        assertThat(store1.get("val")).isEqualTo("2");
    }

    @Test
    public void testLoadIfFresherMergesMultipleKeys() {
        VolatileStringStore store1 = new VolatileStringStore();
        VolatileStringStore store2 = new VolatileStringStore();

        store1.put("key1", "0");
        store1.put("key1", "1");
        store1.put("key1", "2");

        store1.put("key2", "0");

        store2.put("key2", "0");
        store2.put("key2", "1");

        store2.put("key1", "0");

        store2.put("key3", "0");

        store1.mergeAllFresher(store2);

        assertThat(store1.get("key1")).isEqualTo("2");
        assertThat(store1.get("key2")).isEqualTo("1");
        assertThat(store1.get("key3")).isEqualTo("0");
    }

    @Test
    public void testReferenceHasherViaStaticReferenceDoesNotAffectHashCode() {
        String str = "alpha";

        assertThat(VolatileStringStore.hasher.apply(str)).isEqualTo(HashUtil.hash(str));
    }
}