package com.niftysoft.k8s.http;

import com.niftysoft.k8s.data.LifetimeStats;
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

/** @author kalexmills */
public class HttpRouteHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

  private Router<HttpEndpointHandler> router;

  public HttpRouteHandler(Router<HttpEndpointHandler> router) {
    this.router = router;
  }

  @Override
  public void channelRead0(ChannelHandlerContext ctx, FullHttpRequest req) throws Exception {
    FullHttpResponse resp = null;

    try {
      if (HttpUtil.is100ContinueExpected(req)) {
        resp = new DefaultFullHttpResponse(HTTP_1_1, CONTINUE);
        return;
      }

      RouteResult<HttpEndpointHandler> resolvedRoute = router.route(req.method(), req.uri());
      if (resolvedRoute == null) {
        resp = HttpResponseUtil.respond404(req);
        return;
      }

      resolvedRoute.target().setPathParams(resolvedRoute.pathParams());
      resolvedRoute.target().setQueryParams(resolvedRoute.queryParams());

      resp = resolvedRoute.target().handleRequest(req);

    } finally {
      if (resp == null) resp = new DefaultFullHttpResponse(HTTP_1_1, INTERNAL_SERVER_ERROR);

      if (!resp.status().codeClass().equals(HttpStatusClass.SUCCESS)) {
        LifetimeStats.FAILED_HTTP_REQUESTS.increment();
      } else {
        LifetimeStats.SUCCESSFUL_HTTP_REQUESTS.increment();
      }

      ctx.writeAndFlush(resp).addListener(ChannelFutureListener.CLOSE);
    }
  }
}
