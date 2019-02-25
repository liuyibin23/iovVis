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
package org.thingsboard.server.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.thingsboard.rule.engine.api.MailService;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.page.TextPageData;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.common.data.security.UserCredentials;
import org.thingsboard.server.service.security.auth.jwt.RefreshTokenRepository;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.model.UserPrincipal;
import org.thingsboard.server.service.security.model.token.JwtToken;
import org.thingsboard.server.service.security.model.token.JwtTokenFactory;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api")
public class UserController extends BaseController {

    public static final String USER_ID = "userId";
    public static final String YOU_DON_T_HAVE_PERMISSION_TO_PERFORM_THIS_OPERATION = "You don't have permission to perform this operation!";
    public static final String ACTIVATE_URL_PATTERN = "%s/api/noauth/activate?activateToken=%s";
    public static final String ACTIVATE_PAGE_PATTERN = "%s/setPassword?activateToken=%s";//前端用户激活页面

    @Value("${security.user_token_access_enabled}")
    @Getter
    private boolean userTokenAccessEnabled;

    @Autowired
    private MailService mailService;

    @Autowired
    private JwtTokenFactory tokenFactory;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

	/**
	* @Description: 1.2.8.13 以登录用户权限查询所有用户
	* @Author: ShenJi
	* @Date: 2019/2/1
	* @Param: []
	* @return: java.util.List<org.thingsboard.server.common.data.User>
	*/
	@PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
	@RequestMapping(value = "/currentUser/users", method = RequestMethod.GET)
	@ResponseBody
	public TextPageData<User> getUsers(@RequestParam int limit,
                               @RequestParam(required = false) String textSearch,
                               @RequestParam(required = false) String idOffset,
                               @RequestParam(required = false) String textOffset) throws ThingsboardException {
        TextPageLink pageLink = createPageLink(limit, textSearch, idOffset, textOffset);
		TextPageData<User> retobj;
		switch (getCurrentUser().getAuthority()){
			case SYS_ADMIN:
				retobj = userService.findUsers(pageLink);
				break;
			case TENANT_ADMIN:
				retobj = userService.findTenantUsers(getCurrentUser().getTenantId(),pageLink);
				break;
			case CUSTOMER_USER:
				retobj = userService.findUsersByTenantIdAndCustomerId(getTenantId(),getCurrentUser().getCustomerId(),pageLink);
				break;
			default:
				throw new ThingsboardException(ThingsboardErrorCode.AUTHENTICATION);
		}
		if (retobj.getData().size() > 0){
			List <User> tmp = new ArrayList<>();
			retobj.getData().stream().forEach(user -> {
				UserCredentials userCredentials = userService.findUserCredentialsByUserId(null, user.getId());
				user.setActivation(userCredentials.isEnabled());
				tmp.add(user);
			});
			return new TextPageData<User>(tmp,retobj.getNextPageLink(),retobj.hasNext());
		} else {
			return retobj;
		}


	}
    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/user/{userId}", method = RequestMethod.GET)
    @ResponseBody
    public User getUserById(@PathVariable(USER_ID) String strUserId) throws ThingsboardException {
        checkParameter(USER_ID, strUserId);
        try {
            UserId userId = new UserId(toUUID(strUserId));
            SecurityUser authUser = getCurrentUser();
			User retUser;
            if (authUser.getAuthority() == Authority.CUSTOMER_USER && !authUser.getId().equals(userId)) {
                throw new ThingsboardException(YOU_DON_T_HAVE_PERMISSION_TO_PERFORM_THIS_OPERATION,
                        ThingsboardErrorCode.PERMISSION_DENIED);
            }
			retUser = checkUserId(userId);

            switch (retUser.getAuthority()){
				case CUSTOMER_USER:
					retUser.setCustomerName(customerService.findCustomerById(retUser.getTenantId(),retUser.getCustomerId()).getName());
				case TENANT_ADMIN:
					retUser.setTenantName(tenantService.findTenantById(retUser.getTenantId()).getName());
				case SYS_ADMIN:
					default:
						break;
			}
			UserCredentials userCredentials = userService.findUserCredentialsByUserId(null, retUser.getId());
			retUser.setActivation(userCredentials.isEnabled());
            return retUser;
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @RequestMapping(value = "/user/tokenAccessEnabled", method = RequestMethod.GET)
    @ResponseBody
    public boolean isUserTokenAccessEnabled() {
        return userTokenAccessEnabled;
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @RequestMapping(value = "/user/{userId}/token", method = RequestMethod.GET)
    @ResponseBody
    public JsonNode getUserToken(@PathVariable(USER_ID) String strUserId) throws ThingsboardException {
        checkParameter(USER_ID, strUserId);
        try {
            UserId userId = new UserId(toUUID(strUserId));
            SecurityUser authUser = getCurrentUser();
            User user = userService.findUserById(authUser.getTenantId(), userId);
            if (!userTokenAccessEnabled || (authUser.getAuthority() == Authority.SYS_ADMIN && user.getAuthority() != Authority.TENANT_ADMIN)
                    || (authUser.getAuthority() == Authority.TENANT_ADMIN && !authUser.getTenantId().equals(user.getTenantId()))) {
                throw new ThingsboardException(YOU_DON_T_HAVE_PERMISSION_TO_PERFORM_THIS_OPERATION,
                        ThingsboardErrorCode.PERMISSION_DENIED);
            }
            UserPrincipal principal = new UserPrincipal(UserPrincipal.Type.USER_NAME, user.getEmail());
            UserCredentials credentials = userService.findUserCredentialsByUserId(authUser.getTenantId(), userId);
            SecurityUser securityUser = new SecurityUser(user, credentials.isEnabled(), principal);
            JwtToken accessToken = tokenFactory.createAccessJwtToken(securityUser);
            JwtToken refreshToken = refreshTokenRepository.requestRefreshToken(securityUser);
            ObjectMapper objectMapper = new ObjectMapper();
            ObjectNode tokenObject = objectMapper.createObjectNode();
            tokenObject.put("token", accessToken.getToken());
            tokenObject.put("refreshToken", refreshToken.getToken());
            return tokenObject;
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN', 'CUSTOMER_USER')")
    @RequestMapping(value = "/user", method = RequestMethod.POST)
    @ResponseBody
    public User saveUser(@RequestBody User user,
                         @RequestParam(required = false, defaultValue = "true") boolean sendActivationMail,
                         HttpServletRequest request) throws ThingsboardException {
        try {
            SecurityUser authUser = getCurrentUser();
            if (authUser.getAuthority() == Authority.CUSTOMER_USER && !authUser.getId().equals(user.getId())) {
                throw new ThingsboardException(YOU_DON_T_HAVE_PERMISSION_TO_PERFORM_THIS_OPERATION,
                        ThingsboardErrorCode.PERMISSION_DENIED);
            }
            boolean sendEmail = user.getId() == null && sendActivationMail;
            if (getCurrentUser().getAuthority() == Authority.TENANT_ADMIN) {
                user.setTenantId(getCurrentUser().getTenantId());
            }
            User savedUser = checkNotNull(userService.saveUser(user));
            if (sendEmail) {
                UserCredentials userCredentials = userService.findUserCredentialsByUserId(authUser.getTenantId(), savedUser.getId());
                String baseUrl = constructBaseUrl(request);
                String activateUrl = String.format(ACTIVATE_URL_PATTERN, baseUrl,
                        userCredentials.getActivateToken());
//                String activateUrl = String.format(ACTIVATE_PAGE_PATTERN, baseUrl,
//                        userCredentials.getActivateToken());
                String email = savedUser.getEmail();
                try {
                    mailService.sendActivationEmail(activateUrl, email);
                } catch (ThingsboardException e) {
                    userService.deleteUser(authUser.getTenantId(), savedUser.getId());
                    throw e;
                }
            }

            logEntityAction(savedUser.getId(), savedUser,
                    savedUser.getCustomerId(),
                    user.getId() == null ? ActionType.ADDED : ActionType.UPDATED, null);

            return savedUser;
        } catch (Exception e) {

            logEntityAction(emptyId(EntityType.USER), user,
                    null, user.getId() == null ? ActionType.ADDED : ActionType.UPDATED, e);

            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @RequestMapping(value = "/user/sendActivationMail", method = RequestMethod.POST)
    @ResponseStatus(value = HttpStatus.OK)
    public void sendActivationEmail(
            @RequestParam(value = "email") String email,
            HttpServletRequest request) throws ThingsboardException {
        try {
            User user = checkNotNull(userService.findUserByEmail(getCurrentUser().getTenantId(), email));
            UserCredentials userCredentials = userService.findUserCredentialsByUserId(getCurrentUser().getTenantId(), user.getId());
            if (!userCredentials.isEnabled()) {
                String baseUrl = constructBaseUrl(request);
                String activateUrl = String.format(ACTIVATE_URL_PATTERN, baseUrl,
                        userCredentials.getActivateToken());
//                String activateUrl = String.format(ACTIVATE_PAGE_PATTERN, baseUrl,
//                        userCredentials.getActivateToken());
                mailService.sendActivationEmail(activateUrl, email);
            } else {
                throw new ThingsboardException("User is already active!", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
            }
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @RequestMapping(value = "/user/{userId}/activationLink", method = RequestMethod.GET, produces = "text/plain")
    @ResponseBody
    public String getActivationLink(
            @PathVariable(USER_ID) String strUserId,
            HttpServletRequest request) throws ThingsboardException {
        checkParameter(USER_ID, strUserId);
        try {
            UserId userId = new UserId(toUUID(strUserId));
            SecurityUser authUser = getCurrentUser();
            if (authUser.getAuthority() == Authority.CUSTOMER_USER && !authUser.getId().equals(userId)) {
                throw new ThingsboardException(YOU_DON_T_HAVE_PERMISSION_TO_PERFORM_THIS_OPERATION,
                        ThingsboardErrorCode.PERMISSION_DENIED);
            }
            User user = checkUserId(userId);
            UserCredentials userCredentials = userService.findUserCredentialsByUserId(getCurrentUser().getTenantId(), user.getId());
            if (!userCredentials.isEnabled()) {
                String baseUrl = constructBaseUrl(request);
                String activateUrl = String.format(ACTIVATE_URL_PATTERN, baseUrl,
                        userCredentials.getActivateToken());
                return activateUrl;
            } else {
                throw new ThingsboardException("User is already active!", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
            }
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN','CUSTOMER_USER')")
    @RequestMapping(value = "/user/{userId}", method = RequestMethod.DELETE)
    @ResponseStatus(value = HttpStatus.OK)
    public void deleteUser(@PathVariable(USER_ID) String strUserId) throws ThingsboardException {
        checkParameter(USER_ID, strUserId);
        try {
            UserId userId = new UserId(toUUID(strUserId));
            User user = checkUserId(userId);
            userService.deleteUser(getCurrentUser().getTenantId(), userId);

            logEntityAction(userId, user,
                    user.getCustomerId(),
                    ActionType.DELETED, null, strUserId);

        } catch (Exception e) {
            logEntityAction(emptyId(EntityType.USER),
                    null,
                    null,
                    ActionType.DELETED, e, strUserId);
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAuthority('SYS_ADMIN')")
    @RequestMapping(value = "/admin/users", params = {"limit"}, method = RequestMethod.GET)
    @ResponseBody
    public TextPageData<User> getAllusers(
            @RequestParam int limit,
            @RequestParam(required = false) String textSearch,
            @RequestParam(required = false) String idOffset,
            @RequestParam(required = false) String textOffset) throws ThingsboardException {
        try {

            TextPageLink pageLink = createPageLink(limit, textSearch, idOffset, textOffset);
            return checkNotNull(userService.findUsers(pageLink));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAuthority('SYS_ADMIN')")
    @RequestMapping(value = "/tenant/{tenantId}/users", params = {"limit"}, method = RequestMethod.GET)
    @ResponseBody
    public TextPageData<User> getTenantAdmins(
            @PathVariable("tenantId") String strTenantId,
            @RequestParam int limit,
            @RequestParam(required = false) String textSearch,
            @RequestParam(required = false) String idOffset,
            @RequestParam(required = false) String textOffset) throws ThingsboardException {
        checkParameter("tenantId", strTenantId);
        try {
            TenantId tenantId = new TenantId(toUUID(strTenantId));
            TextPageLink pageLink = createPageLink(limit, textSearch, idOffset, textOffset);
            return checkNotNull(userService.findTenantAdmins(tenantId, pageLink));
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN','CUSTOMER_USER')")
    @RequestMapping(value = "/customer/{customerId}/users", params = {"limit"}, method = RequestMethod.GET)
    @ResponseBody
    public TextPageData<User> getCustomerUsers(
            @PathVariable("customerId") String strCustomerId,
            @RequestParam int limit,
            @RequestParam(required = false) String textSearch,
            @RequestParam(required = false) String idOffset,
            @RequestParam(required = false) String textOffset) throws ThingsboardException {
        checkParameter("customerId", strCustomerId);
        try {
            CustomerId customerId = new CustomerId(toUUID(strCustomerId));

            TextPageLink pageLink = createPageLink(limit, textSearch, idOffset, textOffset);

            if (getCurrentUser().getAuthority() == Authority.SYS_ADMIN) {
                return checkNotNull(userService.findCustomerUsers(customerId, pageLink));
            } else if (getCurrentUser().getAuthority() == Authority.TENANT_ADMIN) {
                checkCustomerId(customerId);
                TenantId tenantId = getCurrentUser().getTenantId();
                return checkNotNull(userService.findCustomerUsers(tenantId, customerId, pageLink));
            } else if (getCurrentUser().getAuthority() == Authority.CUSTOMER_USER) {
                if (customerId.equals(getCurrentUser().getCustomerId())){
                    return checkNotNull(userService.findCustomerUsers(getCurrentUser().getTenantId(), customerId, pageLink));
                }
            }
            throw new ThingsboardException(ThingsboardErrorCode.INVALID_ARGUMENTS);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

	@PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN','CUSTOMER_USER')")
	@RequestMapping(value = "/currentUser/{customerId}/users", params = {"limit"}, method = RequestMethod.GET)
	@ResponseBody
	public TextPageData<User> getCustomerUsers(
			@PathVariable("customerId") String strCustomerId,
			@RequestParam int limit,
            @RequestParam(required = false) String textSearch,
            @RequestParam(required = false) String idOffset,
            @RequestParam(required = false) String textOffset,
			@RequestParam Boolean isAdmin) throws ThingsboardException {
		checkParameter("customerId", strCustomerId);
		try {
            TextPageLink pageLink = createPageLink(limit, textSearch, idOffset, textOffset);
			CustomerId customerId = new CustomerId(toUUID(strCustomerId));

			if (getCurrentUser().getAuthority() == Authority.SYS_ADMIN) {
                TextPageData<User> retList = isAdmin?(checkAdminUsers(checkNotNull(userService.findCustomerUsers(customerId,pageLink)),pageLink)):checkNotNull(userService.findCustomerUsers(customerId,pageLink));
				if (retList == null)
					throw new ThingsboardException(ThingsboardErrorCode.ITEM_NOT_FOUND);
				return retList;
			} else if (getCurrentUser().getAuthority() == Authority.TENANT_ADMIN) {
				checkCustomerId(customerId);
				TenantId tenantId = getCurrentUser().getTenantId();
                TextPageData<User> retList = isAdmin?(checkAdminUsers(checkNotNull(userService.findCustomerUsers(customerId,pageLink)),pageLink)):checkNotNull(userService.findCustomerUsers(customerId,pageLink));
				if (retList == null)
					throw new ThingsboardException(ThingsboardErrorCode.ITEM_NOT_FOUND);
				return retList;
			} else if (getCurrentUser().getAuthority() == Authority.CUSTOMER_USER) {
				if (customerId.equals(getCurrentUser().getCustomerId())){
					TextPageData<User> retList = isAdmin?(checkAdminUsers(checkNotNull(userService.findCustomerUsers(customerId,pageLink)),pageLink)):checkNotNull(userService.findCustomerUsers(customerId,pageLink));
					if (retList == null)
						throw new ThingsboardException(ThingsboardErrorCode.ITEM_NOT_FOUND);
					return retList;
				}
			}
			throw new ThingsboardException(ThingsboardErrorCode.INVALID_ARGUMENTS);
		} catch (Exception e) {
			throw handleException(e);
		}
	}
	private TextPageData<User> checkAdminUsers(TextPageData<User> userList,TextPageLink pageLink){
    	List<User> retUserList = new ArrayList<>();
    	userList.getData().forEach(user -> {
    		if (user.getAdditionalInfo().get("power") != null){
				if (user.getAdditionalInfo().get("power").asText().equals("admin"))
					retUserList.add(user);
			}
    	});

    	return new TextPageData<>(retUserList,pageLink);
	}

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @RequestMapping(value = "/user/activationUser", method = RequestMethod.DELETE)
    User unAceivationUser(@RequestParam String strUserId) throws ThingsboardException {
        User user = null;
        try {
            UserId userId = new UserId(toUUID(strUserId));
            user = checkUserId(userId);
            UserCredentials userCredentials = userService.findUserCredentialsByUserId(getTenantId(), user.getId());

            userCredentials.setEnabled(false);

            userService.saveUserCredentials(getTenantId(), userCredentials);


        } catch (ThingsboardException e) {
            throw handleException(e);
        }

        return user;
    }

    @PreAuthorize("hasAnyAuthority('SYS_ADMIN', 'TENANT_ADMIN')")
    @RequestMapping(value = "/user/activationUser", method = RequestMethod.POST)
    User aceivationUser(@RequestParam String strUserId) throws ThingsboardException {
        User user = null;
        try {
            UserId userId = new UserId(toUUID(strUserId));
            user = checkUserId(userId);
            UserCredentials userCredentials = userService.findUserCredentialsByUserId(getTenantId(), user.getId());

            userCredentials.setEnabled(true);

            userService.saveUserCredentials(getTenantId(), userCredentials);


        } catch (ThingsboardException e) {
            throw handleException(e);
        }

        return user;
    }

}
