package org.thingsboard.server.dao.model.sql;

import javax.persistence.*;

@Entity
@Table(name = "vassetattrkv")
public class VassetAttrKV {
	@Column(name = "entity_type")
	private String entityType;
	@Column(name = "entity_id")
	private String entityId;
	@Column(name = "attribute_type")
	private String attributeType;
	@Column(name = "attribute_key")
	private String attributeKey;


	@Column(name = "str_v")
	private String strV;
	@Column(name = "bool_v")
	private Boolean boolV;

	@Column(name = "long_v")
	private Long longV;
	@Column(name = "dbl_v")
	private Double dblV;
	@Column(name = "last_update_ts")
	private Long lastUpdateTs;

	@Id
	@Column(name = "id")
	private String id;
	@Column(name = "additional_info")
	private String additionalInfo;
	@Column(name = "customer_id")
	private String customerId;
	@Column(name = "name")
	private String name;
	@Column(name = "search_text")
	private String searchText;
	@Column(name = "tenant_id")
	private String tenantId;
	@Column(name = "type")
	private String type;

	public String getEntityType() {
		return entityType;
	}

	public void setEntityType(String entityType) {
		this.entityType = entityType;
	}

	public String getEntityId() {
		return entityId;
	}

	public void setEntityId(String entityId) {
		this.entityId = entityId;
	}

	public String getAttributeType() {
		return attributeType;
	}

	public void setAttributeType(String attributeType) {
		this.attributeType = attributeType;
	}

	public String getAttributeKey() {
		return attributeKey;
	}

	public void setAttributeKey(String attributeKey) {
		this.attributeKey = attributeKey;
	}

	public String getStrV() {
		return strV;
	}

	public void setStrV(String strV) {
		this.strV = strV;
	}

	public Boolean getBoolV() {
		return boolV;
	}

	public void setBoolV(Boolean boolV) {
		this.boolV = boolV;
	}

	public Long getLongV() {
		return longV;
	}

	public void setLongV(Long longV) {
		this.longV = longV;
	}

	public Double getDblV() {
		return dblV;
	}

	public void setDblV(Double dblV) {
		this.dblV = dblV;
	}

	public Long getLastUpdateTs() {
		return lastUpdateTs;
	}

	public void setLastUpdateTs(Long lastUpdateTs) {
		this.lastUpdateTs = lastUpdateTs;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getAdditionalInfo() {
		return additionalInfo;
	}

	public void setAdditionalInfo(String additionalInfo) {
		this.additionalInfo = additionalInfo;
	}

	public String getCustomerId() {
		return customerId;
	}

	public void setCustomerId(String customerId) {
		this.customerId = customerId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getSearchText() {
		return searchText;
	}

	public void setSearchText(String searchText) {
		this.searchText = searchText;
	}

	public String getTenantId() {
		return tenantId;
	}

	public void setTenantId(String tenantId) {
		this.tenantId = tenantId;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}
}
