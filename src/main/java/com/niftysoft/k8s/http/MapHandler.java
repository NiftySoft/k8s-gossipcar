package com.niftysoft.k8s.http;

import com.niftysoft.k8s.data.stringstore.VolatileStringStore;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import io.netty.util.AsciiString;
import io.netty.util.CharsetUtil;

import java.util.List;

public class MapHandler extends HttpEndpointHandler {

    private static final AsciiString TEXT_PLAIN_UTF_8 =
            HttpHeaderValues.TEXT_PLAIN.concat("; ").concat(HttpHeaderValues.CHARSET).concat("=").concat("UTF-8");

    private final VolatileStringStore vss;

    public MapHandler(VolatileStringStore vss) {
        this.vss = vss;
    }

    @Override
    public void handleRequest(ChannelHandlerContext ctx, FullHttpRequest req, FullHttpResponse resp) {
        // This and all delegate methods must remain re-entrant with respect to the state of this object.
        switch(req.method().name()) {
            case "GET":
                handleGet(ctx, req, resp);
                return;
            case "PUT":
                handlePut(ctx, req, resp);
                return;
            default:
                DefaultHttpHandler.respond404(ctx, req, resp);
                return;
        }
    }

    public void handleGet(ChannelHandlerContext ctx, FullHttpRequest req, FullHttpResponse resp) {
        if(!queryParams.containsKey("k")) {
            DefaultHttpHandler.respond422(ctx, req, resp);
            return;
        }

        List<String> requestedKeys = queryParams.get("k");
        for (int i = 0 ; i < requestedKeys.size(); ++i) {
            if (!vss.containsKey(requestedKeys.get(i))) {
                requestedKeys.remove(i--);
            }
        }

        if (requestedKeys.isEmpty()) {
            DefaultHttpHandler.respond404(ctx, req, resp);
            return;
        }

        resp.setStatus(HttpResponseStatus.OK);
        resp.headers().set(HttpHeaderNames.CONTENT_TYPE, TEXT_PLAIN_UTF_8);
        ctx.write(resp);

        // TODO: Don't use strings. Keep everything in ByteBuf for performance.
        StringBuffer buffer = new StringBuffer();
        for (String key : requestedKeys) {
            buffer.append(key);
            buffer.append('=');
            buffer.append(vss.get(key));
            buffer.append('\n');
        }
        ctx.write(ByteBufUtil.writeUtf8(ByteBufAllocator.DEFAULT, buffer));
    }

    public void handlePut(ChannelHandlerContext ctx, FullHttpRequest req, FullHttpResponse resp) {
        if (queryParams.size() != 1) {
            DefaultHttpHandler.respond422(ctx, req, resp);
            return;
        }

        String key = queryParams.get("k").get(0);

        if (vss.containsKey(key))
            resp.setStatus(HttpResponseStatus.NO_CONTENT);
        else
            resp.setStatus(HttpResponseStatus.CREATED);

        String content = req.content().toString(CharsetUtil.UTF_8);

        vss.put(key, content);

        resp.headers().set(HttpHeaderNames.CONTENT_TYPE, TEXT_PLAIN_UTF_8);
        ctx.write(resp);
    }
}
