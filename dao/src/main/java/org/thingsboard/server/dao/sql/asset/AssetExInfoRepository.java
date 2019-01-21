package org.thingsboard.server.dao.sql.asset;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.dao.model.sql.AssetExInfoEntity;

import java.util.List;

public interface AssetExInfoRepository extends CrudRepository<AssetExInfoEntity, String> {

    @Query(value = "SELECT a.*,b.basicinfo FROM asset a,asset_attributes b where a.id = b.entity_id AND (a.tenant_id = ?1 or ?1 is null)"
            ,nativeQuery = true)
    List<AssetExInfoEntity> findAssetExInfoByTenantId(String tenantId);
}
