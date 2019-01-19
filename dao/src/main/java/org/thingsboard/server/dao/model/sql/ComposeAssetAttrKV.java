package org.thingsboard.server.dao.model.sql;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "composeassetattrkv")
@Data
public class ComposeAssetAttrKV {

    @Column(name = "entity_type")
    private String entityType;
    @Id
    @Column(name = "entity_id")
    private String entityId;
    @Column(name = "attribute_type")
    private String attributeType;
//    @Column(name = "attribute_key")
//    private String attributeKey;


//    @Column(name = "str_v")
//    private String strV;
//    @Column(name = "bool_v")
//    private Boolean boolV;
//
//    @Column(name = "long_v")
//    private Long longV;
//    @Column(name = "dbl_v")
//    private Double dblV;
    @Column(name = "attr_key1")
    private  String attr_key1;
    @Column(name = "strv1")
    private String strv1;
    @Column(name = "attr_key2")
    private String attr_key2;
    @Column(name = "strv2")
    private String strv2;
    @Column(name = "last_update_ts")
    private Long lastUpdateTs;
//    @Column(name = "composekey")
//	private String composekey;
//	@Column(name = "composestrv")
//	private String composestrv;
}
