/**
 * Copyright © 2016-2018 The Thingsboard Authors
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.CustomerAndAssets;
import org.thingsboard.server.common.data.CustomerExInfo;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.TextPageData;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.common.data.security.Authority;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class CustomerController extends BaseController {

	public static final String CUSTOMER_ID = "customerId";
	public static final String IS_PUBLIC = "isPublic";

	@PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
	@RequestMapping(value = "/customer/{customerId}", method = RequestMethod.GET)
	@ResponseBody
	public Customer getCustomerById(@PathVariable(CUSTOMER_ID) String strCustomerId) throws ThingsboardException {
		checkParameter(CUSTOMER_ID, strCustomerId);
		try {
			CustomerId customerId = new CustomerId(toUUID(strCustomerId));
			return checkCustomerId(customerId);
		} catch (Exception e) {
			throw handleException(e);
		}
	}

	@PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
	@RequestMapping(value = "/customer/{customerId}/shortInfo", method = RequestMethod.GET)
	@ResponseBody
	public JsonNode getShortCustomerInfoById(@PathVariable(CUSTOMER_ID) String strCustomerId) throws ThingsboardException {
		checkParameter(CUSTOMER_ID, strCustomerId);
		try {
			CustomerId customerId = new CustomerId(toUUID(strCustomerId));
			Customer customer = checkCustomerId(customerId);
			ObjectMapper objectMapper = new ObjectMapper();
			ObjectNode infoObject = objectMapper.createObjectNode();
			infoObject.put("title", customer.getTitle());
			infoObject.put(IS_PUBLIC, customer.isPublic());
			return infoObject;
		} catch (Exception e) {
			throw handleException(e);
		}
	}

	@PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'CUSTOMER_USER')")
	@RequestMapping(value = "/customer/{customerId}/title", method = RequestMethod.GET, produces = "application/text")
	@ResponseBody
	public String getCustomerTitleById(@PathVariable(CUSTOMER_ID) String strCustomerId) throws ThingsboardException {
		checkParameter(CUSTOMER_ID, strCustomerId);
		try {
			CustomerId customerId = new CustomerId(toUUID(strCustomerId));
			Customer customer = checkCustomerId(customerId);
			return customer.getTitle();
		} catch (Exception e) {
			throw handleException(e);
		}
	}

	@PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'SYS_ADMIN')")
	@RequestMapping(value = "/customer", method = RequestMethod.POST)
	@ResponseBody
	public Customer saveCustomer(@RequestBody Customer customer,@RequestParam(required = false) String tenantIdStr) throws ThingsboardException {
		try {
			if(getCurrentUser().getAuthority() == Authority.SYS_ADMIN){
				TenantId tenantIdTmp = new TenantId(toUUID(tenantIdStr));
				checkTenantId(tenantIdTmp);
				TenantId tenantId = tenantService.findTenantById(tenantIdTmp).getId();

				customer.setTenantId(tenantId);
				Customer savedCustomer = checkNotNull(customerService.saveCustomer(customer));

				logEntityAction(savedCustomer.getId(), savedCustomer,
						savedCustomer.getId(),
						customer.getId() == null ? ActionType.ADDED : ActionType.UPDATED, null);

				return savedCustomer;
			} else {
				customer.setTenantId(getTenantId());
				Customer savedCustomer = checkNotNull(customerService.saveCustomer(customer));

				logEntityAction(savedCustomer.getId(), savedCustomer,
						savedCustomer.getId(),
						customer.getId() == null ? ActionType.ADDED : ActionType.UPDATED, null);
				return savedCustomer;
			}



		} catch (Exception e) {

			logEntityAction(emptyId(EntityType.CUSTOMER), customer,
					null, customer.getId() == null ? ActionType.ADDED : ActionType.UPDATED, e);

			throw handleException(e);
		}
	}
	@PreAuthorize("hasAuthority('SYS_ADMIN')")
	@RequestMapping(value = "/customer/admin/{customerId}", method = RequestMethod.DELETE)
	@ResponseStatus(value = HttpStatus.OK)
	public void deleteCustomerAdmin(@PathVariable(CUSTOMER_ID) String strCustomerId,@RequestParam(required = false) String tenantIdStr) throws ThingsboardException {
		checkParameter(CUSTOMER_ID, strCustomerId);
		try {
			TenantId tenantIdTmp = new TenantId(toUUID(tenantIdStr));
			checkTenantId(tenantIdTmp);
			TenantId tenantId = tenantService.findTenantById(tenantIdTmp).getId();

			CustomerId customerId = new CustomerId(toUUID(strCustomerId));
			Customer customer = checkCustomerIdAdmin(tenantId,customerId);
			customerService.deleteCustomer(tenantId, customer.getId());

			logEntityAction(customerId, customer,
					customer.getId(),
					ActionType.DELETED, null, strCustomerId);

		} catch (Exception e) {

			logEntityAction(emptyId(EntityType.CUSTOMER),
					null,
					null,
					ActionType.DELETED, e, strCustomerId);

			throw handleException(e);
		}
	}
	@PreAuthorize("hasAuthority('TENANT_ADMIN')")
	@RequestMapping(value = "/customer/{customerId}", method = RequestMethod.DELETE)
	@ResponseStatus(value = HttpStatus.OK)
	public void deleteCustomer(@PathVariable(CUSTOMER_ID) String strCustomerId) throws ThingsboardException {
		checkParameter(CUSTOMER_ID, strCustomerId);
		try {
			CustomerId customerId = new CustomerId(toUUID(strCustomerId));
			Customer customer = checkCustomerId(customerId);
			customerService.deleteCustomer(getTenantId(), customerId);

			logEntityAction(customerId, customer,
					customer.getId(),
					ActionType.DELETED, null, strCustomerId);

		} catch (Exception e) {

			logEntityAction(emptyId(EntityType.CUSTOMER),
					null,
					null,
					ActionType.DELETED, e, strCustomerId);

			throw handleException(e);
		}
	}

	/**
	* @Description: 1.2.8.14 以登录用户权限查询项目级别用户组
	* @Author: ShenJi
	* @Date: 2019/2/2
	* @Param: []
	* @return: java.util.List<org.thingsboard.server.common.data.CustomerExInfo>
	*/
	@PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'SYS_ADMIN')")
	@RequestMapping(value = "/currentUser/customers", method = RequestMethod.GET)
	@ResponseBody
	public List<CustomerExInfo> getCustomers() throws ThingsboardException {
		List<CustomerExInfo> retInfo = new ArrayList<>();
		switch (getCurrentUser().getAuthority()){
			case SYS_ADMIN:
				retInfo.addAll(customerService.findCustomerExinfos());
				break;
			case TENANT_ADMIN:
				retInfo.addAll(customerService.findCustomerByTenantIdExinfos(getCurrentUser().getTenantId()));
				break;
				default:
					throw new ThingsboardException(ThingsboardErrorCode.AUTHENTICATION);
		}

		return retInfo;

	}
	@PreAuthorize("hasAuthority('SYS_ADMIN')")
	@RequestMapping(value = "/admin/customers", params = {"limit"}, method = RequestMethod.GET)
	@ResponseBody
	public TextPageData<CustomerExInfo> getCustomers(@RequestParam int limit,
													 @RequestParam(required = false) String tenantIdStr,
													 @RequestParam(required = false) String textSearch,
													 @RequestParam(required = false) String idOffset,
													 @RequestParam(required = false) String textOffset) throws ThingsboardException {
		try {
			TextPageLink pageLink = createPageLink(limit, textSearch, idOffset, textOffset);
			if (tenantIdStr != null){
				TenantId tenantIdTmp = new TenantId(toUUID(tenantIdStr));
				checkTenantId(tenantIdTmp);
				TenantId tenantId = tenantService.findTenantById(tenantIdTmp).getId();

				// return checkNotNull(customerService.findCustomersByTenantId(tenantId, pageLink));
				return checkNotNull(customerService.findCustomerExInfosByTenantId(tenantId, pageLink));
			}
			else {
				return checkNotNull(customerService.findCustomerExInfos(pageLink));
			}

		} catch (Exception e) {
			throw handleException(e);
		}
	}

	/** 
	* @Description: 1.2.8.15 以登录用户权限查询项目组及所属资产
	* @Author: ShenJi
	* @Date: 2019/2/2 
	* @Param: [limit, tenantIdStr, textSearch, idOffset, textOffset] 
	* @return: java.util.List<org.thingsboard.server.common.data.CustomerAndAssets>
	*/ 
	@PreAuthorize("hasAnyAuthority('SYS_ADMIN','TENANT_ADMIN', 'CUSTOMER_USER')")
	@RequestMapping(value = "/currentUser/customersAndAssets", params = {"limit"}, method = RequestMethod.GET)
	@ResponseBody
	public List<CustomerAndAssets> getCustomersAndAssets(@RequestParam int limit,
														 @RequestParam(required = false) String tenantIdStr,
														 @RequestParam(required = false) String textSearch,
														 @RequestParam(required = false) String idOffset,
														 @RequestParam(required = false) String textOffset) throws ThingsboardException {
		try {
			List<CustomerAndAssets> retObj = new ArrayList<>();
			TextPageData<Customer> customerTextPageData = null;
			TextPageLink pageLink = null;
			CustomerId customerId = getCurrentUser().getCustomerId();
			switch (getCurrentUser().getAuthority()){
				case SYS_ADMIN:
					pageLink = createPageLink(limit, textSearch, idOffset, textOffset);
					if (tenantIdStr != null){
						TenantId tenantIdTmp = new TenantId(toUUID(tenantIdStr));
						checkTenantId(tenantIdTmp);
						TenantId tenantId = tenantService.findTenantById(tenantIdTmp).getId();

						customerTextPageData = checkNotNull(customerService.findCustomersByTenantId(tenantId, pageLink));
					}
					else {
						customerTextPageData = checkNotNull(customerService.findCustomers(pageLink));
					}
					break;
				case TENANT_ADMIN:
					customerTextPageData = checkNotNull(customerService.findCustomersByTenantId(getCurrentUser().getTenantId(), pageLink));
					break;
				case CUSTOMER_USER:
					customerTextPageData = checkNotNull(customerService.findCustomersByTenantId(getCurrentUser().getTenantId(), pageLink));
					List<Customer> customerList = new ArrayList<>();
					customerList = customerTextPageData.getData().stream().filter(customer -> customerId.equals(customer.getId())).collect(Collectors.toList());
					customerList.stream().forEach(customer -> {retObj.add(new CustomerAndAssets(customer,assetService.findAssetExInfoByTenantAndCustomer(customer.getTenantId(),customer.getId(),new TextPageLink(Integer.MAX_VALUE)).getData()));});
					return retObj;
				default:
						throw new ThingsboardException(ThingsboardErrorCode.AUTHENTICATION);
			}

			customerTextPageData.getData().forEach(customer -> {
				retObj.add(new CustomerAndAssets(customer,assetService.findAssetExInfoByTenantAndCustomer(customer.getTenantId(),customer.getId(),new TextPageLink(Integer.MAX_VALUE)).getData()));
			});
			return retObj;

		} catch (Exception e) {
			throw handleException(e);
		}
	}

	@PreAuthorize("hasAuthority('TENANT_ADMIN')")
	@RequestMapping(value = "/customers", params = {"limit"}, method = RequestMethod.GET)
	@ResponseBody
	public TextPageData<Customer> getCustomers(@RequestParam int limit,
											   @RequestParam(required = false) String textSearch,
											   @RequestParam(required = false) String idOffset,
											   @RequestParam(required = false) String textOffset) throws ThingsboardException {
		try {
			TextPageLink pageLink = createPageLink(limit, textSearch, idOffset, textOffset);
			TenantId tenantId = getCurrentUser().getTenantId();
			return checkNotNull(customerService.findCustomersByTenantId(tenantId, pageLink));
		} catch (Exception e) {
			throw handleException(e);
		}
	}

	@PreAuthorize("hasAuthority('TENANT_ADMIN')")
	@RequestMapping(value = "/tenant/customers", params = {"customerTitle"}, method = RequestMethod.GET)
	@ResponseBody
	public Customer getTenantCustomer(
			@RequestParam String customerTitle) throws ThingsboardException {
		try {
			TenantId tenantId = getCurrentUser().getTenantId();
			return checkNotNull(customerService.findCustomerByTenantIdAndTitle(tenantId, customerTitle));
		} catch (Exception e) {
			throw handleException(e);
		}
	}
}
