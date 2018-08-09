package com.niftysoft.k8s.util;

import org.junit.Before;
import org.junit.Test;

import java.net.InetAddress;

import static org.assertj.core.api.Assertions.assertThat;

public class PeerUtilTest {
    InetAddress addr1;
    InetAddress addr2;
    InetAddress addr3;
    InetAddress addr4;

    @Before
    public void before() throws Exception {
        addr1 = InetAddress.getByAddress(new byte[] {(byte)192, (byte) 168, 0, 1});
        addr2 = InetAddress.getByAddress(new byte[] {(byte)192, (byte) 168, 0, 2});
        addr3 = InetAddress.getByAddress(new byte[] {(byte)192, (byte) 168, 0, 3});
        addr4 = InetAddress.getByAddress(new byte[] {(byte)192, (byte) 168, 0, 4});
    }

    @Test
    public void testFindAndRemoveOwnAddressRemovesAddress() {
        InetAddress[] peers = new InetAddress[] { addr1, addr2, addr3, addr4 };
        assertThat(PeerUtil.findAndRemoveOwnAddress(peers, addr1)).containsExactlyInAnyOrder(addr2, addr3, addr4);

        peers = new InetAddress[] { addr1, addr2, addr3, addr4 };
        assertThat(PeerUtil.findAndRemoveOwnAddress(peers, addr2)).containsExactlyInAnyOrder(addr1, addr3, addr4);

        peers = new InetAddress[] { addr1, addr2, addr3, addr4 };
        assertThat(PeerUtil.findAndRemoveOwnAddress(peers, addr3)).containsExactlyInAnyOrder(addr1, addr2, addr4);

        peers = new InetAddress[] { addr1, addr2, addr3, addr4 };
        assertThat(PeerUtil.findAndRemoveOwnAddress(peers, addr4)).containsExactlyInAnyOrder(addr2, addr3, addr1);
    }

    @Test
    public void testFindAndRemoveOwnAddressNoChangeOnNull() {
        InetAddress[] peers = new InetAddress[] { addr1, addr2, addr3, addr4 };
        assertThat(PeerUtil.findAndRemoveOwnAddress(peers, null)).containsExactly(addr1, addr2, addr3, addr4);
    }
}
