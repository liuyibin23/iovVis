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
package org.thingsboard.server.dao.user;

import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.dao.Dao;

import java.util.List;
import java.util.UUID;

public interface UserDao extends Dao<User> {

    /**
     * Save or update user object
     *
     * @param user the user object
     * @return saved user entity
     */
    User save(TenantId tenantId, User user);

    /**
     * Find user by email.
     *
     * @param email the email
     * @return the user entity
     */
    User findByEmail(TenantId tenantId, String email);

    /**
     * Find users by page link.
     *
     *
     * @param pageLink the page link
     * @return the list of user entities
     */
    List<User> findUsers(TextPageLink pageLink);

    /**
     * Find tenant admin users by tenantId and page link.
     *
     * @param tenantId the tenantId
     * @param pageLink the page link
     * @return the list of user entities
     */
    List<User> findTenantAdmins(UUID tenantId, TextPageLink pageLink);
    
    /**
     * Find customer users by tenantId, customerId and page link.
     *
     * @param tenantId the tenantId
     * @param customerId the customerId
     * @param pageLink the page link
     * @return the list of user entities
     */
    List<User> findCustomerUsers(UUID tenantId, UUID customerId, TextPageLink pageLink);

    List<User> findCustomerUsers(UUID customerId, TextPageLink pageLink);



    /**
    * @Description: Count Tenant Admin
    * @Author: ShenJi
    * @Date: 2019/1/5
    * @Param: [tenantId]
    * @return: java.lang.Long
    */
    int countTenant(String tenantId);
    /**
    * @Description: Count customers
    * @Author: ShenJi
    * @Date: 2019/1/5
    * @Param: [tenantId, customerId]
    * @return: java.lang.Long
    */
    int countCustomerUsers(String tenantId, String customerId);
    /**
     * @Description: return first tenant
     * @Author: ShenJi
     * @Date: 2019/1/5
     * @Param: [tenantId, customerId]
     * @return: java.lang.Long
     */
    List<User> findUserByTenantIdAndAuthority(UUID tenantId,Authority authority);

    /**
     * 获取业主管理员用户数量
     * @param tenantId
     * @return
     */
    int countTenantAdmin(String tenantId);

    /**
     * 获取业主普通用户数量
     * @param tenantId
     * @return
     */
    int countTenantUser(String tenantId);

    /**
    * @Description: 按照姓名模糊查找用户
    * @Author: ShenJi
    * @Date: 2019/1/30
    * @Param: [firstName, lastName]
    * @return: java.util.List<org.thingsboard.server.common.data.User>
    */
    List<User> findUsersByFirstNameLikeAndLastNameLike(String firstName,String lastName);

    /**
    * @Description: 按照firstName模糊查找用户
    * @Author: ShenJi
    * @Date: 2019/1/30
    * @Param: [firstname]
    * @return: java.util.List<org.thingsboard.server.common.data.User>
    */
    List<User> findUsersByFirstNameLike(String firstname);

//    List<User> findUsers();
    List<User> findUsersByTenantId(UUID tenantId,TextPageLink pageLink);
	List<User> findUsersByTenantIdAndCustomerId(UUID tenantId,UUID customerId,TextPageLink pageLink);

	User findFirstUserByCustomerId(UUID customerId);

    List<User> findUsersByAuthority(UUID tenantId, UUID customerId, Authority authority,TextPageLink pageLink);

    User findUserByFirstName(String firstName);
}


