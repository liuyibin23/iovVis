/**
 * Copyright © 2016-2018 The Thingsboard Authors
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.dao.asset;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.thingsboard.server.common.data.*;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.asset.AssetExInfo;
import org.thingsboard.server.common.data.asset.AssetSearchQuery;
import org.thingsboard.server.common.data.id.*;
import org.thingsboard.server.common.data.page.TextPageData;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.common.data.patrol.PatrolRecord;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.relation.RelationsSearchParameters;
import org.thingsboard.server.dao.customer.CustomerDao;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.entity.AbstractEntityService;
import org.thingsboard.server.dao.entityview.EntityViewService;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.partol.PatrolRecordService;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.service.PaginatedRemover;
import org.thingsboard.server.dao.tenant.TenantDao;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static org.thingsboard.server.common.data.CacheConstants.ASSET_CACHE;
import static org.thingsboard.server.dao.DaoUtil.toUUIDs;
import static org.thingsboard.server.dao.model.ModelConstants.NULL_UUID;
import static org.thingsboard.server.dao.service.Validator.*;

@Service
@Slf4j
public class BaseAssetService extends AbstractEntityService implements AssetService {

    public static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";
    public static final String INCORRECT_PAGE_LINK = "Incorrect page link ";
    public static final String INCORRECT_CUSTOMER_ID = "Incorrect customerId ";
    public static final String INCORRECT_ASSET_ID = "Incorrect assetId ";
    @Autowired
    private AssetDao assetDao;

    @Autowired
    private TenantDao tenantDao;

    @Autowired
    private CustomerDao customerDao;

    @Autowired
    private EntityViewService entityViewService;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    PatrolRecordService patrolRecordService;

    @Autowired
    private DeviceService deviceService;

    @Override
    public Asset findAssetById(TenantId tenantId, AssetId assetId) {
        log.trace("Executing findAssetById [{}]", assetId);
        validateId(assetId, INCORRECT_ASSET_ID + assetId);
        return assetDao.findById(tenantId, assetId.getId());
    }

    @Override
    public TextPageData<AssetExInfo> findAllAssetExInfo(TextPageLink pageLink) {
        validatePageLink(pageLink, INCORRECT_PAGE_LINK + pageLink);
        List<AssetExInfo> assetExInfos = assetDao.findAllAssetExInfo(pageLink);
        assetExInfos.stream().forEach(asset -> {
            asset.setContainsCount(relationService.findByFromAndType(asset.getTenantId(), asset.getId(), EntityRelation.CONTAINS_TYPE, RelationTypeGroup.COMMON).size());
        });
        return new TextPageData<>(assetExInfos, pageLink);
    }

    @Override
    public TextPageData<AssetExInfo> findAssetExInfoByTenant(TenantId tenantId, TextPageLink pageLink) {
//		List<AssetExInfoEntity> assetExInfos = assetDao.findAssetExInfoByTenantId(tenantId.getId());
//		return assetDao.find(tenantId);
        validatePageLink(pageLink, INCORRECT_PAGE_LINK + pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        List<AssetExInfo> assetExInfos = assetDao.findAssetExInfoByTenantId(tenantId.getId(), pageLink);
        assetExInfos.stream().forEach(asset -> {
            asset.setContainsCount(relationService.findByFromAndType(asset.getTenantId(), asset.getId(), EntityRelation.CONTAINS_TYPE, RelationTypeGroup.COMMON).size());
        });
        return new TextPageData<>(assetExInfos, pageLink);
    }

    @Override
    public TextPageData<AssetExInfo> findAssetExInfoByTenantAndCustomer(TenantId tenantId, CustomerId customerId, TextPageLink pageLink) {
        validatePageLink(pageLink, INCORRECT_PAGE_LINK + pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        List<AssetExInfo> assetExInfos = assetDao.findAssetExInfosByTenantIdAndCustomerId(tenantId.getId(), customerId.getId(), pageLink);
        assetExInfos.stream().forEach(asset -> {
            asset.setContainsCount(relationService.findByFromAndType(asset.getTenantId(), asset.getId(), EntityRelation.CONTAINS_TYPE, RelationTypeGroup.COMMON).size());
        });
        return new TextPageData<>(assetExInfos, pageLink);
    }

    @Override
    public ListenableFuture<Asset> findAssetByIdAsync(TenantId tenantId, AssetId assetId) {
        log.trace("Executing findAssetById [{}]", assetId);
        validateId(assetId, INCORRECT_ASSET_ID + assetId);
        return assetDao.findByIdAsync(tenantId, assetId.getId());
    }

    @Cacheable(cacheNames = ASSET_CACHE, key = "{#tenantId, #name}")
    @Override
    public Asset findAssetByTenantIdAndName(TenantId tenantId, String name) {
        log.trace("Executing findAssetByTenantIdAndName [{}][{}]", tenantId, name);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        return assetDao.findAssetsByTenantIdAndName(tenantId.getId(), name)
                .orElse(null);
    }

    @Override
    public Asset findAssetByName(String name) {
        return assetDao.findAssetByName(name);
    }

    @CacheEvict(cacheNames = ASSET_CACHE, key = "{#asset.tenantId, #asset.name}")
    @Override
    public Asset saveAsset(Asset asset) {
        log.trace("Executing saveAsset [{}]", asset);
        assetValidator.validate(asset, Asset::getTenantId);
        return assetDao.save(asset.getTenantId(), asset);
    }

    @Override
    public Asset assignAssetToCustomer(TenantId tenantId, AssetId assetId, CustomerId customerId) {
        Asset asset = findAssetById(tenantId, assetId);
        asset.setCustomerId(customerId);
        return saveAsset(asset);
    }

    @Override
    public Asset unassignAssetFromCustomer(TenantId tenantId, AssetId assetId) {
        Asset asset = findAssetById(tenantId, assetId);
        asset.setCustomerId(null);
        return saveAsset(asset);
    }

    @Override
    public void deleteAsset(TenantId tenantId, AssetId assetId) {
        log.trace("Executing deleteAsset [{}]", assetId);
        validateId(assetId, INCORRECT_ASSET_ID + assetId);
        deleteEntityRelations(tenantId, assetId);

        Asset asset = assetDao.findById(tenantId, assetId.getId());

        //删除与此设施相关的PatrolRecord
        try {
            List<PatrolRecord> patrolRecords = patrolRecordService.findByOriginatorId(asset.getId().toString());
            patrolRecords.forEach(item -> {
                if (item.getOriginator().getEntityType() == EntityType.ASSET) {
                    patrolRecordService.delete(item.getId());
                }
            });
        } catch (ExecutionException | InterruptedException e) {
            log.error("Exception while finding patrol record for assetId [{}]", assetId, e);
            throw new RuntimeException("Exception while finding patrol record for assetId [" + assetId + "]", e);
        }

        try {
            List<EntityView> entityViews = entityViewService.findEntityViewsByTenantIdAndEntityIdAsync(asset.getTenantId(), assetId).get();
            if (entityViews != null && !entityViews.isEmpty()) {
                throw new DataValidationException("Can't delete asset that is assigned to entity views!");
            }
        } catch (ExecutionException | InterruptedException e) {
            log.error("Exception while finding entity views for assetId [{}]", assetId, e);
            throw new RuntimeException("Exception while finding entity views for assetId [" + assetId + "]", e);
        }

        List<Object> list = new ArrayList<>();
        list.add(asset.getTenantId());
        list.add(asset.getName());
        Cache cache = cacheManager.getCache(ASSET_CACHE);
        cache.evict(list);

        assetDao.removeById(tenantId, assetId.getId());
    }

    @Override
    public TextPageData<Asset> findAssets(TextPageLink pageLink) {
        validatePageLink(pageLink, INCORRECT_PAGE_LINK + pageLink);
        List<Asset> assets = assetDao.findAssets(pageLink);
        assets.stream().forEach(asset -> {
            asset.setContainsCount(relationService.findByFromAndType(asset.getTenantId(), asset.getId(), EntityRelation.CONTAINS_TYPE, RelationTypeGroup.COMMON).size());
        });
        return new TextPageData<>(assets, pageLink);
    }

    @Override
    public TextPageData<Asset> findAssetsByType(String type, TextPageLink pageLink) {
        validatePageLink(pageLink, INCORRECT_PAGE_LINK + pageLink);
        validateString(type, "Incorrect type " + type);
        List<Asset> assets = assetDao.findAssetsType(type, pageLink);
        assets.stream().forEach(asset -> {
            asset.setContainsCount(relationService.findByFromAndType(asset.getTenantId(), asset.getId(), EntityRelation.CONTAINS_TYPE, RelationTypeGroup.COMMON).size());
        });
        return new TextPageData<>(assets, pageLink);
    }

    @Override
    public TextPageData<Asset> findAssetsByTenantId(TenantId tenantId, TextPageLink pageLink) {
        log.trace("Executing findAssetsByTenantId, tenantId [{}], pageLink [{}]", tenantId, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validatePageLink(pageLink, INCORRECT_PAGE_LINK + pageLink);
        List<Asset> assets = assetDao.findAssetsByTenantId(tenantId.getId(), pageLink);
        assets.stream().forEach(asset -> {
            asset.setContainsCount(relationService.findByFromAndType(tenantId, asset.getId(), EntityRelation.CONTAINS_TYPE, RelationTypeGroup.COMMON).size());
        });
        return new TextPageData<>(assets, pageLink);
    }

    @Override
    public TextPageData<Asset> findAssetsByTenantIdAndType(TenantId tenantId, String type, TextPageLink pageLink) {
        log.trace("Executing findAssetsByTenantIdAndType, tenantId [{}], type [{}], pageLink [{}]", tenantId, type, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateString(type, "Incorrect type " + type);
        validatePageLink(pageLink, INCORRECT_PAGE_LINK + pageLink);
        List<Asset> assets = assetDao.findAssetsByTenantIdAndType(tenantId.getId(), type, pageLink);

        assets.stream().forEach(asset -> {
            asset.setContainsCount(relationService.findByFromAndType(tenantId, asset.getId(), EntityRelation.CONTAINS_TYPE, RelationTypeGroup.COMMON).size());
            relationService.findByToAndType(tenantId, asset.getId(), EntityRelation.CONTAINS_TYPE, RelationTypeGroup.COMMON).forEach(entityRelation -> {
                if (entityRelation.getTo().getEntityType() == EntityType.ASSET) {
                    asset.setAffiliation(assetDao.findById(tenantId, entityRelation.getTo().getId()).getName());
                }
            });
        });
        return new TextPageData<>(assets, pageLink);
    }

    @Override
    public ListenableFuture<List<Asset>> findAssetsByTenantIdAndIdsAsync(TenantId tenantId, List<AssetId> assetIds) {
        log.trace("Executing findAssetsByTenantIdAndIdsAsync, tenantId [{}], assetIds [{}]", tenantId, assetIds);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateIds(assetIds, "Incorrect assetIds " + assetIds);
        return assetDao.findAssetsByTenantIdAndIdsAsync(tenantId.getId(), toUUIDs(assetIds));
    }

    @Override
    public void deleteAssetsByTenantId(TenantId tenantId) {
        log.trace("Executing deleteAssetsByTenantId, tenantId [{}]", tenantId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        tenantAssetsRemover.removeEntities(tenantId, tenantId);
    }

    @Override
    public void deleteAssetByCustomerId(TenantId tenantId,CustomerId customerId){
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(customerId, INCORRECT_CUSTOMER_ID + customerId);
        customerAssetsRemover.removeEntities(tenantId,customerId);
    }

    @Override
    public TextPageData<Asset> findAssetsByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId, TextPageLink pageLink) {
        log.trace("Executing findAssetExInfosByTenantIdAndCustomerId, tenantId [{}], customerId [{}], pageLink [{}]", tenantId, customerId, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(customerId, INCORRECT_CUSTOMER_ID + customerId);
        validatePageLink(pageLink, INCORRECT_PAGE_LINK + pageLink);
        List<Asset> assets = assetDao.findAssetsByTenantIdAndCustomerId(tenantId.getId(), customerId.getId(), pageLink);
        return new TextPageData<>(assets, pageLink);
    }

    @Override
    public TextPageData<Asset> findAssetsByTenantIdAndCustomerIdAndType(TenantId tenantId, CustomerId customerId, String type, TextPageLink pageLink) {
        log.trace("Executing findAssetsByTenantIdAndCustomerIdAndType, tenantId [{}], customerId [{}], type [{}], pageLink [{}]", tenantId, customerId, type, pageLink);
        //TODO 下面代码是拷贝过来的，需要优化，不应该把tid和cid的判断逻辑放到DAO中，应该拆分成几个独立的service方法。
//		validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
//		validateId(customerId, INCORRECT_CUSTOMER_ID + customerId);
        validateString(type, "Incorrect type " + type);
        validatePageLink(pageLink, INCORRECT_PAGE_LINK + pageLink);
        UUID tId = null, cId = null;
        if (!tenantId.isNullUid()) {
            tId = tenantId.getId();
        }
        if (!customerId.isNullUid()) {
            cId = customerId.getId();
        }
        List<Asset> assets = assetDao.findAssetsByTenantIdAndCustomerIdAndType(tId, cId, type, pageLink);
        return new TextPageData<>(assets, pageLink);
    }

    @Override
    public ListenableFuture<List<Asset>> findAssetsByTenantIdCustomerIdAndIdsAsync(TenantId tenantId, CustomerId customerId, List<AssetId> assetIds) {
        log.trace("Executing findAssetsByTenantIdAndCustomerIdAndIdsAsync, tenantId [{}], customerId [{}], assetIds [{}]", tenantId, customerId, assetIds);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(customerId, INCORRECT_CUSTOMER_ID + customerId);
        validateIds(assetIds, "Incorrect assetIds " + assetIds);
        return assetDao.findAssetsByTenantIdAndCustomerIdAndIdsAsync(tenantId.getId(), customerId.getId(), toUUIDs(assetIds));
    }

    @Override
    public void unassignCustomerAssets(TenantId tenantId, CustomerId customerId) {
        log.trace("Executing unassignCustomerAssets, tenantId [{}], customerId [{}]", tenantId, customerId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(customerId, INCORRECT_CUSTOMER_ID + customerId);
        customerAssetsUnasigner.removeEntities(tenantId, customerId);
    }

    @Override
    public ListenableFuture<List<Asset>> findAssetsByDeviceId(TenantId tenantId, DeviceId deviceId) {
        AssetSearchQuery query = new AssetSearchQuery();
        RelationsSearchParameters parameters = new RelationsSearchParameters(deviceId, EntitySearchDirection.TO, 1);
        query.setParameters(parameters);
        query.setRelationType(EntityRelation.CONTAINS_TYPE);
        return findAssetsByQueryWithOutTypeFilter(tenantId, query);
    }

    @Override
    public ListenableFuture<List<Asset>> findAssetsByQueryWithOutTypeFilter(TenantId tenantId, AssetSearchQuery query) {
        return innerFindAssetsByQuery(tenantId, query);
    }

    @Override
    public ListenableFuture<List<Asset>> findAssetsByQuery(TenantId tenantId, AssetSearchQuery query) {
//		ListenableFuture<List<EntityRelation>> relations = relationService.findByQuery(tenantId, query.toEntitySearchQuery());
//		ListenableFuture<List<Asset>> assets = Futures.transformAsync(relations, r -> {
//			EntitySearchDirection direction = query.toEntitySearchQuery().getParameters().getDirection();
//			List<ListenableFuture<Asset>> futures = new ArrayList<>();
//			for (EntityRelation relation : r) {
//				EntityId entityId = direction == EntitySearchDirection.FROM ? relation.getTo() : relation.getFrom();
//				if (entityId.getEntityType() == EntityType.ASSET) {
//					futures.add(findAssetByIdAsync(tenantId, new AssetId(entityId.getId())));
//				}
//			}
//			return Futures.successfulAsList(futures);
//		});
        ListenableFuture<List<Asset>> assets = innerFindAssetsByQuery(tenantId, query);
        assets = Futures.transform(assets, assetList ->
                assetList == null ? Collections.emptyList() : assetList.stream().filter(asset -> query.getAssetTypes().contains(asset.getType())).collect(Collectors.toList())
        );
        return assets;
    }

    private ListenableFuture<List<Asset>> innerFindAssetsByQuery(TenantId tenantId, AssetSearchQuery query) {
        ListenableFuture<List<EntityRelation>> relations = relationService.findByQuery(tenantId, query.toEntitySearchQuery());
        ListenableFuture<List<Asset>> assets = Futures.transformAsync(relations, r -> {
            EntitySearchDirection direction = query.toEntitySearchQuery().getParameters().getDirection();
            List<ListenableFuture<Asset>> futures = new ArrayList<>();
            for (EntityRelation relation : r) {
                EntityId entityId = direction == EntitySearchDirection.FROM ? relation.getTo() : relation.getFrom();
                if (entityId.getEntityType() == EntityType.ASSET) {
                    futures.add(findAssetByIdAsync(tenantId, new AssetId(entityId.getId())));
                }
            }
            return Futures.successfulAsList(futures);
        });
        return assets;
    }

    @Override
    public ListenableFuture<List<EntitySubtype>> findAssetTypesByTenantId(TenantId tenantId) {
        log.trace("Executing findAssetTypesByTenantId, tenantId [{}]", tenantId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        ListenableFuture<List<EntitySubtype>> tenantAssetTypes = assetDao.findTenantAssetTypesAsync(tenantId.getId());
        return Futures.transform(tenantAssetTypes,
                assetTypes -> {
                    assetTypes.sort(Comparator.comparing(EntitySubtype::getType));
                    return assetTypes;
                });
    }

    @Override
    public List<Asset> findAssets() {
        return assetDao.findAssets();
    }

    @Override
    public List<Asset> findAssetsByTenantId(TenantId tenantId) {
        return assetDao.findAssetsByTenantId(tenantId.getId());
    }

    @Override
    public List<Asset> findAssetsByCustomerId(CustomerId customerId) {
        return assetDao.findAssetsByCustomerId(customerId.getId());
    }

    @Override
    public List<Asset> findAllAssetsByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId) {
        UUID tId = null, cId = null;
        if (!tenantId.isNullUid()) {
            tId = tenantId.getId();
        }
        if (!customerId.isNullUid()) {
            cId = customerId.getId();
        }
        return assetDao.findAllAssetsByTenantIdAndCustomerId(tId, cId);
    }

    @Override
    public List<Asset> findAllAssetsByTenantIdAndCustomerIdAndType(TenantId tenantId, CustomerId customerId, String type) {
        UUID tId = null, cId = null;
        if (!tenantId.isNullUid()) {
            tId = tenantId.getId();
        }
        if (!customerId.isNullUid()) {
            cId = customerId.getId();
        }
        return assetDao.findAllAssetsByTenantIdAndCustomerIdAndType(tId, cId, type);
    }

    @Override
    public TenantId findTenantIdByAssetId(AssetId assetId, TextPageLink pageLink) {
        TenantId tenantId = null;
        TextPageData<Asset> assets = findAssets(pageLink);
        List<Asset> assetList = assets.getData();
        for (Asset asset : assetList) {
            if (asset.getId().equals(assetId)) {
                tenantId = asset.getTenantId();
                break;
            }
        }
        if (tenantId == null && assets.hasNext()) {
            findTenantIdByAssetId(assetId, assets.getNextPageLink());
        }
        return tenantId;
    }

    private DataValidator<Asset> assetValidator =
            new DataValidator<Asset>() {

                @Override
                protected void validateCreate(TenantId tenantId, Asset asset) {
                    assetDao.findAssetsByTenantIdAndName(asset.getTenantId().getId(), asset.getName()).ifPresent(
                            d -> {
                                throw new DataValidationException("Asset with such name already exists!");
                            }
                    );
                }

                @Override
                protected void validateUpdate(TenantId tenantId, Asset asset) {
                    assetDao.findAssetsByTenantIdAndName(asset.getTenantId().getId(), asset.getName()).ifPresent(
                            d -> {
                                if (!d.getId().equals(asset.getId())) {
                                    throw new DataValidationException("Asset with such name already exists!");
                                }
                            }
                    );
                }

                @Override
                protected void validateDataImpl(TenantId tenantId, Asset asset) {
                    if (StringUtils.isEmpty(asset.getType())) {
                        throw new DataValidationException("Asset type should be specified!");
                    }
                    if (StringUtils.isEmpty(asset.getName())) {
                        throw new DataValidationException("Asset name should be specified!");
                    }
                    if (asset.getTenantId() == null) {
                        throw new DataValidationException("Asset should be assigned to tenant!");
                    } else {
                        Tenant tenant = tenantDao.findById(tenantId, asset.getTenantId().getId());
                        if (tenant == null) {
                            throw new DataValidationException("Asset is referencing to non-existent tenant!");
                        }
                    }
                    if (asset.getCustomerId() == null) {
                        asset.setCustomerId(new CustomerId(NULL_UUID));
                    } else if (!asset.getCustomerId().getId().equals(NULL_UUID)) {
                        Customer customer = customerDao.findById(tenantId, asset.getCustomerId().getId());
                        if (customer == null) {
                            throw new DataValidationException("Can't assign asset to non-existent customer!");
                        }
                        if (!customer.getTenantId().equals(asset.getTenantId())) {
                            throw new DataValidationException("Can't assign asset to customer from different tenant!");
                        }
                    }
                }
            };

    private PaginatedRemover<TenantId, Asset> tenantAssetsRemover =
            new PaginatedRemover<TenantId, Asset>() {

                @Override
                protected List<Asset> findEntities(TenantId tenantId, TenantId id, TextPageLink pageLink) {
                    return assetDao.findAssetsByTenantId(id.getId(), pageLink);
                }

                @Override
                protected void removeEntity(TenantId tenantId, Asset entity) {
                    deleteAsset(tenantId, new AssetId(entity.getId().getId()));
                }
            };

    private PaginatedRemover<CustomerId, Asset> customerAssetsRemover = new PaginatedRemover<CustomerId, Asset>() {
        @Override
        protected List<Asset> findEntities(TenantId tenantId, CustomerId id, TextPageLink pageLink) {
            return assetDao.findAssetsByTenantIdAndCustomerId(tenantId.getId(), id.getId(), pageLink);
        }

        @Override
        protected void removeEntity(TenantId tenantId, Asset entity) {
            deviceService.deleteDevicesBelongToAsset(tenantId,entity.getId());
            deleteAsset(tenantId, new AssetId(entity.getId().getId()));
        }
    };

    private PaginatedRemover<CustomerId, Asset> customerAssetsUnasigner = new PaginatedRemover<CustomerId, Asset>() {

        @Override
        protected List<Asset> findEntities(TenantId tenantId, CustomerId id, TextPageLink pageLink) {
            return assetDao.findAssetsByTenantIdAndCustomerId(tenantId.getId(), id.getId(), pageLink);
        }

        @Override
        protected void removeEntity(TenantId tenantId, Asset entity) {
            unassignAssetFromCustomer(tenantId, new AssetId(entity.getId().getId()));
        }
    };
}
