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
package org.thingsboard.rule.engine.action;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.rule.engine.api.*;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.task.Task;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import static org.thingsboard.rule.engine.api.util.DonAsynchron.withCallback;

@Slf4j
public abstract class TbAbstractTaskNode<C extends TbAbstractTaskNodeConfiguration> implements TbNode {

//    static final String PREV_ALARM_DETAILS = "prevAlarmDetails";

    static final String IS_NEW_TASK = "isNewTask";
    static final String IS_EXISTING_TASK = "isExistingTask";
    static final String IS_CLEARED_TASK = "isClearedTask";

    static final String MSG_TYPE = "TASK";

    protected final ObjectMapper mapper = new ObjectMapper();

    protected C config;
//    private ScriptEngine buildDetailsJsEngine;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = loadTaskNodeConfig(configuration);
//        this.buildDetailsJsEngine = ctx.createJsScriptEngine(config.getAlarmDetailsBuildJs());
    }

    protected abstract C loadTaskNodeConfig(TbNodeConfiguration configuration) throws TbNodeException;

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) {
        withCallback(processTask(ctx, msg),
                alarmResult -> {
                    if (alarmResult.task == null) {
                        ctx.tellNext(msg, "False");
                    } else if (alarmResult.isCreated) {
                        ctx.tellNext(toTaskMsg(ctx, alarmResult, msg), "Created");
                    } else if (alarmResult.isUpdated) {
                        ctx.tellNext(toTaskMsg(ctx, alarmResult, msg), "Updated");
                    } else if (alarmResult.isCleared) {
                        ctx.tellNext(toTaskMsg(ctx, alarmResult, msg), "Cleared");
                    }
                },
                t -> ctx.tellFailure(msg, t), ctx.getDbCallbackExecutor());
    }

    protected abstract ListenableFuture<TaskResult> processTask(TbContext ctx, TbMsg msg);

    private TbMsg toTaskMsg(TbContext ctx, TaskResult taskResult, TbMsg originalMsg) {
        JsonNode jsonNodes = mapper.valueToTree(taskResult.task);
        String data = jsonNodes.toString();
        TbMsgMetaData metaData = originalMsg.getMetaData().copy();
        if (taskResult.isCreated) {
            metaData.putValue(IS_NEW_TASK, Boolean.TRUE.toString());
        } else if (taskResult.isUpdated) {
            metaData.putValue(IS_EXISTING_TASK, Boolean.TRUE.toString());
        } else if (taskResult.isCleared) {
            metaData.putValue(IS_CLEARED_TASK, Boolean.TRUE.toString());
        }
        return ctx.transformMsg(originalMsg, MSG_TYPE, originalMsg.getOriginator(), metaData, data);
    }

    @Override
    public void destroy() {
//        if (buildDetailsJsEngine != null) {
//            buildDetailsJsEngine.destroy();
//        }
    }

    protected static class TaskResult {
        boolean isCreated;
        boolean isUpdated;
        boolean isCleared;
        Task task;

        TaskResult(boolean isCreated, boolean isUpdated, boolean isCleared, Task task) {
            this.isCreated = isCreated;
            this.isUpdated = isUpdated;
            this.isCleared = isCleared;
            this.task = task;
        }
    }
}
