package com.niftysoft.k8s.server;

import io.netty.buffer.ByteBuf;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class HeartbeatHandlerTest {

    @Test
    public void testHeartbeatHandler() {
        EmbeddedChannel chan = new EmbeddedChannel(new HeartbeatHandler());

        assertThat(chan.outboundMessages().size()).isEqualTo(1);

        ByteBuf buf = chan.readOutbound();

        assertThat(buf.readByte()).isEqualTo((byte)'b');
        assertThat(buf.readByte()).isEqualTo((byte)'b');
        assertThat(buf.readByte()).isEqualTo((byte)' ');
        assertThat(buf.readByte()).isEqualTo((byte)'b');
        assertThat(buf.readByte()).isEqualTo((byte)'b');
    }
}