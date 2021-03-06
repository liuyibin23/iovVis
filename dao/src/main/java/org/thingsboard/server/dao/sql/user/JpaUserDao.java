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
package org.thingsboard.server.dao.sql.user;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.sql.UserEntity;
import org.thingsboard.server.dao.sql.JpaAbstractSearchTextDao;
import org.thingsboard.server.dao.user.UserDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static org.thingsboard.server.common.data.UUIDConverter.fromTimeUUID;
import static org.thingsboard.server.dao.model.ModelConstants.NULL_UUID_STR;

/**
 * @author Valerii Sosliuk
 */
@Component
@SqlDao
public class JpaUserDao extends JpaAbstractSearchTextDao<UserEntity, User> implements UserDao {

    private String TENANTADMINFILTERSTR = "common";//"%power%\"common\"%";
    private String TENANTUSERFILTERSTR = "\"admin\"";//"%power%\"\"%";

    @Autowired
    private UserRepository userRepository;

    @Override
    protected Class<UserEntity> getEntityClass() {
        return UserEntity.class;
    }

    @Override
    protected CrudRepository<UserEntity, String> getCrudRepository() {
        return userRepository;
    }

    @Override
    public User findByEmail(TenantId tenantId, String email) {
        return DaoUtil.getData(userRepository.findByEmail(email));
    }

    @Override
    public List<User> findUsers(TextPageLink pageLink) {
        return DaoUtil.convertDataList(
                userRepository
                        .findUsers(
                                pageLink.getIdOffset() == null ? NULL_UUID_STR : fromTimeUUID(pageLink.getIdOffset()),
                                Objects.toString(pageLink.getTextSearch(), ""),
                                new PageRequest(0, pageLink.getLimit())));
    }

    @Override
    public List<User> findTenantAdmins(UUID tenantId, TextPageLink pageLink) {
        return DaoUtil.convertDataList(
                userRepository
                        .findUsersByAuthority(
                                fromTimeUUID(tenantId),
                                NULL_UUID_STR,
                                pageLink.getIdOffset() == null ? NULL_UUID_STR : fromTimeUUID(pageLink.getIdOffset()),
                                Objects.toString(pageLink.getTextSearch(), ""),
                                Authority.TENANT_ADMIN,
                                new PageRequest(0, pageLink.getLimit())));
    }

    @Override
    public List<User> findCustomerUsers(UUID tenantId, UUID customerId, TextPageLink pageLink) {
        return DaoUtil.convertDataList(
                userRepository
                        .findUsersByAuthority(
                                fromTimeUUID(tenantId),
                                fromTimeUUID(customerId),
                                pageLink.getIdOffset() == null ? NULL_UUID_STR : fromTimeUUID(pageLink.getIdOffset()),
                                Objects.toString(pageLink.getTextSearch(), ""),
                                Authority.CUSTOMER_USER,
                                new PageRequest(0, pageLink.getLimit())));

    }

    @Override
    public List<User> findCustomerUsers(UUID customerId, TextPageLink pageLink) {
        return DaoUtil.convertDataList(
                userRepository
                        .findUsersByAuthority(
                                fromTimeUUID(customerId),
                                pageLink.getIdOffset() == null ? NULL_UUID_STR : fromTimeUUID(pageLink.getIdOffset()),
                                Objects.toString(pageLink.getTextSearch(), ""),
                                Authority.CUSTOMER_USER,
                                new PageRequest(0, pageLink.getLimit())));

    }


    @Override
    public int countTenant(String tenantId) {
        return userRepository.countByTenantIdAndAuthority(tenantId, Authority.TENANT_ADMIN);
    }

    @Override
    public int countCustomerUsers(String tenantId, String customerId) {

        return userRepository.countByTenantIdAndCustomerIdAndAuthority(tenantId, customerId, Authority.CUSTOMER_USER);
    }

    @Override
    public List<User> findUserByTenantIdAndAuthority(UUID tenantId, Authority authority) {
        return DaoUtil.convertDataList(userRepository.findAllByTenantIdAndAuthority(fromTimeUUID(tenantId), authority));
    }

    @Override
    public int countTenantAdmin(String tenantId) {
        return userRepository.countByTenantIdAndAuthorityAndAdditionalInfoLike(tenantId, Authority.TENANT_ADMIN, TENANTADMINFILTERSTR);
    }

    @Override
    public int countTenantUser(String tenantId) {
        return userRepository.countByTenantIdAndAuthorityAndAdditionalInfoLike(tenantId, Authority.TENANT_ADMIN, TENANTUSERFILTERSTR);
    }

    @Override
    public List<User> findUsersByFirstNameLikeAndLastNameLike(String firstName, String lastName) {
        return DaoUtil.convertDataList(userRepository.findAllByFirstNameLikeAndLastNameLike(firstName, lastName));
    }

    @Override
    public List<User> findUsersByFirstNameLike(String firstname) {
        return DaoUtil.convertDataList(userRepository.findAllByFirstNameLike(firstname));
    }

    @Override
    public User findUserByFirstName(String firstName) {
        return DaoUtil.getData(userRepository.findFirstByFirstName(firstName));
    }

    @Override
    public List<User> findUsersByTenantId(UUID tenantId, TextPageLink pageLink) {
        return DaoUtil.convertDataList(userRepository.findUsersByTenantId(
                fromTimeUUID(tenantId),
                pageLink.getIdOffset() == null ? NULL_UUID_STR : fromTimeUUID(pageLink.getIdOffset()),
                Objects.toString(pageLink.getTextSearch(), ""),
                new PageRequest(0, pageLink.getLimit())));
    }

    @Override
    public List<User> findUsersByTenantIdAndCustomerId(UUID tenantId, UUID customerId, TextPageLink pageLink) {
        return DaoUtil.convertDataList(
                userRepository
                        .findUsersByTenantAndCustomerId(
                                fromTimeUUID(tenantId),
                                fromTimeUUID(customerId),
                                pageLink.getIdOffset() == null ? NULL_UUID_STR : fromTimeUUID(pageLink.getIdOffset()),
                                Objects.toString(pageLink.getTextSearch(), ""),
                                new PageRequest(0, pageLink.getLimit())));
    }

    @Override
    public User findFirstUserByCustomerId(UUID customerId) {
        return userRepository.findFirstByCustomerId(fromTimeUUID(customerId)).toData();
    }

    @Override
    public List<User> findUsersByAuthority(UUID tenantId, UUID customerId, Authority authority, TextPageLink pageLink) {
        String tenantIdStr;
        String customerIdStr;
        if (tenantId != null) {
            tenantIdStr = fromTimeUUID(tenantId);
        } else {
            tenantIdStr = null;
        }
        if (customerId != null) {
            customerIdStr = fromTimeUUID(customerId);
        } else {
            customerIdStr = null;
        }
        return DaoUtil.convertDataList(
                userRepository
                        .findUsersByAuthority(
                                tenantIdStr,
                                customerIdStr,
                                pageLink.getIdOffset() == null ? NULL_UUID_STR : fromTimeUUID(pageLink.getIdOffset()),
                                Objects.toString(pageLink.getTextSearch(), ""),
                                authority,
                                new PageRequest(0, pageLink.getLimit())));
    }
}
