package com.niftysoft.k8s.server;

import com.niftysoft.k8s.client.SyncTask;
import com.niftysoft.k8s.data.Config;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;

import java.util.concurrent.TimeUnit;

public class GossipServer {
    private Config config;

    public GossipServer(Config config) {
        this.config = config;
    }

    public void run() throws Exception {
        // TODO: Allow the number of boss and worker threads to be configurable.
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
             .channel(NioServerSocketChannel.class)
             .childHandler(new ChannelInitializer<SocketChannel>(){
                @Override
                 public void initChannel(SocketChannel ch) throws Exception {
                    // TODO: Enable sending object larger than 1 MB
                    ch.pipeline().addLast(new ObjectEncoder(),
                                          new ObjectDecoder(ClassResolvers.cacheDisabled(null)),
                                          new GossipServerHandler());
                }
             })
             .option(ChannelOption.SO_BACKLOG, 128)
             .childOption(ChannelOption.SO_KEEPALIVE, true);

            // Start listening
            ChannelFuture f = b.bind(config.peerPort).sync();

            // TODO: Make delay and timing configurable.
            f.channel().eventLoop().scheduleAtFixedRate(new SyncTask(config), 5, 1, TimeUnit.SECONDS);

            f.channel().closeFuture().sync();
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }

    public static void main(String[] args) throws Exception {
        new GossipServer(Config.fromEnvVars()).run();
    }
}
