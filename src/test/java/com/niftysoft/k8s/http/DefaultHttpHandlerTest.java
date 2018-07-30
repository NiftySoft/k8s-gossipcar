package com.niftysoft.k8s.http;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
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

@RunWith(MockitoJUnitRunner.class)
public class DefaultHttpHandlerTest {

    private ChannelHandlerContext ctx = mock(ChannelHandlerContext.class);
    private FullHttpRequest req = mock(FullHttpRequest.class);
    private FullHttpResponse resp = mock(FullHttpResponse.class);

    @Captor
    private ArgumentCaptor<HttpResponseStatus> statusCaptor;

    @Captor
    private ArgumentCaptor<FullHttpResponse> responseCaptor;

    @Before
    public void before() {
        Mockito.reset();
    }

    @Test
    public void test404Handler() {
        DefaultHttpHandler.respond404(ctx, req, resp);

        verify(resp).setStatus(statusCaptor.capture());

        assertThat(statusCaptor.getValue().code()).isEqualTo(404);

        verify(ctx).write(responseCaptor.capture());

        assertThat(responseCaptor.getValue()).isEqualTo(resp);
    }

    @Test
    public void test422Handler() {
        DefaultHttpHandler.respond422(ctx, req, resp);

        verify(resp).setStatus(statusCaptor.capture());

        assertThat(statusCaptor.getValue().code()).isEqualTo(422);

        verify(ctx).write(responseCaptor.capture());

        assertThat(responseCaptor.getValue()).isEqualTo(resp);
    }
}
