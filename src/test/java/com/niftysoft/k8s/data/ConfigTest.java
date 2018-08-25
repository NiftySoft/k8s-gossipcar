package com.niftysoft.k8s.data;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;

import static org.assertj.core.api.Assertions.assertThat;

public class ConfigTest {
  @Rule public final EnvironmentVariables environmentVariables = new EnvironmentVariables();

  @Before
  public void before() {
    environmentVariables.clear("PEER_PORT");
    environmentVariables.clear("CLIENT_PORT");
    environmentVariables.clear("SERVICE_DNS_NAME");
    environmentVariables.clear("MY_POD_IP");
    environmentVariables.clear("MY_POD_NAME");
  }

  @Test
  public void testFromEnvVarsReadsFromSystemEnvironment() {
    environmentVariables.set("PEER_PORT", "1337");
    environmentVariables.set("CLIENT_PORT", "8080");
    environmentVariables.set("SERVICE_DNS_NAME", "fat.tacos");
    environmentVariables.set("MY_POD_IP", "10.4.4.2");
    environmentVariables.set("MY_POD_NAME", "hargleblarg");

    Config config = Config.fromEnvVars(new Config());

    assertThat(config.peerPort).isEqualTo(1337);
    assertThat(config.clientPort).isEqualTo(8080);
    assertThat(config.serviceDnsName).isEqualTo("fat.tacos");
    assertThat(config.podIp).isEqualTo("10.4.4.2");
    assertThat(config.podName).isEqualTo("hargleblarg");
  }

  @Test
  public void testFromEnvVarsDefaults() {
    Config config = Config.fromEnvVars(new Config());

    Config configWithDefaults = new Config();

    assertThat(config.peerPort).isEqualTo(configWithDefaults.peerPort);
    assertThat(config.clientPort).isEqualTo(configWithDefaults.clientPort);
    assertThat(config.serviceDnsName).isEqualTo(configWithDefaults.serviceDnsName);
  }

  @Test
  public void testDefaults() {
    Config config = new Config();

    assertThat(config.peerPort).isEqualTo(46747);
    assertThat(config.clientPort).isEqualTo(80);
    assertThat(config.serviceDnsName).isEqualTo("gossipSidecar.default.svc.cluster.local");
    assertThat(config.podIp).isEqualTo("127.0.0.1");
    assertThat(config.podName).isEqualTo("gossipSidecar-0");
  }
}
