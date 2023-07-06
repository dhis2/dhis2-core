/*
 * Copyright (c) 2004-2022, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.hisp.dhis.user;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.time.ZoneId.systemDefault;
import static java.time.ZonedDateTime.now;
import static org.hisp.dhis.common.CodeGenerator.isValidUid;
import static org.hisp.dhis.common.IdentifiableObjectUtils.getUids;
import static org.hisp.dhis.system.util.ValidationUtils.usernameIsValid;
import static org.hisp.dhis.system.util.ValidationUtils.uuidIsValid;

import com.google.common.collect.Lists;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.cache.Cache;
import org.hisp.dhis.cache.CacheProvider;
import org.hisp.dhis.common.AuditLogUtil;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.UserOrgUnitType;
import org.hisp.dhis.commons.filter.FilterUtils;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.hibernate.exception.UpdateAccessDeniedException;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.security.PasswordManager;
import org.hisp.dhis.security.SecurityService;
import org.hisp.dhis.security.TwoFactoryAuthenticationUtils;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.system.filter.UserRoleCanIssueFilter;
import org.hisp.dhis.util.DateUtils;
import org.hisp.dhis.util.ObjectUtils;
import org.jboss.aerogear.security.otp.api.Base32;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Chau Thu Tran
 */
@Slf4j
@Lazy
@Service("org.hisp.dhis.user.UserService")
public class DefaultUserService implements UserService {
  private final UserStore userStore;

  private final UserGroupService userGroupService;

  private final UserRoleStore userRoleStore;

  private final CurrentUserService currentUserService;

  private final SystemSettingManager systemSettingManager;

  private final PasswordManager passwordManager;

  private final SessionRegistry sessionRegistry;

  private final SecurityService securityService;

  private final Cache<String> userDisplayNameCache;

  private final AclService aclService;

  public DefaultUserService(
      UserStore userStore,
      UserGroupService userGroupService,
      UserRoleStore userRoleStore,
      CurrentUserService currentUserService,
      SystemSettingManager systemSettingManager,
      CacheProvider cacheProvider,
      @Lazy PasswordManager passwordManager,
      @Lazy SessionRegistry sessionRegistry,
      @Lazy SecurityService securityService,
      AclService aclService) {
    checkNotNull(userStore);
    checkNotNull(userGroupService);
    checkNotNull(userRoleStore);
    checkNotNull(systemSettingManager);
    checkNotNull(passwordManager);
    checkNotNull(sessionRegistry);
    checkNotNull(securityService);
    checkNotNull(aclService);

    this.userStore = userStore;
    this.userGroupService = userGroupService;
    this.userRoleStore = userRoleStore;
    this.currentUserService = currentUserService;
    this.systemSettingManager = systemSettingManager;
    this.passwordManager = passwordManager;
    this.sessionRegistry = sessionRegistry;
    this.securityService = securityService;
    this.userDisplayNameCache = cacheProvider.createUserDisplayNameCache();
    this.aclService = aclService;
  }

  @Override
  @Transactional
  public long addUser(User user) {
    String currentUsername = currentUserService.getCurrentUsername();
    AuditLogUtil.infoWrapper(log, currentUsername, user, AuditLogUtil.ACTION_CREATE);

    userStore.save(user);

    return user.getId();
  }

  @Override
  @Transactional
  public void updateUser(User user) {
    userStore.update(user);

    AuditLogUtil.infoWrapper(
        log, currentUserService.getCurrentUsername(), user, AuditLogUtil.ACTION_UPDATE);
  }

  @Override
  @Transactional
  public void deleteUser(User user) {
    AuditLogUtil.infoWrapper(
        log, currentUserService.getCurrentUsername(), user, AuditLogUtil.ACTION_DELETE);

    userStore.delete(user);
  }

  @Override
  @Transactional(readOnly = true)
  public List<User> getAllUsers() {
    return userStore.getAll();
  }

  @Override
  @Transactional(readOnly = true)
  public User getUser(long userId) {
    return userStore.get(userId);
  }

  @Override
  @Transactional(readOnly = true)
  public User getUser(String uid) {
    return userStore.getByUidNoAcl(uid);
  }

  @Override
  @Transactional(readOnly = true)
  public User getUserByUuid(UUID uuid) {
    return userStore.getUserByUuid(uuid);
  }

  @Override
  @Transactional(readOnly = true)
  public User getUserByUsername(String username) {
    return userStore.getUserByUsername(username, false);
  }

