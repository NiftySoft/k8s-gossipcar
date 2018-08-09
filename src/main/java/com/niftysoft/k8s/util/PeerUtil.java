package com.niftysoft.k8s.util;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;

public class PeerUtil {

    public static InetAddress lookupRandomPeer(String host, InetAddress ownAddress) throws UnknownHostException {
        InetAddress[] peers;
        try {
            peers = InetAddress.getAllByName(host);
            if (peers.length == 0) {
                return null;
            }
            peers = PeerUtil.findAndRemoveOwnAddress(peers, ownAddress);
            return peers[(int) (Math.random() * peers.length)];
        } catch (UnknownHostException e) {
            throw new UnknownHostException("Error, unknown host: " + host);
        }
    }

    public static InetAddress[] findAndRemoveOwnAddress(InetAddress[] peers, InetAddress ownAddress) {
        if (ownAddress == null) return peers;

        boolean found = false;
        for (int i = 0; i < peers.length; ++i) {
            if (peers[i].equals(ownAddress)) {
                found = true;
            }
            if (found && i < peers.length - 1) peers[i] = peers[i + 1];
        }
        if (!found) return peers;

        return Arrays.copyOf(peers, peers.length - 1);
    }
}
