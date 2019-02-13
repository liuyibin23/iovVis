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
package org.thingsboard.server.dao.sql.asset;

import com.google.common.util.concurrent.ListenableFuture;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EntitySubtype;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.asset.AssetExInfo;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.asset.AssetDao;
import org.thingsboard.server.dao.model.sql.AssetEntity;
import org.thingsboard.server.dao.sql.JpaAbstractSearchTextDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static org.thingsboard.server.common.data.UUIDConverter.fromTimeUUID;
import static org.thingsboard.server.common.data.UUIDConverter.fromTimeUUIDs;
import static org.thingsboard.server.dao.model.ModelConstants.NULL_UUID_STR;

/**
 * Created by Valerii Sosliuk on 5/19/2017.
 */
@Component
@SqlDao
public class JpaAssetDao extends JpaAbstractSearchTextDao<AssetEntity, Asset> implements AssetDao {

    @Autowired
    private AssetRepository assetRepository;

    @Autowired
    private AssetExInfoRepository assetExInfoRepository;

    @Override
    protected Class<AssetEntity> getEntityClass() {
        return AssetEntity.class;
    }

    @Override
    protected CrudRepository<AssetEntity, String> getCrudRepository() {
        return assetRepository;
    }

    @Override
    public List<Asset> findAssets(TextPageLink pageLink) {
        return DaoUtil.convertDataList(assetRepository
                .findAllBy(
                        Objects.toString(pageLink.getTextSearch(), ""),
                        pageLink.getIdOffset() == null ? NULL_UUID_STR : fromTimeUUID(pageLink.getIdOffset()),
                        new PageRequest(0, pageLink.getLimit())));
    }

    @Override
    public List<Asset> findAssetsType(String type, TextPageLink pageLink) {
        return DaoUtil.convertDataList(assetRepository
                .findByType(
                        type,
                        Objects.toString(pageLink.getTextSearch(), ""),
                        pageLink.getIdOffset() == null ? NULL_UUID_STR : fromTimeUUID(pageLink.getIdOffset()),
                        new PageRequest(0, pageLink.getLimit())));
    }

    @Override
    public List<Asset> findAssetsByCustomerId(UUID customerId) {
        return DaoUtil.convertDataList(assetRepository.findAllByCustomerId(fromTimeUUID(customerId)));
    }

    @Override
    public List<Asset> findAssetsByTenantId(UUID tenantId) {
        return DaoUtil.convertDataList(assetRepository.findAllByTenantId(fromTimeUUID(tenantId)));
    }

    @Override
    public List<Asset> findAssets() {
        return DaoUtil.convertDataList(assetRepository.findAll());
    }

    @Override
    public List<Asset> findAssetsByTenantId(UUID tenantId, TextPageLink pageLink) {
        return DaoUtil.convertDataList(assetRepository
                .findByTenantId(
                        fromTimeUUID(tenantId),
                        Objects.toString(pageLink.getTextSearch(), ""),
                        pageLink.getIdOffset() == null ? NULL_UUID_STR : fromTimeUUID(pageLink.getIdOffset()),
                        new PageRequest(0, pageLink.getLimit())));
    }

    @Override
    public ListenableFuture<List<Asset>> findAssetsByTenantIdAndIdsAsync(UUID tenantId, List<UUID> assetIds) {
        return service.submit(() ->
                DaoUtil.convertDataList(assetRepository.findByTenantIdAndIdIn(fromTimeUUID(tenantId), fromTimeUUIDs(assetIds))));
    }

    @Override
    public List<Asset> findAssetsByTenantIdAndCustomerId(UUID tenantId, UUID customerId, TextPageLink pageLink) {
        return DaoUtil.convertDataList(assetRepository
                .findByTenantIdAndCustomerId(
                        fromTimeUUID(tenantId),
                        fromTimeUUID(customerId),
                        Objects.toString(pageLink.getTextSearch(), ""),
                        pageLink.getIdOffset() == null ? NULL_UUID_STR : fromTimeUUID(pageLink.getIdOffset()),
                        new PageRequest(0, pageLink.getLimit())));
    }

    @Override
    public ListenableFuture<List<Asset>> findAssetsByTenantIdAndCustomerIdAndIdsAsync(UUID tenantId, UUID customerId, List<UUID> assetIds) {
        return service.submit(() ->
                DaoUtil.convertDataList(assetRepository.findByTenantIdAndCustomerIdAndIdIn(fromTimeUUID(tenantId), fromTimeUUID(customerId), fromTimeUUIDs(assetIds))));
    }

    @Override
    public Optional<Asset> findAssetsByTenantIdAndName(UUID tenantId, String name) {
        Asset asset = DaoUtil.getData(assetRepository.findByTenantIdAndName(fromTimeUUID(tenantId), name));
        return Optional.ofNullable(asset);
    }

    @Override
    public List<Asset> findAssetsByTenantIdAndType(UUID tenantId, String type, TextPageLink pageLink) {
        return DaoUtil.convertDataList(assetRepository
                .findByTenantIdAndType(
                        fromTimeUUID(tenantId),
                        type,
                        Objects.toString(pageLink.getTextSearch(), ""),
                        pageLink.getIdOffset() == null ? NULL_UUID_STR : fromTimeUUID(pageLink.getIdOffset()),
                        new PageRequest(0, pageLink.getLimit())));
    }

