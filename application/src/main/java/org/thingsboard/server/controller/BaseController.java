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

import com.datastax.driver.core.utils.UUIDs;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.thingsboard.server.actors.service.ActorService;
import org.thingsboard.server.common.data.*;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmExInfo;
import org.thingsboard.server.common.data.alarm.AlarmId;
import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.batchconfig.DeviceAutoLogon;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.*;
import org.thingsboard.server.common.data.kv.AttributeKvData;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.DataType;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.plugin.ComponentDescriptor;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.widget.WidgetType;
import org.thingsboard.server.common.data.widget.WidgetsBundle;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgDataType;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.msg.cluster.SendToClusterMsg;
import org.thingsboard.server.common.msg.system.ServiceToRuleEngineMsg;
import org.thingsboard.server.dao.alarm.AlarmMonitorItemService;
import org.thingsboard.server.dao.alarm.AlarmService;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.audit.AuditLogService;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.dashboard.DashboardService;
import org.thingsboard.server.dao.device.DeviceAttributesService;
import org.thingsboard.server.dao.device.DeviceCredentialsService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.entityview.EntityViewService;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.exception.IncorrectParameterException;
import org.thingsboard.server.dao.historyvideo.HistoryVideoService;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.model.sql.DeviceAttributesEntity;
import org.thingsboard.server.dao.partol.PatrolRecordService;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.dao.report.ReportService;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.dao.service.Validator;
import org.thingsboard.server.dao.task.TaskService;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.dao.tshourvaluestatistic.BaseTsHourValueStatisticService;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.server.dao.vassetattrkv.VassetAttrKVService;
import org.thingsboard.server.dao.vdeviceattrkv.DeviceAttrKVService;
import org.thingsboard.server.dao.video.VideoInfoService;
import org.thingsboard.server.dao.warnings.WarningsRecordService;
import org.thingsboard.server.dao.widget.WidgetTypeService;
import org.thingsboard.server.dao.widget.WidgetsBundleService;
import org.thingsboard.server.exception.ThingsboardErrorResponseHandler;
import org.thingsboard.server.service.component.ComponentDiscoveryService;
import org.thingsboard.server.service.device.DeviceBaseAttributeService;
import org.thingsboard.server.service.device.DeviceCheckService;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.state.DeviceStateService;
import org.thingsboard.server.service.telemetry.TelemetrySubscriptionService;

import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;
import java.util.stream.Collectors;

import static org.thingsboard.server.dao.service.Validator.validateId;

@Slf4j
public abstract class BaseController {

    public static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";
    public static final String YOU_DON_T_HAVE_PERMISSION_TO_PERFORM_THIS_OPERATION = "You don't have permission to perform this operation!";


    /**
     * 检查tenantId和customerId参数权限时，返回的map的key值。
     */
    protected final String KEY_TENANT_ID = "tenant_id";
    protected final String KEY_CUSTOMER_ID = "customer_id";

    private static final ObjectMapper json = new ObjectMapper();


    @Autowired
    private ThingsboardErrorResponseHandler errorResponseHandler;

    @Autowired
    protected TenantService tenantService;

    @Autowired
    protected CustomerService customerService;

    @Autowired
    protected UserService userService;

    @Autowired
    protected DeviceService deviceService;

    @Autowired
    protected AssetService assetService;

    @Autowired
    protected TaskService taskService;

    @Autowired
    protected AlarmService alarmService;

    @Autowired
    protected AlarmMonitorItemService alarmMonitorItemService;

    @Autowired
    protected DeviceCredentialsService deviceCredentialsService;

    @Autowired
    protected WidgetsBundleService widgetsBundleService;

    @Autowired
    protected WidgetTypeService widgetTypeService;

    @Autowired
    protected DashboardService dashboardService;

    @Autowired
    protected ComponentDiscoveryService componentDescriptorService;

    @Autowired
    protected RuleChainService ruleChainService;

    @Autowired
    protected ActorService actorService;

    @Autowired
    protected RelationService relationService;

    @Autowired
    protected AuditLogService auditLogService;

    @Autowired
    protected DeviceStateService deviceStateService;

    @Autowired
    protected EntityViewService entityViewService;

    @Autowired
    protected TelemetrySubscriptionService tsSubService;

    @Autowired
    protected AttributesService attributesService;

    @Autowired
    protected VassetAttrKVService vassetAttrKVService;

    @Autowired
    protected DeviceAttrKVService deviceAttrKVService;

    @Autowired
    protected DeviceAttributesService deviceAttributesService;

    @Autowired
    protected DeviceCheckService deviceCheckService;

    @Autowired
    protected DeviceBaseAttributeService deviceBaseAttributeService;

    @Autowired
    protected PatrolRecordService patrolRecordService;

    @Autowired
    protected VideoInfoService videoInfoService;

    @Autowired
    protected HistoryVideoService historyVideoService;

    @Autowired
    protected ReportService reportService;

    @Autowired
    protected BaseTsHourValueStatisticService tsHourValueStatisticService;

    @Autowired
    protected WarningsRecordService warningsRecordService;

    @Value("${server.log_controller_error_stack_trace}")
    @Getter
    private boolean logControllerErrorStackTrace;


