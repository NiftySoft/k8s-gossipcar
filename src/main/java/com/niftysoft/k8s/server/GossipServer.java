package com.niftysoft.k8s.server;

import com.niftysoft.k8s.client.SyncTask;
import com.niftysoft.k8s.data.Config;
import com.niftysoft.k8s.data.stringstore.VolatileStringStore;
import com.niftysoft.k8s.http.HttpEndpointHandler;
import com.niftysoft.k8s.http.HttpRouteHandler;
import com.niftysoft.k8s.http.MapHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.BadClientSilencer;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.router.Router;

import java.util.concurrent.TimeUnit;

public class GossipServer {
    private Config config;

    public GossipServer(Config config) {
        this.config = config;
    }

    public void run() throws Exception {
        // TODO: Allow the number of boss and worker threads to be configurable.
        VolatileStringStore myStore = new VolatileStringStore();

        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup peerWorkerGroup = new NioEventLoopGroup();
        EventLoopGroup clientWorkerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            // Configure channel used to sync with peers.
            b.group(bossGroup, peerWorkerGroup)
                 .channel(NioServerSocketChannel.class)
                 .childHandler(new ChannelInitializer<SocketChannel>(){
                    @Override
                     public void initChannel(SocketChannel ch) throws Exception {
                        // TODO: Enable sending object larger than 1 MB
                        ch.pipeline().addLast(new GossipServerHandler(myStore));
                    }
                 }).option(ChannelOption.SO_BACKLOG, 128)
                 .childOption(ChannelOption.SO_KEEPALIVE, true);

            // Start listening to peers
            ChannelFuture f1 = b.bind(config.peerPort).sync();

            // TODO: Make delay and timing configurable.
            f1.channel().eventLoop().scheduleAtFixedRate(new SyncTask(config, myStore), 5, 1, TimeUnit.SECONDS);

            // Configure HTTP channel used to receive data from clients.
            b = new ServerBootstrap();
            b.group(bossGroup, clientWorkerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) throws Exception {
                        MapHandler mapHandler = new MapHandler(myStore);
                        ch.pipeline()
                                .addLast(new HttpServerCodec())
                                .addLast(new HttpRouteHandler(new Router<HttpEndpointHandler>()
                                        .addRoute(HttpMethod.GET, "/map", mapHandler)
                                        .addRoute(HttpMethod.PUT, "/map", mapHandler)))
                                .addLast(new BadClientSilencer());
                    }
                }).childOption(ChannelOption.TCP_NODELAY, java.lang.Boolean.TRUE)
                .childOption(ChannelOption.SO_KEEPALIVE, java.lang.Boolean.TRUE);;

            // Start listening for clients
            ChannelFuture f2 = b.bind(config.clientPort).sync();

            f1.channel().closeFuture().sync();
            f2.channel().closeFuture().sync();
        } finally {
            peerWorkerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }

    public static void main(String[] args) throws Exception {
        new GossipServer(Config.fromEnvVars()).run();
    }
}