  @Override
  @Transactional(readOnly = true)
  public User getUserByUsernameIgnoreCase(String username) {
    return userStore.getUserByUsername(username, true);
  }

  @Override
  @Transactional(readOnly = true)
  public User getUserByIdentifier(String id) {
    User user = null;

    if (isValidUid(id) && (user = getUser(id)) != null) {
      return user;
    }

    if (uuidIsValid(id) && (user = getUserByUuid(UUID.fromString(id))) != null) {
      return user;
    }

    if (usernameIsValid(id, false) && (user = getUserByUsername(id)) != null) {
      return user;
    }

    return user;
  }

  @Override
  @Transactional(readOnly = true)
  public List<User> getUsers(@Nonnull Collection<String> uids) {
    return userStore.getByUid(uids);
  }

  @Override
  @Transactional(readOnly = true)
  public List<User> getAllUsersBetweenByName(String name, int first, int max) {
    UserQueryParams params = new UserQueryParams();
    params.setQuery(name);
    params.setFirst(first);
    params.setMax(max);

    return userStore.getUsers(params);
  }

  @Override
  @Transactional(readOnly = true)
  public List<User> getUsers(UserQueryParams params) {
    return getUsers(params, null);
  }

  @Override
  @Transactional(readOnly = true)
  public List<User> getUsers(UserQueryParams params, @Nullable List<String> orders) {
    handleUserQueryParams(params);

    if (!validateUserQueryParams(params)) {
      return Lists.newArrayList();
    }

    return userStore.getUsers(params, orders);
  }

  @Override
  @Transactional(readOnly = true)
  public int getUserCount(UserQueryParams params) {
    handleUserQueryParams(params);

    if (!validateUserQueryParams(params)) {
      return 0;
    }

    return userStore.getUserCount(params);
  }

  @Override
  @Transactional(readOnly = true)
  public int getUserCount() {
    return userStore.getUserCount();
  }

  private void handleUserQueryParams(UserQueryParams params) {
    boolean canSeeOwnRoles =
        params.isCanSeeOwnRoles()
            || systemSettingManager.getBoolSetting(SettingKey.CAN_GRANT_OWN_USER_ROLES);
    params.setDisjointRoles(!canSeeOwnRoles);

    if (!params.hasUser()) {
      params.setUser(currentUserService.getCurrentUser());
    }

    if (params.hasUser() && params.getUser().isSuper()) {
      params.setCanManage(false);
      params.setAuthSubset(false);
      params.setDisjointRoles(false);
    }

    if (params.getInactiveMonths() != null) {
      Calendar cal = PeriodType.createCalendarInstance();
      cal.add(Calendar.MONTH, (params.getInactiveMonths() * -1));
      params.setInactiveSince(cal.getTime());
    }

    if (params.hasUser()) {
      UserOrgUnitType orgUnitBoundary = params.getOrgUnitBoundary();
      if (params.isUserOrgUnits() || orgUnitBoundary == UserOrgUnitType.DATA_CAPTURE) {
        params.setOrganisationUnits(params.getUser().getOrganisationUnits());
        params.setOrgUnitBoundary(UserOrgUnitType.DATA_CAPTURE);
      } else if (orgUnitBoundary == UserOrgUnitType.DATA_OUTPUT) {
        params.setOrganisationUnits(params.getUser().getDataViewOrganisationUnits());
      } else if (orgUnitBoundary == UserOrgUnitType.TEI_SEARCH) {
        params.setOrganisationUnits(params.getUser().getTeiSearchOrganisationUnits());
      }
    }
  }

  private boolean validateUserQueryParams(UserQueryParams params) {
    if (params.isCanManage()
        && (params.getUser() == null || !params.getUser().hasManagedGroups())) {
      log.warn("Cannot get managed users as user does not have any managed groups");
      return false;
    }

    if (params.isAuthSubset() && (params.getUser() == null || !params.getUser().hasAuthorities())) {
      log.warn("Cannot get users with authority subset as user does not have any authorities");
      return false;
    }

    if (params.isDisjointRoles()
        && (params.getUser() == null || !params.getUser().hasUserRoles())) {
      log.warn("Cannot get users with disjoint roles as user does not have any user roles");
      return false;
    }

    return true;
  }

  @Override
  @Transactional(readOnly = true)
  public List<User> getUsersByPhoneNumber(String phoneNumber) {
    UserQueryParams params = new UserQueryParams();
    params.setPhoneNumber(phoneNumber);
    return getUsers(params);
  }

