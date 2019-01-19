package org.thingsboard.server.dao.vassetattrkv;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.server.dao.model.sql.ComposeAssetAttrKV;
import org.thingsboard.server.dao.model.sql.VassetAttrKV;
import org.thingsboard.server.dao.sql.vassetattrkv.ComposeAssetAttrKVJpaRepository;
import org.thingsboard.server.dao.sql.vassetattrkv.VassetAttrKVRepository;

import java.util.List;

@Service()
public class VassetAttrKVServiceImpl implements VassetAttrKVService {

	@Autowired
	private VassetAttrKVRepository vassetAttrKVRepository;
	@Autowired
	private ComposeAssetAttrKVJpaRepository composeAssetAttrKVJpaRepository;
	@Override
	public List<VassetAttrKV> getVassetAttrKV() {
		return vassetAttrKVRepository.findAll();
	}

	@Override
	public List<VassetAttrKV> findAll() {
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

	@Override
	public List<VassetAttrKV> findbyAttributeKey(String attributeKey) {
        return vassetAttrKVRepository.findbyAttributeKey(attributeKey);
	}

	@Override
	public List<VassetAttrKV> findbyAttributeKeyAndValueLike(String attributeKey, String strV) {
		return vassetAttrKVRepository.findbyAttributeKeyAndValueLink(attributeKey,strV);
	}

	@Override
	public List<VassetAttrKV> findbyAttributeValueLike(String strV) {
		return vassetAttrKVRepository.findbyAttributeValueLink(strV);
	}

    @Override
	public List<ComposeAssetAttrKV> findByComposekey(String attrKey1,String attrKey2){
		return composeAssetAttrKVJpaRepository.findByComposekey(attrKey1,attrKey2);
	}

    @Override
	public List<ComposeAssetAttrKV> findByTenantIdAndComposekey(String tenantId, String attrKey1, String attrKey2){
		return composeAssetAttrKVJpaRepository.findByTenantIdAndComposekey(tenantId,attrKey1,attrKey2);
	}
}
