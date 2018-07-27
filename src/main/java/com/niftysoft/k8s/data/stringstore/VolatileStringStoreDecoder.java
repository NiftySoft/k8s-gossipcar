package com.niftysoft.k8s.data.stringstore;

import com.niftysoft.k8s.data.VersionedString;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ReplayingDecoder;

import java.util.List;

public class VolatileStringStoreDecoder extends ReplayingDecoder<VolatileStringStore> {
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        VolatileStringStore result = new VolatileStringStore();
        int numEntries = in.readInt();
        for (int i = 0; i < numEntries; ++i) {
            long key = in.readLong();
            long verstrVersion = in.readLong();

            byte[] stringBuffer = new byte[in.readInt()];
            in.readBytes(stringBuffer);
            String verstrValue = new String(stringBuffer, "UTF-8");

            VersionedString verstr = new VersionedString();
            verstr.setVersion(verstrVersion);
            verstr.setValue(verstrValue);

            result.internalMap.put(key, verstr);
        }
        out.add(result);
    }
}
