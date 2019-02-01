package org.thingsboard.server.dao.sql.tshourvaluestatistic;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.dao.model.sql.TsHourValueStatisticCompositeKey;
import org.thingsboard.server.dao.model.sql.TsHourValueStatisticEntity;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.List;

@SqlDao
public interface TsHourValueStatisticRepository extends CrudRepository<TsHourValueStatisticEntity,TsHourValueStatisticCompositeKey> {


    @Query(value = "SELECT (ts / (3600*1000) * (3600*1000)) as tshour " +
            "FROM ts_hour_value_statistic  " +
            "WHERE (entity_type = :entityType  or :entityType is null)  " +
            "AND ts BETWEEN :startTs and :endTs  " +
            "GROUP BY tshour  " +
            "ORDER BY tshour",
    nativeQuery = true)
    List<Object> findTsHours(@Param("entityType")String entityType,
                               @Param("startTs")long startTs,
                               @Param("endTs")long endTs);

    @Query(value = "SELECT (ts /(3600*1000) * (3600*1000)) as tshour " +
            "FROM ts_hour_value_statistic  " +
            "WHERE (entity_type = :entityType  or :entityType is null)  " +
            "AND (customer_id = :customerId or :customerId is null)" +
            "AND (tenant_id = :tenantId or :tenantId is null)" +
            "AND ts BETWEEN :startTs and :endTs  " +
            "GROUP BY tshour  " +
            "ORDER BY tshour",
            nativeQuery = true)
    List<Object> findTsHoursByTenantIdAndCustomerId(@Param("entityType")String entityType,
                                                    @Param("customerId") String customerId,
                                                    @Param("tenantId") String tenantId,
                                                    @Param("startTs")long startTs,
                                                    @Param("endTs")long endTs);

    @Query(value = "SELECT (ts / (3600*1000) * (3600*1000)) as tshour " +
            "FROM ts_hour_value_statistic  " +
            "WHERE (entity_type = :entityType  or :entityType is null)  " +
            "AND (tenant_id = :tenantId or :tenantId is null)" +
            "AND ts BETWEEN :startTs and :endTs  " +
            "GROUP BY tshour  " +
            "ORDER BY tshour",
            nativeQuery = true)
    List<Object> findTsHoursByTenantId(@Param("entityType")String entityType,
                                       @Param("tenantId") String tenantId,
                                       @Param("startTs")long startTs,
                                       @Param("endTs")long endTs);

    @Query(value="SELECT (ts / (3600*1000) * (3600*1000)) as tshour " +
            "FROM ts_hour_value_statistic  " +
            "WHERE entity_id = :entityId  " +
            "AND ts BETWEEN :startTs and :endTs  " +
            "GROUP BY tshour  " +
            "ORDER BY tshour",
    nativeQuery = true)
    List<Object> findTsHoursByEntityId(@Param("entityId")String entityId,
                                         @Param("startTs")long startTs,
                                         @Param("endTs")long endTs);
}
