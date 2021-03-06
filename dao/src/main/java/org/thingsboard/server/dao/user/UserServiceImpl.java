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
package org.thingsboard.server.dao.user;

import com.google.common.util.concurrent.ListenableFuture;
import joptsimple.internal.Strings;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.page.TextPageData;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.security.UserCredentials;
import org.thingsboard.server.dao.customer.CustomerDao;
import org.thingsboard.server.dao.entity.AbstractEntityService;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.exception.IncorrectParameterException;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.service.PaginatedRemover;
import org.thingsboard.server.dao.tenant.TenantDao;

import java.util.List;
import java.util.UUID;

import static org.thingsboard.server.dao.service.Validator.validateId;
import static org.thingsboard.server.dao.service.Validator.validatePageLink;
import static org.thingsboard.server.dao.service.Validator.validateString;

@Service
@Slf4j
public class UserServiceImpl extends AbstractEntityService implements UserService {

    private static final int DEFAULT_TOKEN_LENGTH = 30;
    public static final String INCORRECT_USER_ID = "Incorrect userId ";
    public static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";

    @Value("${security.user_login_case_sensitive:true}")
    private boolean userLoginCaseSensitive;

    @Autowired
    private UserDao userDao;

    @Autowired
    private UserCredentialsDao userCredentialsDao;

    @Autowired
    private TenantDao tenantDao;

    @Autowired
    private CustomerDao customerDao;

    @Override
    public User findUserByEmail(TenantId tenantId, String email) {
        log.trace("Executing findUserByEmail [{}]", email);
        validateString(email, "Incorrect email " + email);
        if (userLoginCaseSensitive) {
            return userDao.findByEmail(tenantId, email);
        } else {
            return userDao.findByEmail(tenantId, email.toLowerCase());
        }
    }

    @Override
    public User findUserByFirstName(TenantId tenantId, String firstName) {
        log.trace("Executing findUserByFirstName [{}]", firstName);
        validateString(firstName, "Incorrect firstName " + firstName);
        return userDao.findUserByFirstName(firstName);
    }

    @Override
    public User findUserById(TenantId tenantId, UserId userId) {
        log.trace("Executing findUserById [{}]", userId);
        validateId(userId, INCORRECT_USER_ID + userId);
        return userDao.findById(tenantId, userId.getId());
    }

    @Override
    public ListenableFuture<User> findUserByIdAsync(TenantId tenantId, UserId userId) {
        log.trace("Executing findUserByIdAsync [{}]", userId);
        validateId(userId, INCORRECT_USER_ID + userId);
        return userDao.findByIdAsync(tenantId, userId.getId());
    }

    @Override
    public User saveUser(User user) {
        log.trace("Executing saveUser [{}]", user);
        userValidator.validate(user, User::getTenantId);
        if (user.getId() == null && !userLoginCaseSensitive) {
            user.setEmail(user.getEmail().toLowerCase());
        }
        User savedUser = userDao.save(user.getTenantId(), user);
        if (user.getId() == null) {
            UserCredentials userCredentials = new UserCredentials();
            userCredentials.setEnabled(false);
            userCredentials.setActivateToken(RandomStringUtils.randomAlphanumeric(DEFAULT_TOKEN_LENGTH));
            userCredentials.setUserId(new UserId(savedUser.getUuidId()));
            userCredentialsDao.save(user.getTenantId(), userCredentials);
        }
        return savedUser;
    }

    @Override
    public UserCredentials findUserCredentialsByUserId(TenantId tenantId, UserId userId) {
        log.trace("Executing findUserCredentialsByUserId [{}]", userId);
        validateId(userId, INCORRECT_USER_ID + userId);
        return userCredentialsDao.findByUserId(tenantId, userId.getId());
    }

    @Override
    public UserCredentials findUserCredentialsByActivateToken(TenantId tenantId, String activateToken) {
        log.trace("Executing findUserCredentialsByActivateToken [{}]", activateToken);
        validateString(activateToken, "Incorrect activateToken " + activateToken);
        return userCredentialsDao.findByActivateToken(tenantId, activateToken);
    }

    @Override
    public UserCredentials findUserCredentialsByResetToken(TenantId tenantId, String resetToken) {
        log.trace("Executing findUserCredentialsByResetToken [{}]", resetToken);
        validateString(resetToken, "Incorrect resetToken " + resetToken);
        return userCredentialsDao.findByResetToken(tenantId, resetToken);
    }

    @Override
    public UserCredentials saveUserCredentials(TenantId tenantId, UserCredentials userCredentials) {
        log.trace("Executing saveUserCredentials [{}]", userCredentials);
        userCredentialsValidator.validate(userCredentials, data -> tenantId);
        return userCredentialsDao.save(tenantId, userCredentials);
    }