    @Override
    public List<Asset> findAssetsByTenantIdAndCustomerIdAndType(UUID tenantId, UUID customerId, String type, TextPageLink pageLink) {
        if (tenantId != null && customerId != null) {
            return DaoUtil.convertDataList(assetRepository
                    .findByTenantIdAndCustomerIdAndType(
                            fromTimeUUID(tenantId),
                            fromTimeUUID(customerId),
                            type,
                            Objects.toString(pageLink.getTextSearch(), ""),
                            pageLink.getIdOffset() == null ? NULL_UUID_STR : fromTimeUUID(pageLink.getIdOffset()),
                            new PageRequest(0, pageLink.getLimit())));
        } else if (tenantId != null && customerId == null) {
            return DaoUtil.convertDataList(assetRepository
                    .findByTenantIdAndType(
                            fromTimeUUID(tenantId),
                            type,
                            Objects.toString(pageLink.getTextSearch(), ""),
                            pageLink.getIdOffset() == null ? NULL_UUID_STR : fromTimeUUID(pageLink.getIdOffset()),
                            new PageRequest(0, pageLink.getLimit())));
        }
        return DaoUtil.convertDataList(assetRepository
                .findByType(
                        type,
                        Objects.toString(pageLink.getTextSearch(), ""),
                        pageLink.getIdOffset() == null ? NULL_UUID_STR : fromTimeUUID(pageLink.getIdOffset()),
                        new PageRequest(0, pageLink.getLimit())));
    }

    @Override
    public List<Asset> findAllAssetsByTenantIdAndCustomerIdAndType(UUID tenantId, UUID customerId, String type) {
        if (tenantId != null && customerId != null) {
            return DaoUtil.convertDataList(assetRepository
                    .findAllByTenantIdAndCustomerIdAndType(
                            fromTimeUUID(tenantId),
                            fromTimeUUID(customerId),
                            type));
        } else if (tenantId != null && customerId == null) {
            return DaoUtil.convertDataList(assetRepository
                    .findAllByTenantIdAndType(
                            fromTimeUUID(tenantId),
                            type));
        }
        return DaoUtil.convertDataList(assetRepository.findAllByType(type));
    }

    @Override
    public List<Asset> findAllAssetsByTenantIdAndCustomerId(UUID tenantId, UUID customerId) {
        if (tenantId != null && customerId != null) {
            return DaoUtil.convertDataList(assetRepository
                    .findAllByCustomerId(fromTimeUUID(customerId)));
        } else if (tenantId != null && customerId == null) {
            return DaoUtil.convertDataList(assetRepository
                    .findAllByTenantId(fromTimeUUID(tenantId)));
        }
        return DaoUtil.convertDataList(assetRepository.findAll());
    }

    @Override
    public ListenableFuture<List<EntitySubtype>> findTenantAssetTypesAsync(UUID tenantId) {
        return service.submit(() -> convertTenantAssetTypesToDto(tenantId, assetRepository.findTenantAssetTypes(fromTimeUUID(tenantId))));
    }

    @Override
    public List<AssetExInfo> findAssetExInfosByTenantIdAndCustomerId(UUID tenantId, UUID customerId,TextPageLink pageLink) {
//        return DaoUtil.convertDataList(assetRepository
//                .findByTenantIdAndCustomerId(
//                        fromTimeUUID(tenantId),
//                        fromTimeUUID(customerId)));

        return DaoUtil.convertDataList(assetExInfoRepository.
                findAssetExInfoByTenantIdAndCustomerId(
                        fromTimeUUID(tenantId),fromTimeUUID(customerId),
                        Objects.toString(pageLink.getTextSearch(), ""),
                        pageLink.getIdOffset() == null ? NULL_UUID_STR : fromTimeUUID(pageLink.getIdOffset()),
                        pageLink.getLimit()));
    }

    @Override
    public List<AssetExInfo> findAssetExInfoByTenantId(UUID tenantId,TextPageLink pageLink) {
        return DaoUtil.convertDataList(assetExInfoRepository.
                findAssetExInfoByTenantId(fromTimeUUID(tenantId),
                        Objects.toString(pageLink.getTextSearch(), ""),
                        pageLink.getIdOffset() == null ? NULL_UUID_STR : fromTimeUUID(pageLink.getIdOffset()),
                        pageLink.getLimit()));
    }

    @Override
    public List<AssetExInfo> findAllAssetExInfo(TextPageLink pageLink) {
        return DaoUtil.convertDataList(assetExInfoRepository.findAllAssetExInfo(
                Objects.toString(pageLink.getTextSearch(), ""),
                pageLink.getIdOffset() == null ? NULL_UUID_STR : fromTimeUUID(pageLink.getIdOffset()),
                pageLink.getLimit()));
    }

    private List<EntitySubtype> convertTenantAssetTypesToDto(UUID tenantId, List<String> types) {
        List<EntitySubtype> list = Collections.emptyList();
        if (types != null && !types.isEmpty()) {
            list = new ArrayList<>();
            for (String type : types) {
                list.add(new EntitySubtype(new TenantId(tenantId), EntityType.ASSET, type));
            }
        }
        return list;
    }
}
