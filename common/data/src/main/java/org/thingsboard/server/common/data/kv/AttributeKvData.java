package org.thingsboard.server.common.data.kv;

public class AttributeKvData implements Comparable<AttributeKvData>{

    private final long lastUpdateTs;
    private final String key;
    private final Object value;

    public AttributeKvData(long lastUpdateTs, String key, Object value) {
        super();
        this.lastUpdateTs = lastUpdateTs;
        this.key = key;
        this.value = value;
    }

    public long getLastUpdateTs() {
        return lastUpdateTs;
    }

    public String getKey() {
        return key;
    }

    public Object getValue() {
        return value;
    }

    @Override
    public int compareTo(AttributeKvData o) {
        return Long.compare(lastUpdateTs, o.lastUpdateTs);
    }

}
