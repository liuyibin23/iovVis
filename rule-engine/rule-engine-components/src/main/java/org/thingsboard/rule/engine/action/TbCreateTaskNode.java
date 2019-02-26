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

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Function;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.UUIDConverter;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmStatus;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.data.task.Task;
import org.thingsboard.server.common.data.task.TaskStatus;
import org.thingsboard.server.common.msg.TbMsg;
import sun.security.krb5.Config;

import java.util.UUID;

@Slf4j
@RuleNode(
        type = ComponentType.ACTION,
        name = "create task", relationTypes = {"Created", "Updated", "False"},
        configClazz = TbCreateTaskNodeConfiguration.class,
        nodeDescription = "Create or Update Task",
        nodeDetails =
                "Details - JS function that creates JSON object based on incoming message. This object will be added into Alarm.details field.\n" +
                        "Node output:\n" +
                        "If alarm was not created, original message is returned. Otherwise new Message returned with type 'ALARM', Alarm object in 'msg' property and 'matadata' will contains one of those properties 'isNewAlarm/isExistingAlarm'. " +
                        "Message payload can be accessed via <code>msg</code> property. For example <code>'temperature = ' + msg.temperature ;</code>. " +
                        "Message metadata can be accessed via <code>metadata</code> property. For example <code>'name = ' + metadata.customerName;</code>.",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbActionNodeCreateTaskConfig", //todo 适配UI
        icon = "notifications_active" //todo 修改icon
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
            Task task = buildTask(msg);
            return new TaskResult(true, false, false, task);
        });
    }

    private ListenableFuture<TaskResult> updateTask(TbContext ctx, TbMsg msg, Task task) {
        return ctx.getDbCallbackExecutor().executeAsync(() -> {
            modifyTask(task, msg);
            ctx.getTaskService().createOrUpdateTask(task);
            return new TaskResult(false, true, false, task);
        });
    }

    private Task buildTask(TbMsg msg) {
        return Task.builder()
                .tenantId(new TenantId(UUID.fromString(config.getTenantId())))
                .customerId(new CustomerId(UUID.fromString(config.getCustomerId())))
                .userId(new UserId(UUID.fromString(config.getUserId())))
                .originator(msg.getOriginator())
                .taskStatus(TaskStatus.ACTIVE_UNACK)
                .additionalInfo(config.getAdditionalInfo())
                .build();
    }

    private void modifyTask(Task task, TbMsg msg) {
        task.setTaskKind(config.getTaskKind());
        task.setAdditionalInfo(config.getAdditionalInfo());
        task.setTenantId(new TenantId(UUID.fromString(config.getTenantId())));
        task.setCustomerId(new CustomerId(UUID.fromString(config.getCustomerId())));
        task.setUserId(new UserId(UUID.fromString(config.getUserId())));
    }
}
