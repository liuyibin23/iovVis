package org.thingsboard.server.dao.model.sql;

import lombok.*;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "deviceattrkv")

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DeviceAttrKV {
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
	@Id
	@Column(name = "id")
	private String id;
}