    @ExceptionHandler(ThingsboardException.class)
    public void handleThingsboardException(ThingsboardException ex, HttpServletResponse response) {
        errorResponseHandler.handle(ex, response);
    }

    ThingsboardException handleException(Exception exception) {
        return handleException(exception, true);
    }

    private ThingsboardException handleException(Exception exception, boolean logException) {
        if (logException && logControllerErrorStackTrace) {
            log.error("Error [{}]", exception.getMessage(), exception);
        }

        String cause = "";
        if (exception.getCause() != null) {
            cause = exception.getCause().getClass().getCanonicalName();
        }

        if (exception instanceof ThingsboardException) {
            return (ThingsboardException) exception;
        } else if (exception instanceof IllegalArgumentException || exception instanceof IncorrectParameterException
                || exception instanceof DataValidationException || cause.contains("IncorrectParameterException")) {
            return new ThingsboardException(exception.getMessage(), ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        } else if (exception instanceof MessagingException) {
            return new ThingsboardException("Unable to send mail: " + exception.getMessage(), ThingsboardErrorCode.GENERAL);
        } else {
            return new ThingsboardException(exception.getMessage(), ThingsboardErrorCode.GENERAL);
        }
    }

    <T> T checkNotNull(T reference) throws ThingsboardException {
        if (reference == null) {
            throw new ThingsboardException("Requested item wasn't found!", ThingsboardErrorCode.ITEM_NOT_FOUND);
        }
        return reference;
    }

    <T> T checkNotNull(Optional<T> reference) throws ThingsboardException {
        if (reference.isPresent()) {
            return reference.get();
        } else {
            throw new ThingsboardException("Requested item wasn't found!", ThingsboardErrorCode.ITEM_NOT_FOUND);
        }
    }

    void checkParameter(String name, String param) throws ThingsboardException {
        if (StringUtils.isEmpty(param)) {
            throw new ThingsboardException("Parameter '" + name + "' can't be empty!", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
    }

    void checkArrayParameter(String name, String[] params) throws ThingsboardException {
        if (params == null || params.length == 0) {
            throw new ThingsboardException("Parameter '" + name + "' can't be empty!", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        } else {
            for (String param : params) {
                checkParameter(name, param);
            }
        }
    }

    UUID toUUID(String id) {
        return UUID.fromString(id);
    }

    TimePageLink createPageLink(int limit, Long startTime, Long endTime, boolean ascOrder, String idOffset) {
        UUID idOffsetUuid = null;
        if (StringUtils.isNotEmpty(idOffset)) {
            idOffsetUuid = toUUID(idOffset);
        }
        return new TimePageLink(limit, startTime, endTime, ascOrder, idOffsetUuid);
    }


    TextPageLink createPageLink(int limit, String textSearch, String idOffset, String textOffset) {
        UUID idOffsetUuid = null;
        if (StringUtils.isNotEmpty(idOffset)) {
            idOffsetUuid = toUUID(idOffset);
        }
        return new TextPageLink(limit, textSearch, idOffsetUuid, textOffset);
    }

    protected SecurityUser getCurrentUser() throws ThingsboardException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof SecurityUser) {
            return (SecurityUser) authentication.getPrincipal();
        } else {
            throw new ThingsboardException("You aren't authorized to perform this operation!", ThingsboardErrorCode.AUTHENTICATION);
        }
    }


    /**
     * 检查当前登录的用户是否有权限访问参数传入的tenantId和customerId。
     * 1. 如果没有访问权限，抛异常。
     * 2. 如果有访问权限，则返回包含解析后id的Map，通过 KEY_TENANT_ID 和 KEY_CUSTOMER_ID 获取。
     * 3. 如果参数{tenantIdStr} 或者 {customerIdStr}为null，那么返回当前登录用户权限下的tenantId或者customerId, 如果是SYS_ADMIN，则返回null，表示可以访问所有tenant和customer。
     * <p>
     * 此接口通常用于对tenantId和customerId进行条件筛选时，进行权限判断。
     * <p>
     * add by zhengtao 2019/04/26
     *
     * @param tenantIdStr   筛选指定的tenantId，可以为null
     * @param customerIdStr 筛选指定的customerId，可以为null
     */
    protected Map<String, EntityId> checkTenantIdAndCustomerIdParams(String tenantIdStr, String customerIdStr) throws ThingsboardException {
        TenantId tenantId = null;  //Do not filter tenant if null
        CustomerId customerId = null;

        if (!Strings.isNullOrEmpty(tenantIdStr)) {
            tenantId = new TenantId(UUID.fromString(tenantIdStr));
            checkTenantId(tenantId);
        }
        if (!Strings.isNullOrEmpty(customerIdStr)) {
            customerId = new CustomerId(UUID.fromString(customerIdStr));
            if (tenantId != null) {
                checkCustomerId(tenantId, customerId);
            } else {
                checkCustomerId(customerId);
            }
        }

        /**
         * if tenantId and customerId NOT specified, we use the tenantId and customerId of the current logined-user.
         */
        if (getCurrentUser().getAuthority() == Authority.SYS_ADMIN) {
            //do nothing
        } else if (getCurrentUser().getAuthority() == Authority.TENANT_ADMIN) {
            if (tenantId == null) {
                tenantId = getCurrentUser().getTenantId();
            }
        } else {
            if (tenantId == null) {
                tenantId = getCurrentUser().getTenantId();
            }
            if (customerId == null) {
                customerId = getCurrentUser().getCustomerId();
            }
        }

        Map<String, EntityId> tenantAndCustomerMap = Maps.newHashMapWithExpectedSize(2);
        tenantAndCustomerMap.put(KEY_TENANT_ID, tenantId);
        tenantAndCustomerMap.put(KEY_CUSTOMER_ID, customerId);
        return tenantAndCustomerMap;
    }

    void checkTenantId(TenantId tenantId) throws ThingsboardException {
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        SecurityUser authUser = getCurrentUser();
        if (authUser.getAuthority() != Authority.SYS_ADMIN &&
                (authUser.getTenantId() == null || !authUser.getTenantId().equals(tenantId))) {
            throw new ThingsboardException(YOU_DON_T_HAVE_PERMISSION_TO_PERFORM_THIS_OPERATION,
                    ThingsboardErrorCode.PERMISSION_DENIED);
        }
    }

    protected TenantId getTenantId() throws ThingsboardException {
        return getCurrentUser().getTenantId();
    }

    Customer checkCustomerIdAdmin(TenantId tenantId, CustomerId customerId) throws ThingsboardException {
        try {
            if (customerId != null && !customerId.isNullUid()) {
                Customer customer = customerService.findCustomerById(tenantId, customerId);
                checkCustomer(customer);
                return customer;
            } else {
                return null;
            }
        } catch (Exception e) {
            throw handleException(e, false);
        }
    }

    Customer checkCustomerId(TenantId tenantId, CustomerId customerId) throws ThingsboardException {
        if (getCurrentUser().getAuthority() == Authority.SYS_ADMIN) {
            return checkCustomerIdAdmin(tenantId, customerId);
        } else {
            return checkCustomerId(customerId);
        }
    }

    Customer checkCustomerId(CustomerId customerId) throws ThingsboardException {
        try {
            SecurityUser authUser = getCurrentUser();
            if (authUser.getAuthority() != Authority.SYS_ADMIN &&
                    (authUser.getAuthority() != Authority.TENANT_ADMIN &&
                            (authUser.getCustomerId() == null || !authUser.getCustomerId().equals(customerId)))) {
                throw new ThingsboardException(YOU_DON_T_HAVE_PERMISSION_TO_PERFORM_THIS_OPERATION,
                        ThingsboardErrorCode.PERMISSION_DENIED);
            }
            if (customerId != null && !customerId.isNullUid()) {
                Customer customer = customerService.findCustomerById(authUser.getTenantId(), customerId);
                checkCustomer(customer);
                return customer;
            } else {
                return null;
            }
        } catch (Exception e) {
            throw handleException(e, false);
        }
    }

    private void checkCustomer(Customer customer) throws ThingsboardException {
        checkNotNull(customer);
        checkTenantId(customer.getTenantId());
    }

    User checkUserId(UserId userId) throws ThingsboardException {
        try {
            validateId(userId, "Incorrect userId " + userId);
            User user = userService.findUserById(getCurrentUser().getTenantId(), userId);
            checkUser(user);
            return user;
        } catch (Exception e) {
            throw handleException(e, false);
        }
    }

    /**
     * 检查userId是否归属于tenantId和customerId,并且当前登录用户有访问userId的权限。
     * add by zhengtao at 2019/04/26
     *
     * @return
     * @throws ThingsboardException
     */
    protected User checkUserId(TenantId tenantId, CustomerId customerId, UserId userId) throws ThingsboardException {
        User user = checkUserId(userId);
        if (tenantId != null && !user.getTenantId().equals(tenantId)) {
            throw new ThingsboardException(YOU_DON_T_HAVE_PERMISSION_TO_PERFORM_THIS_OPERATION, ThingsboardErrorCode.PERMISSION_DENIED);
        }
        if (customerId != null && !user.getCustomerId().equals(customerId)) {
            throw new ThingsboardException(YOU_DON_T_HAVE_PERMISSION_TO_PERFORM_THIS_OPERATION, ThingsboardErrorCode.PERMISSION_DENIED);
        }
        return user;
    }

    private void checkUser(User user) throws ThingsboardException {
        checkNotNull(user);
        checkTenantId(user.getTenantId());
        if (user.getAuthority() == Authority.CUSTOMER_USER) {
            checkCustomerId(user.getCustomerId());
        }
    }

    protected void checkEntityId(EntityId entityId) throws ThingsboardException {
        try {
            checkNotNull(entityId);
            validateId(entityId.getId(), "Incorrect entityId " + entityId);
            SecurityUser authUser = getCurrentUser();
            switch (entityId.getEntityType()) {
                case DEVICE:
                    checkDevice(deviceService.findDeviceById(authUser.getTenantId(), new DeviceId(entityId.getId())));
                    return;
                case CUSTOMER:
                    checkCustomerId(new CustomerId(entityId.getId()));
                    return;
                case TENANT:
                    checkTenantId(new TenantId(entityId.getId()));
                    return;
                case RULE_CHAIN:
                    checkRuleChain(new RuleChainId(entityId.getId()));
                    return;
                case ASSET:
                    checkAsset(assetService.findAssetById(authUser.getTenantId(), new AssetId(entityId.getId())));
                    return;
                case DASHBOARD:
                    checkDashboardId(new DashboardId(entityId.getId()));
                    return;
                case USER:
                    checkUserId(new UserId(entityId.getId()));
                    return;
                case ENTITY_VIEW:
                    checkEntityViewId(new EntityViewId(entityId.getId()));
                    return;
                case REPORT:
                    return;
                default:
                    throw new IllegalArgumentException("Unsupported entity type: " + entityId.getEntityType());
            }
        } catch (Exception e) {
            throw handleException(e, false);
        }
    }

    Device checkDeviceId(TenantId tenantId, DeviceId deviceId) throws ThingsboardException {
        try {
            validateId(deviceId, "Incorrect deviceId " + deviceId);
            Device device = deviceService.findDeviceById(tenantId, deviceId);
            checkNotNull(device);
            checkDevice(tenantId, device);
            return device;
        } catch (Exception e) {
            throw handleException(e, false);
        }
    }

    Device checkDeviceId(DeviceId deviceId) throws ThingsboardException {
        try {
            validateId(deviceId, "Incorrect deviceId " + deviceId);
            Device device = deviceService.findDeviceById(getCurrentUser().getTenantId(), deviceId);
            checkDevice(device);
            return device;
        } catch (Exception e) {
            throw handleException(e, false);
        }
    }

    protected void checkDevice(TenantId tenantId, Device device) throws ThingsboardException {
        checkNotNull(device);
        checkTenantId(tenantId);
        checkCustomerId(tenantId, device.getCustomerId());
    }

    protected void checkDevice(Device device) throws ThingsboardException {
        checkNotNull(device);
        checkTenantId(device.getTenantId());
        checkCustomerId(device.getCustomerId());
    }

    protected EntityView checkEntityViewId(EntityViewId entityViewId) throws ThingsboardException {
        try {
            validateId(entityViewId, "Incorrect entityViewId " + entityViewId);
            EntityView entityView = entityViewService.findEntityViewById(getCurrentUser().getTenantId(), entityViewId);
            checkEntityView(entityView);
            return entityView;
        } catch (Exception e) {
            throw handleException(e, false);
        }
    }

    protected void checkEntityView(EntityView entityView) throws ThingsboardException {
        checkNotNull(entityView);
        checkTenantId(entityView.getTenantId());
        checkCustomerId(entityView.getCustomerId());
    }

    /**
     * 检查assetId是否归属于tenantId和customerId,并且当前登录用户有访问assetId的权限。
     * add by zhengtao at 2019/04/26
     *
     * @return
     * @throws ThingsboardException
     */
    protected Asset checkAssetId(TenantId tenantId, CustomerId customerId, AssetId assetId) throws ThingsboardException {
        Asset asset = checkAssetId(assetId);
        if (tenantId != null && !asset.getTenantId().equals(tenantId)) {
            throw new ThingsboardException(YOU_DON_T_HAVE_PERMISSION_TO_PERFORM_THIS_OPERATION, ThingsboardErrorCode.PERMISSION_DENIED);
        }
        if (customerId != null && !asset.getCustomerId().equals(customerId)) {
            throw new ThingsboardException(YOU_DON_T_HAVE_PERMISSION_TO_PERFORM_THIS_OPERATION, ThingsboardErrorCode.PERMISSION_DENIED);
        }
        return asset;
    }

    Asset checkAssetId(TenantId tenantId, AssetId assetId) throws ThingsboardException {
        try {
            validateId(assetId, "Incorrect assetId " + assetId);
            Asset asset = assetService.findAssetById(tenantId, assetId);
            checkNotNull(asset);
            //todo SYS_ADMIN check
            //checkAsset(asset);
            return asset;
        } catch (Exception e) {
            throw handleException(e, false);
        }
    }

    Asset checkAssetId(AssetId assetId) throws ThingsboardException {
        try {
            validateId(assetId, "Incorrect assetId " + assetId);
            Asset asset = assetService.findAssetById(getCurrentUser().getTenantId(), assetId);
            checkAsset(asset);
            return asset;
        } catch (Exception e) {
            throw handleException(e, false);
        }
    }

    protected void checkAsset(Asset asset) throws ThingsboardException {
        checkNotNull(asset);
        checkTenantId(asset.getTenantId());
        checkCustomerId(asset.getCustomerId());
    }

    Alarm checkAlarmId(AlarmId alarmId) throws ThingsboardException {
        try {
            validateId(alarmId, "Incorrect alarmId " + alarmId);
            Alarm alarm = alarmService.findAlarmByIdAsync(getCurrentUser().getTenantId(), alarmId).get();
            checkAlarm(alarm);
            return alarm;
        } catch (Exception e) {
            throw handleException(e, false);
        }
    }

    AlarmInfo checkAlarmInfoId(AlarmId alarmId) throws ThingsboardException {
        try {
            validateId(alarmId, "Incorrect alarmId " + alarmId);
            AlarmInfo alarmInfo = alarmService.findAlarmInfoByIdAsync(getCurrentUser().getTenantId(), alarmId).get();
            checkAlarm(alarmInfo);
            return alarmInfo;
        } catch (Exception e) {
            throw handleException(e, false);
        }
    }

    protected void checkAlarm(Alarm alarm) throws ThingsboardException {
        checkNotNull(alarm);
        checkTenantId(alarm.getTenantId());
    }

    protected void checkTimestamp(Long ts) throws ThingsboardException {
        if (ts <= 0) {
            throw handleException(new IllegalArgumentException("Timestamp must greater than zero."));
        }
    }

    protected void checkTimestamps(Long startTs, Long endTs) throws ThingsboardException {
        checkTimestamp(startTs);
        checkTimestamp(endTs);
        if (endTs < startTs) {
            throw handleException(new IllegalArgumentException("End Timestamp must greater than StartTs"));
        }
    }

    WidgetsBundle checkWidgetsBundleId(WidgetsBundleId widgetsBundleId, boolean modify) throws ThingsboardException {
        try {
            validateId(widgetsBundleId, "Incorrect widgetsBundleId " + widgetsBundleId);
            WidgetsBundle widgetsBundle = widgetsBundleService.findWidgetsBundleById(getCurrentUser().getTenantId(), widgetsBundleId);
            checkWidgetsBundle(widgetsBundle, modify);
            return widgetsBundle;
        } catch (Exception e) {
            throw handleException(e, false);
        }
    }

    private void checkWidgetsBundle(WidgetsBundle widgetsBundle, boolean modify) throws ThingsboardException {
        checkNotNull(widgetsBundle);
        if (widgetsBundle.getTenantId() != null && !widgetsBundle.getTenantId().getId().equals(ModelConstants.NULL_UUID)) {
            checkTenantId(widgetsBundle.getTenantId());
        } else if (modify && getCurrentUser().getAuthority() != Authority.SYS_ADMIN) {
            throw new ThingsboardException(YOU_DON_T_HAVE_PERMISSION_TO_PERFORM_THIS_OPERATION,
                    ThingsboardErrorCode.PERMISSION_DENIED);
        }
    }

    WidgetType checkWidgetTypeId(WidgetTypeId widgetTypeId, boolean modify) throws ThingsboardException {
        try {
            validateId(widgetTypeId, "Incorrect widgetTypeId " + widgetTypeId);
            WidgetType widgetType = widgetTypeService.findWidgetTypeById(getCurrentUser().getTenantId(), widgetTypeId);
            checkWidgetType(widgetType, modify);
            return widgetType;
        } catch (Exception e) {
            throw handleException(e, false);
        }
    }

    void checkWidgetType(WidgetType widgetType, boolean modify) throws ThingsboardException {
        checkNotNull(widgetType);
        if (widgetType.getTenantId() != null && !widgetType.getTenantId().getId().equals(ModelConstants.NULL_UUID)) {
            checkTenantId(widgetType.getTenantId());
        } else if (modify && getCurrentUser().getAuthority() != Authority.SYS_ADMIN) {
            throw new ThingsboardException(YOU_DON_T_HAVE_PERMISSION_TO_PERFORM_THIS_OPERATION,
                    ThingsboardErrorCode.PERMISSION_DENIED);
        }
    }

    Dashboard checkDashboardId(DashboardId dashboardId) throws ThingsboardException {
        try {
            validateId(dashboardId, "Incorrect dashboardId " + dashboardId);
            Dashboard dashboard = dashboardService.findDashboardById(getCurrentUser().getTenantId(), dashboardId);
            checkDashboard(dashboard);
            return dashboard;
        } catch (Exception e) {
            throw handleException(e, false);
        }
    }

    DashboardInfo checkDashboardInfoId(DashboardId dashboardId) throws ThingsboardException {
        try {
            validateId(dashboardId, "Incorrect dashboardId " + dashboardId);
            DashboardInfo dashboardInfo = dashboardService.findDashboardInfoById(getCurrentUser().getTenantId(), dashboardId);
            checkDashboard(dashboardInfo);
            return dashboardInfo;
        } catch (Exception e) {
            throw handleException(e, false);
        }
    }

    private void checkDashboard(DashboardInfo dashboard) throws ThingsboardException {
        checkNotNull(dashboard);
        checkTenantId(dashboard.getTenantId());
        SecurityUser authUser = getCurrentUser();
        if (authUser.getAuthority() == Authority.CUSTOMER_USER) {
            if (!dashboard.isAssignedToCustomer(authUser.getCustomerId())) {
                throw new ThingsboardException(YOU_DON_T_HAVE_PERMISSION_TO_PERFORM_THIS_OPERATION,
                        ThingsboardErrorCode.PERMISSION_DENIED);
            }
        }
    }


    ComponentDescriptor checkComponentDescriptorByClazz(String clazz) throws ThingsboardException {
        try {
            log.debug("[{}] Lookup component descriptor", clazz);
            return checkNotNull(componentDescriptorService.getComponent(clazz));
        } catch (Exception e) {
            throw handleException(e, false);
        }
    }

    List<ComponentDescriptor> checkComponentDescriptorsByType(ComponentType type) throws ThingsboardException {
        try {
            log.debug("[{}] Lookup component descriptors", type);
            return componentDescriptorService.getComponents(type);
        } catch (Exception e) {
            throw handleException(e, false);
        }
    }

    List<ComponentDescriptor> checkComponentDescriptorsByTypes(Set<ComponentType> types) throws ThingsboardException {
        try {
            log.debug("[{}] Lookup component descriptors", types);
            return componentDescriptorService.getComponents(types);
        } catch (Exception e) {
            throw handleException(e, false);
        }
    }

    protected RuleChain checkRuleChain(RuleChainId ruleChainId) throws ThingsboardException {
        checkNotNull(ruleChainId);
        return checkRuleChain(ruleChainService.findRuleChainById(getCurrentUser().getTenantId(), ruleChainId));
    }

    protected RuleChain checkRuleChain(RuleChain ruleChain) throws ThingsboardException {
        checkNotNull(ruleChain);
        SecurityUser authUser = getCurrentUser();
        TenantId tenantId = ruleChain.getTenantId();
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        if (authUser.getAuthority() != Authority.TENANT_ADMIN ||
                !authUser.getTenantId().equals(tenantId)) {
            throw new ThingsboardException(YOU_DON_T_HAVE_PERMISSION_TO_PERFORM_THIS_OPERATION,
                    ThingsboardErrorCode.PERMISSION_DENIED);
        }
        return ruleChain;
    }


    protected String constructBaseUrl(HttpServletRequest request) {
        String scheme = request.getScheme();
        if (request.getHeader("x-forwarded-proto") != null) {
            scheme = request.getHeader("x-forwarded-proto");
        }
        int serverPort = request.getServerPort();
        if (request.getHeader("x-forwarded-port") != null) {
            try {
                serverPort = request.getIntHeader("x-forwarded-port");
            } catch (NumberFormatException e) {
            }
        }

        String baseUrl = String.format("%s://%s:%d",
                scheme,
                request.getServerName(),
                serverPort);
        return baseUrl;
    }

    protected <I extends EntityId> I emptyId(EntityType entityType) {
        return (I) EntityIdFactory.getByTypeAndUuid(entityType, ModelConstants.NULL_UUID);
    }

    protected <E extends HasName, I extends EntityId> void logEntityAction(I entityId, E entity, CustomerId customerId,
                                                                           ActionType actionType, Exception e, Object... additionalInfo) throws ThingsboardException {
        logEntityAction(getCurrentUser(), entityId, entity, customerId, actionType, e, additionalInfo);
    }

    protected <E extends HasName, I extends EntityId> void logEntityAction(User user, I entityId, E entity, CustomerId customerId,
                                                                           ActionType actionType, Exception e, Object... additionalInfo) throws ThingsboardException {
        if (customerId == null || customerId.isNullUid()) {
            customerId = user.getCustomerId();
        }
        if (e == null) {
            pushEntityActionToRuleEngine(entityId, entity, user, customerId, actionType, additionalInfo);
        }
        auditLogService.logEntityAction(user.getTenantId(), customerId, user.getId(), user.getName(), entityId, entity, actionType, e, additionalInfo);
    }


    public static Exception toException(Throwable error) {
        return error != null ? (Exception.class.isInstance(error) ? (Exception) error : new Exception(error)) : null;
    }

    public Boolean checkAuthority(Authority groupType,String groupId) throws ThingsboardException {
        switch (groupType) {
            case SYS_ADMIN:
                if (!getCurrentUser().getAuthority().equals(groupType))
                    throw new ThingsboardException(ThingsboardErrorCode.PERMISSION_DENIED);
                break;
            case TENANT_ADMIN:
                if (getCurrentUser().getAuthority().equals(Authority.SYS_ADMIN))
                    break;
                if (getTenantId().equals(new TenantId(UUID.fromString(groupId))))
                    break;
                throw new ThingsboardException(ThingsboardErrorCode.PERMISSION_DENIED);
            case CUSTOMER_USER:
                switch (getCurrentUser().getAuthority()) {
                    case SYS_ADMIN:
                        break;
                    case TENANT_ADMIN:
                        Optional<Customer> optionalCustomer = Optional.ofNullable(customerService.findCustomerById(null,new CustomerId(UUID.fromString(groupId))));
                        if (!optionalCustomer.isPresent())
                            throw new ThingsboardException(ThingsboardErrorCode.INVALID_ARGUMENTS);
                        if(optionalCustomer.get().getTenantId().equals(getTenantId()))
                            break;
                        else
							throw new ThingsboardException(ThingsboardErrorCode.PERMISSION_DENIED);
                    case CUSTOMER_USER:
                        if (getCurrentUser().getCustomerId().equals(new CustomerId(UUID.fromString(groupId))))
                            break;
                        throw new ThingsboardException(ThingsboardErrorCode.PERMISSION_DENIED);
                }
                break;
        }
        return Boolean.TRUE;
    }
    private <E extends HasName, I extends EntityId> void pushEntityActionToRuleEngine(I entityId, E entity, User user, CustomerId customerId,
                                                                                      ActionType actionType, Object... additionalInfo) {
        String msgType = null;
        switch (actionType) {
            case ADDED:
                msgType = DataConstants.ENTITY_CREATED;
                break;
            case DELETED:
                msgType = DataConstants.ENTITY_DELETED;
                break;
            case UPDATED:
                msgType = DataConstants.ENTITY_UPDATED;
                break;
            case ASSIGNED_TO_CUSTOMER:
                msgType = DataConstants.ENTITY_ASSIGNED;
                break;
            case UNASSIGNED_FROM_CUSTOMER:
                msgType = DataConstants.ENTITY_UNASSIGNED;
                break;
            case ATTRIBUTES_UPDATED:
                msgType = DataConstants.ATTRIBUTES_UPDATED;
                break;
            case ATTRIBUTES_DELETED:
                msgType = DataConstants.ATTRIBUTES_DELETED;
                break;
            case ALARM_ACK:
                msgType = DataConstants.ALARM_ACK;
                break;
            case ALARM_CLEAR:
                msgType = DataConstants.ALARM_CLEAR;
                break;
        }
        if (!StringUtils.isEmpty(msgType)) {
            try {
                TbMsgMetaData metaData = new TbMsgMetaData();
                metaData.putValue("userId", user.getId().toString());
                metaData.putValue("userName", user.getName());
                if (customerId != null && !customerId.isNullUid()) {
                    metaData.putValue("customerId", customerId.toString());
                }
                if (actionType == ActionType.ASSIGNED_TO_CUSTOMER) {
                    String strCustomerId = extractParameter(String.class, 1, additionalInfo);
                    String strCustomerName = extractParameter(String.class, 2, additionalInfo);
                    metaData.putValue("assignedCustomerId", strCustomerId);
                    metaData.putValue("assignedCustomerName", strCustomerName);
                } else if (actionType == ActionType.UNASSIGNED_FROM_CUSTOMER) {
                    String strCustomerId = extractParameter(String.class, 1, additionalInfo);
                    String strCustomerName = extractParameter(String.class, 2, additionalInfo);
                    metaData.putValue("unassignedCustomerId", strCustomerId);
                    metaData.putValue("unassignedCustomerName", strCustomerName);
                }
                ObjectNode entityNode;
                if (entity != null) {
                    entityNode = json.valueToTree(entity);
                    if (entityId.getEntityType() == EntityType.DASHBOARD) {
                        entityNode.put("configuration", "");
                    }
                } else {
                    entityNode = json.createObjectNode();
                    if (actionType == ActionType.ATTRIBUTES_UPDATED) {
                        String scope = extractParameter(String.class, 0, additionalInfo);
                        List<AttributeKvEntry> attributes = extractParameter(List.class, 1, additionalInfo);
                        metaData.putValue("scope", scope);
                        if (attributes != null) {
                            for (AttributeKvEntry attr : attributes) {
                                if (attr.getDataType() == DataType.BOOLEAN) {
                                    entityNode.put(attr.getKey(), attr.getBooleanValue().get());
                                } else if (attr.getDataType() == DataType.DOUBLE) {
                                    entityNode.put(attr.getKey(), attr.getDoubleValue().get());
                                } else if (attr.getDataType() == DataType.LONG) {
                                    entityNode.put(attr.getKey(), attr.getLongValue().get());
                                } else {
                                    entityNode.put(attr.getKey(), attr.getValueAsString());
                                }
                            }
                        }
                    } else if (actionType == ActionType.ATTRIBUTES_DELETED) {
                        String scope = extractParameter(String.class, 0, additionalInfo);
                        List<String> keys = extractParameter(List.class, 1, additionalInfo);
                        metaData.putValue("scope", scope);
                        ArrayNode attrsArrayNode = entityNode.putArray("attributes");
                        if (keys != null) {
                            keys.forEach(attrsArrayNode::add);
                        }
                    }
                }
                TbMsg tbMsg = new TbMsg(UUIDs.timeBased(), msgType, entityId, metaData, TbMsgDataType.JSON
                        , json.writeValueAsString(entityNode)
                        , null, null, 0L);
                actorService.onMsg(new SendToClusterMsg(entityId, new ServiceToRuleEngineMsg(user.getTenantId(), tbMsg)));
            } catch (Exception e) {
                log.warn("[{}] Failed to push entity action to rule engine: {}", entityId, actionType, e);
            }
        }
    }

    private <T> T extractParameter(Class<T> clazz, int index, Object... additionalInfo) {
        T result = null;
        if (additionalInfo != null && additionalInfo.length > index) {
            Object paramObject = additionalInfo[index];
            if (clazz.isInstance(paramObject)) {
                result = clazz.cast(paramObject);
            }
        }
        return result;
    }

    protected List<AlarmExInfo> fillAlarmExInfo(List<Alarm> alarmList) throws ThingsboardException {
        checkNotNull(alarmList);
        List<AlarmExInfo> retList = new ArrayList<>();

        alarmList.stream().forEach(alarm -> {
            AlarmExInfo tmpInfo = new AlarmExInfo();
            tmpInfo.setAlarmId(alarm.getId().toString());
            tmpInfo.setAlarmLevel(alarm.getSeverity().name());
            tmpInfo.setAlarmStatus(alarm.getStatus().name());
            tmpInfo.setAlarmTime(alarm.getStartTs());
            tmpInfo.setAlarmStartTime(alarm.getStartTs());
            tmpInfo.setAlarmEndTime(alarm.getEndTs());
            tmpInfo.setAlarmCount(alarm.getAlarmCount());

            if (null != alarm.getOriginator()) {
                if (alarm.getOriginator().getEntityType() == EntityType.DEVICE) {
                    Device device = deviceService.findDeviceById(null, new DeviceId(alarm.getOriginator().getId()));
                    if (null != device) {
                        tmpInfo.setDeviceId(device.getId().toString());
                        tmpInfo.setDeviceName(device.getName());
                        tmpInfo.setDeviceType(device.getType());
                        tmpInfo.setAdditionalInfo(alarm.getDetails());
                        DeviceAttributesEntity deviceAttributes = deviceAttributesService.findByEntityId(UUIDConverter.fromTimeUUID(device.getId().getId()));
                        if (null != deviceAttributes.getMeasureid()) {
                            tmpInfo.setMeasureid(deviceAttributes.getMeasureid());
                        }
//                        List<EntityRelation> tmpEntityRelationList = relationService.findByToAndType(null,device.getId(),EntityRelation.CONTAINS_TYPE,RelationTypeGroup.COMMON);
//                        for (EntityRelation entityRelation : tmpEntityRelationList){
//                            if (entityRelation.getFrom().getEntityType() == EntityType.ASSET){
//                                Asset tmpAsset = assetService.findAssetById(null,new AssetId(entityRelation.getFrom().getId()));
//                                if (null != tmpAsset){
//                                    tmpInfo.setAssetName(tmpAsset.getName());
//                                    break;
//                                }
//                            }
//                        }
                    } else {
                        tmpInfo.setDeviceId("Deleted");
                        tmpInfo.setDeviceName("Deleted");
                        tmpInfo.setAdditionalInfo(alarm.getDetails());
                    }
                    List<EntityRelation> tmpEntityRelationList = relationService.findByToAndType(null, alarm.getId(), "ALARM_ANY", RelationTypeGroup.ALARM);
                    for (EntityRelation entityRelation : tmpEntityRelationList) {
                        if (entityRelation.getFrom().getEntityType() == EntityType.ASSET) {
                            Asset tmpAsset = assetService.findAssetById(null, new AssetId(entityRelation.getFrom().getId()));
                            if (null != tmpAsset) {
                                tmpInfo.setAssetName(tmpAsset.getName());
                                break;
                            }
                        }
                    }


                }
            }
            retList.add(tmpInfo);

        });
        return retList;
    }

    /**
     * @Description: 计算设备特征值
     * @Author: ShenJi
     * @Date: 2019/3/21
     * @Param: [assetId, deviceIp, deviceChannle]
     * @return: java.lang.String
     */
    protected String calculateDeviceCode(String assetId, String deviceIp, String deviceChannle) {
        return (assetId + "|" + deviceIp + "|" + deviceChannle).hashCode() + "";
    }

    protected String calculateDeviceCode(String assetId, DeviceAutoLogon deviceAutoLogon) {
        return calculateDeviceCode(assetId, deviceAutoLogon.getDeviceShareAttrib().getIp(), deviceAutoLogon.getDeviceShareAttrib().getChannel());
    }

    /**
     * 获取指定entity的对应keys和scope的属性集合
     * @param entityId
     * @param keys
     * @param scope
     * @return
     */
    protected ListenableFuture<List<AttributeKvData>> getEntityAttrKvDatas(EntityId entityId, String keys, String scope){
        Validator.validateId(entityId.getId(),"Incorrect id " + entityId);
        final ListenableFuture<List<AttributeKvData>> attributeKvDatasFuture;
        List<String> keyList = toKeysList(keys);

        if (!StringUtils.isEmpty(scope)){
            if (keyList != null && !keyList.isEmpty()){

                ListenableFuture<List<AttributeKvEntry>> attributeKvEntriesFuture = attributesService.find(getCurrentUser().getTenantId(), entityId, scope, keyList);
                attributeKvDatasFuture = Futures.transform(attributeKvEntriesFuture,attributeKvEntries -> {
                    List<AttributeKvData> values;
                    if(attributeKvEntries != null){
                        values = attributeKvEntries.stream().map(attribute -> new AttributeKvData(attribute.getLastUpdateTs(),
                                attribute.getKey(), attribute.getValue())).collect(Collectors.toList());
                    } else {
                        values = new ArrayList<>();
                    }

                    return values;
                });
            } else {
                attributeKvDatasFuture = Futures.immediateFuture(new ArrayList<>());
            }
        } else {
            List<ListenableFuture<List<AttributeKvEntry>>> futures = new ArrayList<>();
            Arrays.stream(DataConstants.allScopes()).forEach(tmpScope->{
                if (keyList != null && !keyList.isEmpty()){
                    futures.add(attributesService.find(getCurrentUser().getTenantId(), entityId, tmpScope, keyList));
                }
            });
            ListenableFuture<List<List<AttributeKvEntry>>> attrKvEntryListFuture =Futures.successfulAsList(futures);
            attributeKvDatasFuture =  Futures.transform(attrKvEntryListFuture,attrKvEntryList->{
                List<AttributeKvData> values = new ArrayList<>();
                if(attrKvEntryList != null){
                    attrKvEntryList.forEach(attrKvEntrys->{
                        values.addAll(attrKvEntrys.stream().map(attribute -> new AttributeKvData(attribute.getLastUpdateTs(),
                                attribute.getKey(), attribute.getValue())).collect(Collectors.toList()));
                    });
                }
                return values;
            });
        }
        return attributeKvDatasFuture;
    }

    private List<String> toKeysList(String keys) {
        List<String> keyList = null;
        if (!org.springframework.util.StringUtils.isEmpty(keys)) {
            keyList = Arrays.asList(keys.split(","));
        }
        return keyList;
    }
}
