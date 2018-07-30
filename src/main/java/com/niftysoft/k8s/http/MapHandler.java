package com.niftysoft.k8s.http;

import com.niftysoft.k8s.data.stringstore.VolatileStringStore;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufHolder;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import io.netty.util.AsciiString;
import io.netty.util.CharsetUtil;
import org.apache.http.HttpStatus;

import java.util.List;

import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

public class

MapHandler extends HttpEndpointHandler {

    private static final AsciiString TEXT_PLAIN_UTF_8 =
            HttpHeaderValues.TEXT_PLAIN.concat("; ").concat(HttpHeaderValues.CHARSET).concat("=").concat("UTF-8");

    private final VolatileStringStore vss;

    public MapHandler(VolatileStringStore vss) {
        this.vss = vss;
    }

    @Override
    public FullHttpResponse handleRequest(FullHttpRequest req) {
        // This and all delegate methods must remain re-entrant with respect to the state of this object.
        switch(req.method().name()) {
            case "GET":
                return handleGet(req);
            case "PUT":
                return handlePut(req);
            default:
                return DefaultHttpHandler.respond404(req);
        }
    }

    public FullHttpResponse handleGet(FullHttpRequest req) {
        if(!queryParams.containsKey("k")) {
            return DefaultHttpHandler.respond422(req);
        }

        List<String> requestedKeys = queryParams.get("k");
        for (int i = 0 ; i < requestedKeys.size(); ++i) {
            if (!vss.containsKey(requestedKeys.get(i))) {
                requestedKeys.remove(i--);
            }
        }

        if (requestedKeys.isEmpty()) {
            return DefaultHttpHandler.respond404(req);
        }


        // TODO: Don't use strings. Keep everything in ByteBuf for performance.
        StringBuffer buffer = new StringBuffer();
        for (String key : requestedKeys) {
            buffer.append(key);
            buffer.append('=');
            buffer.append(vss.get(key));
            buffer.append('\n');
        }
        FullHttpResponse resp = new DefaultFullHttpResponse(HTTP_1_1, HttpResponseStatus.OK,
                ByteBufUtil.writeUtf8(ByteBufAllocator.DEFAULT, buffer));

        resp.setStatus(HttpResponseStatus.OK);
        resp.headers().set(HttpHeaderNames.CONTENT_TYPE, TEXT_PLAIN_UTF_8);
        return resp;
    }

    public FullHttpResponse handlePut(FullHttpRequest req) {
        if (queryParams.size() != 1) {
            return DefaultHttpHandler.respond422(req);
        }

        String key = queryParams.get("k").get(0);

        FullHttpResponse resp = new DefaultFullHttpResponse(HTTP_1_1, HttpResponseStatus.OK);

        if (vss.containsKey(key))
            resp.setStatus(HttpResponseStatus.NO_CONTENT);
        else
            resp.setStatus(HttpResponseStatus.CREATED);

        String content = req.content().toString(CharsetUtil.UTF_8);

        vss.put(key, content);

        resp.headers().set(HttpHeaderNames.CONTENT_TYPE, TEXT_PLAIN_UTF_8);
        return resp;
    }
}
