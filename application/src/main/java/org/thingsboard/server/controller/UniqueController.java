package org.thingsboard.server.controller;

import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;

/**
 * 名称唯一性检查
 * Created by morninz at 2019/4/29
 */
@RestController
@RequestMapping("/api/unique")
@Slf4j
public class UniqueController extends BaseController {


    @ApiOperation(value = "检查用户名是否被占用", notes = "如果可以使用，返回true，否则返回错误信息")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/user/name", method = RequestMethod.GET)
    @ResponseBody
    public boolean isUserNameAvailable(@RequestParam String userName) throws ThingsboardException {
        User user = userService.findUserByFirstName(getCurrentUser().getTenantId(), userName);
        if (user != null) {
            throw new ThingsboardException("user name '" + userName + "' already present.", ThingsboardErrorCode.USER_NAME_ALREADY_PRESENT);
        }
        return true;
    }

    @ApiOperation(value = "检查email是否被占用", notes = "如果可以使用，返回true，否则返回错误信息")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/user/email", method = RequestMethod.GET)
    @ResponseBody
    public boolean isEmailAvailable(@RequestParam String email) throws ThingsboardException {
        User user = userService.findUserByEmail(getCurrentUser().getTenantId(), email);
        if (user != null) {
            throw new ThingsboardException("user email '" + email + "' already present.", ThingsboardErrorCode.USER_EMAIL_ALREADY_PRESENT);
        }
        return true;
    }

    @ApiOperation(value = "检查业主名称是否被占用", notes = "如果可以使用，返回true，否则返回错误信息")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/tenant/name", method = RequestMethod.GET)
    @ResponseBody
    public boolean isTenantNameAvailable(@RequestParam String tenantName) throws ThingsboardException {
        Tenant tenant = tenantService.findTenantByTitle(getCurrentUser().getTenantId(), tenantName);
        if (tenant != null) {
            throw new ThingsboardException("tenant name '" + tenantName + "' already present.", ThingsboardErrorCode.TENANT_NAME_ALREADY_PRESENT);
        }
        return true;
    }

    @ApiOperation(value = "检查项目名称是否被占用", notes = "如果可以使用，返回true，否则返回错误信息")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/customer/name", method = RequestMethod.GET)
    @ResponseBody
    public boolean isCustomerNameAvailable(@RequestParam String customerName) throws ThingsboardException {
        Customer customer = customerService.findCustomerByTitle(getCurrentUser().getTenantId(), customerName);
        if (customer != null) {
            throw new ThingsboardException("customer name '" + customerName + "' already present.", ThingsboardErrorCode.CUSTOMER_NAME_ALREADY_PRESENT);
        }
        return true;
    }

    @ApiOperation(value = "检查基础设施名称是否被占用", notes = "如果可以使用，返回true，否则返回错误信息")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/asset/name", method = RequestMethod.GET)
    @ResponseBody
    public boolean isAssetNameAvailable(@RequestParam String assetName) throws ThingsboardException {
        Asset asset = assetService.findAssetByName(assetName);
        if (asset != null) {
            throw new ThingsboardException("asset name '" + assetName + "' already present.", ThingsboardErrorCode.ASSET_NAME_ALREADY_PRESENT);
        }
        return true;
    }

    @ApiOperation(value = "检查设备名称是否被占用", notes = "如果可以使用，返回true，否则返回错误信息")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/device/name", method = RequestMethod.GET)
    @ResponseBody
    public boolean isDeviceNameAvailable(@RequestParam String deviceName) throws ThingsboardException {
        Device device = deviceService.findByNameExactly(deviceName);
        if (device != null) {
            throw new ThingsboardException("device name '" + deviceName + "' already present.", ThingsboardErrorCode.DEVICE_NAME_ALREADY_PRESENT);
        }
        return true;
    }
}
