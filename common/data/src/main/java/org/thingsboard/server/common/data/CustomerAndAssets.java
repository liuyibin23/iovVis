package org.thingsboard.server.common.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.asset.AssetExInfo;

import java.util.ArrayList;
import java.util.List;
@Data
@NoArgsConstructor
@AllArgsConstructor

public class CustomerAndAssets extends Customer {

	List<AssetExInfo> assetList = new ArrayList<>();

	public CustomerAndAssets(Customer customer){
		this.id = customer.getId();
		this.setAdditionalInfo(customer.getAdditionalInfo());
		this.address = customer.address;
		this.address2 = customer.address2;
		this.city = customer.city;
		this.country = customer.country;
		this.email = customer.email;
		this.phone = customer.phone;
		//search_text
		this.state = customer.state;
		this.setTenantId(customer.getTenantId());
		this.setTitle(customer.getTitle());
		this.zip = customer.zip;
		this.setAdminCount(customer.getAdminCount());
		this.setUserCount(customer.getUserCount());
		this.setInfrastructureCount(customer.getInfrastructureCount());
	}

	public CustomerAndAssets(Customer customer,List<AssetExInfo> assetList){
		this.id = customer.getId();
		this.setAdditionalInfo(customer.getAdditionalInfo());
		this.address = customer.address;
		this.address2 = customer.address2;
		this.city = customer.city;
		this.country = customer.country;
		this.email = customer.email;
		this.phone = customer.phone;
		//search_text
		this.state = customer.state;
		this.setTenantId(customer.getTenantId());
		this.setTitle(customer.getTitle());
		this.zip = customer.zip;
		this.setAdminCount(customer.getAdminCount());
		this.setUserCount(customer.getUserCount());
		this.setInfrastructureCount(customer.getInfrastructureCount());
		this.assetList = assetList;
	}

}
