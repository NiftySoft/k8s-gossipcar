package com.niftysoft.k8s.http;

import io.netty.handler.codec.http.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class DefaultHttpHandlerTest {

  @Test
  public void test404Handler() {
    FullHttpRequest req = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/");
    FullHttpResponse resp = DefaultHttpHandler.respond404(req);
    assertThat(resp.status().code()).isEqualTo(404);
    assertThat(resp.protocolVersion()).isEqualTo(req.protocolVersion());
  }

  @Test
  public void test422Handler() {
    FullHttpRequest req = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/");
    FullHttpResponse resp = DefaultHttpHandler.respond422(req);
    assertThat(resp.status().code()).isEqualTo(422);
    assertThat(resp.protocolVersion()).isEqualTo(req.protocolVersion());
  }
}
