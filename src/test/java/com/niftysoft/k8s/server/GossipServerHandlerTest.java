package com.niftysoft.k8s.server;

import com.niftysoft.k8s.data.stringstore.VolatileStringStore;
import com.niftysoft.k8s.data.stringstore.VolatileStringStoreDecoder;
import com.niftysoft.k8s.data.stringstore.VolatileStringStoreEncoder;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class GossipServerHandlerTest {
    VolatileStringStore vss = mock(VolatileStringStore.class);

    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;

    @Before
    public void setUpStreams() {
        System.setOut(new PrintStream(outContent));
        System.setErr(new PrintStream(errContent));
    }

    @After
    public void restoreStreams() {
        System.setOut(originalOut);
        System.setErr(originalErr);
    }


    @Test
    public void testGossipServerHandlerSwitchesToSyncHandlersAndSetsEncoderAndDecoder() {
        EmbeddedChannel chan = constructTestStack(vss);

        chan.writeInbound(ByteBufUtil.writeAscii(ByteBufAllocator.DEFAULT, "SY"));
        assertThat(chan.pipeline().get("handler")).hasSameClassAs(new SyncServerHandler(null));
        assertThat(chan.pipeline().get("encoder")).hasSameClassAs(new VolatileStringStoreEncoder());
        assertThat(chan.pipeline().get("decoder")).hasSameClassAs(new VolatileStringStoreDecoder());
    }

    @Test
    public void testGossipServerHandlerRespondsToHeartBeat() {
        EmbeddedChannel chan = constructTestStack(vss);

        chan.writeInbound(ByteBufUtil.writeAscii(ByteBufAllocator.DEFAULT, "HB"));

        assertThat(chan.outboundMessages().size()).isGreaterThanOrEqualTo(1);
    }

    @Test
    public void testGossipServerHandlerRespondsToSyncRequest() {
        VolatileStringStore localStore = new VolatileStringStore();

        EmbeddedChannel chan = constructTestStack(localStore);

        chan.writeInbound(ByteBufUtil.writeAscii(ByteBufAllocator.DEFAULT, "SY"));
        chan.writeInbound(localStore);

        assertThat(chan.outboundMessages().size()).isGreaterThanOrEqualTo(1);
    }

    @Test
    public void testGossipServerPrintsExceptionAndClosesStreamOnBadProtocol() {
        EmbeddedChannel channel = constructTestStack(vss);

        channel.writeInbound(ByteBufUtil.writeAscii(ByteBufAllocator.DEFAULT, "XX"));

        assertThat(channel.isOpen()).isFalse();
        assertThat(errContent.toString()).isNotBlank();

    }

    @Test
    public void testGossipServerIsPatientAndWaitsForMultipleBytes() throws InterruptedException {
        EmbeddedChannel channel = constructTestStack(vss);

        channel.writeInbound(ByteBufUtil.writeAscii(ByteBufAllocator.DEFAULT, "H"));

        Thread.sleep(1000);

        channel.writeInbound(ByteBufUtil.writeAscii(ByteBufAllocator.DEFAULT, "B"));

        assertThat(channel.outboundMessages().size()).isGreaterThanOrEqualTo(1);
    }

    public EmbeddedChannel constructTestStack(VolatileStringStore store) {
        return new EmbeddedChannel(new GossipServerHandler(store));
    }
}