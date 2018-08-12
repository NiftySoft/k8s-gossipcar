package com.niftysoft.k8s.data;

public class VersionedByteArr {
    private byte[] value;
    private long version;

    public VersionedByteArr(byte[] arr) {
        this.value = arr;
    }

    public VersionedByteArr() {}

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }

    public byte[] getValue() {
        return value;
    }

    public void setValue(byte[] value ) {
        this.value = value;
    }
}
