package org.thingsboard.server.common.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UUIDBased;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TenantAndAsset extends SearchTextBased<UUIDBased> {
	Tenant tenant = new Tenant();
	List<Asset> assetList = new ArrayList<>();

	@Override
	public String getSearchText() {
		return null;
	}
}
