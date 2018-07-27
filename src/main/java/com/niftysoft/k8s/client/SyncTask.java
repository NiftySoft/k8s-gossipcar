package com.niftysoft.k8s.client;

import com.niftysoft.k8s.data.Config;
import com.niftysoft.k8s.server.GossipServer;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class SyncTask implements Runnable {

    private int port;
    private String hostname;

    public SyncTask(Config config) {
        this.hostname = config.serviceDnsName;
        this.port = config.peerPort;
    }

    @Override
    public void run() {
        try {
            InetAddress host = lookupRandomPeer(hostname);

            if (host == null) return; // No friends. :-(

            Bootstrap b = new Bootstrap();
            b.channel(NioSocketChannel.class);
            b.option(ChannelOption.SO_KEEPALIVE, true);
            b.handler(new ChannelInitializer<SocketChannel>() {
                protected void initChannel(SocketChannel ch) throws Exception {
                    ch.pipeline().addLast(
                            new ObjectEncoder(),
                            new ObjectDecoder(ClassResolvers.cacheDisabled(null)),
                            new SyncClientHandler());
                }
            });
            ChannelFuture f = b.connect(host, port).sync();

            f.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private InetAddress lookupRandomPeer(String host) {
        InetAddress[] peers;
        try {
            peers = InetAddress.getAllByName(host);
            if (peers.length == 0) {
                return null;
            }
            return peers[(int)(Math.random() * peers.length)];
        } catch (UnknownHostException e) {
            System.err.println("Error, unknown host: " + host);
            throw new RuntimeException(e);
        }
    }

}
