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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.CustomerExInfo;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.TextPageData;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.dashboard.DashboardService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.entity.AbstractEntityService;
import org.thingsboard.server.dao.entityview.EntityViewService;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.exception.IncorrectParameterException;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.service.PaginatedRemover;
import org.thingsboard.server.dao.service.Validator;
import org.thingsboard.server.dao.tenant.TenantDao;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.dao.user.UserService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.thingsboard.server.common.data.UUIDConverter.fromTimeUUID;
import static org.thingsboard.server.dao.service.Validator.validateId;

@Service
@Slf4j
public class CustomerServiceImpl extends AbstractEntityService implements CustomerService {

    private static final String PUBLIC_CUSTOMER_TITLE = "Public";
    public static final String INCORRECT_CUSTOMER_ID = "Incorrect customerId ";
    public static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";

    @Autowired
    private CustomerDao customerDao;

    @Autowired
    private UserService userService;

    @Autowired
    private TenantDao tenantDao;

    @Autowired
    private TenantService tenantService;

    @Autowired
    private AssetService assetService;

    @Autowired
    private DeviceService deviceService;

    @Autowired
    private EntityViewService entityViewService;

    @Autowired
    private DashboardService dashboardService;

    @Override
    public Customer findCustomerById(TenantId tenantId, CustomerId customerId) {
        log.trace("Executing findCustomerById [{}]", customerId);
        Validator.validateId(customerId, INCORRECT_CUSTOMER_ID + customerId);
        return customerDao.findById(tenantId, customerId.getId());
    }

    @Override
    public Optional<Customer> findCustomerByTenantIdAndTitle(TenantId tenantId, String title) {
        log.trace("Executing findCustomerByTenantIdAndTitle [{}] [{}]", tenantId, title);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        return customerDao.findCustomersByTenantIdAndTitle(tenantId.getId(), title);
    }

    @Override
    public ListenableFuture<Customer> findCustomerByIdAsync(TenantId tenantId, CustomerId customerId) {
        log.trace("Executing findCustomerByIdAsync [{}]", customerId);
        validateId(customerId, INCORRECT_CUSTOMER_ID + customerId);
        return customerDao.findByIdAsync(tenantId, customerId.getId());
    }

    @Override
    public Customer saveCustomer(Customer customer) {
        log.trace("Executing saveCustomer [{}]", customer);
        customerValidator.validate(customer, Customer::getTenantId);
        Customer savedCustomer = customerDao.save(customer.getTenantId(), customer);
        dashboardService.updateCustomerDashboards(savedCustomer.getTenantId(), savedCustomer.getId());
        return savedCustomer;
    }

    @Override
    public void deleteCustomer(TenantId tenantId, CustomerId customerId) {
        log.trace("Executing deleteCustomer [{}]", customerId);
        Validator.validateId(customerId, INCORRECT_CUSTOMER_ID + customerId);
        Customer customer = findCustomerById(tenantId, customerId);
        if (customer == null) {
            throw new IncorrectParameterException("Unable to delete non-existent customer.");
        }
        dashboardService.unassignCustomerDashboards(tenantId, customerId);
        entityViewService.unassignCustomerEntityViews(customer.getTenantId(), customerId);
        assetService.unassignCustomerAssets(customer.getTenantId(), customerId);
        deviceService.unassignCustomerDevices(customer.getTenantId(), customerId);
        userService.deleteCustomerUsers(customer.getTenantId(), customerId);
        deleteEntityRelations(tenantId, customerId);
        customerDao.removeById(tenantId, customerId.getId());
    }

    @Override
    public Customer findOrCreatePublicCustomer(TenantId tenantId) {
        log.trace("Executing findOrCreatePublicCustomer, tenantId [{}]", tenantId);
        Validator.validateId(tenantId, INCORRECT_CUSTOMER_ID + tenantId);
        Optional<Customer> publicCustomerOpt = customerDao.findCustomersByTenantIdAndTitle(tenantId.getId(), PUBLIC_CUSTOMER_TITLE);
        if (publicCustomerOpt.isPresent()) {
            return publicCustomerOpt.get();
        } else {
            Customer publicCustomer = new Customer();
            publicCustomer.setTenantId(tenantId);
            publicCustomer.setTitle(PUBLIC_CUSTOMER_TITLE);
            try {
                publicCustomer.setAdditionalInfo(new ObjectMapper().readValue("{ \"isPublic\": true }", JsonNode.class));
            } catch (IOException e) {
                throw new IncorrectParameterException("Unable to create public customer.", e);
            }
            return customerDao.save(tenantId, publicCustomer);
        }
    }

