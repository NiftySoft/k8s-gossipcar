package com.niftysoft.k8s;

import com.niftysoft.k8s.data.LifetimeStats;
import com.niftysoft.k8s.http.HttpEndpointHandler;
import com.niftysoft.k8s.http.HttpRouteHandler;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.router.Router;
import org.junit.Test;

import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LifetimeStatsIT {

    @Test
    public void testLifetimeStatFailedHttpRequestsIncrementsOnRouteHandler() throws Exception {
        ChannelHandlerContext ctx = mock(ChannelHandlerContext.class);

        when(ctx.writeAndFlush(any())).thenReturn(mock(ChannelFuture.class));

        FullHttpRequest req = new DefaultFullHttpRequest(HTTP_1_1, HttpMethod.GET, "/");

        Router<HttpEndpointHandler> router = new Router<>();
        HttpRouteHandler handler = new HttpRouteHandler(router);

        long prevValue = LifetimeStats.FAILED_HTTP_REQUESTS.longValue();

        handler.channelRead0(ctx, req);

        assertThat(prevValue + 1).isEqualTo(LifetimeStats.FAILED_HTTP_REQUESTS.longValue());
    }

    @Test
    public void testLifetimeStatSuccessfulHttpRequestsIncrementsAtRouteHandler() throws Exception {
        ChannelHandlerContext ctx = mock(ChannelHandlerContext.class);

        when(ctx.writeAndFlush(any())).thenReturn(mock(ChannelFuture.class));

        FullHttpRequest req = new DefaultFullHttpRequest(HTTP_1_1, HttpMethod.GET, "/");

        Router<HttpEndpointHandler> router = new Router<>();
        router.addRoute(HttpMethod.GET, "/", new HttpEndpointHandler() {
            @Override
            public FullHttpResponse handleRequest(FullHttpRequest req) {
                return new DefaultFullHttpResponse(HTTP_1_1, HttpResponseStatus.OK);
            }
        });
        HttpRouteHandler handler = new HttpRouteHandler(router);

        long prevValue = LifetimeStats.SUCCESSFUL_HTTP_REQUESTS.longValue();

        handler.channelRead0(ctx, req);

        assertThat(prevValue + 1).isEqualTo(LifetimeStats.SUCCESSFUL_HTTP_REQUESTS.longValue());
    }
}
