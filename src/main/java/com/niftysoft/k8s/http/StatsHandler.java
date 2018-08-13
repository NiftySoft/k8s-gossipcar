package com.niftysoft.k8s.http;

import com.niftysoft.k8s.data.LifetimeStats;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufUtil;
import io.netty.handler.codec.http.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;

/** @author kalexmills */
public class StatsHandler extends HttpEndpointHandler {

    private static final String PLAINTEXT_UTF_8 = "text/plain; charset=UTF-8";

    @Override
    public FullHttpResponse handleRequest(FullHttpRequest req) {
        if (!req.method().name().equals("GET")) {
            return HttpResponseUtil.respond404(req);
        }

        if (req.headers().contains(HttpHeaderNames.ACCEPT)) {
            boolean canAcceptTextPlain =
                Arrays.asList(req.headers().get(HttpHeaderNames.ACCEPT).split("\\s*,\\s*"))
                    .stream().anyMatch((str) -> str.startsWith("text/plain"));

            if (!canAcceptTextPlain) HttpResponseUtil.respond406(req, PLAINTEXT_UTF_8);
        }

        if(req.headers().contains(HttpHeaderNames.ACCEPT_CHARSET)) {
            boolean canAcceptUtf8 =
                Arrays.asList(req.headers().get(HttpHeaderNames.ACCEPT).split("\\s*,\\s*"))
                    .stream().anyMatch((str) -> str.toLowerCase().startsWith("utf-8*"));

            if (!canAcceptUtf8) HttpResponseUtil.respond406(req, PLAINTEXT_UTF_8);
        }

        ByteBuf buf = ByteBufAllocator.DEFAULT.buffer();

        ByteBufUtil.writeUtf8(buf, "upTimeSecs=" +
                Duration.between(LifetimeStats.START_TIME, LocalDateTime.now()).getSeconds() + "\n");
        ByteBufUtil.writeUtf8(buf, "upSince=" + LifetimeStats.START_TIME.toString() + "\n");

        ByteBufUtil.writeUtf8(buf, "nOutSyncs=" + LifetimeStats.SUCCESSFUL_OUTGOING_SYNCS + "\n");
        ByteBufUtil.writeUtf8(buf, "nInSyncs=" + LifetimeStats.SUCCESSFUL_INCOMING_SYNCS + "\n");

        ByteBufUtil.writeUtf8(buf, "nBadHttpReqs=" + LifetimeStats.FAILED_HTTP_REQUESTS + "\n");
        ByteBufUtil.writeUtf8(buf, "nGoodHttpReqs=" + LifetimeStats.SUCCESSFUL_HTTP_REQUESTS + "\n");

        FullHttpResponse resp = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, buf);

        resp.headers().set(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.TEXT_PLAIN + "; charset=UTF-8");
        resp.headers().set(HttpHeaderNames.CONTENT_LENGTH, resp.content().readableBytes());

        return resp;
    }
}
