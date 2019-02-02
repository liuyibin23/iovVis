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

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.page.TextPageData;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.security.UserCredentials;

import java.util.List;

public interface UserService {

	TextPageData<User> findUsers(TextPageLink pageLink);

	List<User> findUserByTenantIdAndAuthority(TenantId tenantId, Authority authority);

	User findUserById(TenantId tenantId, UserId userId);

	ListenableFuture<User> findUserByIdAsync(TenantId tenantId, UserId userId);

	User findUserByEmail(TenantId tenantId, String email);

	User saveUser(User user);

	UserCredentials findUserCredentialsByUserId(TenantId tenantId, UserId userId);
	
	UserCredentials findUserCredentialsByActivateToken(TenantId tenantId, String activateToken);

	UserCredentials findUserCredentialsByResetToken(TenantId tenantId, String resetToken);

	UserCredentials saveUserCredentials(TenantId tenantId, UserCredentials userCredentials);
	
	UserCredentials activateUserCredentials(TenantId tenantId, String activateToken, String password);
	
	UserCredentials requestPasswordReset(TenantId tenantId, String email);

	void deleteUser(TenantId tenantId, UserId userId);
	
	TextPageData<User> findTenantAdmins(TenantId tenantId, TextPageLink pageLink);
	
	void deleteTenantAdmins(TenantId tenantId);
	
	TextPageData<User> findCustomerUsers(TenantId tenantId, CustomerId customerId, TextPageLink pageLink);

	TextPageData<User> findCustomerUsers(CustomerId customerId, TextPageLink pageLink);

	List<User> findUsers();
	List<User> findCustomerUsers(CustomerId customerId);
	List<User> findTenantUsers(TenantId tenantId);

	    
	void deleteCustomerUsers(TenantId tenantId, CustomerId customerId);

	int countByTenantId(String tenantId);

	int countByTenantIdAndCustomerId(String tenantId,String customerId);

	/**
	 * 获取业主管理员用户数量
	 * @param tenantId
	 * @return
	 */
	int countTenantAdminByTenantId(String tenantId);

	/**
	 * 获取业主普通用户数量
	 * @param tenantId
	 * @return
	 */
	int countTenantUserByTenantId(String tenantId);

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
	* @Param: [firstName]
	* @return: java.util.List<org.thingsboard.server.common.data.User>
	*/
	List<User> findUsersByFirstNameLike(String firstName);
}
