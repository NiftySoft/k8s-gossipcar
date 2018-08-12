package com.niftysoft.k8s.http;

import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.*;

import static org.assertj.core.api.Assertions.assertThat;

public class HttpTestUtil {
    public static void assertRequestTypeReturns404WithEmptyContent(EmbeddedChannel chan, HttpMethod method, String uri) {
        FullHttpRequest req = constructRequest();

        req.setUri(uri);
        req.setMethod(method);

        chan.writeInbound(req);
        chan.flush();

        assertThat(chan.outboundMessages().size()).isEqualTo(1);

        FullHttpResponse resp = pollHttpResponse(chan);

        assertThat(resp.status().code()).isEqualTo(404);
    }

    public static FullHttpResponse pollHttpResponse(EmbeddedChannel chan) {
        return (FullHttpResponse) chan.outboundMessages().poll();
    }

    public static FullHttpRequest constructRequest() {
        return new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/stats");
    }
}
