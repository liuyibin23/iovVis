package org.thingsboard.server.controller;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.thingsboard.server.common.data.*;
import org.thingsboard.server.common.data.alarm.AlarmId;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.*;
import org.thingsboard.server.common.data.kv.AttributeKvData;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.page.TimePageData;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.task.Task;
import org.thingsboard.server.common.data.task.TaskExInfo;
import org.thingsboard.server.common.data.task.TaskKind;
import org.thingsboard.server.common.data.task.TaskQuery;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static org.thingsboard.server.controller.AlarmController.ALARM_ID;

@RestController
@RequestMapping("/api")
@Slf4j
public class TaskController extends BaseController {

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/task", method = RequestMethod.POST)
    @ResponseBody
    public Task saveTask(@RequestBody Task taskSaveRequest) throws ThingsboardException {
        try {
            log.info(taskSaveRequest.toString());
            Task savedTask = checkNotNull(taskService.createOrUpdateTask(taskSaveRequest));
            logEntityAction(savedTask.getId(), savedTask,
                    getCurrentUser().getCustomerId(),
                    taskSaveRequest.getId() == null ? ActionType.ADDED : ActionType.UPDATED, null);
            return savedTask;
        } catch (Exception e) {
            logEntityAction(emptyId(EntityType.TASK), taskSaveRequest,
                    null, taskSaveRequest.getId() == null ? ActionType.ADDED : ActionType.UPDATED, e);
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/tasks", method = RequestMethod.GET)
    @ResponseBody
    public List<TaskExInfo> checkTasks() throws ThingsboardException {
        try {
            if (getCurrentUser().getAuthority().equals(Authority.SYS_ADMIN)) {
                return setTasksExInfo(checkNotNull(taskService.checkTasks()),null,null);
            } else if (getCurrentUser().getAuthority().equals(Authority.TENANT_ADMIN)) {
                return setTasksExInfo(checkNotNull(taskService.checkTasks(getCurrentUser().getTenantId())),null,null);
            } else
                return setTasksExInfo(checkNotNull(taskService.checkTasks(getCurrentUser().getTenantId(), getCurrentUser().getCustomerId())),null,null);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    /**
     * 1.2.14.6 查询所有任务（支持分页）
     * @return
     * @throws ThingsboardException
     */
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/page/tasks", method = RequestMethod.GET)
    @ResponseBody
    public TimePageData<TaskExInfo> getTasks(@RequestParam(required = false) String tenantIdStr,
                                             @RequestParam(required = false) String customerIdStr,
                                             @RequestParam(required = false) String assetIdStr,
                                             @RequestParam(required = false) String userIdStr,
                                             @RequestParam(required = false) String userName,
                                             @RequestParam(required = false) TaskKind taskKind,
                                             @RequestParam(required = false, defaultValue = "ALL") TaskQuery.StatusFilter statusFilter,
                                             @RequestParam int limit,
                                             @RequestParam(required = false) Long startTime,
                                             @RequestParam(required = false) Long endTime,
                                             @RequestParam(required = false) String idOffset,
                                             @RequestParam(required = false, defaultValue = "false") boolean ascOrder,
                                             @ApiParam(value = "查询时附带该task的指定key的属性在查询结果中返回，多个key用逗号分隔")
                                             @RequestParam(required = false) String keys,
                                             @ApiParam(value = "要获取属性的scope，空值表示获取所有scope")
                                             @RequestParam(required = false) String scope) throws ThingsboardException {
        Map<String,EntityId> tcIdMap = checkTenantIdAndCustomerIdParams(tenantIdStr,customerIdStr);
        TenantId tenantId = (TenantId) tcIdMap.get(KEY_TENANT_ID);
        CustomerId customerId = (CustomerId) tcIdMap.get(KEY_CUSTOMER_ID);

        UUID assetId = null;
        if(!Strings.isNullOrEmpty(assetIdStr)){
            assetId = UUID.fromString(assetIdStr);
            checkAssetId(tenantId,customerId,new AssetId(assetId));
        }

        List<UserId> userIds = null;
        UserId userId = null;
        if (!Strings.isNullOrEmpty(userIdStr)) {
            userId = new UserId(UUID.fromString(userIdStr));
            User user = checkUserId(userId);
            if (user != null) {
                if (tenantId != null && !user.getTenantId().equals(tenantId)) {
                    throw new ThingsboardException(YOU_DON_T_HAVE_PERMISSION_TO_PERFORM_THIS_OPERATION, ThingsboardErrorCode.PERMISSION_DENIED);
                }
                if (customerId != null && !user.getCustomerId().equals(customerId)) {
                    throw new ThingsboardException(YOU_DON_T_HAVE_PERMISSION_TO_PERFORM_THIS_OPERATION, ThingsboardErrorCode.PERMISSION_DENIED);
                }
            }

            userIds = Lists.newArrayListWithExpectedSize(1);
            userIds.add(userId);
        }

        if (startTime != null && endTime != null) {
            checkTimestamps(startTime, endTime);
        }
        TimePageLink pageLink = createPageLink(limit, startTime, endTime, ascOrder, idOffset);

        //userName Parameter should be ignored if userId is specified.
        if (userId == null && !Strings.isNullOrEmpty(userName)) {
            List<User> tmpUsers = userService.findUsersByFirstNameLike(userName);
            if (tmpUsers != null && !tmpUsers.isEmpty()) {
                userIds = Lists.newArrayListWithExpectedSize(tmpUsers.size());
                for (int i = 0; i < tmpUsers.size(); i++) {
                    userIds.add(tmpUsers.get(i).getId());
                }
            } else {
                //return empty list directly if there is no user called 'userName'.
                return new TimePageData<>(new ArrayList<>(), pageLink);
            }
        }

        TaskQuery query = TaskQuery.builder()
                .customerId(customerId)
                .assetId(assetId)
                .tenantId(tenantId)
                .taskKind(taskKind)
                .statusFilter(statusFilter)
                .userIdList(userIds)
                .build();

        try {
            List<Task> tasks = taskService.findTasks(query, pageLink).get();
            return new TimePageData<>(setTasksExInfo(tasks,keys,scope), pageLink);
        } catch (InterruptedException | ExecutionException e) {
            throw handleException(e);
        }
    }

    /**
     * 1.2.14.7 查询所有任务的数量
     *
     * @return
     * @throws ThingsboardException
     */
    @ApiOperation(value = "获取所有任务数量", notes = "根据当前登录用户的权限，统计所有任务的总数量。")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/count/tasks", method = RequestMethod.GET)
    @ResponseBody
    public CountData getTasksCount(@ApiParam(value = "业主id") @RequestParam(required = false) String tenantIdStr,
                                   @ApiParam(value = "项目id") @RequestParam(required = false) String customerIdStr,
                                   @ApiParam(value = "用户id") @RequestParam(required = false) String userIdStr,
                                   @ApiParam(value = "模糊匹配用户名称，如果userIdStr被指定，那么忽略该字段")
                                   @RequestParam(required = false) String userName,
                                   @RequestParam(required = false) TaskKind taskKind,
                                   @RequestParam(required = false, defaultValue = "ALL") TaskQuery.StatusFilter statusFilter,
                                   @RequestParam(required = false) Long startTime,
                                   @RequestParam(required = false) Long endTime) throws ThingsboardException {

        TenantId tenantId = null;
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

        List<UserId> userIds = null;
        UserId userId = null;
        if (!Strings.isNullOrEmpty(userIdStr)) {
            userId = new UserId(UUID.fromString(userIdStr));
            User user = checkUserId(userId);
            if (user != null) {
                if (tenantId != null && !user.getTenantId().equals(tenantId)) {
                    throw new ThingsboardException(YOU_DON_T_HAVE_PERMISSION_TO_PERFORM_THIS_OPERATION, ThingsboardErrorCode.PERMISSION_DENIED);
                }
                if (customerId != null && !user.getCustomerId().equals(customerId)) {
                    throw new ThingsboardException(YOU_DON_T_HAVE_PERMISSION_TO_PERFORM_THIS_OPERATION, ThingsboardErrorCode.PERMISSION_DENIED);
                }
            }

            userIds = Lists.newArrayListWithExpectedSize(1);
            userIds.add(userId);
        }

        if (startTime != null && endTime != null) {
            checkTimestamps(startTime, endTime);
        }
        TimePageLink pageLink = createPageLink(10, startTime, endTime,false,null);

        //userName Parameter should be ignored if userId is specified.
        if (userId == null && !Strings.isNullOrEmpty(userName)) {
            List<User> tmpUsers = userService.findUsersByFirstNameLike(userName);
            if (tmpUsers != null && !tmpUsers.isEmpty()) {
                userIds = Lists.newArrayListWithExpectedSize(tmpUsers.size());
                for (int i = 0; i < tmpUsers.size(); i++) {
                    userIds.add(tmpUsers.get(i).getId());
                }
            } else {
                //return empty list directly if there is no user called 'userName'.
                return new CountData(0L);
            }
        }

        TaskQuery query = TaskQuery.builder()
                .customerId(customerId)
                .tenantId(tenantId)
                .taskKind(taskKind)
                .statusFilter(statusFilter)
                .userIdList(userIds)
                .build();
        try {
            return new CountData(taskService.getTasksCount(query, pageLink).get());
        } catch (InterruptedException | ExecutionException e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/currentUser/tasks", method = RequestMethod.GET)
    @ResponseBody
    public List<TaskExInfo> findTasks(@RequestParam TaskKind taskKind) throws ThingsboardException {
        List<Task> taskList = null;
        try {
            if (getCurrentUser().getAuthority().equals(Authority.SYS_ADMIN)) {
                taskList = checkNotNull(taskService.checkTasks());
            } else if (getCurrentUser().getAuthority().equals(Authority.TENANT_ADMIN)) {
                taskList = checkNotNull(taskService.checkTasks(getCurrentUser().getTenantId()));
            } else
                taskList = checkNotNull(taskService.checkTasks(getCurrentUser().getTenantId(), getCurrentUser().getCustomerId()));

            taskList = taskList.stream().filter(task -> taskKind.equals(task.getTaskKind())).collect(Collectors.toList());

            return setTasksExInfo(taskList,null,null);
        } catch (Exception e) {
            throw handleException(e);
        }
    }


    /**
     * 1.2.14.8 根据任务id获取任务信息
     */
    @ApiOperation(value = "根据任务id获取任务信息", notes = "根据当前登录用户的权限，查询指定任务id的任务信息。")
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/task", method = RequestMethod.GET)
    @ResponseBody
    public Task getTaskById(@ApiParam(value = "任务id", required = true) @RequestParam() String taskIdStr)
            throws ThingsboardException {
        UUID taskId = UUID.fromString(taskIdStr);
        Task task = taskService.findTaskById(taskId);

        if (task == null) {
            throw new ThingsboardException("not found task with id[" + taskIdStr + "]", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
        }
        SecurityUser currentUser = getCurrentUser();
        if (currentUser.getAuthority() == Authority.SYS_ADMIN) {
            //do nothing
        } else if (currentUser.getAuthority() == Authority.TENANT_ADMIN) {
            if (task.getTenantId() == null || !task.getTenantId().equals(currentUser.getTenantId())) {
                throw new ThingsboardException(YOU_DON_T_HAVE_PERMISSION_TO_PERFORM_THIS_OPERATION, ThingsboardErrorCode.BAD_REQUEST_PARAMS);
            }
        } else if (currentUser.getAuthority() == Authority.CUSTOMER_USER) {
            if (task.getCustomerId() == null || !task.getCustomerId().equals(currentUser.getCustomerId())) {
                throw new ThingsboardException(YOU_DON_T_HAVE_PERMISSION_TO_PERFORM_THIS_OPERATION, ThingsboardErrorCode.BAD_REQUEST_PARAMS);
            }
            if (task.getTenantId() == null || !task.getTenantId().equals(currentUser.getTenantId())) {
                throw new ThingsboardException(YOU_DON_T_HAVE_PERMISSION_TO_PERFORM_THIS_OPERATION, ThingsboardErrorCode.BAD_REQUEST_PARAMS);
            }
        }
        return task;
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/currentUser/findTasksByUserName", method = RequestMethod.GET)
    @ResponseBody
    public List<TaskExInfo> findTasksByUserName(@RequestParam String firstName,
                                          @RequestParam(required = false) String lastName,
                                          @RequestParam(required = false) TaskKind taskKind
                                          ) throws ThingsboardException {
        List<User> userList = null;
        List<Task> taskList = new ArrayList<>();
        checkNotNull(firstName);
        if (null != lastName)
            userList = checkNotNull(userService.findUsersByFirstNameLikeAndLastNameLike("%" + firstName + "%", "%" + lastName + "%"));
        else
            userList = checkNotNull(userService.findUsersByFirstNameLike("%" + firstName + "%"));

        userList.stream()
                .forEach(user -> {
                    List<Task> taskTmpList = taskService.findTasksByUserId(user.getId());
                    if (null != taskTmpList) {
                        taskList.addAll(taskTmpList);
                    }
                });
        List<Task> tempTaskList = taskList.stream().filter(task -> {
            return taskKind == null || taskKind == task.getTaskKind();
        }).collect(Collectors.toList());
        return setTasksExInfo(tempTaskList,null,null);
    }

    /**
     * @Description: 跟据告警ID查询任务
     * @Author: ShenJi
     * @Date: 2019/3/14
     * @Param: [strAlarmId]
     * @return: java.util.List<org.thingsboard.server.common.data.task.Task>
     */
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/currentUser/getTasks", method = RequestMethod.GET)
    @ResponseBody
    //todo
    public List<TaskExInfo> getTaskByAlarmId(@RequestParam String strAlarmId,
                                             @ApiParam(value = "查询时附带该task的指定key的属性在查询结果中返回，多个key用逗号分隔")
                                             @RequestParam(required = false) String keys,
                                             @ApiParam(value = "要获取属性的scope，空值表示获取所有scope")
                                             @RequestParam(required = false) String scope) throws ThingsboardException {
        checkParameter(ALARM_ID, strAlarmId);
        try {

            List<Task> retTask = new ArrayList<>();
            AlarmId alarmId = new AlarmId(toUUID(strAlarmId));
            checkAlarmId(alarmId);
            log.error("alarmId : " + alarmId);
            Optional<List<EntityRelation>> optionalEntityRelations = Optional.ofNullable(relationService.findByToAndType(null, alarmId, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.COMMON));
            if (optionalEntityRelations.isPresent()) {
                if (optionalEntityRelations.get().size() > 0) {
                    log.error("relation size: " + optionalEntityRelations.get().size());
                    for (EntityRelation relation : optionalEntityRelations.get()) {
                        if (relation.getFrom().getEntityType() == EntityType.TASK) {
                            log.error("relation : " + relation.getFrom().getId());
                            Optional<Task> op = Optional.ofNullable(taskService.findTaskById(relation.getFrom().getId()));
                            if (op.isPresent()) {
                                Task tmpTask = op.get();
                                tmpTask.setAlarmId(alarmId);
                                retTask.add(tmpTask);
                            }

                        }
                    }
                }

            }

            return setTasksExInfo(retTask,keys,scope);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    private List<TaskExInfo> setTasksExInfo(List<Task> taskList, String attrKeys,String attrScope) throws ThingsboardException {
        checkNotNull(taskList);
        List<TaskExInfo> taskExInfos = new ArrayList<>();
        taskList.stream().forEach(task -> {
            if (null != task.getAssetId()) {
                Optional<Asset> optionalAsset = Optional.ofNullable(assetService.findAssetById(null, task.getAssetId()));
                if (optionalAsset.isPresent())
                    task.setAssetName(optionalAsset.get().getName());
                else
                    task.setAssetName(new String("Asset deleted"));
            }

            if (null != task.getUserId()) {
                Optional<User> optionalUser = Optional.ofNullable(userService.findUserById(null, task.getUserId()));
                if (optionalUser.isPresent())
                    task.setUserFirstName(optionalUser.get().getFirstName());
                else
                    task.setUserFirstName(new String("User deleted"));
            }

            if (null != task.getCustomerId()) {
                Optional<Customer> optionalCustomer = Optional.ofNullable(customerService.findCustomerById(null, task.getCustomerId()));
                if (optionalCustomer.isPresent())
                    task.setCustomerName(optionalCustomer.get().getName());
                else
                    task.setCustomerName(new String("Customer deleted"));
            }

            if (null != task.getOriginator()) {
                if (task.getOriginator().getEntityType() == EntityType.DEVICE) {
                    DeviceId deviceId = new DeviceId(task.getOriginator().getId());
                    Optional<Device> opdevice = Optional.ofNullable(deviceService.findDeviceById(null, deviceId));
                    if (opdevice.isPresent()) {
                        Optional<String> op = Optional.ofNullable(opdevice.get().getName());
                        if (op.isPresent())
                            task.setOriginatorName(op.get());
                    }

                }
            }
            try {
                TaskExInfo taskExInfo = new TaskExInfo(task);
                taskExInfo.setTaskAttrKv(getEntityAttrKvDatas(task.getId(),attrKeys,attrScope).get());
                taskExInfos.add(taskExInfo);
//                taskExInfos.add(setTaskAttrKvDatas(task,attrKeys,attrScope));
            } catch (Exception e) {
                throw handleException(e);
            }
        });
        return taskExInfos;
    }

//    private TaskExInfo setTaskAttrKvDatas(Task task,String keys,String scope) throws ExecutionException, InterruptedException {
//        TaskExInfo taskExInfo = new TaskExInfo(task);
//        List<String> keyList = toKeysList(keys);
//
//        if (!StringUtils.isEmpty(scope)){
//            if (keyList != null && !keyList.isEmpty()){
//                List<AttributeKvEntry> attributeKvEntries = attributesService.find(getCurrentUser().getTenantId(), taskExInfo.getId(), scope, keyList).get();
//                List<AttributeKvData> values = attributeKvEntries.stream().map(attribute -> new AttributeKvData(attribute.getLastUpdateTs(),
//                        attribute.getKey(), attribute.getValue())).collect(Collectors.toList());
//                taskExInfo.getTaskAttrKv().addAll(values);
//            }
//        } else {
//            for (String tmpScope : DataConstants.allScopes()){
//                if (keyList != null && !keyList.isEmpty()){
//                    List<AttributeKvEntry> attributeKvEntries = attributesService.find(getCurrentUser().getTenantId(), taskExInfo.getId(), tmpScope, keyList).get();
//                    List<AttributeKvData> values = attributeKvEntries.stream().map(attribute -> new AttributeKvData(attribute.getLastUpdateTs(),
//                            attribute.getKey(), attribute.getValue())).collect(Collectors.toList());
//                    taskExInfo.getTaskAttrKv().addAll(values);
//                }
//            }
//        }
//        return taskExInfo;
//    }

    private List<String> toKeysList(String keys) {
        List<String> keyList = null;
        if (!StringUtils.isEmpty(keys)) {
            keyList = Arrays.asList(keys.split(","));
        }
        return keyList;
    }
    private Task setTasksExInfo(Task task) throws ThingsboardException {
        checkNotNull(task);

        if (null != task.getAssetId()) {
            Optional<Asset> op = Optional.ofNullable(assetService.findAssetById(null, task.getAssetId()));
            if (op.isPresent())
                task.setAssetName(op.get().getName());
        }
        if (null != task.getUserId()) {
            Optional<User> op = Optional.ofNullable(userService.findUserById(null, task.getUserId()));
            if (op.isPresent())
                task.setUserFirstName(op.get().getFirstName());
        }
        if (null != task.getCustomerId()) {
            Optional<Customer> op = Optional.ofNullable(customerService.findCustomerById(null, task.getCustomerId()));
            if (op.isPresent())
                task.setCustomerName(op.get().getName());
        }
        if (null != task.getOriginator()) {
            if (task.getOriginator().getEntityType() == EntityType.DEVICE) {
                DeviceId deviceId = new DeviceId(task.getOriginator().getId());
                Optional<Device> op = Optional.ofNullable(deviceService.findDeviceById(null, deviceId));
                if (op.isPresent())
                    task.setOriginatorName(op.get().getName());
            }
        }
        return task;
    }
}