  @Override
  @Transactional(readOnly = true)
  public boolean isLastSuperUser(User user) {
    if (!user.isSuper()) {
      return false; // Cannot be last if not superuser
    }

    Collection<User> allUsers = userStore.getAll();

    for (User u : allUsers) {
      if (u.isSuper() && !u.equals(user)) {
        return false;
      }
    }

    return true;
  }

  @Override
  @Transactional(readOnly = true)
  public boolean canAddOrUpdateUser(Collection<String> userGroups) {
    return canAddOrUpdateUser(userGroups, currentUserService.getCurrentUser());
  }

  @Override
  @Transactional(readOnly = true)
  public boolean canAddOrUpdateUser(Collection<String> userGroups, User currentUser) {
    if (currentUser == null) {
      return false;
    }

    boolean canAdd = currentUser.isAuthorized(UserGroup.AUTH_USER_ADD);

    if (canAdd) {
      return true;
    }

    boolean canAddInGroup = currentUser.isAuthorized(UserGroup.AUTH_USER_ADD_IN_GROUP);

    if (!canAddInGroup) {
      return false;
    }

    boolean canManageAnyGroup = false;

    for (String uid : userGroups) {
      UserGroup userGroup = userGroupService.getUserGroup(uid);

      if (currentUser.canManage(userGroup)) {
        canManageAnyGroup = true;
        break;
      }
    }

    return canManageAnyGroup;
  }

  // -------------------------------------------------------------------------
  // UserRole
  // -------------------------------------------------------------------------

  @Override
  @Transactional
  public long addUserRole(UserRole userRole) {
    userRoleStore.save(userRole);
    return userRole.getId();
  }

  @Override
  @Transactional
  public void updateUserRole(UserRole userRole) {
    userRoleStore.update(userRole);
  }

  @Override
  @Transactional
  public void deleteUserRole(UserRole userRole) {
    userRoleStore.delete(userRole);
  }

  @Override
  @Transactional(readOnly = true)
  public List<UserRole> getAllUserRoles() {
    return userRoleStore.getAll();
  }

  @Override
  @Transactional(readOnly = true)
  public UserRole getUserRole(long id) {
    return userRoleStore.get(id);
  }

  @Override
  @Transactional(readOnly = true)
  public UserRole getUserRole(String uid) {
    return userRoleStore.getByUid(uid);
  }

  @Override
  @Transactional(readOnly = true)
  public UserRole getUserRoleByName(String name) {
    return userRoleStore.getByName(name);
  }

  @Override
  @Transactional(readOnly = true)
  public List<UserRole> getUserRolesByUid(@Nonnull Collection<String> uids) {
    return userRoleStore.getByUid(uids);
  }

  @Override
  @Transactional(readOnly = true)
  public List<UserRole> getUserRolesBetween(int first, int max) {
    return userRoleStore.getAllOrderedName(first, max);
  }

  @Override
  @Transactional(readOnly = true)
  public List<UserRole> getUserRolesBetweenByName(String name, int first, int max) {
    return userRoleStore.getAllLikeName(name, first, max);
  }

  @Override
  @Transactional(readOnly = true)
  public int countDataSetUserRoles(DataSet dataSet) {
    return userRoleStore.countDataSetUserRoles(dataSet);
  }

  @Override
  @Transactional(readOnly = true)
  public void canIssueFilter(Collection<UserRole> userRoles) {
    User user = currentUserService.getCurrentUser();

    boolean canGrantOwnUserRoles =
        systemSettingManager.getBoolSetting(SettingKey.CAN_GRANT_OWN_USER_ROLES);

    FilterUtils.filter(userRoles, new UserRoleCanIssueFilter(user, canGrantOwnUserRoles));
  }

  @Override
  @Transactional(readOnly = true)
  public List<User> getUsersByUsernames(Collection<String> usernames) {
    return userStore.getUserByUsernames(usernames);
  }

  @Override
  @Transactional
  public void encodeAndSetPassword(User user, String rawPassword) {
    if (StringUtils.isEmpty(rawPassword) && !user.isExternalAuth()) {
      return; // Leave unchanged if internal authentication and no
      // password supplied
    }

    if (user.isExternalAuth()) {
      user.setPassword(UserService.PW_NO_INTERNAL_LOGIN);

      return; // Set unusable, not-encoded password if external
      // authentication
    }

    boolean isNewPassword =
        StringUtils.isBlank(user.getPassword())
            || !passwordManager.matches(rawPassword, user.getPassword());

    if (isNewPassword) {
      user.setPasswordLastUpdated(new Date());
    }

    // Encode and set password
    Matcher matcher = UserService.BCRYPT_PATTERN.matcher(rawPassword);
    if (matcher.matches()) {
      throw new IllegalArgumentException("Raw password look like BCrypt: " + rawPassword);
    }

    String encode = passwordManager.encode(rawPassword);
    user.setPassword(encode);
    user.getPreviousPasswords().add(encode);
  }

