package org.thingsboard.server.dao.timeseries;

import lombok.Getter;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.kv.TsKvQuery;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Created by ztao at 2019/3/14 11:18.
 */
public class CountQueryCursor extends QueryCursor {

    /**
     * 只有一个count值，为了符合接口一致性，定义成List
     */
    @Getter
    private final List<TsKvEntry> data;

    public CountQueryCursor(String entityType, UUID entityId, TsKvQuery baseQuery, List<Long> partitions) {
        super(entityType, entityId, baseQuery, partitions);
        this.data = new ArrayList<>();
    }

    /**
     * @param newData
     */
    public void addData(TsKvEntry newData) {
        if (data.isEmpty()) {
            data.add(newData);
        } else {
            TsKvEntry oldEntry = data.get(0);
            Long newCount = oldEntry.getLongValue().get() + newData.getLongValue().get();
            TsKvEntry newEntry = new BasicTsKvEntry(oldEntry.getTs(), new LongDataEntry(oldEntry.getKey(), newCount));
            data.set(0, newEntry);
        }
    }
}