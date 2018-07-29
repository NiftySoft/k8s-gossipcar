package com.niftysoft.k8s.http;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;

public class DefaultHttpHandler {

    public static void respond404(ChannelHandlerContext ctx, FullHttpRequest req, FullHttpResponse resp) {
        resp.setStatus(HttpResponseStatus.NOT_FOUND);
        ctx.write(resp);
    }

    public static void respond422(ChannelHandlerContext ctx, FullHttpRequest req, FullHttpResponse resp) {
        resp.setStatus(HttpResponseStatus.UNPROCESSABLE_ENTITY);
        ctx.write(resp);
    }
}
