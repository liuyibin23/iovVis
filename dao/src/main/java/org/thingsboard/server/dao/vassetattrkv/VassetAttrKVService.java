package org.thingsboard.server.dao.vassetattrkv;



import org.thingsboard.server.dao.model.sql.ComposeAssetAttrKV;
import org.thingsboard.server.dao.model.sql.VassetAttrKV;

import java.util.List;

public interface VassetAttrKVService {
	List<VassetAttrKV> getVassetAttrKV();

	List<VassetAttrKV> findAll();

	List<VassetAttrKV> findbytenantId(String tenandId);

	List<VassetAttrKV> findbyAttributeKey(String attributeKey, String tenantId);

	List<VassetAttrKV> findbyAttributeKeyAndValueLike(String attributeKey, String tenantId, String strV);

	List<VassetAttrKV> findbyAttributeValueLike(String tenantId, String strV);

	List<VassetAttrKV> findbyAttributeKey(String attributeKey);

	List<VassetAttrKV> findbyAttributeKeyAndValueLike(String attributeKey,String strV);

	List<VassetAttrKV> findbyAttributeValueLike(String strV);

	List<ComposeAssetAttrKV> findByComposekey(String attrKey1, String attrKey2);

	List<ComposeAssetAttrKV> findByTenantIdAndComposekey(String tenantId, String attrKey1, String attrKey2);
}
