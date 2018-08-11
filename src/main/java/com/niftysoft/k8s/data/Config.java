package com.niftysoft.k8s.data;

/** @author K. Alex Mills */
public final class Config {
  public static final String PEER_PORT = "PEER_PORT";
  public static final String CLIENT_PORT = "CLIENT_PORT";
  public static final String SERVICE_DNS_NAME = "SERVICE_DNS_NAME";
  public static final String MY_POD_NAME = "MY_POD_NAME";
  public static final String MY_POD_IP = "MY_POD_IP";

  public int peerPort = 46747;
  public int clientPort = 80;
  public String serviceDnsName = "gossipSidecar.default.svc.cluster.local";
  public String podIp = "127.0.0.1";
  public String podName = "gossipSidecar-0";

  public static Config fromEnvVars() {
    Config result = new Config();
    result.peerPort = parseIntIfPossible(System.getenv(PEER_PORT), result.peerPort);
    result.clientPort = parseIntIfPossible(System.getenv(CLIENT_PORT), result.clientPort);
    result.serviceDnsName = ifNotNull(System.getenv(SERVICE_DNS_NAME), result.serviceDnsName);
    result.podIp = ifNotNull(System.getenv(MY_POD_IP), result.podIp);
    result.podName = ifNotNull(System.getenv(MY_POD_NAME), result.podName);
    return result;
  }

  private static String ifNotNull(String str, String defaultValue) {
    return str == null ? defaultValue : str;
  }

  private static int parseIntIfPossible(String str, int defaultValue) {
    try {
      return Integer.parseInt(str);
    } catch (Exception e) {
      return defaultValue;
    }
  }
}
