package org.thingsboard.server.common.data.asset;

import lombok.Data;
import org.thingsboard.server.common.data.kv.AttributeKvData;

import java.util.ArrayList;
import java.util.List;

@Data
public class AssetExInfo extends Asset{

    private String basicinfo;
    private String warningRuleCfg;//告警规则配置
    /**
     * 资产属性列表
     */
    private List<AttributeKvData> assetAttrKv = new ArrayList<>();
}
