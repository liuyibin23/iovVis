package org.thingsboard.server.dao.sql.asset;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.dao.model.sql.AssetExInfoEntity;

import java.util.List;

public interface AssetExInfoRepository extends CrudRepository<AssetExInfoEntity, String> {

    @Query(value = "SELECT a.*,b.basicinfo,b.warning_rule_cfg FROM asset a,asset_attributes b where a.id = b.entity_id " +
            "AND (a.tenant_id = :tenantId or :tenantId is null) " +
            "AND a.search_text LIKE LOWER(CONCAT('%',:searchText, '%')) " +
            "AND a.id > :idOffset ORDER BY a.id LIMIT :pageLimit"
            ,nativeQuery = true)
    List<AssetExInfoEntity> findAssetExInfoByTenantId(@Param("tenantId")String tenantId,
                                                      @Param("searchText") String searchText,
                                                      @Param("idOffset") String idOffset,
                                                      @Param("pageLimit") int pageLimit);

    @Query(value = "SELECT a.*,b.basicinfo,b.warning_rule_cfg FROM asset a,asset_attributes b where a.id = b.entity_id AND (a.tenant_id = :tenantId or :tenantId is null) " +
            "AND (a.customer_id = :customerId or :customerId is null) " +
            "AND a.search_text LIKE LOWER(CONCAT('%',:searchText, '%')) " +
            "AND a.id > :idOffset ORDER BY a.id LIMIT :pageLimit"
            ,nativeQuery = true)
    List<AssetExInfoEntity> findAssetExInfoByTenantIdAndCustomerId(@Param("tenantId")String tenantId,
                                                                   @Param("customerId")String customerId,
                                                                   @Param("searchText") String searchText,
                                                                   @Param("idOffset") String idOffset,
                                                                   @Param("pageLimit") int pageLimit);

    @Query(value = "SELECT a.*,b.basicinfo,b.warning_rule_cfg FROM asset a,asset_attributes b WHERE a.id = b.entity_id " +
            "AND a.search_text LIKE LOWER(CONCAT('%',:searchText, '%')) " +
            "AND a.id > :idOffset ORDER BY a.id LIMIT :pageLimit"
            ,nativeQuery = true)
    List<AssetExInfoEntity> findAllAssetExInfo(@Param("searchText") String searchText,
                                               @Param("idOffset") String idOffset,
                                               @Param("pageLimit") int pageLimit);

}
