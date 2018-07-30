package com.niftysoft.k8s.http;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DefaultHttpHandlerTest {

    @Test
    public void test404Handler() {
        FullHttpRequest req = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/");
        FullHttpResponse resp = DefaultHttpHandler.respond404(req);
        assertThat(resp.status().code()).isEqualTo(404);
        assertThat(resp.protocolVersion()).isEqualTo(req.protocolVersion());
    }

    @Test
    public void test422Handler() {
        FullHttpRequest req = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/");
        FullHttpResponse resp = DefaultHttpHandler.respond422(req);
        assertThat(resp.status().code()).isEqualTo(422);
        assertThat(resp.protocolVersion()).isEqualTo(req.protocolVersion());
    }
}
