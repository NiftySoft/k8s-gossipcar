package com.niftysoft.k8s.data;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class VersionedByteTest {

  @Test
  public void testDefaultVersionIsZero() {
    VersionedByteArr str = new VersionedByteArr();

    assertThat(str.getVersion()).isEqualTo(0L);
  }

  @Test
  public void testDefaultValueIsNull() {
    VersionedByteArr str = new VersionedByteArr();

    assertThat(str.getValue()).isEqualTo(null);
  }

  @Test
  public void testGetValueReturnsLastSetValue() {
    VersionedByteArr str = new VersionedByteArr();

    str.setValue("hello".getBytes());

    assertThat(str.getValue()).isEqualTo("hello".getBytes());
  }

  @Test
  public void testGetVersionReturnsLastSetValue() {
    VersionedByteArr str = new VersionedByteArr();

    str.setVersion(123L);

    assertThat(str.getVersion()).isEqualTo(123L);
  }
}
