package com.niftysoft.k8s.http;

import com.niftysoft.k8s.data.VolatileByteStore;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufUtil;
import io.netty.handler.codec.http.*;

import java.util.List;

import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

/** @author kalexmills */
public class MapHandler extends HttpEndpointHandler {

  private final VolatileByteStore vss;

  public MapHandler(VolatileByteStore vss) {
    this.vss = vss;
  }

  @Override
  public FullHttpResponse handleRequest(FullHttpRequest req) {
    // This and all delegate methods must remain re-entrant with respect to the state of this
    // object.
    switch (req.method().name()) {
      case "GET":
        return handleGet(req);
      case "PUT":
        return handlePut(req);
      default:
        return DefaultHttpHandler.respond404(req);
    }
  }

  public FullHttpResponse handleGet(FullHttpRequest req) {
    if (!queryParams.containsKey("k") || !queryParams.containsKey("k")) {
      return DefaultHttpHandler.respond422(req);
    }

    List<String> requestedKeys = queryParams.get("k");
    for (int i = 0; i < requestedKeys.size(); ++i) {
      if (!vss.containsKey(requestedKeys.get(i))) {
        requestedKeys.remove(i--);
      }
    }

    if (requestedKeys.isEmpty()) {
      return DefaultHttpHandler.respond404(req);
    }

    ByteBuf buf = ByteBufAllocator.DEFAULT.buffer();

    for (String key : requestedKeys) {
      ByteBufUtil.writeUtf8(buf,key+"=");
      buf.writeBytes(vss.get(key));
      ByteBufUtil.writeUtf8(buf, "\n");
    }
    FullHttpResponse resp = new DefaultFullHttpResponse(HTTP_1_1, HttpResponseStatus.OK, buf);

    resp.headers().set(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_OCTET_STREAM);
    resp.headers().set(HttpHeaderNames.CONTENT_LENGTH, resp.content().readableBytes());
    return resp;
  }

  public FullHttpResponse handlePut(FullHttpRequest req) {
    if (queryParams.size() != 1) {
      return DefaultHttpHandler.respond422(req);
    }

    String key = queryParams.get("k").get(0);

    FullHttpResponse resp = new DefaultFullHttpResponse(HTTP_1_1, HttpResponseStatus.OK);

    if (vss.containsKey(key)) resp.setStatus(HttpResponseStatus.NO_CONTENT);
    else resp.setStatus(HttpResponseStatus.CREATED);

    ByteBuf content = req.content();
    byte[] bytes = new byte[content.readableBytes()];
    content.readBytes(bytes);
    vss.put(key, bytes);

    resp.headers().set(HttpHeaderNames.CONTENT_LENGTH, 0);
    return resp;
  }
}
