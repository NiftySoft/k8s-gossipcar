package com.niftysoft.k8s.http;

import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;

import java.util.List;
import java.util.Map;

/**
 * Typically only one instance of an endpoint handler is constructed and statically glued together
 * at run-time, so they are intended to be stateless.
 *
 * @author K. Alex Mills
 */
public abstract class HttpEndpointHandler {

  protected Map<String, String> pathParams;
  protected Map<String, List<String>> queryParams;

  public void setPathParams(Map<String, String> params) {
    this.pathParams = params;
  }

  public void setQueryParams(Map<String, List<String>> params) {
    this.queryParams = params;
  }

  /**
   * Handles an HTTP request. This method is accessed via multiple-threads, and must remain
   * re-entrant.
   *
   * @param req
   */
  public abstract FullHttpResponse handleRequest(FullHttpRequest req);
}
