/**
 * Copyright © 2016-2018 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.TenantAndAsset;
import org.thingsboard.server.common.data.TenantExInfo;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.TextPageData;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.service.install.InstallScripts;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api")
@Slf4j
public class TenantController extends BaseController {

    @Autowired
    private InstallScripts installScripts;

    @Autowired
    private TenantService tenantService;

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN','CUSTOMER_USER')")
    @RequestMapping(value = "/tenant/{tenantId}", method = RequestMethod.GET)
    @ResponseBody
    public Tenant getTenantById(@PathVariable("tenantId") String strTenantId) throws ThingsboardException {
        checkParameter("tenantId", strTenantId);
        try {
            TenantId tenantId = new TenantId(toUUID(strTenantId));
            checkTenantId(tenantId);
            return checkNotNull(tenantService.findTenantById(tenantId));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAuthority('SYS_ADMIN')")
    @RequestMapping(value = "/tenant", method = RequestMethod.POST)
    @ResponseBody
    public Tenant saveTenant(@RequestBody Tenant tenant) throws ThingsboardException {
        try {
            boolean newTenant = tenant.getId() == null;
            tenant = checkNotNull(tenantService.saveTenant(tenant));
            if (newTenant) {
                installScripts.createDefaultRuleChains(tenant.getId());
            }
            return tenant;
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAuthority('SYS_ADMIN')")
    @RequestMapping(value = "/tenant/{tenantId}", method = RequestMethod.DELETE)
    @ResponseStatus(value = HttpStatus.OK)
    public void deleteTenant(@PathVariable("tenantId") String strTenantId) throws ThingsboardException {
        checkParameter("tenantId", strTenantId);
        try {
            TenantId tenantId = new TenantId(toUUID(strTenantId));
            tenantService.deleteTenant(tenantId);

            actorService.onEntityStateChange(tenantId, tenantId, ComponentLifecycleEvent.DELETED);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAuthority('SYS_ADMIN')")
    @RequestMapping(value = "/tenants", params = {"limit"}, method = RequestMethod.GET)
    @ResponseBody
    public TextPageData<TenantExInfo> getTenants(@RequestParam int limit,
                                                 @RequestParam(required = false) String textSearch,
                                                 @RequestParam(required = false) String idOffset,
                                                 @RequestParam(required = false) String textOffset) throws ThingsboardException {
        try {
            TextPageLink pageLink = createPageLink(limit, textSearch, idOffset, textOffset);
//            return checkNotNull(tenantService.findTenants(pageLink));
            return checkNotNull(tenantService.findTenantExInfos(pageLink));
        } catch (Exception e) {
            throw handleException(e);
        }
    }
    @PreAuthorize("hasAuthority('SYS_ADMIN')")
    @RequestMapping(value = "/tenantsAndAsset", params = {"limit"}, method = RequestMethod.GET)
    @ResponseBody
    public List<TenantAndAsset> getTenantsAndAsset(@RequestParam int limit,
                                                   @RequestParam(required = false) String textSearch,
                                                   @RequestParam(required = false) String idOffset,
                                                   @RequestParam(required = false) String textOffset) throws ThingsboardException {
        try {
            List<TenantAndAsset> retObj = new ArrayList<>();
            TextPageLink pageLink = createPageLink(limit, textSearch, idOffset, textOffset);
            TextPageData<Tenant> tmpTenant = checkNotNull(tenantService.findTenants(pageLink));
            tmpTenant.getData().forEach(tenant -> {
                retObj.add(new TenantAndAsset(tenant,assetService.findAssetExInfoByTenant(tenant.getId(),new TextPageLink(Integer.MAX_VALUE)).getData()));
            });


            return retObj;
        } catch (Exception e) {
            throw handleException(e);
        }
    }

}
