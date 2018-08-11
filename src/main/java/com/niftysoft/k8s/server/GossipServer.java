package com.niftysoft.k8s.server;

import com.niftysoft.k8s.client.SyncInitiateTask;
import com.niftysoft.k8s.data.Config;
import com.niftysoft.k8s.data.stringstore.VolatileStringStore;
import com.niftysoft.k8s.http.HttpEndpointHandler;
import com.niftysoft.k8s.http.HttpRouteHandler;
import com.niftysoft.k8s.http.MapHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.BadClientSilencer;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.router.Router;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/** @author K. Alex Mills */
public class GossipServer {

  private Config config;

  private boolean isStarted = false;

  public GossipServer(Config config) {
    this.config = config;
  }

  public static List<ChannelHandler> buildHttpPipeline(VolatileStringStore store) {
    MapHandler mapHandler = new MapHandler(store);
    return Arrays.asList(
        new HttpServerCodec(),
        new HttpObjectAggregator(1048576),
        new HttpRouteHandler(
            new Router<HttpEndpointHandler>()
                .addRoute(HttpMethod.GET, "/map", mapHandler)
                .addRoute(HttpMethod.PUT, "/map", mapHandler)),
        new BadClientSilencer());
  }

  public boolean isStarted() { return isStarted; }

  public static void main(String[] args) throws Exception {
    new GossipServer(Config.fromEnvVars()).run();
  }

  public void run() throws Exception {
    // TODO: Allow the number of boss and worker threads to be configurable.
    VolatileStringStore myStore = new VolatileStringStore();

    EventLoopGroup bossGroup = new NioEventLoopGroup();
    EventLoopGroup peerWorkerGroup = new NioEventLoopGroup(1);
    EventLoopGroup syncInitiateGroup = new NioEventLoopGroup(1);
    EventLoopGroup clientWorkerGroup = new NioEventLoopGroup(1);

    try {
      ServerBootstrap b = new ServerBootstrap();
      // Configure channel used to sync with peers.
      b.group(bossGroup, peerWorkerGroup)
          .channel(NioServerSocketChannel.class)
          .handler(new LoggingHandler("SYNC", LogLevel.INFO))
          .childHandler(new GossipServerInitializer(myStore))
          .option(ChannelOption.SO_BACKLOG, 128)
          .childOption(ChannelOption.SO_KEEPALIVE, true);

      // Start listening to peers
      ChannelFuture f1 = b.bind(config.peerPort).sync();

      // TODO: Make delay and timing configurable.
      syncInitiateGroup
          .scheduleAtFixedRate(new SyncInitiateTask(config, myStore, syncInitiateGroup), 5, 1, TimeUnit.SECONDS);

      // Configure HTTP channel used to receive data from clients.
      b = new ServerBootstrap();
      b.group(bossGroup, clientWorkerGroup)
          .channel(NioServerSocketChannel.class)
          .handler(new LoggingHandler("HTTP", LogLevel.INFO))
          .childHandler(
              new ChannelInitializer<SocketChannel>() {
                @Override
                public void initChannel(SocketChannel ch) {
                  ch.pipeline().addLast(buildHttpPipeline(myStore).toArray(new ChannelHandler[0]));
                }
              })
          .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
          .option(ChannelOption.SO_BACKLOG, 128)
          .childOption(ChannelOption.TCP_NODELAY, java.lang.Boolean.TRUE);

      // Start listening for clients
      ChannelFuture f2 = b.bind(config.clientPort).sync();

      isStarted = true;
      f1.channel().closeFuture().sync();
      f2.channel().closeFuture().sync();
    } finally {
      peerWorkerGroup.shutdownGracefully();
      bossGroup.shutdownGracefully();
      syncInitiateGroup.shutdownGracefully();
      clientWorkerGroup.shutdownGracefully();
    }
  }
}
