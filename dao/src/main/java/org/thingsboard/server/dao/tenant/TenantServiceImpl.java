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
package org.thingsboard.server.dao.tenant;

import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.TenantExInfo;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.TextPageData;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.dashboard.DashboardService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.entity.AbstractEntityService;
import org.thingsboard.server.dao.entityview.EntityViewService;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.service.PaginatedRemover;
import org.thingsboard.server.dao.service.Validator;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.server.dao.widget.WidgetsBundleService;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.thingsboard.server.common.data.UUIDConverter.fromTimeUUID;
import static org.thingsboard.server.dao.service.Validator.validateId;

@Service
@Slf4j
public class TenantServiceImpl extends AbstractEntityService implements TenantService {

    private static final String DEFAULT_TENANT_REGION = "Global";
    public static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";

    @Autowired
    private TenantDao tenantDao;

    @Autowired
    private UserService userService;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private AssetService assetService;

    @Autowired
    private DeviceService deviceService;

    @Autowired
    private EntityViewService entityViewService;

    @Autowired
    private WidgetsBundleService widgetsBundleService;

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private RuleChainService ruleChainService;

    @Override
    public Tenant findTenantById(TenantId tenantId) {
        log.trace("Executing findTenantById [{}]", tenantId);
        Validator.validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        return tenantDao.findById(tenantId, tenantId.getId());
    }

    @Override
    public ListenableFuture<Tenant> findTenantByIdAsync(TenantId callerId, TenantId tenantId) {
        log.trace("Executing TenantIdAsync [{}]", tenantId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        return tenantDao.findByIdAsync(callerId, tenantId.getId());
    }

    @Override
    public Tenant saveTenant(Tenant tenant) {
        log.trace("Executing saveTenant [{}]", tenant);
        tenant.setRegion(DEFAULT_TENANT_REGION);
        tenantValidator.validate(tenant, Tenant::getId);
        return tenantDao.save(tenant.getId(), tenant);
    }

    @Override
    public void deleteTenant(TenantId tenantId) {
        log.trace("Executing deleteTenant [{}]", tenantId);
        Validator.validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        customerService.deleteCustomersByTenantId(tenantId);
        widgetsBundleService.deleteWidgetsBundlesByTenantId(tenantId);
        dashboardService.deleteDashboardsByTenantId(tenantId);
        entityViewService.deleteEntityViewsByTenantId(tenantId);
        assetService.deleteAssetsByTenantId(tenantId);
        deviceService.deleteDevicesByTenantId(tenantId);
        userService.deleteTenantAdmins(tenantId);
        ruleChainService.deleteRuleChainsByTenantId(tenantId);
        tenantDao.removeById(tenantId, tenantId.getId());
        deleteEntityRelations(tenantId, tenantId);
    }

    @Override
    public TextPageData<Tenant> findTenants(TextPageLink pageLink) {
        log.trace("Executing findTenants pageLink [{}]", pageLink);
        Validator.validatePageLink(pageLink, "Incorrect page link " + pageLink);
        List<Tenant> tenants = tenantDao.findTenantsByRegion(new TenantId(EntityId.NULL_UUID), DEFAULT_TENANT_REGION, pageLink);

        tenants.stream().forEach(
                tenant -> {
//					tenant.setAdminCount(userService.countByTenantId(fromTimeUUID(tenant.getUuidId())));
//					tenant.setUserCount(0);
                    tenant.setAdminCount(userService.countTenantAdminByTenantId(fromTimeUUID(tenant.getUuidId())));
                    tenant.setUserCount(userService.countTenantUserByTenantId(fromTimeUUID(tenant.getUuidId())));
                    tenant.setCustomerCount(customerService.countCustomerByTenantId(tenant.getId()));
                }
        );

        return new TextPageData<>(tenants, pageLink);
    }

    @Override
    public TextPageData<TenantExInfo> findTenantExInfos(TextPageLink pageLink) {
        log.trace("Executing findTenants pageLink [{}]", pageLink);
        Validator.validatePageLink(pageLink, "Incorrect page link " + pageLink);
        List<Tenant> tenants = tenantDao.findTenantsByRegion(new TenantId(EntityId.NULL_UUID), DEFAULT_TENANT_REGION, pageLink);
        return new TextPageData<>(tenantsToTenantExInfos(tenants), pageLink);
    }

    @Override
    public Tenant findTenantByTitle(TenantId tenantId, String title) {
        return tenantDao.findTenantByTitle(tenantId, title);
    }

    @Override
    public void deleteTenants() {
        log.trace("Executing deleteTenants");
        tenantsRemover.removeEntities(new TenantId(EntityId.NULL_UUID), DEFAULT_TENANT_REGION);
    }

    private List<TenantExInfo> tenantsToTenantExInfos(List<Tenant> tenants) {
        List<TenantExInfo> tenantExInfos = tenants.stream().map(TenantExInfo::new).collect(Collectors.toList());
        tenantExInfos.forEach(tenantExInfo -> {
            List<User> adminUsers = new ArrayList<>();
            List<User> commonUsers = new ArrayList<>();

            userService.findTenantAdmins(tenantExInfo.getId(), new TextPageLink(Integer.MAX_VALUE))
                    .getData().forEach(user -> {
                if (user.getAdditionalInfo().has("power")) {
                    if (user.getAdditionalInfo().get("power").asText().equals("admin")) {
                        adminUsers.add(user);
                        tenantExInfo.getAdminUserNameList().add(user.getFirstName());
                    } else if (user.getAdditionalInfo().get("power").asText().equals("common")) {
                        commonUsers.add(user);
                        tenantExInfo.getUserNameList().add(user.getFirstName());
                    }
                }
            });

            tenantExInfo.setAdminCount(adminUsers.size());
            tenantExInfo.setUserCount(commonUsers.size());
            tenantExInfo.setCustomerCount(customerService.countCustomerByTenantId(tenantExInfo.getId()));
        });
        return tenantExInfos;
    }

    private DataValidator<Tenant> tenantValidator =
            new DataValidator<Tenant>() {
                @Override
                protected void validateDataImpl(TenantId tenantId, Tenant tenant) {
                    if (StringUtils.isEmpty(tenant.getTitle())) {
                        throw new DataValidationException("Tenant title should be specified!");
                    }
                    if (!StringUtils.isEmpty(tenant.getEmail())) {
                        validateEmail(tenant.getEmail());
                    }
                }
            };

    private PaginatedRemover<String, Tenant> tenantsRemover =
            new PaginatedRemover<String, Tenant>() {

                @Override
                protected List<Tenant> findEntities(TenantId tenantId, String region, TextPageLink pageLink) {
                    return tenantDao.findTenantsByRegion(tenantId, region, pageLink);
                }

                @Override
                protected void removeEntity(TenantId tenantId, Tenant entity) {
                    deleteTenant(new TenantId(entity.getUuidId()));
                }
            };
}
