package com.niftysoft.k8s.data;

import org.junit.Test;

import java.util.function.Function;

import static com.niftysoft.k8s.data.HashUtil.hash;
import static org.assertj.core.api.Assertions.assertThat;

public class HashUtilTest {

  @Test
  public void testSameStringsGetSameCode() {
    String str1 = "abc1235";
    String str2 = "abc1235";

    assertThat(hash(str1)).isEqualTo(hash(str2));
  }

  @Test
  public void testDifferentStringsGetNotSameCode() {
    String str1 = "abc1235";
    String str2 = "54321abc";

    assertThat(hash(str1)).isNotEqualTo(hash(str2));
  }

  @Test
  public void testMultipleCallsDoNotChangeHashCode() {
    String str = "alpha";
    long code = hash(str);

    for (int i = 0; i < 1000; ++i) {
      assertThat(code).isEqualTo(hash(str));
    }
  }

  @Test
  public void testReferenceViaFunctionDoesNotChangeHashCode() {
    Function<String, Long> hasher = HashUtil::hash;

    String str = "alpha";

    assertThat(hash(str)).isEqualTo(hasher.apply(str));
  }

  @Test
  public void testUnicodeHashesToSameCode() {
    String str1 = new String(new int[] {0x1f600, 0x1f601, 0x1f602}, 0, 3);
    String str2 = new String(new int[] {0x1f600, 0x1f601, 0x1f602}, 0, 3);

    assertThat(hash(str1)).isEqualTo(hash(str2));
  }

  @Test
  public void testDifferentUnicodeHashesToDifferentCode() {
    String str1 = new String(new int[] {0x1f600, 0x1f601, 0x1f602}, 0, 3);
    String str2 = new String(new int[] {0x1f601, 0x1f600, 0x1f602}, 0, 3);

    assertThat(hash(str1)).isNotEqualTo(hash(str2));
  }
}
