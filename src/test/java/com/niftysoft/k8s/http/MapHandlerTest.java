package com.niftysoft.k8s.http;

import com.niftysoft.k8s.data.stringstore.VolatileStringStore;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.router.Router;
import org.junit.Before;
import org.junit.Test;

import java.nio.charset.Charset;

import static org.assertj.core.api.Assertions.assertThat;

public class MapHandlerTest {

    VolatileStringStore vss;

    @Before
    public void before() {
        vss = new VolatileStringStore();
    }

    @Test
    public void testMapHandlerAcceptsGetRequests() throws Exception {
        vss.put("key", "value");

        EmbeddedChannel chan = constructTestStack(vss);

        FullHttpRequest req = constructRequest();

        req.setUri("/a?k=key");
        req.setMethod(HttpMethod.GET);

        chan.writeInbound(req);
        chan.flush();

        assertThat(chan.outboundMessages().size()).isEqualTo(1);

        FullHttpResponse resp = pollHttpResponse(chan);

        assertThat(resp.status().code()).isEqualTo(200);
        assertThat(resp.headers().contains(HttpHeaderNames.CONTENT_TYPE)).isTrue();
        assertThat(resp.headers().get(HttpHeaderNames.CONTENT_TYPE)).isEqualTo("text/plain; charset=UTF-8");

        ByteBuf buf = resp.content();
        String content = new String(ByteBufUtil.getBytes(buf), Charset.forName("UTF-8"));

        assertThat(content).isEqualTo("key=value\n");
    }

    @Test
    public void testMapHandlerAcceptsMultipleKeysInGetRequests() throws Exception {
        vss.put("key1", "value1");
        vss.put("key2", "value2");
        vss.put("key3", "value3");

        EmbeddedChannel chan = constructTestStack(vss);

        FullHttpRequest req = constructRequest();

        req.setUri("/a?k=key1&k=key2&k=key3");
        req.setMethod(HttpMethod.GET);

        chan.writeInbound(req);
        chan.flush();

        assertThat(chan.outboundMessages().size()).isEqualTo(1);

        FullHttpResponse resp = pollHttpResponse(chan);

        assertThat(resp.status().code()).isEqualTo(200);
        assertThat(resp.headers().contains(HttpHeaderNames.CONTENT_TYPE)).isTrue();
        assertThat(resp.headers().get(HttpHeaderNames.CONTENT_TYPE)).isEqualTo("text/plain; charset=UTF-8");

        ByteBuf buf = resp.content();
        String content = new String(ByteBufUtil.getBytes(buf), Charset.forName("UTF-8"));

        assertThat(content).isEqualTo("key1=value1\nkey2=value2\nkey3=value3\n");
    }

    @Test
    public void testMapHandlerAcceptsGetAndIgnoresMissingKeys() throws Exception {
        vss.put("key1", "value1");
        vss.put("key3", "value3");

        EmbeddedChannel chan = constructTestStack(vss);

        FullHttpRequest req = constructRequest();

        req.setUri("/a?k=key1&k=key2&k=key3");
        req.setMethod(HttpMethod.GET);

        chan.writeInbound(req);
        chan.flush();

        assertThat(chan.outboundMessages().size()).isEqualTo(1);

        FullHttpResponse resp = pollHttpResponse(chan);

        assertThat(resp.status().code()).isEqualTo(200);
        assertThat(resp.headers().contains(HttpHeaderNames.CONTENT_TYPE)).isTrue();
        assertThat(resp.headers().get(HttpHeaderNames.CONTENT_TYPE)).isEqualTo("text/plain; charset=UTF-8");

        ByteBuf buf = resp.content();
        String content = new String(ByteBufUtil.getBytes(buf), Charset.forName("UTF-8"));

        assertThat(content).isEqualTo("key1=value1\nkey3=value3\n");
    }

    @Test
    public void testMapHandlerAcceptsPutRequests() throws Exception {
        EmbeddedChannel chan = constructTestStack(vss);

        FullHttpRequest req = constructRequest();

        req.setUri("/a?k=key");
        req.setMethod(HttpMethod.PUT);
        ByteBufUtil.writeUtf8(req.content(), "value");

        chan.writeInbound(req);
        chan.flush();

        assertThat(chan.outboundMessages().size()).isEqualTo(1);

        FullHttpResponse resp = pollHttpResponse(chan);

        assertThat(resp.status().code()).isEqualTo(201);
        assertThat(resp.headers().contains(HttpHeaderNames.CONTENT_TYPE)).isTrue();
        assertThat(resp.headers().get(HttpHeaderNames.CONTENT_TYPE)).isEqualTo("text/plain; charset=UTF-8");

        assertThat(vss.get("key")).isEqualTo("value");
    }

    @Test
    public void testMapHandlerResponds404ToNonGetOrPutRequests() {
        EmbeddedChannel chan = constructTestStack(vss);

        assertRequestTypeReturns404WithEmptyContent(chan, HttpMethod.POST);
        assertRequestTypeReturns404WithEmptyContent(chan, HttpMethod.DELETE);
        assertRequestTypeReturns404WithEmptyContent(chan, HttpMethod.CONNECT);
        assertRequestTypeReturns404WithEmptyContent(chan, HttpMethod.HEAD);
        assertRequestTypeReturns404WithEmptyContent(chan, HttpMethod.OPTIONS);
        assertRequestTypeReturns404WithEmptyContent(chan, HttpMethod.PATCH);
        assertRequestTypeReturns404WithEmptyContent(chan, HttpMethod.TRACE);
    }

    private void assertRequestTypeReturns404WithEmptyContent(EmbeddedChannel chan, HttpMethod method) {
        FullHttpRequest req = constructRequest();

        req.setUri("/a?k=key");
        req.setMethod(method);
        ByteBufUtil.writeUtf8(req.content(), "value");

        chan.writeInbound(req);
        chan.flush();

        assertThat(chan.outboundMessages().size()).isEqualTo(1);

        FullHttpResponse resp = pollHttpResponse(chan);

        assertThat(resp.status().code()).isEqualTo(404);
    }

    public FullHttpResponse pollHttpResponse(EmbeddedChannel chan) {
        return (FullHttpResponse) chan.outboundMessages().poll();
    }

    public EmbeddedChannel constructTestStack(VolatileStringStore vss) {
        MapHandler handler = new MapHandler(vss);
        Router<HttpEndpointHandler> router = new Router<>();
        router.addRoute(HttpMethod.GET, "/a", handler);
        router.addRoute(HttpMethod.PUT, "/a", handler);

        return new EmbeddedChannel(new HttpRouteHandler(router));
    }

    public FullHttpRequest constructRequest() {
        return new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/");
    }
}
