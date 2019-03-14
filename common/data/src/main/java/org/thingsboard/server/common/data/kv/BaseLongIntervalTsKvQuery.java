package org.thingsboard.server.common.data.kv;

import lombok.Data;

/**
 * Created by ztao at 2019/3/14 10:28.
 */
@Data
public class BaseLongIntervalTsKvQuery extends BaseTsKvQuery implements LongIntervalTsKvQuery {
    private DataType dataType;
    private long floorV;
    private long ceilV;

    public BaseLongIntervalTsKvQuery(String key, long startTs, long endTs, long floorV, long ceilV, DataType dataType) {
        super(key, startTs, endTs);
        this.floorV = floorV;
        this.ceilV = ceilV;
        this.dataType = dataType;
    }
}