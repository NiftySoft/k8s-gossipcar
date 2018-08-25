package com.niftysoft.k8s.data;

/** @author kalexmills */
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

  public static Config fromEnvVars(Config config) {
    config.peerPort = parseIntIfPossible(System.getenv(PEER_PORT), config.peerPort);
    config.clientPort = parseIntIfPossible(System.getenv(CLIENT_PORT), config.clientPort);
    config.serviceDnsName = ifNotNull(System.getenv(SERVICE_DNS_NAME), config.serviceDnsName);
    config.podIp = ifNotNull(System.getenv(MY_POD_IP), config.podIp);
    config.podName = ifNotNull(System.getenv(MY_POD_NAME), config.podName);
    return config;
  }

  public static Config load() {
    Config result = new Config();
    fromEnvVars(result);
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
