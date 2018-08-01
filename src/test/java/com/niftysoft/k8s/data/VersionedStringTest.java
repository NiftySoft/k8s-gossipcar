package com.niftysoft.k8s.data;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class VersionedStringTest {

  @Test
  public void testDefaultVersionIsZero() {
    VersionedString str = new VersionedString();

    assertThat(str.getVersion()).isEqualTo(0L);
  }

  @Test
  public void testDefaultValueIsNull() {
    VersionedString str = new VersionedString();

    assertThat(str.getValue()).isEqualTo(null);
  }

  @Test
  public void testGetValueReturnsLastSetValue() {
    VersionedString str = new VersionedString();

    str.setValue("hello");

    assertThat(str.getValue()).isEqualTo("hello");
  }

  @Test
  public void testGetVersionReturnsLastSetValue() {
    VersionedString str = new VersionedString();

    str.setVersion(123L);

    assertThat(str.getVersion()).isEqualTo(123L);
  }
}
