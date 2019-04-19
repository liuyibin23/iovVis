package org.thingsboard.server.dao.device;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.page.TextPageLink;

import java.util.List;

/**
 * Created by ztao at 2019/4/19 14:06.
 */
public interface AssetDevicesDao {

    ListenableFuture<List<Device>> findAllByQuery(AssetDevicesQuery query, TextPageLink pageLink);
}
