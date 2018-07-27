package com.niftysoft.k8s.data;

/**
 * A lightweight String which is associated with a version ID.
 */
public class VersionedString {
    /**
     * Value to
     */
    String value;
    /**
     * Lamport logical clock value.
     */
    long version;

    public VersionedString(String str) {
        this.value = str;
    }

    public VersionedString() {

    }

    public long getVersion() {
        return version;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public void setVersion(long version) {
        this.version = version;
    }
}