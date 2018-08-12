package com.niftysoft.k8s.data;

import com.niftysoft.k8s.data.HashUtil;
import com.niftysoft.k8s.data.VolatileByteStore;
import org.junit.Before;
import org.junit.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class VolatileByteStoreTest {

  private VolatileByteStore store;

  @Before
  public void before() {
    store = new VolatileByteStore();
  }

  @Test
  public void testGetNonExistingKeyReturnsNull() {
    assertThat(store.get("non-existent-key")).isNull();
  }

  @Test
  public void testGetReturnsLastKeyPut() {
    store.put("key", "value".getBytes());

    assertThat(store.get("key")).isEqualTo("value".getBytes());
  }

  @Test
  public void testGetVersionForNonExistentKeyReturnsNotPresentOptional() {
    assertThat(store.getVersion("non-existent-key")).isNotPresent();
  }

  @Test
  public void testGetVersionStartsAtZero() {
    store.put("test", "value".getBytes());
    Optional<Long> version = store.getVersion("test");
    assertThat(version).isPresent();
    assertThat(version.get()).isEqualTo(0L);
  }

  @Test
  public void testGetVersionIncrementsByOneForEachPut() {
    store.put("test", "value".getBytes());
    for (int i = 1; i <= 10; ++i) {
      store.put("test", "value".getBytes());
      Optional<Long> version = store.getVersion("test");
      assertThat(version).isPresent();
      assertThat(version.get()).isEqualTo(i);
    }
  }

  @Test
  public void testPutOverwritesLastKeyPut() {
    store.put("key", "value".getBytes());
    store.put("key", "a totally different value".getBytes());

    assertThat(store.get("key")).isEqualTo("a totally different value".getBytes());
  }

  @Test
  public void testLoadIfFresherOverwritesStaleData() {
    VolatileByteStore store1 = new VolatileByteStore();
    VolatileByteStore store2 = new VolatileByteStore();

    store1.put("val", "0".getBytes());
    store1.put("val", "1".getBytes());
    store1.put("val", "2".getBytes());

    store2.put("val", "dead dove".getBytes());

    store2.mergeAllFresher(store1);

    assertThat(store2.get("val")).isEqualTo("2".getBytes());
  }

  @Test
  public void testLoadIfFresherMaintainsFresherVersion() {
    VolatileByteStore store1 = new VolatileByteStore();
    VolatileByteStore store2 = new VolatileByteStore();

    store1.put("val", "0".getBytes());
    store1.put("val", "1".getBytes());
    store1.put("val", "2".getBytes());
    assertThat(store1.getVersion("val").get()).isEqualTo(2L);

    store2.put("val", "0b".getBytes());

    store2.mergeAllFresher(store1);
    Optional<Long> version = store2.getVersion("val");
    assertThat(version).isPresent();
    assertThat(version.get()).isEqualTo(2L);
  }

  @Test
  public void testLoadIfFresherWritesNewData() {
    VolatileByteStore store1 = new VolatileByteStore();
    VolatileByteStore store2 = new VolatileByteStore();

    store1.put("val", "0".getBytes());

    store2.mergeAllFresher(store1);

    assertThat(store2.get("val")).isEqualTo("0".getBytes());
  }

  @Test
  public void testLoadIfFresherIgnoresStaleData() {
    VolatileByteStore store1 = new VolatileByteStore();
    VolatileByteStore store2 = new VolatileByteStore();

    store1.put("val", "0".getBytes());
    store1.put("val", "1".getBytes());
    store1.put("val", "2".getBytes());

    store2.put("val", "0".getBytes());

    store1.mergeAllFresher(store2);

    assertThat(store1.get("val")).isEqualTo("2".getBytes());
  }

  @Test
  public void testLoadIfFresherSyncIsIdempotent() {
    VolatileByteStore store1 = new VolatileByteStore();
    VolatileByteStore store2 = new VolatileByteStore();

    store1.put("val", "0".getBytes());
    store1.put("val", "1".getBytes());
    store1.put("val", "2".getBytes());

    store2.put("val", "dead dove".getBytes());

    store1.mergeAllFresher(store2);
    store2.mergeAllFresher(store1);

    assertThat(store2.get("val")).isEqualTo("2".getBytes());
    assertThat(store1.get("val")).isEqualTo("2".getBytes());
  }

  @Test
  public void testLoadIfFresherMergesMultipleKeys() {
    VolatileByteStore store1 = new VolatileByteStore();
    VolatileByteStore store2 = new VolatileByteStore();

    store1.put("key1", "0".getBytes());
    store1.put("key1", "1".getBytes());
    store1.put("key1", "2".getBytes());

    store1.put("key2", "0".getBytes());

    store2.put("key2", "0".getBytes());
    store2.put("key2", "1".getBytes());

    store2.put("key1", "0".getBytes());

    store2.put("key3", "0".getBytes());

    store1.mergeAllFresher(store2);

    assertThat(store1.get("key1")).isEqualTo("2".getBytes());
    assertThat(store1.get("key2")).isEqualTo("1".getBytes());
    assertThat(store1.get("key3")).isEqualTo("0".getBytes());
  }

  @Test
  public void testReferenceHasherViaStaticReferenceDoesNotAffectHashCode() {
    String str = "alpha";

    assertThat(VolatileByteStore.getHasher().apply(str)).isEqualTo(HashUtil.hash(str));
  }
}
