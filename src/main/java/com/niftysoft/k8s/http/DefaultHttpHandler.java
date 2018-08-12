package com.niftysoft.k8s.http;

import com.niftysoft.k8s.data.LifetimeStats;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.EmptyByteBuf;
import io.netty.handler.codec.http.*;

/** @author kalexmills */
public class DefaultHttpHandler {

  public static FullHttpResponse respond404(FullHttpRequest req) {
    return new DefaultFullHttpResponse(req.protocolVersion(), HttpResponseStatus.NOT_FOUND);
  }

  public static FullHttpResponse respond422(FullHttpRequest req) {
    return new DefaultFullHttpResponse(
        req.protocolVersion(), HttpResponseStatus.UNPROCESSABLE_ENTITY);
  }

  public static FullHttpResponse respond406(FullHttpRequest req, String contentType) {
    return new DefaultFullHttpResponse(req.protocolVersion(),
            HttpResponseStatus.NOT_ACCEPTABLE,
            new EmptyByteBuf(ByteBufAllocator.DEFAULT),
            new DefaultHttpHeaders().add(HttpHeaderNames.CONTENT_TYPE, contentType),
            EmptyHttpHeaders.INSTANCE);
  }
}
