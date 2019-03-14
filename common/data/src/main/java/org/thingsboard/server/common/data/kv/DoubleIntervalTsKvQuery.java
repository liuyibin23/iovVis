package org.thingsboard.server.common.data.kv;

/**
 * Created by ztao at 2019/3/14 10:11.
 */
public interface DoubleIntervalTsKvQuery extends IntervalTsKvQuery {
    double getFloorV();
    double getCeilV();
}
