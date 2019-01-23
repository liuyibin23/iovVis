package org.thingsboard.server.common.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.asset.AssetExInfo;

import java.util.ArrayList;
import java.util.List;
@Data
@NoArgsConstructor
@AllArgsConstructor

public class CustomerAndAssets {
	Customer customer = new Customer();
	List<AssetExInfo> assetList = new ArrayList<>();
}
