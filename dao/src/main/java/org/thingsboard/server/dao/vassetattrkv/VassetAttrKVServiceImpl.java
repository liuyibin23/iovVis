package org.thingsboard.server.dao.vassetattrkv;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.server.dao.model.sql.VassetAttrKV;
import org.thingsboard.server.dao.sql.vassetattrkv.VassetAttrKVRepository;

import java.util.List;

@Service()
public class VassetAttrKVServiceImpl implements VassetAttrKVService {

	@Autowired
	private VassetAttrKVRepository vassetAttrKVRepository;
	@Override
	public List<VassetAttrKV> getVassetAttrKV() {
		return vassetAttrKVRepository.findAll();
	}
	@Override
	public List<VassetAttrKV> findbytenantId(String tenantId)
	{
		return vassetAttrKVRepository.findbyTenantId(tenantId);

	}

	@Override
	public List<VassetAttrKV> findbyAttributeKey(String attributeKey,String tenantId)
	{
		return vassetAttrKVRepository.findbyAttributeKey(attributeKey,tenantId);
	}
	@Override
	public List<VassetAttrKV> findbyAttributeKeyAndValueLike(String attributeKey, String tenantId, String strV){
		return vassetAttrKVRepository.findbyAttributeKeyAndValueLinkWithTenantId(attributeKey,tenantId,strV);
	}
	@Override
	public List<VassetAttrKV> findbyAttributeValueLike(String tenantId, String strV)
	{
		return vassetAttrKVRepository.findbyAttributeValueLink(tenantId,strV);
	}
}
