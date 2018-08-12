package com.niftysoft.k8s.http;

import com.niftysoft.k8s.data.LifetimeStats;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.router.Router;
import org.junit.Test;
import org.mockito.Mockito;

import java.nio.charset.Charset;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

public class StatsHandlerTest {

  @Test
  public void testStatsHandlerRespondsToGetRequests() throws Exception {
    EmbeddedChannel chan = constructTestStack();

    FullHttpRequest req = HttpTestUtil.constructRequest();
    req.setUri("/stats");

    chan.writeInbound(req);
    chan.flush();

    assertThat(chan.outboundMessages().size()).isEqualTo(1);

    FullHttpResponse resp = HttpTestUtil.pollHttpResponse(chan);

    assertThat(resp.status().code()).isEqualTo(200);
    assertThat(resp.headers().contains(HttpHeaderNames.CONTENT_TYPE)).isTrue();
    assertThat(resp.headers().get(HttpHeaderNames.CONTENT_TYPE))
        .isEqualTo("text/plain; charset=UTF-8");

    ByteBuf buf = resp.content();
    String content = new String(ByteBufUtil.getBytes(buf), Charset.forName("UTF-8"));

    assertThat(content).isNotEmpty();
  }

  @Test
  public void testStatsHandlerReturnsCurrentRepresentationofLifetimeStats() throws Exception {
    EmbeddedChannel chan = constructTestStack();

    FullHttpRequest req = HttpTestUtil.constructRequest();
    req.setUri("/stats");

    chan.writeInbound(req);
    chan.flush();

    assertThat(chan.outboundMessages().size()).isEqualTo(1);

    FullHttpResponse resp = HttpTestUtil.pollHttpResponse(chan);

    assertThat(resp.status().code()).isEqualTo(200);

    ByteBuf buf = resp.content();
    String content = new String(ByteBufUtil.getBytes(buf), Charset.forName("UTF-8"));

    assertThat(content).isNotEmpty();

    String[] stats = content.split("\n");

    Map<String, String> statsMap = new HashMap<>();
    for (String stat : stats) {
      String[] kv = stat.split("=");
      assertThat(kv.length).isEqualTo(2);

      statsMap.put(kv[0], kv[1]);
    }

    for (final Map.Entry<String, String> stat: statsMap.entrySet()) {
      switch (stat.getKey()) {
        case "upTimeSecs":
          assertThat(Integer.valueOf(stat.getValue())).isNotNegative();
          break;
        case "upSince":
          assertThatCode(() -> {
            LocalDateTime date = LocalDateTime.parse(stat.getValue());
            assertThat(date).isEqualToIgnoringMinutes(LifetimeStats.START_TIME);
          }).doesNotThrowAnyException();
          break;
        case "nOutSyncs":
          assertThat(stat.getValue()).isEqualTo("" + LifetimeStats.SUCCESSFUL_OUTGOING_SYNCS);
          break;
        case "nInSyncs":
          assertThat(stat.getValue()).isEqualTo("" + LifetimeStats.SUCCESSFUL_INCOMING_SYNCS);
          break;
        case "nBadHttpReqs":
          assertThat(stat.getValue()).isEqualTo("" + LifetimeStats.FAILED_HTTP_REQUESTS);
          break;
        case "nGoodHttpReqs":
          // Subtract the succesful HTTP request represented by the stats request itself.
          assertThat(stat.getValue()).isEqualTo("" + (LifetimeStats.SUCCESSFUL_HTTP_REQUESTS.intValue() - 1));
          break;
        default:
          fail("Unexpected stat returned by StatsHandler " + stat.getKey());
          break;
      }
    }
  }

  @Test
  public void testMapHandlerResponds404ToNonGetRequests() {
    EmbeddedChannel chan;

    chan = constructTestStack();
    HttpTestUtil.assertRequestTypeReturns404WithEmptyContent(chan, HttpMethod.POST, "/stats");
    chan = constructTestStack();
    HttpTestUtil.assertRequestTypeReturns404WithEmptyContent(chan, HttpMethod.DELETE, "/stats");
    chan = constructTestStack();
    HttpTestUtil.assertRequestTypeReturns404WithEmptyContent(chan, HttpMethod.CONNECT, "/stats");
    chan = constructTestStack();
    HttpTestUtil.assertRequestTypeReturns404WithEmptyContent(chan, HttpMethod.HEAD, "/stats");
    chan = constructTestStack();
    HttpTestUtil.assertRequestTypeReturns404WithEmptyContent(chan, HttpMethod.OPTIONS, "/stats");
    chan = constructTestStack();
    HttpTestUtil.assertRequestTypeReturns404WithEmptyContent(chan, HttpMethod.PATCH, "/stats");
    chan = constructTestStack();
    HttpTestUtil.assertRequestTypeReturns404WithEmptyContent(chan, HttpMethod.TRACE, "/stats");
  }

  public EmbeddedChannel constructTestStack() {
    StatsHandler handler = new StatsHandler();
    Router<HttpEndpointHandler> router = new Router<>();
    router.addRoute(HttpMethod.GET, "/stats", handler);

    return new EmbeddedChannel(new HttpRouteHandler(router));
  }

}
