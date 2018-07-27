package com.niftysoft.k8s.client;

import com.niftysoft.k8s.data.Config;
import com.niftysoft.k8s.data.stringstore.VolatileStringStore;
import com.niftysoft.k8s.data.stringstore.VolatileStringStoreDecoder;
import com.niftysoft.k8s.data.stringstore.VolatileStringStoreEncoder;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class SyncTask implements Runnable {

    private final int port;
    private final String hostname;
    private final VolatileStringStore myStore;

    public SyncTask(Config config, VolatileStringStore store) {
        this.myStore = store;
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
                            new VolatileStringStoreDecoder(),
                            new VolatileStringStoreEncoder(),
                            new SyncClientWriteHandler(myStore),
                            new SyncClientReadHandler(myStore));
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