    @Override
    public UserCredentials activateUserCredentials(TenantId tenantId, String activateToken, String password) {
        log.trace("Executing activateUserCredentials activateToken [{}], password [{}]", activateToken, password);
        validateString(activateToken, "Incorrect activateToken " + activateToken);
        validateString(password, "Incorrect password " + password);
        UserCredentials userCredentials = userCredentialsDao.findByActivateToken(tenantId, activateToken);
        if (userCredentials == null) {
            throw new IncorrectParameterException(String.format("Unable to find user credentials by activateToken [%s]", activateToken));
        }
        if (userCredentials.isEnabled()) {
            throw new IncorrectParameterException("User credentials already activated");
        }
        userCredentials.setEnabled(true);
        userCredentials.setActivateToken(null);
        userCredentials.setPassword(password);

        return saveUserCredentials(tenantId, userCredentials);
    }

    @Override
    public UserCredentials requestPasswordReset(TenantId tenantId, String email) {
        log.trace("Executing requestPasswordReset email [{}]", email);
        validateString(email, "Incorrect email " + email);
        User user = userDao.findByEmail(tenantId, email);
        if (user == null) {
            throw new IncorrectParameterException(String.format("Unable to find user by email [%s]", email));
        }
        UserCredentials userCredentials = userCredentialsDao.findByUserId(tenantId, user.getUuidId());
        if (!userCredentials.isEnabled()) {
            throw new IncorrectParameterException("Unable to reset password for inactive user");
        }
        userCredentials.setResetToken(RandomStringUtils.randomAlphanumeric(DEFAULT_TOKEN_LENGTH));
        return saveUserCredentials(tenantId, userCredentials);
    }


    @Override
    public void deleteUser(TenantId tenantId, UserId userId) {
        log.trace("Executing deleteUser [{}]", userId);
        validateId(userId, INCORRECT_USER_ID + userId);
        UserCredentials userCredentials = userCredentialsDao.findByUserId(tenantId, userId.getId());
        userCredentialsDao.removeById(tenantId, userCredentials.getUuidId());
        deleteEntityRelations(tenantId, userId);
        userDao.removeById(tenantId, userId.getId());
    }

    @Override
    public TextPageData<User> findUsers(TextPageLink pageLink) {
        log.trace("Executing findUsers, pageLink [{}]", pageLink);
        validatePageLink(pageLink, "Incorrect page link " + pageLink);
        List<User> users = userDao.findUsers(pageLink);
        return new TextPageData<>(users, pageLink);
    }

    @Override
    public List<User> findUserByTenantIdAndAuthority(TenantId tenantId, Authority authority) {
        return userDao.findUserByTenantIdAndAuthority(tenantId.getId(), authority);
    }

