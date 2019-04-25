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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.rule.engine.api.*;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.id.*;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.data.warnings.WarningsRecord;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import static org.thingsboard.rule.engine.api.util.DonAsynchron.withCallback;

@Slf4j
@RuleNode(
        type = ComponentType.ACTION,
        name = "create warning",
        configClazz = EmptyNodeConfiguration.class,
        relationTypes = {"Created", "Updated", "False"},
        nodeDescription = "Create or Update warning",
        nodeDetails =
                "Details - \n" +
                        "Node output:\n" +
                        "If warning was not created, original message is returned. Otherwise new Message returned with type 'WARNING', WarningRecord object in 'msg' property and 'matadata' will contains one of those properties 'isNewWarning/isExistingWarning'. " +
                        "Message payload can be accessed via <code>msg</code> property. For example <code>'temperature = ' + msg.temperature ;</code>. " +
                        "Message metadata can be accessed via <code>metadata</code> property. For example <code>'name = ' + metadata.customerName;</code>.",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbNodeEmptyConfig",
        icon = "warning"
)
public class TbCreateWarningNode implements TbNode {

    static final String IS_NEW_WARNING = "isNewWarning";
    static final String IS_EXISTING_WARNING = "isExistingWarning";
    static final String IS_CLEARED_WARNING = "isClearedWarning";

    private final ObjectMapper mapper = new ObjectMapper();
    protected EmptyNodeConfiguration config;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, EmptyNodeConfiguration.class);
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) {
        withCallback(processWarning(ctx, msg),
                warningResult -> {
                    if (warningResult.warning == null) {
                        ctx.tellNext(msg, "False");
                    } else if (warningResult.isCreated) {
                        ctx.tellNext(toWarningMsg(ctx, warningResult, msg), "Created");
                    } else if (warningResult.isUpdated) {
                        ctx.tellNext(toWarningMsg(ctx, warningResult, msg), "Updated");
                    } else if (warningResult.isCleared) {
                        ctx.tellNext(toWarningMsg(ctx, warningResult, msg), "Cleared");
                    }
                },
                t -> ctx.tellFailure(msg, t), ctx.getDbCallbackExecutor());
    }

    protected ListenableFuture<WarningResult> processWarning(TbContext ctx, TbMsg msg) {
        return createNewWarning(ctx, msg);
    }

    private ListenableFuture<WarningResult> createNewWarning(TbContext ctx, TbMsg msg) {
        return ctx.getDbCallbackExecutor().executeAsync(() -> {
            EntityId originatorId = msg.getOriginator();


            TenantId tenantId = new TenantId(EntityId.NULL_UUID);
            CustomerId customerId = new CustomerId(EntityId.NULL_UUID);
            UserId userId = new UserId(EntityId.NULL_UUID);
            String entityName = "";

            if (originatorId.getEntityType() == EntityType.ASSET) {
                Asset asset = ctx.getAssetService().findAssetById(null, new AssetId(originatorId.getId()));
                tenantId = asset.getTenantId();
                customerId = asset.getCustomerId();
                entityName = asset.getName();
            } else {
                throw new IllegalArgumentException(String.format(" [%s] not supported entity type, must be %s.",
                        originatorId.getEntityType(), EntityType.ASSET));
            }

            if (customerId != null && !customerId.isNullUid()) {
                userId = ctx.getUserService().findFirstUserByCustomerId(customerId).getId();
            }
            //陈莞魔鬼代码
            WarningsRecord warning = WarningsRecord.builder()
                    .tenantId(tenantId)
                    .customerId(customerId)
                    .userId(userId)
                    .assetId(new AssetId(originatorId.getId()))
                    .recordTs(System.currentTimeMillis())
                    .recordType(msg.getMetaData().getValue("recordType"))
                    .info(msg.getData())
                    .build();

            warning = ctx.getWarningService().save(warning);
            return new WarningResult(true, false, false, warning);
        });
    }

    private TbMsg toWarningMsg(TbContext ctx, WarningResult warningResult, TbMsg originalMsg) {
        JsonNode jsonNodes = mapper.valueToTree(warningResult.warning);
        String data = jsonNodes.toString();
        TbMsgMetaData metaData = originalMsg.getMetaData().copy();
        if (warningResult.isCreated) {
            metaData.putValue(IS_NEW_WARNING, Boolean.TRUE.toString());
        } else if (warningResult.isUpdated) {
            metaData.putValue(IS_EXISTING_WARNING, Boolean.TRUE.toString());
        } else if (warningResult.isCleared) {
            metaData.putValue(IS_CLEARED_WARNING, Boolean.TRUE.toString());
        }
        return ctx.transformMsg(originalMsg, "WARNING", originalMsg.getOriginator(), metaData, data);
    }

    @Override
    public void destroy() {
    }

    protected static class WarningResult {
        boolean isCreated;
        boolean isUpdated;
        boolean isCleared;
        WarningsRecord warning;

        WarningResult(boolean isCreated, boolean isUpdated, boolean isCleared, WarningsRecord warning) {
            this.isCreated = isCreated;
            this.isUpdated = isUpdated;
            this.isCleared = isCleared;
            this.warning = warning;
        }
    }
}
