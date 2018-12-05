package org.thingsboard.server.dao.vassetattrkv;



import org.thingsboard.server.dao.model.sql.VassetAttrKV;

import java.util.List;

public interface VassetAttrKVService {
	List<VassetAttrKV> getVassetAttrKV();

	List<VassetAttrKV> findbytenantId(String tenandId);

	List<VassetAttrKV> findbyAttributeKey(String attributeKey, String tenantId);

	List<VassetAttrKV> findbyAttributeKeyAndValueLike(String attributeKey, String tenantId, String strV);

	List<VassetAttrKV> findbyAttributeValueLike(String tenantId, String strV);
}
