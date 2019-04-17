package org.thingsboard.server.dao.alarm;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.AssetDeviceAlarm;
import org.thingsboard.server.common.data.alarm.AssetDeviceAlarmQuery;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.dao.Dao;

import java.util.List;


/**
 * Created by ztao at 2019/4/17 15:43.
 */
public interface AssetDeviceAlarmDao extends Dao<AssetDeviceAlarm> {

    ListenableFuture<List<AssetDeviceAlarm>> findAll(AssetDeviceAlarmQuery query, TimePageLink pageLink);
}
