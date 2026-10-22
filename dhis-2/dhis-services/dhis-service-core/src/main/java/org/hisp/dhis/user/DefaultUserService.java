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
import static org.hisp.dhis.security.DefaultSecurityService.LOGIN_MAX_FAILED_ATTEMPTS;
import static org.hisp.dhis.system.util.ValidationUtils.usernameIsValid;
import static org.hisp.dhis.system.util.ValidationUtils.uuidIsValid;

import com.google.common.collect.Lists;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
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
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.MergeMode;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.common.UserOrgUnitType;
import org.hisp.dhis.commons.filter.FilterUtils;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.hibernate.exception.UpdateAccessDeniedException;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
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

  private final SecurityService securityService;

  private final Cache<String> userDisplayNameCache;

  private final AclService aclService;

  private final OrganisationUnitService organisationUnitService;

  private final Cache<Integer> twoFaDisableFailedAttemptCache;

  private final SessionRegistry sessionRegistry;

  public DefaultUserService(
      UserStore userStore,
      UserGroupService userGroupService,
      UserRoleStore userRoleStore,
      CurrentUserService currentUserService,
      SystemSettingManager systemSettingManager,
      CacheProvider cacheProvider,
      @Lazy PasswordManager passwordManager,
      @Lazy SecurityService securityService,
      AclService aclService,
      @Lazy OrganisationUnitService organisationUnitService,
      SessionRegistry sessionRegistry) {
    checkNotNull(userStore);
    checkNotNull(userGroupService);
    checkNotNull(userRoleStore);
    checkNotNull(systemSettingManager);
    checkNotNull(passwordManager);
    checkNotNull(securityService);
    checkNotNull(aclService);
    checkNotNull(organisationUnitService);
    checkNotNull(sessionRegistry);

    this.userStore = userStore;
    this.userGroupService = userGroupService;
    this.userRoleStore = userRoleStore;
    this.currentUserService = currentUserService;
    this.systemSettingManager = systemSettingManager;
    this.passwordManager = passwordManager;
    this.securityService = securityService;
    this.userDisplayNameCache = cacheProvider.createUserDisplayNameCache();
    this.aclService = aclService;
    this.organisationUnitService = organisationUnitService;
    this.twoFaDisableFailedAttemptCache = cacheProvider.createDisable2FAFailedAttemptCache(0);
    this.sessionRegistry = sessionRegistry;
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
    if (currentUser.isAuthorized(UserGroup.AUTH_USER_ADD)) {
      return true;
    }
    if (!currentUser.isAuthorized(UserGroup.AUTH_USER_ADD_IN_GROUP) || userGroups.isEmpty()) {
      return false;
    }
    return userGroupService.getUserGroupsByUid(userGroups).stream()
        .anyMatch(currentUser::canManage);
  }

  private boolean canAddOrUpdateUserInGroups(Collection<UserGroup> groups, User currentUser) {
    return currentUser.isAuthorized(UserGroup.AUTH_USER_ADD)
        || (currentUser.isAuthorized(UserGroup.AUTH_USER_ADD_IN_GROUP)
            && groups.stream().anyMatch(currentUser::canManage));
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
  public void updateUserRoleNoAcl(UserRole userRole) {
    userRoleStore.update(userRole, null);
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
      throw new IllegalArgumentException(
          "Raw password look like BCrypt encoded password, this is most certainly a bug");
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
      user.getAllRestrictions();
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
    if (user == null) {
      return true;
    }

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
  public List<ErrorReport> validateUserCreateOrUpdateAccess(User user, User currentUser) {

    List<ErrorReport> errors = new ArrayList<>();

    if (currentUser == null || user == null || currentUser.isSuper()) {
      return errors;
    }

    User userToChange = userStore.get(user.getId());
    if (userToChange != null && userToChange.isSuper()) {
      errors.add(new ErrorReport(User.class, ErrorCode.E3041, currentUser.getUsername()));
    }

    checkHasAccessToUserRoles(user, currentUser, errors);
    checkHasAccessToUserGroups(user, currentUser, errors);

    checkIsInOrgUnitHierarchy(user, currentUser, errors);

    return errors;
  }

  /**
   * Checks that the organisation units, data view organisation units and search organisation units
   * of the user are within the hierarchy of the current user. They are loaded in one query, as the
   * payload objects lack the path.
   */
  private void checkIsInOrgUnitHierarchy(User user, User currentUser, List<ErrorReport> errors) {
    List<Collection<OrganisationUnit>> collections =
        List.of(
            user.getOrganisationUnits(),
            user.getDataViewOrganisationUnits(),
            user.getTeiSearchOrganisationUnits());
    Set<String> uids = new HashSet<>();
    collections.forEach(units -> uids.addAll(getUids(units)));
    if (uids.isEmpty()) {
      return;
    }
    Map<String, OrganisationUnit> byUid = new HashMap<>();
    for (OrganisationUnit unit : organisationUnitService.getOrganisationUnitsByUid(uids)) {
      byUid.put(unit.getUid(), unit);
    }
    for (Collection<OrganisationUnit> units : collections) {
      List<OrganisationUnit> loaded = new ArrayList<>(units.size());
      for (OrganisationUnit orgUnit : units) {
        OrganisationUnit fetched = byUid.get(orgUnit.getUid());
        if (fetched == null) {
          // Unknown (or not readable): it cannot be shown to be within the hierarchy.
          errors.add(
              new ErrorReport(
                  OrganisationUnit.class,
                  ErrorCode.E7617,
                  orgUnit.getUid(),
                  currentUser.getUsername()));
        } else {
          loaded.add(fetched);
        }
      }
      checkLoadedOrgUnitsInHierarchy(loaded, currentUser, errors);
    }
  }

  private void checkLoadedOrgUnitsInHierarchy(
      Collection<OrganisationUnit> units, User currentUser, List<ErrorReport> errors) {
    Set<String> ancestors =
        currentUser.getOrganisationUnits().stream()
            .map(OrganisationUnit::getUid)
            .collect(Collectors.toSet());
    for (OrganisationUnit unit : units) {
      OrganisationUnit ancestor = unit;
      while (ancestor != null && !ancestors.contains(ancestor.getUid())) {
        ancestor = ancestor.getParent();
      }
      if (ancestor == null) {
        errors.add(
            new ErrorReport(
                OrganisationUnit.class, ErrorCode.E7617, unit.getUid(), currentUser.getUsername()));
      }
    }
  }

  private void checkHasAccessToUserGroups(User user, User currentUser, List<ErrorReport> errors) {

    boolean canAdd = currentUser.isAuthorized(UserGroup.AUTH_USER_ADD);
    if (canAdd) {
      return;
    }

    boolean canAddInGroup = currentUser.isAuthorized(UserGroup.AUTH_USER_ADD_IN_GROUP);
    if (!canAddInGroup) {
      errors.add(new ErrorReport(UserGroup.class, ErrorCode.E3004, currentUser));
      return;
    }

    user.getGroups()
        .forEach(
            ug -> {
              if (!(currentUser.canManage(ug)
                  || userGroupService.canAddOrRemoveMember(ug.getUid()))) {
                errors.add(new ErrorReport(UserGroup.class, ErrorCode.E3005, currentUser, ug));
              }
            });
  }

  private void checkHasAccessToUserRoles(User user, User currentUser, List<ErrorReport> errors) {
    Set<UserRole> userRoles = user.getUserRoles();

    boolean canGrantOwnUserRoles =
        systemSettingManager.getBoolSetting(SettingKey.CAN_GRANT_OWN_USER_ROLES);

    if (userRoles != null) {
      List<UserRole> roles =
          userRoleStore.getByUidNoAcl(
              userRoles.stream().map(BaseIdentifiableObject::getUid).collect(Collectors.toList()));

      roles.forEach(
          ur -> {
            if (ur == null) {
              errors.add(new ErrorReport(UserRole.class, ErrorCode.E3032, user.getUsername()));

            } else if (!currentUser.canIssueUserRole(ur, canGrantOwnUserRoles)) {
              errors.add(
                  new ErrorReport(
                      UserRole.class,
                      ErrorCode.E3003,
                      currentUser.getUsername(),
                      roleLabel(ur, currentUser)));
            }
          });
    }
  }

  @Override
  @Transactional(readOnly = true)
  public List<ErrorReport> validateSharingUpdate(IdentifiableObject object, User currentUser) {
    return object instanceof UserRole
        ? validateUserRoleCreateOrUpdate((UserRole) object, currentUser)
        : List.of();
  }

  @Override
  @Transactional(readOnly = true)
  public List<ErrorReport> validateUserRoleCreateOrUpdate(UserRole role, User currentUser) {
    return validateUserRoleCreateOrUpdate(role, currentUser, MergeMode.REPLACE);
  }

  @Override
  @Transactional(readOnly = true)
  public List<ErrorReport> validateUserRoleCreateOrUpdate(
      UserRole role, User currentUser, MergeMode mergeMode) {
    List<ErrorReport> errors = new ArrayList<>();

    if (currentUser == null || role == null || currentUser.isSuper()) {
      return errors;
    }

    UserRole stored = storedRole(role);
    checkUserRoleAllAuthority(role, stored, currentUser, errors);

    // The resulting role may only carry authorities the current user holds. Own roles are allowed
    // here (canGrantOwnUserRole = true): editing a role you hold does not grant it to anyone.
    if (!currentUser.canIssueUserRole(resultingRole(role, stored), true)) {
      errors.add(
          new ErrorReport(
              UserRole.class, ErrorCode.E3003, currentUser.getUsername(), role.getName()));
    }

    // A merge other than replace keeps the stored authorities next to the submitted ones, so the
    // stored role must be issuable as well.
    if (mergeMode != null
        && !mergeMode.isReplace()
        && stored != null
        && !currentUser.canIssueUserRole(stored, true)) {
      errors.add(
          new ErrorReport(
              UserRole.class,
              ErrorCode.E3003,
              currentUser.getUsername(),
              roleLabel(stored, currentUser)));
    }

    return errors;
  }

  /**
   * An update without authorities ({@code null}) keeps the stored authorities of the role (the
   * metadata merge skips {@code null} collections), so the stored role is the one to check then. A
   * new role without authorities has none.
   */
  private static UserRole resultingRole(UserRole role, UserRole stored) {
    return role.getAuthorities() == null && stored != null ? stored : role;
  }

  /**
   * The stored state of a role being updated, or {@code null} for a new role. Loaded once per check
   * and shared by the rules that need it.
   */
  private UserRole storedRole(UserRole role) {
    if (role.getId() != 0) {
      return userRoleStore.get(role.getId());
    }
    return role.getUid() == null ? null : userRoleStore.getByUidNoAcl(role.getUid());
  }

  @Override
  @Transactional(readOnly = true)
  public List<ErrorReport> validateUserRoleDelete(UserRole role, User currentUser) {
    List<ErrorReport> errors = new ArrayList<>();

    if (currentUser != null && role != null && !currentUser.isSuper()) {
      checkUserRoleAllAuthority(role, storedRole(role), currentUser, errors);
    }

    return errors;
  }

  private static void checkUserRoleAllAuthority(
      UserRole role, UserRole stored, User currentUser, List<ErrorReport> errors) {
    if (role.isSuper()) {
      errors.add(new ErrorReport(UserRole.class, ErrorCode.E3032, currentUser.getUsername()));
    }

    if (stored != null && stored.isSuper()) {
      errors.add(new ErrorReport(UserRole.class, ErrorCode.E3032, currentUser.getUsername()));
    }
  }

  /**
   * The name of a role for an error report, or its UID when the current user cannot read the role:
   * an error must not reveal the name of a role hidden by sharing.
   */
  private String roleLabel(UserRole role, User currentUser) {
    return aclService.canRead(currentUser, role) ? role.getName() : role.getUid();
  }

  private List<ErrorReport> validateCanIssueUserRoles(
      Collection<UserRole> roles, User currentUser) {

    if (roles.isEmpty() || currentUser.isSuper()) {
      return List.of();
    }

    boolean canGrantOwnUserRoles =
        systemSettingManager.getBoolSetting(SettingKey.CAN_GRANT_OWN_USER_ROLES);

    return roles.stream()
        .filter(role -> !currentUser.canIssueUserRole(role, canGrantOwnUserRoles))
        .map(
            role ->
                new ErrorReport(
                    UserRole.class,
                    ErrorCode.E3003,
                    currentUser.getUsername(),
                    roleLabel(role, currentUser)))
        .collect(Collectors.toList());
  }

  private void checkCanAddUserRoles(User user, Collection<UserRole> roles, User currentUser)
      throws ForbiddenException {
    checkCanChangeUser(user, currentUser);

    throwIfAny(validateCanIssueUserRoles(roles, currentUser));
  }

  private void checkCanRemoveUserRoles(User user, User currentUser) throws ForbiddenException {
    checkCanChangeUser(user, currentUser);
  }

  @Override
  @Transactional(readOnly = true)
  @SuppressWarnings("unchecked")
  public void checkCanChangeUserCollection(
      @Nonnull Collection<User> users,
      @Nonnull Class<?> itemType,
      @Nonnull Collection<? extends IdentifiableObject> additions,
      @Nonnull Collection<? extends IdentifiableObject> deletions,
      @Nonnull User currentUser)
      throws ForbiddenException {
    for (User user : users) {
      checkCanChangeUser(user, currentUser);
    }

    List<ErrorReport> errors = new ArrayList<>();
    if (UserRole.class == itemType) {
      errors.addAll(validateCanIssueUserRoles((Collection<UserRole>) additions, currentUser));
    } else if (UserGroup.class == itemType) {
      List<IdentifiableObject> groups = new ArrayList<>(additions);
      groups.addAll(deletions);
      for (IdentifiableObject group : groups) {
        if (!userGroupService.canAddOrRemoveMember((UserGroup) group, currentUser)) {
          errors.add(
              new ErrorReport(
                  UserGroup.class, ErrorCode.E3005, currentUser.getUsername(), group.getName()));
        }
      }
    } else if (OrganisationUnit.class == itemType && !currentUser.isSuper()) {
      checkLoadedOrgUnitsInHierarchy((Collection<OrganisationUnit>) additions, currentUser, errors);
    }

    throwIfAny(errors);
  }

  @Override
  @Transactional(readOnly = true)
  public void checkCanChangeUser(User user, User currentUser) throws ForbiddenException {
    if (!currentUser.isSuper() && user.isSuper()) {
      throw new ForbiddenException(ErrorCode.E3041, currentUser.getUsername());
    }

    List<ErrorReport> errors = new ArrayList<>();
    if (!canCurrentUserCanModify(currentUser, user, errors::add)) {
      throwIfAny(errors);
    }
  }

  /** Throws the first of the given errors, if any, keeping its error code. */
  private static void throwIfAny(List<ErrorReport> errors) throws ForbiddenException {
    if (!errors.isEmpty()) {
      ErrorReport error = errors.get(0);
      throw new ForbiddenException(error.getErrorCode(), error.getErrorProperties().toArray());
    }
  }

  @Override
  @Transactional
  public void addUserToRole(@Nonnull UserRole role, @Nonnull User user, @Nonnull User currentUser)
      throws ForbiddenException {
    if (!aclService.canUpdate(currentUser, role)) {
      throw new ForbiddenException("You don't have the proper permissions to update this object.");
    }

    checkCanAddUserRoles(user, List.of(role), currentUser);

    if (!user.getUserRoles().contains(role)) {
      user.getUserRoles().add(role);
      updateUser(user);
    }
  }

  @Override
  @Transactional
  public void removeUserFromRole(
      @Nonnull UserRole role, @Nonnull User user, @Nonnull User currentUser)
      throws ForbiddenException {
    if (!aclService.canUpdate(currentUser, role)) {
      throw new ForbiddenException("You don't have the proper permissions to delete this object.");
    }

    checkCanRemoveUserRoles(user, currentUser);

    if (user.getUserRoles().contains(role)) {
      user.getUserRoles().remove(role);
      updateUser(user);
    }
  }

  @Override
  @Transactional(readOnly = true)
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

  @Override
  public void registerFailed2FADisableAttempt(String username) {
    Integer attempts = twoFaDisableFailedAttemptCache.get(username).orElse(0);
    attempts++;
    twoFaDisableFailedAttemptCache.put(username, attempts);
  }

  @Override
  public void registerSuccess2FADisable(String username) {
    twoFaDisableFailedAttemptCache.invalidate(username);
  }

  @Override
  public boolean twoFaDisableIsLocked(String username) {
    return twoFaDisableFailedAttemptCache.get(username).orElse(0) >= LOGIN_MAX_FAILED_ATTEMPTS;
  }

  @Transactional
  @Override
  public void disableTwoFa(User user, String code) {
    if (user.getSecret() == null) {
      throw new IllegalStateException("Two factor is not enabled, enable first");
    }

    if (twoFaDisableIsLocked(user.getUsername())) {
      throw new IllegalStateException("Too many failed attempts, try again later");
    }

    if (!TwoFactoryAuthenticationUtils.verify(code, user.getSecret())) {
      registerFailed2FADisableAttempt(user.getUsername());
      throw new IllegalStateException("Invalid code");
    }

    resetTwoFactor(user);

    registerSuccess2FADisable(user.getUsername());
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
    invalidateUserSessions(user.getUid());
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
  @Transactional(readOnly = true)
  public List<User> getUsersWithAuthority(String authority) {
    return userStore.getHasAuthority(authority);
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

    List<UserGroup> readableGroups =
        userToModify.getGroups().stream()
            .filter(group -> aclService.canRead(currentUser, group))
            .collect(Collectors.toList());
    if (!canAddOrUpdateUserInGroups(readableGroups, currentUser)
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
  @Transactional(readOnly = true)
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
  public List<UserLookup> getLinkedUserAccounts(@Nonnull User actingUser) {
    List<User> linkedUserAccounts = userStore.getLinkedUserAccounts(actingUser);

    List<UserLookup> userLookups =
        linkedUserAccounts.stream().map(UserLookup::fromUser).collect(Collectors.toList());

    for (int i = 0; i < linkedUserAccounts.size(); i++) {
      userLookups
          .get(i)
          .setRoles(
              linkedUserAccounts.get(i).getUserRoles().stream()
                  .map(UserRole::getUid)
                  .collect(Collectors.toSet()));
      userLookups
          .get(i)
          .setGroups(
              linkedUserAccounts.get(i).getGroups().stream()
                  .map(UserGroup::getUid)
                  .collect(Collectors.toSet()));
    }

    return userLookups;
  }

  @Override
  @Transactional(readOnly = true)
  public List<User> getUsersWithOrgUnits(
      @Nonnull UserOrgUnitProperty orgUnitProperty, @Nonnull Set<UID> uids) {
    return userStore.getUsersWithOrgUnits(orgUnitProperty, uids);
  }

  @Transactional
  @Override
  public void setActiveLinkedAccounts(@Nonnull String actingUser, @Nonnull String activeUsername) {
    userStore.setActiveLinkedAccounts(actingUser, activeUsername);
  }

  @Override
  public void invalidateUserSessions(String userUid) {
    User user = userStore.getByUid(userUid);
    CurrentUserDetailsImpl userDetails = createUserDetails(user, true, true);
    if (userDetails != null) {
      List<SessionInformation> allSessions = sessionRegistry.getAllSessions(userDetails, false);
      allSessions.forEach(SessionInformation::expireNow);
    }
  }

  @Override
  @Transactional(readOnly = true)
  public void checkCanReplicateUser(@Nonnull User source, @Nonnull User currentUser)
      throws ForbiddenException {
    checkCanChangeUser(source, currentUser);

    throwIfAny(validateUserCreateOrUpdateAccess(source, currentUser));

    if (currentUser.isSuper()) {
      return;
    }

    // Creating a user gives it the dimension constraints of the current user, so that it cannot see
    // more data than its creator. A replica gets those of its source instead.
    if (!source.getCatDimensionConstraints().containsAll(currentUser.getCatDimensionConstraints())
        || !source
            .getCogsDimensionConstraints()
            .containsAll(currentUser.getCogsDimensionConstraints())) {
      throw new ForbiddenException(ErrorCode.E3053, currentUser.getUsername());
    }
  }
}
