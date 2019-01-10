package org.thingsboard.server.dao.sql.vdeviceattrkv;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.dao.model.sql.DeviceAttrKV;

import java.util.List;

public interface DeviceAttrKVJpaRepository extends CrudRepository<DeviceAttrKV,String> {

	List<DeviceAttrKV> findAll();

	@Query("select rc from DeviceAttrKV rc where rc.tenantId = :tenantId")
	public List<DeviceAttrKV> findbyTenantId(@Param("tenantId") String tenantId);

	@Query("select rc from DeviceAttrKV rc where rc.tenantId = :tenantId AND rc.attributeKey = :attributeKey")
	List<DeviceAttrKV> findbyAttributeKey(@Param("attributeKey") String attributeKey,
										  @Param("tenantId") String tenantId);
	@Query("select DISTINCT rc.strV from DeviceAttrKV rc where rc.tenantId = :tenantId AND rc.attributeKey = :attributeKey AND strV LIKE :strV% ")
	List<DeviceAttrKV> findbyAttributeKeyAndValueLinkWithTenantId(@Param("attributeKey") String attributeKey,
																  @Param("tenantId") String tenantId,
																  @Param("strV") String strV);
	@Query("select rc from DeviceAttrKV rc where rc.tenantId = :tenantId AND strV LIKE :strV%")
	List<DeviceAttrKV> findbyAttributeValueLink(@Param("tenantId") String tenantId,@Param("strV") String strV);
}
