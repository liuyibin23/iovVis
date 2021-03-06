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
package org.thingsboard.server.dao.customer;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.CustomerExInfo;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.TextPageData;
import org.thingsboard.server.common.data.page.TextPageLink;

import java.util.List;
import java.util.Optional;

public interface CustomerService {

    Customer findCustomerById(TenantId tenantId, CustomerId customerId);

    Optional<Customer> findCustomerByTenantIdAndTitle(TenantId tenantId, String title);

    ListenableFuture<Customer> findCustomerByIdAsync(TenantId tenantId, CustomerId customerId);

    Customer saveCustomer(Customer customer);

    void deleteCustomer(TenantId tenantId, CustomerId customerId);

    Customer findOrCreatePublicCustomer(TenantId tenantId);

    TextPageData<Customer> findCustomersByTenantId(TenantId tenantId, TextPageLink pageLink);

    TextPageData<CustomerExInfo> findCustomerExInfosByTenantId(TenantId tenantId, TextPageLink pageLink);

    TextPageData<Customer> findCustomers(TextPageLink pageLink);

    TextPageData<CustomerExInfo> findCustomerExInfos(TextPageLink pageLink);

    List<CustomerExInfo> findCustomerExinfos();

    List<CustomerExInfo> findCustomerByTenantIdExinfos(TenantId tenantId);

    void deleteCustomersByTenantId(TenantId tenantId);

    TenantId findTenantIdByCustomerId(CustomerId customerId, TextPageLink pageLink);

    int countCustomerByTenantId(TenantId tenantId);

    Customer findCustomerByTitle(TenantId tenantId, String customer);
}
