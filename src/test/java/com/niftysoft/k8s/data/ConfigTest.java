package com.niftysoft.k8s.data;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;

import static org.assertj.core.api.Assertions.assertThat;

public class ConfigTest {
    @Rule
    public final EnvironmentVariables environmentVariables
            = new EnvironmentVariables();

    @Before
    public void before() {
        environmentVariables.clear("PEER_PORT");
        environmentVariables.clear("CLIENT_PORT");
        environmentVariables.clear("SERVICE_DNS_NAME");
    }

    @Test
    public void testFromEnvVarsReadsFromSystemEnvironment() {
        environmentVariables.set("PEER_PORT", "1337");
        environmentVariables.set("CLIENT_PORT", "8080");
        environmentVariables.set("SERVICE_DNS_NAME", "fat.tacos");

        Config config = Config.fromEnvVars();

        assertThat(config.peerPort).isEqualTo(1337);
        assertThat(config.clientPort).isEqualTo(8080);
        assertThat(config.serviceDnsName).isEqualTo("fat.tacos");
    }

    @Test
    public void testFromEnvVarsDefaults() {
        Config config = Config.fromEnvVars();

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
        assertThat(config.serviceDnsName).isEqualTo("gossipSidecar.default");
    }
}