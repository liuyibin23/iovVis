/**
 * Copyright © 2016-2018 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.dao.asset;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.EntitySubtype;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.asset.AssetExInfo;
import org.thingsboard.server.common.data.asset.AssetSearchQuery;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.TextPageData;
import org.thingsboard.server.common.data.page.TextPageLink;

import java.util.List;

public interface AssetService {

    Asset findAssetById(TenantId tenantId, AssetId assetId);

    TextPageData<AssetExInfo> findAllAssetExInfo(TextPageLink pageLink);

    //    List<Asset> findAssetExInfoByTenant(TenantId tenantId);
    TextPageData<AssetExInfo> findAssetExInfoByTenant(TenantId tenantId,TextPageLink pageLink);

    TextPageData<AssetExInfo> findAssetExInfoByTenantAndCustomer(TenantId tenantId, CustomerId customerId,TextPageLink pageLink);

    ListenableFuture<Asset> findAssetByIdAsync(TenantId tenantId, AssetId assetId);

    Asset findAssetByTenantIdAndName(TenantId tenantId, String name);

    Asset findAssetByName(String name);

    Asset saveAsset(Asset asset);

    Asset assignAssetToCustomer(TenantId tenantId, AssetId assetId, CustomerId customerId);

    Asset unassignAssetFromCustomer(TenantId tenantId, AssetId assetId);

    void deleteAsset(TenantId tenantId, AssetId assetId);

    TextPageData<Asset> findAssets(TextPageLink pageLink);

    TextPageData<Asset> findAssetsByType(String type,TextPageLink pageLink);

    TextPageData<Asset> findAssetsByTenantId(TenantId tenantId, TextPageLink pageLink);

    TextPageData<Asset> findAssetsByTenantIdAndType(TenantId tenantId, String type, TextPageLink pageLink);

    ListenableFuture<List<Asset>> findAssetsByTenantIdAndIdsAsync(TenantId tenantId, List<AssetId> assetIds);

    void deleteAssetsByTenantId(TenantId tenantId);

    void deleteAssetByCustomerId(TenantId tenantId,CustomerId customerId);

    TextPageData<Asset> findAssetsByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId, TextPageLink pageLink);

    TextPageData<Asset> findAssetsByTenantIdAndCustomerIdAndType(TenantId tenantId, CustomerId customerId, String type, TextPageLink pageLink);

    ListenableFuture<List<Asset>> findAssetsByTenantIdCustomerIdAndIdsAsync(TenantId tenantId, CustomerId customerId, List<AssetId> assetIds);

    void unassignCustomerAssets(TenantId tenantId, CustomerId customerId);

    ListenableFuture<List<Asset>> findAssetsByDeviceId(TenantId tenantId, DeviceId deviceId);

    ListenableFuture<List<Asset>> findAssetsByQueryWithOutTypeFilter(TenantId tenantId, AssetSearchQuery query);

    ListenableFuture<List<Asset>> findAssetsByQuery(TenantId tenantId, AssetSearchQuery query);

    ListenableFuture<List<EntitySubtype>> findAssetTypesByTenantId(TenantId tenantId);

    List<Asset> findAssets();
    List<Asset> findAssetsByTenantId(TenantId tenantId);
    List<Asset> findAssetsByCustomerId(CustomerId customerId);

    List<Asset> findAllAssetsByTenantIdAndCustomerIdAndType(TenantId tenantId, CustomerId customerId, String type);
    List<Asset> findAllAssetsByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId);

    TenantId findTenantIdByAssetId(AssetId assetId,TextPageLink pageLink);
}