    @Override
    public TextPageData<Customer> findCustomersByTenantId(TenantId tenantId, TextPageLink pageLink) {
        log.trace("Executing findCustomersByTenantId, tenantId [{}], pageLink [{}]", tenantId, pageLink);
        Validator.validateId(tenantId, "Incorrect tenantId " + tenantId);
        Validator.validatePageLink(pageLink, "Incorrect page link " + pageLink);
        List<Customer> customers = customerDao.findCustomersByTenantId(tenantId.getId(), pageLink);
        customers.stream().forEach(customer -> {

            customer.setAdminCount(userService.countByTenantIdAndCustomerId(fromTimeUUID(tenantId.getId()), fromTimeUUID(customer.getUuidId())));
            customer.setUserCount(0);
            customer.setInfrastructureCount(
                    assetService.findAssetExInfoByTenantAndCustomer(tenantId, customer.getId(), new TextPageLink(Integer.MAX_VALUE)).getData()
                            .stream().filter(asset -> "BRIDGE".equals(asset.getType()) || "TUNNEL".equals(asset.getType()) || "SLOPE".equals(asset.getType()) || "ROAD".equals(asset.getType())).count());
        });
        return new TextPageData<>(customers, pageLink);
    }

    @Override
    public TextPageData<CustomerExInfo> findCustomerExInfosByTenantId(TenantId tenantId, TextPageLink pageLink) {
        log.trace("Executing findCustomersByTenantId, tenantId [{}], pageLink [{}]", tenantId, pageLink);
        Validator.validateId(tenantId, "Incorrect tenantId " + tenantId);
        Validator.validatePageLink(pageLink, "Incorrect page link " + pageLink);
        List<Customer> customers = customerDao.findCustomersByTenantId(tenantId.getId(), pageLink);
        List<CustomerExInfo> customerExInfos = customersToCustomerExInfos(customers);
        return new TextPageData<>(customerExInfos, pageLink);
    }

    @Override
    public TextPageData<Customer> findCustomers(TextPageLink pageLink) {
        log.trace("Executing findCustomersByTenantId, pageLink [{}]", pageLink);

        Validator.validatePageLink(pageLink, "Incorrect page link " + pageLink);
        List<Customer> customers = customerDao.findCustomers(pageLink);
        customers.stream().forEach(customer -> {

            customer.setAdminCount(userService.countByTenantIdAndCustomerId(fromTimeUUID(customer.getTenantId().getId()), fromTimeUUID(customer.getUuidId())));
            customer.setUserCount(0);
            customer.setInfrastructureCount(
                    assetService.findAssetExInfoByTenantAndCustomer(customer.getTenantId(), customer.getId(), new TextPageLink(Integer.MAX_VALUE)).getData()
                            .stream().filter(asset -> "BRIDGE".equals(asset.getType()) || "TUNNEL".equals(asset.getType()) || "SLOPE".equals(asset.getType()) || "ROAD".equals(asset.getType())).count());
        });
        return new TextPageData<>(customers, pageLink);
    }

    @Override
    public TextPageData<CustomerExInfo> findCustomerExInfos(TextPageLink pageLink) {
        log.trace("Executing findCustomersByTenantId, pageLink [{}]", pageLink);

        Validator.validatePageLink(pageLink, "Incorrect page link " + pageLink);
        List<Customer> customers = customerDao.findCustomers(pageLink);
        List<CustomerExInfo> customerExInfos = customersToCustomerExInfos(customers);
        return new TextPageData<>(customerExInfos, pageLink);
    }

    @Override
    public List<CustomerExInfo> findCustomerExinfos() {
        List<Customer> customers = customerDao.findCustomers();
        List<CustomerExInfo> customerExInfos = customersToCustomerExInfos(customers);
        return customerExInfos;
    }

    @Override
    public List<CustomerExInfo> findCustomerByTenantIdExinfos(TenantId tenantId) {
        List<Customer> customers = customerDao.findCustomersByTenantId(tenantId.getId());
        List<CustomerExInfo> customerExInfos = customersToCustomerExInfos(customers);
        return customerExInfos;
    }

    @Override
    public void deleteCustomersByTenantId(TenantId tenantId) {
        log.trace("Executing deleteCustomersByTenantId, tenantId [{}]", tenantId);
        Validator.validateId(tenantId, "Incorrect tenantId " + tenantId);
        customersByTenantRemover.removeEntities(tenantId, tenantId);
    }

    @Override
    public TenantId findTenantIdByCustomerId(CustomerId customerId, TextPageLink pageLink) {
        TenantId tenantId = null;
        TextPageData<Customer> customers = findCustomers(pageLink);
        List<Customer> customerList = customers.getData();
        for (Customer customer : customerList) {
            if (customer.getId().equals(customerId)) {
                tenantId = customer.getTenantId();
                break;
            }
        }
        if (tenantId == null && customers.hasNext()) {
            findTenantIdByCustomerId(customerId, customers.getNextPageLink());
        }
        return tenantId;
    }

