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
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.TextPageData;
import org.thingsboard.server.common.data.page.TextPageLink;

import java.util.ArrayList;
import java.util.List;

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

	@PreAuthorize("hasAuthority('TENANT_ADMIN')")
	@RequestMapping(value = "/customer", method = RequestMethod.POST)
	@ResponseBody
	public Customer saveCustomer(@RequestBody Customer customer) throws ThingsboardException {
		try {
			customer.setTenantId(getCurrentUser().getTenantId());
			Customer savedCustomer = checkNotNull(customerService.saveCustomer(customer));

			logEntityAction(savedCustomer.getId(), savedCustomer,
					savedCustomer.getId(),
					customer.getId() == null ? ActionType.ADDED : ActionType.UPDATED, null);

			return savedCustomer;
		} catch (Exception e) {

			logEntityAction(emptyId(EntityType.CUSTOMER), customer,
					null, customer.getId() == null ? ActionType.ADDED : ActionType.UPDATED, e);

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

	@PreAuthorize("hasAuthority('SYS_ADMIN')")
	@RequestMapping(value = "/admin/customers", params = {"limit"}, method = RequestMethod.GET)
	@ResponseBody
	public TextPageData<Customer> getCustomers(@RequestParam int limit,
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

				return checkNotNull(customerService.findCustomersByTenantId(tenantId, pageLink));
			}
			else {
				return checkNotNull(customerService.findCustomers(pageLink));
			}

		} catch (Exception e) {
			throw handleException(e);
		}
	}
	@PreAuthorize("hasAuthority('SYS_ADMIN')")
	@RequestMapping(value = "/admin/customersAndAssets", params = {"limit"}, method = RequestMethod.GET)
	@ResponseBody
	public List<CustomerAndAssets> getCustomersAndAssets(@RequestParam int limit,
														 @RequestParam(required = false) String tenantIdStr,
														 @RequestParam(required = false) String textSearch,
														 @RequestParam(required = false) String idOffset,
														 @RequestParam(required = false) String textOffset) throws ThingsboardException {
		try {
			List<CustomerAndAssets> retObj = new ArrayList<>();
			TextPageData<Customer> customerTextPageData;
			TextPageLink pageLink = createPageLink(limit, textSearch, idOffset, textOffset);
			if (tenantIdStr != null){
				TenantId tenantIdTmp = new TenantId(toUUID(tenantIdStr));
				checkTenantId(tenantIdTmp);
				TenantId tenantId = tenantService.findTenantById(tenantIdTmp).getId();

				customerTextPageData = checkNotNull(customerService.findCustomersByTenantId(tenantId, pageLink));
			}
			else {
				customerTextPageData = checkNotNull(customerService.findCustomers(pageLink));
			}
			customerTextPageData.getData().forEach(customer -> {
				retObj.add(new CustomerAndAssets(customer,assetService.findAssetByTenantAndCustomer(customer.getTenantId(),customer.getId())));
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