    @Override
    public TextPageData<User> findTenantAdmins(TenantId tenantId, TextPageLink pageLink) {
        log.trace("Executing findTenantAdmins, tenantId [{}], pageLink [{}]", tenantId, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validatePageLink(pageLink, "Incorrect page link " + pageLink);
        List<User> users = userDao.findTenantAdmins(tenantId.getId(), pageLink);
        return new TextPageData<>(users, pageLink);
    }

    @Override
    public void deleteTenantAdmins(TenantId tenantId) {
        log.trace("Executing deleteTenantAdmins, tenantId [{}]", tenantId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        tenantAdminsRemover.removeEntities(tenantId, tenantId);
    }

    @Override
    public TextPageData<User> findCustomerUsers(TenantId tenantId, CustomerId customerId, TextPageLink pageLink) {
        log.trace("Executing findUsersByTenantIdAndCustomerId, tenantId [{}], customerId [{}], pageLink [{}]", tenantId, customerId, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(customerId, "Incorrect customerId " + customerId);
        validatePageLink(pageLink, "Incorrect page link " + pageLink);
        List<User> users = userDao.findCustomerUsers(tenantId.getId(), customerId.getId(), pageLink);
        return new TextPageData<>(users, pageLink);
    }


    @Override
    public TextPageData<User> findCustomerUsers(CustomerId customerId, TextPageLink pageLink) {
        log.trace("Executing findUsersByTenantIdAndCustomerId, customerId [{}], pageLink [{}]", customerId, pageLink);
        validateId(customerId, "Incorrect customerId " + customerId);
        validatePageLink(pageLink, "Incorrect page link " + pageLink);
        List<User> users = userDao.findCustomerUsers(customerId.getId(), pageLink);
        return new TextPageData<>(users, pageLink);
    }

//    @Override
//    public List<User> findUsers() {
//        return userDao.findUsers();
//    }

    @Override
    public TextPageData<User> findUsersByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId, TextPageLink pageLink) {
        log.trace("Executing findUsersByTenantIdAndCustomerId, customerId [{}]", customerId);
        validateId(customerId, "Incorrect customerId " + customerId);

        List<User> users = userDao.findUsersByTenantIdAndCustomerId(tenantId.getId(), customerId.getId(), pageLink);
        return new TextPageData<>(users, pageLink);
    }

    @Override
    public TextPageData<User> findTenantUsers(TenantId tenantId, TextPageLink pageLink) {
        log.trace("Executing findTenantUsers, TenantId [{}]", tenantId);
        validateId(tenantId, "Incorrect tenantId " + tenantId);
        List<User> users = userDao.findUsersByTenantId(tenantId.getId(), pageLink);
        return new TextPageData<>(users, pageLink);
    }

    @Override
    public void deleteCustomerUsers(TenantId tenantId, CustomerId customerId) {
        log.trace("Executing deleteCustomerUsers, customerId [{}]", customerId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(customerId, "Incorrect customerId " + customerId);
        customerUsersRemover.removeEntities(tenantId, customerId);
    }

    @Override
    public int countByTenantId(String tenantId) {
        return userDao.countTenant(tenantId);
    }

    @Override
    public int countByTenantIdAndCustomerId(String tenantId, String customerId) {
        return userDao.countCustomerUsers(tenantId, customerId);
    }

    @Override
    public int countTenantAdminByTenantId(String tenantId) {
        return userDao.countTenantAdmin(tenantId);
    }

    @Override
    public int countTenantUserByTenantId(String tenantId) {
        return userDao.countTenantUser(tenantId);
    }

    @Override
    public List<User> findUsersByFirstNameLikeAndLastNameLike(String firstName, String lastName) {
        return userDao.findUsersByFirstNameLikeAndLastNameLike(firstName, lastName);
    }

    @Override
    public List<User> findUsersByFirstNameLike(String firstName) {
        return userDao.findUsersByFirstNameLike(firstName);
    }


    @Override
    public User findFirstUserByCustomerId(CustomerId customerId) {
        return userDao.findFirstUserByCustomerId(customerId.getId());
    }

    @Override
    public TextPageData<User> findUsersByAuthority(TenantId tenantId, CustomerId customerId, Authority authority, TextPageLink pageLink) {
        UUID tenantUUID;
        UUID customerUUID;
        if (tenantId == null || tenantId.getId() == null) {
            tenantUUID = null;
        } else {
            tenantUUID = tenantId.getId();
        }
        if (customerId == null || customerId.getId() == null) {
            customerUUID = null;
        } else {
            customerUUID = customerId.getId();
        }
        List<User> users = userDao.findUsersByAuthority(tenantUUID, customerUUID, authority, pageLink);
        return new TextPageData<>(users, pageLink);
    }

    private DataValidator<User> userValidator =
            new DataValidator<User>() {
                @Override
                protected void validateDataImpl(TenantId requestTenantId, User user) {
                    if (StringUtils.isEmpty(user.getEmail())) {
//                        throw new DataValidationException("User email should be specified!");
                        throw new ThingsboardException("User email should be specified!", ThingsboardErrorCode.USER_EMAIL_NOT_SPECIFIED);
                    }

                    if (Strings.isNullOrEmpty(user.getFirstName())) {
                        throw new ThingsboardException("User name should be specified!", ThingsboardErrorCode.USER_NAME_NOT_SPECIFIED);
                    }

                    try {
                        validateEmail(user.getEmail());
                    } catch (Exception e) {
                        throw new ThingsboardException("Invalid email address format '" + user.getEmail() + "'!", ThingsboardErrorCode.USER_EMAIL_FORMAT_ERROR);
                    }

                    Authority authority = user.getAuthority();
                    if (authority == null) {
                        throw new DataValidationException("User authority isn't defined!");
                    }
                    TenantId tenantId = user.getTenantId();
                    if (tenantId == null) {
                        tenantId = new TenantId(ModelConstants.NULL_UUID);
                        user.setTenantId(tenantId);
                    }
                    CustomerId customerId = user.getCustomerId();
                    if (customerId == null) {
                        customerId = new CustomerId(ModelConstants.NULL_UUID);
                        user.setCustomerId(customerId);
                    }

                    switch (authority) {
                        case SYS_ADMIN:
                            if (!tenantId.getId().equals(ModelConstants.NULL_UUID)
                                    || !customerId.getId().equals(ModelConstants.NULL_UUID)) {
                                throw new DataValidationException("System administrator can't be assigned neither to tenant nor to customer!");
                            }
                            break;
                        case TENANT_ADMIN:
                            if (tenantId.getId().equals(ModelConstants.NULL_UUID)) {
                                throw new DataValidationException("Tenant administrator should be assigned to tenant!");
                            } else if (!customerId.getId().equals(ModelConstants.NULL_UUID)) {
                                throw new DataValidationException("Tenant administrator can't be assigned to customer!");
                            }
                            break;
                        case CUSTOMER_USER:
                            if (tenantId.getId().equals(ModelConstants.NULL_UUID)
                                    || customerId.getId().equals(ModelConstants.NULL_UUID)) {
                                throw new DataValidationException("Customer user should be assigned to customer!");
                            }
                            break;
                        default:
                            break;
                    }

                    User existentUserWithFirstName = findUserByFirstName(tenantId, user.getFirstName());
                    if (existentUserWithFirstName != null && !isSameData(existentUserWithFirstName, user)) {
                        throw new ThingsboardException("User with firstName '" + user.getFirstName() + "' "
                                + " already present in database!", ThingsboardErrorCode.USER_NAME_ALREADY_PRESENT);
                    }

                    User existentUserWithEmail = findUserByEmail(tenantId, user.getEmail());

                    if (existentUserWithEmail != null && !isSameData(existentUserWithEmail, user)) {
                        throw new ThingsboardException("User with email '" + user.getEmail() + "' "
                                + " already present in database!", ThingsboardErrorCode.USER_EMAIL_ALREADY_PRESENT);

//                        throw new DataValidationException("User with email '" + user.getEmail() + "' "
//                                + " already present in database!");
                    }
                    if (!tenantId.getId().equals(ModelConstants.NULL_UUID)) {
                        Tenant tenant = tenantDao.findById(tenantId, user.getTenantId().getId());
                        if (tenant == null) {
                            throw new DataValidationException("User is referencing to non-existent tenant!");
                        }
                    }
                    if (!customerId.getId().equals(ModelConstants.NULL_UUID)) {
                        Customer customer = customerDao.findById(tenantId, user.getCustomerId().getId());
                        if (customer == null) {
                            throw new DataValidationException("User is referencing to non-existent customer!");
                        } else if (!customer.getTenantId().getId().equals(tenantId.getId())) {
                            throw new DataValidationException("User can't be assigned to customer from different tenant!");
                        }
                    }
                }
            };

    private DataValidator<UserCredentials> userCredentialsValidator =
            new DataValidator<UserCredentials>() {

                @Override
                protected void validateCreate(TenantId tenantId, UserCredentials userCredentials) {
                    throw new IncorrectParameterException("Creation of new user credentials is prohibited.");
                }

                @Override
                protected void validateDataImpl(TenantId tenantId, UserCredentials userCredentials) {
                    if (userCredentials.getUserId() == null) {
                        throw new DataValidationException("User credentials should be assigned to user!");
                    }
                    if (userCredentials.isEnabled()) {
                        if (StringUtils.isEmpty(userCredentials.getPassword())) {
                            throw new DataValidationException("Enabled user credentials should have password!");
                        }
                        if (StringUtils.isNotEmpty(userCredentials.getActivateToken())) {
                            throw new DataValidationException("Enabled user credentials can't have activate token!");
                        }
                    }
                    UserCredentials existingUserCredentialsEntity = userCredentialsDao.findById(tenantId, userCredentials.getId().getId());
                    if (existingUserCredentialsEntity == null) {
                        throw new DataValidationException("Unable to update non-existent user credentials!");
                    }
                    User user = findUserById(tenantId, userCredentials.getUserId());
                    if (user == null) {
                        throw new DataValidationException("Can't assign user credentials to non-existent user!");
                    }
                }
            };

    private PaginatedRemover<TenantId, User> tenantAdminsRemover = new PaginatedRemover<TenantId, User>() {
        @Override
        protected List<User> findEntities(TenantId tenantId, TenantId id, TextPageLink pageLink) {
            return userDao.findTenantAdmins(id.getId(), pageLink);
        }

        @Override
        protected void removeEntity(TenantId tenantId, User entity) {
            deleteUser(tenantId, new UserId(entity.getUuidId()));
        }
    };

    private PaginatedRemover<CustomerId, User> customerUsersRemover = new PaginatedRemover<CustomerId, User>() {
        @Override
        protected List<User> findEntities(TenantId tenantId, CustomerId id, TextPageLink pageLink) {
            return userDao.findCustomerUsers(tenantId.getId(), id.getId(), pageLink);

        }

        @Override
        protected void removeEntity(TenantId tenantId, User entity) {
            deleteUser(tenantId, new UserId(entity.getUuidId()));
        }
    };

}
