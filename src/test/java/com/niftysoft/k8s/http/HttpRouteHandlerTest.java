package com.niftysoft.k8s.http;

import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.router.Router;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class HttpRouteHandlerTest {

  @Captor private ArgumentCaptor<FullHttpRequest> requestCaptor;

  @Test
  public void testHttpRouteHandlerSends100Continue() {
    EmbeddedChannel chan = constructTestStack(new Router<>());

    FullHttpRequest req = constructRequest();
    req.headers().set(HttpHeaderNames.EXPECT, HttpHeaderValues.CONTINUE);

    chan.writeInbound(req);
    chan.flush();

    assertThat(chan.outboundMessages().size()).isEqualTo(1);

    Object obj = chan.outboundMessages().poll();
    assertThat(obj)
        .hasSameClassAs(new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK));
    FullHttpResponse resp = (FullHttpResponse) obj;

    assertThat(resp.status()).isEqualTo(HttpResponseStatus.CONTINUE);
  }

  @Test
  public void testHttpRouteHandlerHandlesRoutesAsRequested() {
    HttpEndpointHandler endpoint1 = mock(HttpEndpointHandler.class);
    when(endpoint1.handleRequest(any()))
        .thenReturn(new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK));

    HttpEndpointHandler endpoint2 = mock(HttpEndpointHandler.class);
    when(endpoint2.handleRequest(any()))
        .thenReturn(new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK));

    Router<HttpEndpointHandler> router = new Router<>();

    router.addRoute(HttpMethod.GET, "/this/is/a/route", endpoint1);
    router.addRoute(HttpMethod.PUT, "/another/route", endpoint2);

    EmbeddedChannel chan = constructTestStack(router);

    FullHttpRequest req1 = constructRequest();
    req1.setUri("/this/is/a/route");
    req1.setMethod(HttpMethod.GET);

    chan.writeInbound(req1);
    chan.flush();

    verify(endpoint1).handleRequest(requestCaptor.capture());
    assertThat(requestCaptor.getValue()).isEqualTo(req1);

    chan = constructTestStack(router);

    FullHttpRequest req2 = constructRequest();
    req2.setUri("/another/route");
    req2.setMethod(HttpMethod.PUT);

    chan.writeInbound(req2);
    chan.flush();

    verify(endpoint2).handleRequest(requestCaptor.capture());
    assertThat(requestCaptor.getValue()).isEqualTo(req2);
  }

  @Test
  public void testHttpRouteHandlerReturns404ForUnknownRoutes() {
    Router<HttpEndpointHandler> router = new Router<>();
    EmbeddedChannel chan = constructTestStack(router);

    FullHttpRequest req = constructRequest();
    req.setUri("/this/is/a/route");
    req.setMethod(HttpMethod.GET);

    chan.writeInbound(req);
    chan.flush();

    FullHttpResponse resp = (FullHttpResponse) chan.outboundMessages().poll();

    assertThat(resp.status().code()).isEqualTo(404);
  }

  public FullHttpRequest constructRequest() {
    return new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/");
  }

  public EmbeddedChannel constructTestStack(Router<HttpEndpointHandler> router) {
    return new EmbeddedChannel(new HttpRouteHandler(router));
  }
}
