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
package org.thingsboard.rule.engine.action;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmId;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.id.*;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.task.Task;
import org.thingsboard.server.common.data.task.TaskStatus;
import org.thingsboard.server.common.msg.TbMsg;

@Slf4j
@RuleNode(
        type = ComponentType.ACTION,
        name = "create task", relationTypes = {"Created", "Updated", "False"},
        configClazz = TbCreateTaskNodeConfiguration.class,
        nodeDescription = "Create or Update Task",
        nodeDetails =
                "Details - \n" +
                        "Node output:\n" +
                        "If task was not created, original message is returned. Otherwise new Message returned with type 'TASK', Task object in 'msg' property and 'matadata' will contains one of those properties 'isNewTask/isExistingTask'. " +
                        "Message payload can be accessed via <code>msg</code> property. For example <code>'temperature = ' + msg.temperature ;</code>. " +
                        "Message metadata can be accessed via <code>metadata</code> property. For example <code>'name = ' + metadata.customerName;</code>.",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbActionNodeCreateTaskConfig",
        icon = "schedule"
)
public class TbCreateTaskNode extends TbAbstractTaskNode<TbCreateTaskNodeConfiguration> {

    @Override
    protected TbCreateTaskNodeConfiguration loadTaskNodeConfig(TbNodeConfiguration configuration) throws TbNodeException {
        return TbNodeUtils.convert(configuration, TbCreateTaskNodeConfiguration.class);
    }

    @Override
    protected ListenableFuture<TaskResult> processTask(TbContext ctx, TbMsg msg) {
        Task task = ctx.getTaskService().findTaskByOriginator(msg.getOriginator());
        if (task == null || task.getTaskStatus().isCleared()) {
            return createNewTask(ctx, msg);
        } else {
            return updateTask(ctx, msg, task);
        }
    }

    private ListenableFuture<TaskResult> createNewTask(TbContext ctx, TbMsg msg) {
        return ctx.getDbCallbackExecutor().executeAsync(() -> {
            EntityId originatorId = msg.getOriginator();

            TenantId tenantId = new TenantId(EntityId.NULL_UUID);
            CustomerId customerId = new CustomerId(EntityId.NULL_UUID);
            UserId userId = new UserId(EntityId.NULL_UUID);
            String entityName = "";

            //for JIRA_369
            AssetId assetId = new AssetId(EntityId.NULL_UUID);
            String assetName = "";
            AlarmId alarmId = new AlarmId(EntityId.NULL_UUID);

            // 如果上一个Rule_Node是TbAbstractAlarmNode，解析Alarm内容
            if (msg.getType() == "ALARM") {
                if (Boolean.parseBoolean(msg.getMetaData().getValue(TbAbstractAlarmNode.IS_NEW_ALARM))) {
                    Alarm alarm = mapper.readValue(msg.getData(),Alarm.class);
                    alarmId = alarm.getId();
                }
            }

            if (originatorId.getEntityType() == EntityType.DEVICE) {
                Device device = ctx.getDeviceService().findDeviceById(null, new DeviceId(originatorId.getId()));
                tenantId = device.getTenantId();
                customerId = device.getCustomerId();
                entityName = device.getName();

                EntityRelation assetRelation = ctx.getRelationService().findByToAndType(null, originatorId, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.COMMON).stream()
                        .filter(rel -> rel.getFrom().getEntityType() == EntityType.ASSET).findFirst().get();
                if (assetRelation != null) {
                    assetId = (AssetId) assetRelation.getFrom();
                    Asset asset = ctx.getAssetService().findAssetById(null, assetId);
                    assetName = asset.getName();
                }
            } else if (originatorId.getEntityType() == EntityType.ASSET) {
                Asset asset = ctx.getAssetService().findAssetById(null, new AssetId(originatorId.getId()));
                tenantId = asset.getTenantId();
                customerId = asset.getCustomerId();
                entityName = asset.getName();
            } else {
                throw new IllegalArgumentException(String.format(" [%s] not supported entity type, must be %s and %s.",
                        originatorId.getEntityType(), EntityType.ASSET, EntityType.DEVICE));
            }

            if (customerId != null && !customerId.isNullUid()) {
                userId = ctx.getUserService().findFirstUserByCustomerId(customerId).getId();
            }

            Task task = Task.builder()
                    .tenantId(tenantId)
                    .customerId(customerId)
                    .userId(userId)
                    .taskStatus(TaskStatus.ACTIVE_UNACK)
                    .taskKind(config.getTaskKind())
                    .startTs(System.currentTimeMillis())
                    .taskName(entityName + "_自动创建")
                    .originator(originatorId)
                    .assetId(assetId)
                    .assetName(assetName)
                    .alarmId(alarmId)
//                    .additionalInfo(new ObjectMapper().readTree(msg.getData()))
                    .build();
            ctx.getTaskService().createOrUpdateTask(task);
            return new TaskResult(true, false, false, task);
        });
    }

    private ListenableFuture<TaskResult> updateTask(TbContext ctx, TbMsg msg, Task task) {
        return ctx.getDbCallbackExecutor().executeAsync(() -> {
            ctx.getTaskService().createOrUpdateTask(task);
            return new TaskResult(false, true, false, task);
        });
    }
}
