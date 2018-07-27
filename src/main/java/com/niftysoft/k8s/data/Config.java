package com.niftysoft.k8s.data;

public final class Config {
    private static final String PEER_PORT = "PEER_PORT";
    private static final String CLIENT_PORT = "CLIENT_PORT";
    private static final String SERVICE_DNS_NAME = "SERVICE_DNS_NAME";

    public int peerPort = 46747;
    public int clientPort = 80;
    public String serviceDnsName = "gossipSidecar.default";

    public static Config fromEnvVars() {
        Config result = new Config();
        result.peerPort = parseIntIfPossible(System.getenv(PEER_PORT), result.peerPort);
        result.clientPort = parseIntIfPossible(System.getenv(CLIENT_PORT), result.clientPort);
        result.serviceDnsName = ifNotNull(System.getenv(SERVICE_DNS_NAME), result.serviceDnsName);
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