package org.thingsboard.server.common.data;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class TenantExInfo extends Tenant{

    private List<String> adminUserNameList = new ArrayList<>();
    private List<String> userNameList = new ArrayList<>();

    public TenantExInfo(){}

    public TenantExInfo(Tenant tenant){
        this.id = tenant.getId();
        this.setAdditionalInfo(tenant.getAdditionalInfo());
        this.address = tenant.address;
        this.address2 = tenant.address2;
        this.city = tenant.city;
        this.country = tenant.country;
        this.email = tenant.email;
        this.phone = tenant.phone;
        this.setRegion(tenant.getRegion());
        //search_text
        this.state = tenant.state;
        this.setTitle(tenant.getTitle());
        this.zip = tenant.zip;
        this.setAdminCount(tenant.getAdminCount());
        this.setUserCount(tenant.getUserCount());
    }

}
