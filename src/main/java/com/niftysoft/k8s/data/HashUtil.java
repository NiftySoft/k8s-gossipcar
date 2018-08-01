package com.niftysoft.k8s.data;

/** @author K. Alex Mills */
public class HashUtil {

  public static long hash(String string) {
    long h = 1125899906842597L; // prime
    int len = string.length();

    for (int i = 0; i < len; i++) {
      h = 31 * h + string.charAt(i);
    }
    return h;
  }
}
