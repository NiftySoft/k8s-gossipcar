package com.niftysoft.k8s.data.stringstore;

import com.niftysoft.k8s.data.VersionedString;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.util.CharsetUtil;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetDecoder;
import java.util.Map;
import java.util.Set;

public class VolatileStringStoreEncoder extends MessageToByteEncoder<VolatileStringStore> {
    @Override
    protected void encode(ChannelHandlerContext ctx, VolatileStringStore in, ByteBuf out) throws Exception {
        Set<Map.Entry<Long, VersionedString>> entrySet = in.internalMap.entrySet();
        out.writeInt(entrySet.size());
        for (Map.Entry<Long, VersionedString> entry : entrySet) {
            out.writeLong(entry.getKey());

            VersionedString verstr = entry.getValue();
            out.writeLong(verstr.getVersion());

            int nBytes = ByteBufUtil.utf8Bytes(verstr.getValue());
            out.writeInt(nBytes);
            ByteBufUtil.reserveAndWriteUtf8(out, verstr.getValue(), nBytes);
        }
    }
}
