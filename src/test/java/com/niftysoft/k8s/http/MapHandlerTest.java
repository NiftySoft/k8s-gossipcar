package com.niftysoft.k8s.http;

import com.niftysoft.k8s.data.VolatileByteStore;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.router.Router;
import org.junit.Before;
import org.junit.Test;

import java.nio.charset.Charset;

import static org.assertj.core.api.Assertions.assertThat;

public class MapHandlerTest {

  private VolatileByteStore vss;

  @Before
  public void before() {
    vss = new VolatileByteStore();
  }

  @Test
  public void testMapHandlerAcceptsGetRequests() throws Exception {
    vss.put("key", "value".getBytes());

    EmbeddedChannel chan = constructTestStack(vss);

    FullHttpRequest req = HttpTestUtil.constructRequest();

    req.setUri("/a?k=key");
    req.setMethod(HttpMethod.GET);

    chan.writeInbound(req);
    chan.flush();

    assertThat(chan.outboundMessages().size()).isEqualTo(1);

    FullHttpResponse resp = HttpTestUtil.pollHttpResponse(chan);

    assertThat(resp.status().code()).isEqualTo(200);
    assertThat(resp.headers().contains(HttpHeaderNames.CONTENT_TYPE)).isTrue();
    assertThat(resp.headers().get(HttpHeaderNames.CONTENT_TYPE))
        .isEqualTo("application/octet-stream");

    ByteBuf buf = resp.content();
    String content = new String(ByteBufUtil.getBytes(buf), Charset.forName("UTF-8"));

    assertThat(content).isEqualTo("key=value\n");
  }

  @Test
  public void testMapHandlerAcceptsMultipleKeysInGetRequests() throws Exception {
    vss.put("key1", "value1".getBytes());
    vss.put("key2", "value2".getBytes());
    vss.put("key3", "value3".getBytes());

    EmbeddedChannel chan = constructTestStack(vss);

    FullHttpRequest req = HttpTestUtil.constructRequest();

    req.setUri("/a?k=key1&k=key2&k=key3");
    req.setMethod(HttpMethod.GET);

    chan.writeInbound(req);
    chan.flush();

    assertThat(chan.outboundMessages().size()).isEqualTo(1);

    FullHttpResponse resp = HttpTestUtil.pollHttpResponse(chan);

    assertThat(resp.status().code()).isEqualTo(200);
    assertThat(resp.headers().contains(HttpHeaderNames.CONTENT_TYPE)).isTrue();
    assertThat(resp.headers().get(HttpHeaderNames.CONTENT_TYPE))
        .isEqualTo("application/octet-stream");

    ByteBuf buf = resp.content();
    String content = new String(ByteBufUtil.getBytes(buf), Charset.forName("UTF-8"));

    assertThat(content).isEqualTo("key1=value1\nkey2=value2\nkey3=value3\n");
  }

  @Test
  public void testMapHandlerAcceptsGetAndIgnoresMissingKeys() throws Exception {
    vss.put("key1", "value1".getBytes());
    vss.put("key3", "value3".getBytes());

    EmbeddedChannel chan = constructTestStack(vss);

    FullHttpRequest req = HttpTestUtil.constructRequest();

    req.setUri("/a?k=key1&k=key2&k=key3");
    req.setMethod(HttpMethod.GET);

    chan.writeInbound(req);
    chan.flush();

    assertThat(chan.outboundMessages().size()).isEqualTo(1);

    FullHttpResponse resp = HttpTestUtil.pollHttpResponse(chan);

    assertThat(resp.status().code()).isEqualTo(200);
    assertThat(resp.headers().contains(HttpHeaderNames.CONTENT_TYPE)).isTrue();
    assertThat(resp.headers().get(HttpHeaderNames.CONTENT_TYPE))
        .isEqualTo("application/octet-stream");

    ByteBuf buf = resp.content();
    String content = new String(ByteBufUtil.getBytes(buf), Charset.forName("UTF-8"));

    assertThat(content).isEqualTo("key1=value1\nkey3=value3\n");
  }

  @Test
  public void testMapHandlerAcceptsPutRequests() throws Exception {
    EmbeddedChannel chan = constructTestStack(vss);

    FullHttpRequest req = HttpTestUtil.constructRequest();

    req.setUri("/a?k=key");
    req.setMethod(HttpMethod.PUT);
    ByteBufUtil.writeUtf8(req.content(), "value");

    chan.writeInbound(req);
    chan.flush();

    assertThat(chan.outboundMessages().size()).isEqualTo(1);

    FullHttpResponse resp = HttpTestUtil.pollHttpResponse(chan);

    assertThat(resp.status().code()).isEqualTo(201);

    assertThat(vss.get("key")).isEqualTo("value".getBytes());
  }

  @Test
  public void testMapHandlerResponds404ToNonGetOrPutRequests() {
    EmbeddedChannel chan;

    chan = constructTestStack(vss);
    HttpTestUtil.assertRequestTypeReturns404WithEmptyContent(chan, HttpMethod.POST, "/a?k=key");
    chan = constructTestStack(vss);
    HttpTestUtil.assertRequestTypeReturns404WithEmptyContent(chan, HttpMethod.DELETE, "/a?k=key");
    chan = constructTestStack(vss);
    HttpTestUtil.assertRequestTypeReturns404WithEmptyContent(chan, HttpMethod.CONNECT, "/a?k=key");
    chan = constructTestStack(vss);
    HttpTestUtil.assertRequestTypeReturns404WithEmptyContent(chan, HttpMethod.HEAD, "/a?k=key");
    chan = constructTestStack(vss);
    HttpTestUtil.assertRequestTypeReturns404WithEmptyContent(chan, HttpMethod.OPTIONS, "/a?k=key");
    chan = constructTestStack(vss);
    HttpTestUtil.assertRequestTypeReturns404WithEmptyContent(chan, HttpMethod.PATCH, "/a?k=key");
    chan = constructTestStack(vss);
    HttpTestUtil.assertRequestTypeReturns404WithEmptyContent(chan, HttpMethod.TRACE, "/a?k=key");
  }

  public EmbeddedChannel constructTestStack(VolatileByteStore vss) {
    MapHandler handler = new MapHandler(vss);
    Router<HttpEndpointHandler> router = new Router<>();
    router.addRoute(HttpMethod.GET, "/a", handler);
    router.addRoute(HttpMethod.PUT, "/a", handler);

    return new EmbeddedChannel(new HttpRouteHandler(router));
  }

}