  @Override
  @Transactional(readOnly = true)
  public User getUserByIdToken(String token) {
    return userStore.getUserByIdToken(token);
  }

  @Override
  @Transactional(readOnly = true)
  public User getUserWithEagerFetchAuthorities(String username) {
    User user = userStore.getUserByUsername(username, false);

    if (user != null) {
      user.getAllAuthorities();
    }

    return user;
  }

  @Override
  @Transactional(readOnly = true)
  @CheckForNull
  public User getUserByOpenId(@Nonnull String openId) {
    User user = userStore.getUserByOpenId(openId);

    if (user != null) {
      user.getAllAuthorities();
    }

    return user;
  }

  @Override
  @Transactional(readOnly = true)
  public User getUserByLdapId(String ldapId) {
    return userStore.getUserByLdapId(ldapId);
  }

  @Override
  @Transactional
  public void setLastLogin(String username) {
    User user = getUserByUsername(username);

    if (user != null) {
      user.setLastLogin(new Date());
      updateUser(user);
    }
  }

  @Override
  @Transactional(readOnly = true)
  public int getActiveUsersCount(int days) {
    Calendar cal = PeriodType.createCalendarInstance();
    cal.add(Calendar.DAY_OF_YEAR, (days * -1));

    return getActiveUsersCount(cal.getTime());
  }

  @Override
  @Transactional(readOnly = true)
  public int getActiveUsersCount(Date since) {
    UserQueryParams params = new UserQueryParams();
    params.setLastLogin(since);

    return getUserCount(params);
  }

  @Override
  @Transactional(readOnly = true)
  public boolean userNonExpired(User user) {
    int credentialsExpires = systemSettingManager.credentialsExpires();

    if (credentialsExpires == 0) {
      return true;
    }

    if (user == null || user.getPasswordLastUpdated() == null) {
      return true;
    }

    int months = DateUtils.monthsBetween(user.getPasswordLastUpdated(), new Date());

    return months < credentialsExpires;
  }

  @Override
  public boolean isAccountExpired(User user) {
    return !user.isAccountNonExpired();
  }

  @Override
  @Transactional(readOnly = true)
  public List<ErrorReport> validateUser(User user, User currentUser) {
    List<ErrorReport> errors = new ArrayList<>();

    if (currentUser == null || user == null) {
      return errors;
    }

    // Validate user role

    boolean canGrantOwnUserRoles =
        systemSettingManager.getBoolSetting(SettingKey.CAN_GRANT_OWN_USER_ROLES);

    Set<UserRole> userRoles = user.getUserRoles();

    if (userRoles != null) {
      List<UserRole> roles =
          userRoleStore.getByUid(
              userRoles.stream().map(IdentifiableObject::getUid).collect(Collectors.toList()));

      roles.forEach(
          ur -> {
            if (ur == null) {
              errors.add(new ErrorReport(UserRole.class, ErrorCode.E3032, user.getUsername()));
            } else if (!currentUser.canIssueUserRole(ur, canGrantOwnUserRoles)) {
              errors.add(
                  new ErrorReport(
                      UserRole.class, ErrorCode.E3003, currentUser.getUsername(), ur.getName()));
            }
          });
    }

    // Validate user group
    boolean canAdd = currentUser.isAuthorized(UserGroup.AUTH_USER_ADD);

    if (canAdd) {
      return errors;
    }

    boolean canAddInGroup = currentUser.isAuthorized(UserGroup.AUTH_USER_ADD_IN_GROUP);

    if (!canAddInGroup) {
      errors.add(new ErrorReport(UserGroup.class, ErrorCode.E3004, currentUser));
      return errors;
    }

    user.getGroups()
        .forEach(
            ug -> {
              if (!(currentUser.canManage(ug)
                  || userGroupService.canAddOrRemoveMember(ug.getUid()))) {
                errors.add(new ErrorReport(UserGroup.class, ErrorCode.E3005, currentUser, ug));
              }
            });

    return errors;
  }

