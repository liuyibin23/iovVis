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
package org.thingsboard.server.common.data.exception;

import com.fasterxml.jackson.annotation.JsonValue;

public enum ThingsboardErrorCode {

    GENERAL(2),
    AUTHENTICATION(10),
    JWT_TOKEN_EXPIRED(11),
    PERMISSION_DENIED(20),
    INVALID_ARGUMENTS(30),
    BAD_REQUEST_PARAMS(31),
    ITEM_NOT_FOUND(32),
    TOO_MANY_REQUESTS(33),
    TOO_MANY_UPDATES(34),

    /**
     * 用户管理 user
     */
    USER_NAME_ALREADY_PRESENT(1001),
    USER_EMAIL_ALREADY_PRESENT(1002),
    USER_EMAIL_FORMAT_ERROR(1003),
    USER_PHONE_NUMBER_FORMAT_ERROR(1004),
    USER_NAME_NOT_SPECIFIED(1005),
    USER_EMAIL_NOT_SPECIFIED(1006),

    /**
     * 业主管理 tenant
     */
    TENANT_NAME_ALREADY_PRESENT(2001),

    /**
     * 项目管理 customer
     */
    CUSTOMER_NAME_ALREADY_PRESENT(3001),

    /**
     * 基础设施管理 asset
     */
    ASSET_NAME_ALREADY_PRESENT(4001),

    /**
     * 设备管理 device
     */
    DEVICE_NAME_ALREADY_PRESENT(5001),

    THIS_ERROR_CODE_IS_PLACEHOLDER(99999);

    private int errorCode;

    ThingsboardErrorCode(int errorCode) {
        this.errorCode = errorCode;
    }

    @JsonValue
    public int getErrorCode() {
        return errorCode;
    }

}
