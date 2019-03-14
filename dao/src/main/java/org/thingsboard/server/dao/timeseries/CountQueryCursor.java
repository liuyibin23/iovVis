package org.thingsboard.server.dao.timeseries;

import lombok.Getter;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.kv.TsKvQuery;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Created by ztao at 2019/3/14 11:18.
 */
public class CountQueryCursor extends QueryCursor {

    @Getter
    private final List<TsKvEntry> data;

    public CountQueryCursor(String entityType, UUID entityId, TsKvQuery baseQuery, List<Long> partitions) {
        super(entityType, entityId, baseQuery, partitions);
        this.data = new ArrayList<>();
    }

    public void addData(List<TsKvEntry> newData) {
        data.addAll(newData);
    }
}