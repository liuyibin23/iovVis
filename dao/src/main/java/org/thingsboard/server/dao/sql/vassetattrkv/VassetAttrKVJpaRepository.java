package org.thingsboard.server.dao.sql.vassetattrkv;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.dao.model.sql.VassetAttrKV;

import java.io.Serializable;
import java.util.List;

@NoRepositoryBean
public interface VassetAttrKVJpaRepository extends CrudRepository<VassetAttrKV,String> {
	List<VassetAttrKV> findAll();

	@Query("select rc from VassetAttrKV rc where rc.tenantId = :tenantId")
	public List<VassetAttrKV> findbyTenantId(@Param("tenantId") String tenantId);

	@Query("select rc from VassetAttrKV rc where rc.attributeKey = :attributeKey")
	List<VassetAttrKV> findbyAttributeKey(@Param("attributeKey") String attributeKey);

	@Query("select rc from VassetAttrKV rc where rc.tenantId = :tenantId AND rc.attributeKey = :attributeKey")
	List<VassetAttrKV> findbyAttributeKey(@Param("attributeKey") String attributeKey,
										  @Param("tenantId") String tenantId);
	@Query("select DISTINCT rc.strV from VassetAttrKV rc where rc.tenantId = :tenantId AND rc.attributeKey = :attributeKey AND strV LIKE :strV% ")
	List<VassetAttrKV> findbyAttributeKeyAndValueLink(@Param("attributeKey") String attributeKey,
													  @Param("strV") String strV);
	@Query("select DISTINCT rc.strV from VassetAttrKV rc where rc.tenantId = :tenantId AND rc.attributeKey = :attributeKey AND strV LIKE :strV% ")
	List<VassetAttrKV> findbyAttributeKeyAndValueLinkWithTenantId(@Param("attributeKey") String attributeKey,
																  @Param("tenantId") String tenantId,
																  @Param("strV") String strV);
	@Query("select rc from VassetAttrKV rc where strV LIKE :strV%")
	List<VassetAttrKV> findbyAttributeValueLink(@Param("strV") String strV);

	@Query("select rc from VassetAttrKV rc where rc.tenantId = :tenantId AND strV LIKE :strV%")
	List<VassetAttrKV> findbyAttributeValueLink(@Param("tenantId") String tenantId,@Param("strV") String strV);
}
