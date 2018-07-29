package com.niftysoft.k8s.http;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.router.RouteResult;
import io.netty.handler.codec.http.router.Router;

import static io.netty.handler.codec.http.HttpResponseStatus.CONTINUE;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

public class HttpRouteHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private Router<HttpEndpointHandler> router;

    public HttpRouteHandler(Router<HttpEndpointHandler> router) {
        this.router = router;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest req) throws Exception {
        if (HttpUtil.is100ContinueExpected(req)) {
            send100Continue(ctx);
            return;
        }

        RouteResult<HttpEndpointHandler> resolvedRoute = router.route(req.method(),req.uri());

        resolvedRoute.target().setPathParams(resolvedRoute.pathParams());
        resolvedRoute.target().setQueryParams(resolvedRoute.queryParams());

        FullHttpResponse resp = new DefaultFullHttpResponse(req.protocolVersion(), INTERNAL_SERVER_ERROR);

        boolean keepAlive = HttpUtil.isKeepAlive(req);
        if (keepAlive) resp.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);

        resolvedRoute.target().handleRequest(ctx, req, resp);

        ChannelFuture future = ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
        if (!keepAlive) {
            future.addListener(ChannelFutureListener.CLOSE);
        }
    }

    private static void send100Continue(ChannelHandlerContext ctx) {
        FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, CONTINUE);
        ctx.write(response);
    }
}