  @Override
  public List<UserAccountExpiryInfo> getExpiringUserAccounts(int inDays) {
    return userStore.getExpiringUserAccounts(inDays);
  }

  @Transactional
  @Override
  public void resetTwoFactor(User user) {
    user.setSecret(null);
    updateUser(user);
  }

  @Transactional
  @Override
  public void enableTwoFa(User user, String code) {
    if (user.getSecret() == null) {
      throw new IllegalStateException(
          "User has not asked for a QR code yet, call the /qr endpoint first");
    }

    if (!UserService.hasTwoFactorSecretForApproval(user)) {
      throw new IllegalStateException(
          "QR already approved, you must call /disable and then call /qr before you can enable");
    }

    if (!TwoFactoryAuthenticationUtils.verify(code, user.getSecret())) {
      throw new IllegalStateException("Invalid code");
    }

    approveTwoFactorSecret(user);
  }

  @Transactional
  @Override
  public void disableTwoFa(User user, String code) {
    if (user.getSecret() == null) {
      throw new IllegalStateException("Two factor is not enabled, enable first");
    }

    if (!TwoFactoryAuthenticationUtils.verify(code, user.getSecret())) {
      throw new IllegalStateException("Invalid code");
    }

    resetTwoFactor(user);
  }

  @Override
  @Transactional
  public void privilegedTwoFactorDisable(
      User currentUser, String userUid, Consumer<ErrorReport> errors) {
    User user = getUser(userUid);
    if (user == null) {
      throw new IllegalArgumentException("User not found");
    }

    if (currentUser.getUid().equals(user.getUid())
        || !canCurrentUserCanModify(currentUser, user, errors)) {
      throw new UpdateAccessDeniedException(ErrorCode.E3021.getMessage());
    }

    resetTwoFactor(user);
  }

  @Override
  public void expireActiveSessions(User user) {
    List<SessionInformation> sessions = sessionRegistry.getAllSessions(user, false);

    sessions.forEach(SessionInformation::expireNow);
  }

  @Override
  @Transactional
  public int disableUsersInactiveSince(Date inactiveSince) {
    if (ZonedDateTime.ofInstant(inactiveSince.toInstant(), systemDefault())
        .plusMonths(1)
        .isAfter(now())) {
      // we never disable users that have been active during last month
      return 0;
    }
    return userStore.disableUsersInactiveSince(inactiveSince);
  }

  @Override
  @Transactional(readOnly = true)
  public Map<String, Optional<Locale>> findNotifiableUsersWithLastLoginBetween(Date from, Date to) {
    return userStore.findNotifiableUsersWithLastLoginBetween(from, to);
  }

  @Override
  @Transactional(readOnly = true)
  public Map<String, Optional<Locale>> findNotifiableUsersWithPasswordLastUpdatedBetween(
      Date from, Date to) {
    return userStore.findNotifiableUsersWithPasswordLastUpdatedBetween(from, to);
  }

  @Override
  public String getDisplayName(String userUid) {
    return userDisplayNameCache.get(userUid, c -> userStore.getDisplayName(userUid));
  }

  @Override
  public List<User> getUsersWithAuthority(String authority) {
    return userStore.getHasAuthority(authority);
  }

  @Override
  @Transactional(readOnly = true)
  public CurrentUserDetails createUserDetails(String userUid) throws NotFoundException {
    User user = userStore.getByUid(userUid);
    if (user == null) {
      throw new NotFoundException(User.class, userUid);
    }
    return createUserDetails(user);
  }

  @Override
  @Transactional(readOnly = true)
  public CurrentUserDetails createUserDetails(User user) {
    Objects.requireNonNull(user);

    String username = user.getUsername();
    boolean enabled = !user.isDisabled();
    boolean credentialsNonExpired = userNonExpired(user);
    boolean accountNonLocked = !securityService.isLocked(user.getUsername());
    boolean accountNonExpired = !isAccountExpired(user);

    if (ObjectUtils.anyIsFalse(
        enabled, credentialsNonExpired, accountNonLocked, accountNonExpired)) {
      log.info(
          String.format(
              "Login attempt for disabled/locked user: '%s', enabled: %b, account non-expired: %b, user non-expired: %b, account non-locked: %b",
              username, enabled, accountNonExpired, credentialsNonExpired, accountNonLocked));
    }

    return createUserDetails(user, accountNonLocked, credentialsNonExpired);
  }

