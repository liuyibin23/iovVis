package org.thingsboard.server.dao.model.sql;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Table;

@Data
@Table(name = "asset_attributes")
public class AssetAttrsEntity {

    @Column(name = "entity_id")
    private String entity_id;
    @Column(name = "areadivide")
    private String areadivide;
    @Column(name = "basicinfo")
    private String basicinfo;
    @Column(name = "cardinfo")
    private String cardinfo;
    @Column(name = "structureinfo")
    private String structureinfo;
}
