package com.niftysoft.k8s.http;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;

import java.util.List;
import java.util.Map;

public abstract class HttpEndpointHandler {

    protected Map<String, String> pathParams;
    protected Map<String, List<String>> queryParams;

    public void setPathParams(Map<String,String> params) {
        this.pathParams = params;
    }

    public void setQueryParams(Map<String,List<String>> params) {
        this.queryParams = params;
    }

    public abstract void handleRequest(ChannelHandlerContext ctx, FullHttpRequest req, FullHttpResponse resp);
}