  @Override
  public CurrentUserDetailsImpl createUserDetails(
      User user, boolean accountNonLocked, boolean credentialsNonExpired) {
    return CurrentUserDetailsImpl.builder()
        .uid(user.getUid())
        .username(user.getUsername())
        .password(user.getPassword())
        .enabled(user.isEnabled())
        .accountNonExpired(user.isAccountNonExpired())
        .accountNonLocked(accountNonLocked)
        .credentialsNonExpired(credentialsNonExpired)
        .authorities(user.getAuthorities())
        .userSettings(new HashMap<>())
        .userGroupIds(
            user.getUid() == null
                ? Set.of()
                : currentUserService.getCurrentUserGroupsInfo(user.getUid()).getUserGroupUIDs())
        .isSuper(user.isSuper())
        .build();
  }

  @Override
  @Transactional(readOnly = true)
  public boolean canCurrentUserCanModify(
      User currentUser, User userToModify, Consumer<ErrorReport> errors) {
    if (!aclService.canUpdate(currentUser, userToModify)) {
      errors.accept(
          new ErrorReport(
              UserRole.class, ErrorCode.E3001, currentUser.getUsername(), userToModify.getName()));
      return false;
    }

    if (!canAddOrUpdateUser(getUids(userToModify.getGroups()), currentUser)
        || !currentUser.canModifyUser(userToModify)) {
      errors.accept(new ErrorReport(UserRole.class, ErrorCode.E3020, userToModify.getName()));
      return false;
    }

    return true;
  }

  @Override
  @Transactional
  public void generateTwoFactorOtpSecretForApproval(User user) {
    String newSecret = TWO_FACTOR_CODE_APPROVAL_PREFIX + Base32.random();
    user.setSecret(newSecret);
    updateUser(user);
  }

  @Override
  @Transactional
  public void approveTwoFactorSecret(User user) {
    if (user.getSecret() != null && UserService.hasTwoFactorSecretForApproval(user)) {
      user.setSecret(user.getSecret().replace(TWO_FACTOR_CODE_APPROVAL_PREFIX, ""));
      updateUser(user);
    }
  }

  @Override
  public boolean hasTwoFactorRoleRestriction(User user) {
    return user.hasAnyRestrictions(Set.of(TWO_FACTOR_AUTH_REQUIRED_RESTRICTION_NAME));
  }

  @Override
  @Transactional
  public void validateTwoFactorUpdate(boolean before, boolean after, User userToModify) {
    if (before == after) {
      return;
    }

    if (!before) {
      throw new UpdateAccessDeniedException(
          "You can not enable 2FA with this API endpoint, only disable.");
    }

    CurrentUserDetails currentUserDetails = CurrentUserUtil.getCurrentUserDetails();
    if (currentUserDetails == null) {
      throw new UpdateAccessDeniedException("No current user in session, can not update user.");
    }

    // Current user can not update their own 2FA settings, must use
    // /2fa/enable or disable API, even if they are admin.
    if (currentUserDetails.getUid().equals(userToModify.getUid())) {
      throw new UpdateAccessDeniedException(ErrorCode.E3030.getMessage());
    }

    // If current user has access to manage this user, they can disable 2FA.
    User currentUser = getUser(currentUserDetails.getUid());
    if (!aclService.canUpdate(currentUser, userToModify)) {
      throw new UpdateAccessDeniedException(
          String.format(
              "User `%s` is not allowed to update object `%s`.",
              currentUser.getUsername(), userToModify));
    }

    if (!canAddOrUpdateUser(getUids(userToModify.getGroups()), currentUser)
        || !currentUser.canModifyUser(userToModify)) {
      throw new UpdateAccessDeniedException(
          "You don't have the proper permissions to update this user.");
    }
  }

  @Override
  @Nonnull
  @Transactional(readOnly = true)
  public List<User> getLinkedUserAccounts(@Nonnull User actingUser) {
    return userStore.getLinkedUserAccounts(actingUser);
  }

  @Override
  @Transactional
  public void setActiveLinkedAccounts(@Nonnull User actingUser, @Nonnull String activeUsername) {
    Instant oneHourAgo = Instant.now().minus(1, ChronoUnit.HOURS);
    Instant oneHourInTheFuture = Instant.now().plus(1, ChronoUnit.HOURS);

    List<User> linkedUserAccounts = getLinkedUserAccounts(actingUser);
    for (User user : linkedUserAccounts) {
      user.setLastLogin(
          user.getUsername().equals(activeUsername)
              ? Date.from(oneHourInTheFuture)
              : Date.from(oneHourAgo));

      updateUser(user);
    }
  }
}
