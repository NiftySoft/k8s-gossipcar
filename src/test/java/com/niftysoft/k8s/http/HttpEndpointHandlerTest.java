package com.niftysoft.k8s.http;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import org.assertj.core.util.Lists;
import org.assertj.core.util.Maps;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class HttpEndpointHandlerTest {

    @Test
    public void testEndpointHandlerStoresQueryAndPathParams() {
        UnderTest test = new UnderTest();

        test.setPathParams(Maps.newHashMap("query", "me"));
        test.setQueryParams(Maps.newHashMap("test", Lists.list("p1","p2")));

        test.handleRequest(null, null, null);

        // Included to make Codacy shut up.
        assertThat(true).isTrue();
    }

    private static class UnderTest extends HttpEndpointHandler {

        @Override
        public void handleRequest(ChannelHandlerContext ctx, FullHttpRequest req, FullHttpResponse resp) {
            assertThat(queryParams).containsEntry("test", Lists.list("p1", "p2"));
            assertThat(pathParams).containsEntry("query", "me");
        }
    }
}
