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
package org.thingsboard.server.common.data;

/**
 * 注意：枚举的顺序不能随意变动，新增类型只能往后追加，因为部分表（比如alarm）保存的是枚举的下标ordinary，不是字符串。
 * 注意：枚举的顺序不能随意变动，新增类型只能往后追加，因为部分表（比如alarm）保存的是枚举的下标ordinary，不是字符串。
 * 注意：枚举的顺序不能随意变动，新增类型只能往后追加，因为部分表（比如alarm）保存的是枚举的下标ordinary，不是字符串。
 *
 * @author Andrew Shvayka
 */
public enum EntityType {
    TENANT, CUSTOMER, USER, DASHBOARD, ASSET, DEVICE, ALARM, RULE_CHAIN, RULE_NODE, ENTITY_VIEW,
    PROJECT, BRIDGE, TUNNEL, ROAD, SLOPE, TASK, PATROL, WARNINGS, ALL, UNDEFINED, REPORT, HISTORY_VIDEO
}
