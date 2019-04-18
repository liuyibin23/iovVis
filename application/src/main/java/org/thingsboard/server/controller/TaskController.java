package org.thingsboard.server.controller;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.alarm.AlarmId;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.page.TimePageData;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.task.Task;
import org.thingsboard.server.common.data.task.TaskKind;
import org.thingsboard.server.common.data.task.TaskQuery;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
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
    public List<Task> checkTasks() throws ThingsboardException {
        try {
            if (getCurrentUser().getAuthority().equals(Authority.SYS_ADMIN)) {
                return getTasksNameInfo(checkNotNull(taskService.checkTasks()));
            } else if (getCurrentUser().getAuthority().equals(Authority.TENANT_ADMIN)) {
                return getTasksNameInfo(checkNotNull(taskService.checkTasks(getCurrentUser().getTenantId())));
            } else
                return getTasksNameInfo(checkNotNull(taskService.checkTasks(getCurrentUser().getTenantId(), getCurrentUser().getCustomerId())));
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
    public TimePageData<Task> getTasks(@RequestParam(required = false) String tenantIdStr,
                                       @RequestParam(required = false) String customerIdStr,
                                       @RequestParam(required = false) String userIdStr,
                                       @RequestParam(required = false) String userName,
                                       @RequestParam(required = false) TaskKind taskKind,
                                       @RequestParam int limit,
                                       @RequestParam(required = false) Long startTime,
                                       @RequestParam(required = false) Long endTime,
                                       @RequestParam(required = false) String idOffset,
                                       @RequestParam(required = false, defaultValue = "false") boolean ascOrder) throws ThingsboardException {

//        TaskKind taskKind = null;
//        if (!Strings.isNullOrEmpty(taskKindStr)) {
//            taskKind = TaskKind.valueOf(taskKindStr);
//        }

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
                .tenantId(tenantId)
                .taskKind(taskKind)
                .userIdList(userIds)
                .build();

        try {
            List<Task> tasks = taskService.findTasks(query, pageLink).get();
            return new TimePageData<>(getTasksNameInfo(tasks), pageLink);
        } catch (InterruptedException | ExecutionException e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/currentUser/tasks", method = RequestMethod.GET)
    @ResponseBody
    public List<Task> findTasks(@RequestParam TaskKind taskKind) throws ThingsboardException {
        List<Task> taskList = null;
        try {
            if (getCurrentUser().getAuthority().equals(Authority.SYS_ADMIN)) {
                taskList = checkNotNull(taskService.checkTasks());
            } else if (getCurrentUser().getAuthority().equals(Authority.TENANT_ADMIN)) {
                taskList = checkNotNull(taskService.checkTasks(getCurrentUser().getTenantId()));
            } else
                taskList = checkNotNull(taskService.checkTasks(getCurrentUser().getTenantId(), getCurrentUser().getCustomerId()));

            taskList = taskList.stream().filter(task -> taskKind.equals(task.getTaskKind())).collect(Collectors.toList());

            return getTasksNameInfo(taskList);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/currentUser/findTasksByUserName", method = RequestMethod.GET)
    @ResponseBody
    public List<Task> findTasksByUserName(@RequestParam String firstName,
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
        return getTasksNameInfo(tempTaskList);
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
    public List<Task> getTaskByAlarmId(@RequestParam String strAlarmId) throws ThingsboardException {
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

            return getTasksNameInfo(retTask);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    private List<Task> getTasksNameInfo(List<Task> taskList) throws ThingsboardException {
        checkNotNull(taskList);
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
        });
        return taskList;
    }

    private Task getTasksNameInfo(Task task) throws ThingsboardException {
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
