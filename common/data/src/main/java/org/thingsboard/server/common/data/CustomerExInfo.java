package org.thingsboard.server.common.data;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class CustomerExInfo extends Customer {

    private List<String> adminUserNameList = new ArrayList<>();

    public CustomerExInfo(){}
    public CustomerExInfo(Customer customer){
        this.id = customer.getId();
        this.setAdditionalInfo(customer.getAdditionalInfo());
        this.address = customer.address;
        this.address2 = customer.address2;
        this.city = customer.city;
        this.country = customer.country;
        this.email = customer.email;
        this.phone = customer.phone;
        this.createdTime = customer.createdTime;
        //search_text
        this.state = customer.state;
        this.setTenantId(customer.getTenantId());
        this.setTitle(customer.getTitle());
        this.zip = customer.zip;
        this.setAdminCount(customer.getAdminCount());
        this.setUserCount(customer.getUserCount());
        this.setInfrastructureCount(customer.getInfrastructureCount());
    }

}
