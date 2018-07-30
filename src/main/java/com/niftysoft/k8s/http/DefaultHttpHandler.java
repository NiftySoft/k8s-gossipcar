package com.niftysoft.k8s.http;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;

/**
 * @author K. Alex Mills
 */
public class DefaultHttpHandler {


    public static FullHttpResponse respond404(FullHttpRequest req) {
        return new DefaultFullHttpResponse(req.protocolVersion(), HttpResponseStatus.NOT_FOUND);
    }

    public static FullHttpResponse respond422(FullHttpRequest req) {
        return new DefaultFullHttpResponse(req.protocolVersion(), HttpResponseStatus.UNPROCESSABLE_ENTITY);
    }
}
