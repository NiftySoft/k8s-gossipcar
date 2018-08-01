package com.niftysoft.k8s.util;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;

import java.util.List;

public class ChannelUtil {
  public static void loadPipeline(Channel channel, List<ChannelHandler> pipeline) {
    for (ChannelHandler handler : pipeline) {
      channel.pipeline().addLast(handler);
    }
  }
}
