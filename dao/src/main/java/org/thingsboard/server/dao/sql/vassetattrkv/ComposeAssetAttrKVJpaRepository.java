package org.thingsboard.server.dao.sql.vassetattrkv;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.dao.model.sql.ComposeAssetAttrKV;

import java.util.List;

public interface ComposeAssetAttrKVJpaRepository extends CrudRepository<ComposeAssetAttrKV,String> {

//    	@Query(value = "select entity_type, entity_id, attribute_type, attribute_key, bool_v, str_v, long_v, dbl_v, last_update_ts from attribute_kv rc where rc.attribute_key = ?1",nativeQuery = true)
//        List<ComposeAssetAttrKV> findbyAttributeKey(String attributeKey);

//    	@Query(value = "SELECT a.entity_type, a.entity_id, a.attribute_type, a.attribute_key as attr_key1,a.str_v as strv1,b.attribute_key as attr_key2, b.str_v as strv2, a.last_update_ts " +
//				"FROM (SELECT * FROM public.attribute_kv WHERE attribute_key = 'areaDivide') a " +
//				"join " +
//				"(SELECT * FROM public.attribute_kv WHERE attribute_key = 'basicInfo') b  " +
//				"on a.entity_id = b.entity_id  " +
//				"WHERE a.entity_id in (SELECT id FROM public.asset WHERE tenant_id = '1e918a07edf05d0893415428c622f6e')"
//                ,nativeQuery = true)
//	    List<ComposeAssetAttrKV> findByComposekey();//String key1,String key2 areaDivide

	@Query(value = "SELECT a.entity_type, a.entity_id, a.attribute_type, a.attribute_key as attr_key1,a.str_v as strv1,b.attribute_key as attr_key2, b.str_v as strv2, a.last_update_ts " +
			"FROM (SELECT * FROM public.attribute_kv WHERE attribute_key = :attr_key1) a " +
			"join " +
			"(SELECT * FROM public.attribute_kv WHERE attribute_key = :attr_key2) b  " +
			"on a.entity_id = b.entity_id  " +
			"WHERE a.entity_id in (SELECT id FROM public.asset)"
			,nativeQuery = true)
	List<ComposeAssetAttrKV> findByComposekey(@Param("attr_key1")String attr_key1, @Param("attr_key2")String attr_key2);

	@Query(value = "SELECT a.entity_type, a.entity_id, a.attribute_type, a.attribute_key as attr_key1,a.str_v as strv1,b.attribute_key as attr_key2, b.str_v as strv2, a.last_update_ts " +
			"FROM (SELECT * FROM public.attribute_kv WHERE attribute_key = :attr_key1) a " +
			"join " +
			"(SELECT * FROM public.attribute_kv WHERE attribute_key = :attr_key2) b  " +
			"on a.entity_id = b.entity_id  " +
			"WHERE a.entity_id in (SELECT id FROM public.asset WHERE tenant_id = :tenantId)"
			,nativeQuery = true)
	List<ComposeAssetAttrKV> findByTenantIdAndComposekey(@Param("tenantId") String tenantId, @Param("attr_key1")String attr_key1, @Param("attr_key2")String attr_key2);//1e918a07edf05d0893415428c622f6e

	@Query(value = "SELECT a.entity_type, a.entity_id, a.attribute_type, a.attribute_key as attr_key1,a.str_v as strv1,b.attribute_key as attr_key2, b.str_v as strv2, a.last_update_ts " +
			"FROM (SELECT * FROM public.attribute_kv WHERE attribute_key = :attr_key1) a " +
			"join " +
			"(SELECT * FROM public.attribute_kv WHERE attribute_key = :attr_key2) b  " +
			"on a.entity_id = b.entity_id  " +
			"WHERE a.entity_id in (SELECT id FROM public.asset WHERE customer_id = :customerId)"
			,nativeQuery = true)
	List<ComposeAssetAttrKV> findByCustomerIdAndComposekey(@Param("customerId") String customerId, @Param("attr_key1")String attr_key1, @Param("attr_key2")String attr_key2);//1e918a07edf05d0893415428c622f6e
}
