package org.thingsboard.server.common.data.kv;

import lombok.Data;

/**
 * Created by ztao at 2019/3/14 10:28.
 */
@Data
public class BaseDoubleIntervalTsKvQuery extends BaseTsKvQuery implements DoubleIntervalTsKvQuery {
    private DataType dataType;
    private double floorV;
    private double ceilV;

    public BaseDoubleIntervalTsKvQuery(String key, long startTs, long endTs, double floorV, double ceilV, DataType dataType) {
        super(key, startTs, endTs);
        this.floorV = floorV;
        this.ceilV = ceilV;
        this.dataType = dataType;
    }
}