    @Override
    public int countCustomerByTenantId(TenantId tenantId) {
        return customerDao.countCustomersByTenantId(tenantId.getId());
    }

    @Override
    public Customer findCustomerByTitle(TenantId tenantId, String customer) {
        return customerDao.findCustomerByTitle(tenantId, customer);
    }

    private List<CustomerExInfo> customersToCustomerExInfos(List<Customer> customers) {
        List<CustomerExInfo> customerExInfos = customers.stream().map(CustomerExInfo::new).collect(Collectors.toList());
        customerExInfos.forEach(customerExInfo -> {
            List<User> adminUsers = new ArrayList<>();
            List<User> commonUsers = new ArrayList<>();
//					List<User> adminUsers = userService.findUsersByTenantIdAndCustomerId(customerExInfo.getTenantId(),customerExInfo.getId(),new TextPageLink(Integer.MAX_VALUE))
//					.getData().stream().filter(user ->
//							user.getAdditionalInfo().has("power") && user.getAdditionalInfo().get("power").asText().equals("admin")
//					).collect(Collectors.toList());

            userService.findCustomerUsers(customerExInfo.getTenantId(), customerExInfo.getId(), new TextPageLink(Integer.MAX_VALUE))
                    .getData().stream().forEach(user -> {
                if (user.getAdditionalInfo().has("power")) {
                    if (user.getAdditionalInfo().get("power").asText().equals("admin")) {
                        adminUsers.add(user);
                        customerExInfo.getAdminUserNameList().add(user.getFirstName());
                    } else if (user.getAdditionalInfo().get("power").asText().equals("common")) {
                        commonUsers.add(user);
                        customerExInfo.getUserNameList().add(user.getFirstName());
                    }
                }
            });

            Tenant tenant = tenantService.findTenantById(customerExInfo.getTenantId());
            customerExInfo.setTenantName(tenant.getName());

            customerExInfo.setAdminCount(adminUsers.size());
            customerExInfo.setUserCount(commonUsers.size());
            customerExInfo.setInfrastructureCount(
                    assetService.findAssetExInfoByTenantAndCustomer(customerExInfo.getTenantId(), customerExInfo.getId(), new TextPageLink(Integer.MAX_VALUE)).getData()
                            .stream().filter(asset -> "BRIDGE".equals(asset.getType()) || "TUNNEL".equals(asset.getType()) || "SLOPE".equals(asset.getType()) || "ROAD".equals(asset.getType())).count());
        });
        return customerExInfos;
    }

    private DataValidator<Customer> customerValidator =
            new DataValidator<Customer>() {

                @Override
                protected void validateCreate(TenantId tenantId, Customer customer) {
                    customerDao.findCustomersByTenantIdAndTitle(customer.getTenantId().getId(), customer.getTitle()).ifPresent(
                            c -> {
                                throw new DataValidationException("Customer with such title already exists!");
                            }
                    );
                }

                @Override
                protected void validateUpdate(TenantId tenantId, Customer customer) {
                    customerDao.findCustomersByTenantIdAndTitle(customer.getTenantId().getId(), customer.getTitle()).ifPresent(
                            c -> {
                                if (!c.getId().equals(customer.getId())) {
                                    throw new DataValidationException("Customer with such title already exists!");
                                }
                            }
                    );
                }

                @Override
                protected void validateDataImpl(TenantId tenantId, Customer customer) {
                    if (StringUtils.isEmpty(customer.getTitle())) {
                        throw new DataValidationException("Customer title should be specified!");
                    }
                    if (customer.getTitle().equals(PUBLIC_CUSTOMER_TITLE)) {
                        throw new DataValidationException("'Public' title for customer is system reserved!");
                    }
                    if (!StringUtils.isEmpty(customer.getEmail())) {
                        validateEmail(customer.getEmail());
                    }
                    if (customer.getTenantId() == null) {
                        throw new DataValidationException("Customer should be assigned to tenant!");
                    } else {
                        Tenant tenant = tenantDao.findById(tenantId, customer.getTenantId().getId());
                        if (tenant == null) {
                            throw new DataValidationException("Customer is referencing to non-existent tenant!");
                        }
                    }
                }
            };

    private PaginatedRemover<TenantId, Customer> customersByTenantRemover =
            new PaginatedRemover<TenantId, Customer>() {

                @Override
                protected List<Customer> findEntities(TenantId tenantId, TenantId id, TextPageLink pageLink) {
                    return customerDao.findCustomersByTenantId(id.getId(), pageLink);
                }

                @Override
                protected void removeEntity(TenantId tenantId, Customer entity) {
                    deleteCustomer(tenantId, new CustomerId(entity.getUuidId()));
                }
            };
}
