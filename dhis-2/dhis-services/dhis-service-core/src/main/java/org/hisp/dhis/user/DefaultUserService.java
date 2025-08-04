/*
 * Copyright (c) 2004-2022, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors 
 * may be used to endorse or promote products derived from this software without
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
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.hisp.dhis.common.CodeGenerator.isValidUid;
import static org.hisp.dhis.common.IdentifiableObjectUtils.getUids;
import static org.hisp.dhis.system.util.ValidationUtils.usernameIsValid;
import static org.hisp.dhis.system.util.ValidationUtils.uuidIsValid;
import static org.hisp.dhis.user.UserConstants.BCRYPT_PATTERN;
import static org.hisp.dhis.user.UserConstants.DEFAULT_APPLICATION_TITLE;
import static org.hisp.dhis.user.UserConstants.EMAIL_TOKEN_EXPIRY_MILLIS;
import static org.hisp.dhis.user.UserConstants.LOGIN_MAX_FAILED_ATTEMPTS;
import static org.hisp.dhis.user.UserConstants.PW_NO_INTERNAL_LOGIN;
import static org.hisp.dhis.user.UserConstants.RECAPTCHA_VERIFY_URL;
import static org.hisp.dhis.user.UserConstants.RECOVER_MAX_ATTEMPTS;
import static org.hisp.dhis.user.UserConstants.RESTORE_PATH;
import static org.hisp.dhis.user.UserConstants.TBD_NAME;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import jakarta.persistence.EntityManager;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Base64;
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
import org.hibernate.Session;
import org.hisp.dhis.attribute.Attribute;
import org.hisp.dhis.attribute.AttributeService;
import org.hisp.dhis.cache.Cache;
import org.hisp.dhis.cache.CacheProvider;
import org.hisp.dhis.common.AuditLogUtil;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.MergeMode;
import org.hisp.dhis.common.PasswordGenerator;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.common.UserOrgUnitType;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.email.EmailResponse;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.i18n.I18n;
import org.hisp.dhis.i18n.I18nManager;
import org.hisp.dhis.message.MessageSender;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.outboundmessage.OutboundMessageResponse;
import org.hisp.dhis.period.Cal;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.schema.MetadataMergeParams;
import org.hisp.dhis.schema.MetadataMergeService;
import org.hisp.dhis.security.PasswordManager;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.setting.SystemSettingsProvider;
import org.hisp.dhis.setting.UserSettings;
import org.hisp.dhis.system.util.ValidationUtils;
import org.hisp.dhis.system.velocity.VelocityManager;
import org.hisp.dhis.util.DateUtils;
import org.hisp.dhis.util.ObjectUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

/**
 * @author Chau Thu Tran
 * @author Morten Svanæs
 */
@Slf4j
@Lazy
@Service("org.hisp.dhis.user.UserService")
public class DefaultUserService implements UserService {

  private final UserStore userStore;
  private final UserGroupService userGroupService;
  private final UserRoleStore userRoleStore;
  private final SystemSettingsProvider settingsProvider;
  private final PasswordManager passwordManager;
  private final AclService aclService;
  private final OrganisationUnitService organisationUnitService;
  private final SessionRegistry sessionRegistry;
  private final UserSettingsService userSettingsService;
  private final RestTemplate restTemplate;
  private final MessageSender emailMessageSender;
  private final I18nManager i18nManager;
  private final ObjectMapper jsonMapper;
  private final MetadataMergeService metadataMergeService;
  private final AttributeService attributeService;
  private final EntityManager entityManager;

  private final Cache<String> userDisplayNameCache;
  private final Cache<Integer> userFailedLoginAttemptCache;
  private final Cache<Integer> userAccountRecoverAttemptCache;
  private final Cache<Integer> twoFaDisableFailedAttemptCache;

  public DefaultUserService(
      UserSettingsService userSettingsService,
      RestTemplate restTemplate,
      MessageSender emailMessageSender,
      I18nManager i18nManager,
      ObjectMapper jsonMapper,
      UserStore userStore,
      UserGroupService userGroupService,
      UserRoleStore userRoleStore,
      SystemSettingsProvider settingsProvider,
      CacheProvider cacheProvider,
      PasswordManager passwordManager,
      AclService aclService,
      OrganisationUnitService organisationUnitService,
      SessionRegistry sessionRegistry,
      MetadataMergeService metadataMergeService,
      AttributeService attributeService,
      EntityManager entityManager) {
    checkNotNull(userStore);
    checkNotNull(userGroupService);
    checkNotNull(userRoleStore);
    checkNotNull(settingsProvider);
    checkNotNull(passwordManager);
    checkNotNull(aclService);
    checkNotNull(organisationUnitService);
    checkNotNull(sessionRegistry);
    checkNotNull(userSettingsService);
    checkNotNull(restTemplate);
    checkNotNull(cacheProvider);
    checkNotNull(emailMessageSender);
    checkNotNull(i18nManager);
    checkNotNull(jsonMapper);
    checkNotNull(metadataMergeService);
    checkNotNull(attributeService);
    checkNotNull(entityManager);

    this.userStore = userStore;
    this.userGroupService = userGroupService;
    this.userRoleStore = userRoleStore;
    this.settingsProvider = settingsProvider;
    this.passwordManager = passwordManager;
    this.userDisplayNameCache = cacheProvider.createUserDisplayNameCache();
    this.aclService = aclService;
    this.organisationUnitService = organisationUnitService;
    this.sessionRegistry = sessionRegistry;
    this.userSettingsService = userSettingsService;
    this.restTemplate = restTemplate;
    this.emailMessageSender = emailMessageSender;
    this.i18nManager = i18nManager;
    this.jsonMapper = jsonMapper;
    this.metadataMergeService = metadataMergeService;
    this.attributeService = attributeService;
    this.entityManager = entityManager;
    this.userFailedLoginAttemptCache = cacheProvider.createUserFailedLoginAttemptCache(0);
    this.userAccountRecoverAttemptCache = cacheProvider.createUserAccountRecoverAttemptCache(0);
    this.twoFaDisableFailedAttemptCache = cacheProvider.createDisable2FAFailedAttemptCache(0);
  }

  @Override
  @Transactional
  public long addUser(User user) {
    String currentUsername = CurrentUserUtil.getCurrentUsername();
    AuditLogUtil.infoWrapper(log, currentUsername, user, AuditLogUtil.ACTION_CREATE);

    userStore.save(user);

    return user.getId();
  }

  @Override
  @Transactional
  public long addUser(User user, UserDetails actingUser) {
    AuditLogUtil.infoWrapper(log, actingUser.getUsername(), user, AuditLogUtil.ACTION_CREATE);

    userStore.save(user, actingUser, false);

    return user.getId();
  }

  @Override
  @Transactional
  public void updateUser(User user) {
    userStore.update(user);

    AuditLogUtil.infoWrapper(
        log, CurrentUserUtil.getCurrentUsername(), user, AuditLogUtil.ACTION_UPDATE);
  }

  @Override
  @Transactional
  public void updateUser(User user, UserDetails actingUser) {
    userStore.update(user, actingUser);

    AuditLogUtil.infoWrapper(log, actingUser.getUsername(), user, AuditLogUtil.ACTION_UPDATE);
  }

  @Override
  @Transactional
  public void deleteUser(User user) {
    AuditLogUtil.infoWrapper(
        log, CurrentUserUtil.getCurrentUsername(), user, AuditLogUtil.ACTION_DELETE);

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
    return userStore.getUserByUsername(username);
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

    try {
      validateUserQueryParams(params);
    } catch (ConflictException ex) {
      log.warn(ex.getMessage());
      return Lists.newArrayList();
    }

    return userStore.getUsers(params, orders);
  }

  @Override
  @Transactional(readOnly = true)
  public List<UID> getUserIds(UserQueryParams params, @CheckForNull List<String> orders)
      throws ConflictException {
    handleUserQueryParams(params);
    validateUserQueryParams(params);

    return userStore.getUserIds(params, orders);
  }

  @Override
  @Transactional(readOnly = true)
  public int getUserCount(UserQueryParams params) {
    handleUserQueryParams(params);

    try {
      validateUserQueryParams(params);
    } catch (ConflictException ex) {
      log.warn(ex.getMessage());
      return 0;
    }

    return userStore.getUserCount(params);
  }

  @Override
  @Transactional(readOnly = true)
  public int getUserCount() {
    return userStore.getUserCount();
  }

  /**
   * Handles the user query parameters by setting defaults and processing specific fields.
   *
   * @param params the {@link UserQueryParams}.
   */
  private void handleUserQueryParams(UserQueryParams params) {
    boolean canSeeOwnRoles =
        params.isCanSeeOwnRoles()
            || settingsProvider.getCurrentSettings().getCanGrantOwnUserRoles();
    params.setDisjointRoles(!canSeeOwnRoles);

    if (!params.hasUserDetails() && CurrentUserUtil.hasCurrentUser()) {
      UserDetails currentUserDetails = CurrentUserUtil.getCurrentUserDetails();
      params.setUserDetails(currentUserDetails);
    }

    if (params.hasUserDetails() && params.getUserDetails().isSuper()) {
      params.setCanManage(false);
      params.setAuthSubset(false);
      params.setDisjointRoles(false);
    }

    if (params.getInactiveMonths() != null) {
      Calendar cal = PeriodType.createCalendarInstance();
      cal.add(Calendar.MONTH, (params.getInactiveMonths() * -1));
      params.setInactiveSince(cal.getTime());
    }

    if (params.hasUserDetails()) {
      UserOrgUnitType orgUnitBoundary = params.getOrgUnitBoundary();
      if (params.isUserOrgUnits() || orgUnitBoundary == UserOrgUnitType.DATA_CAPTURE) {
        params.setOrganisationUnits(
            params.getUserDetails().getUserOrgUnitIds().stream()
                .map(organisationUnitService::getOrganisationUnit)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet()));
        params.setOrgUnitBoundary(UserOrgUnitType.DATA_CAPTURE);
      } else if (orgUnitBoundary == UserOrgUnitType.DATA_OUTPUT) {
        params.setOrganisationUnits(
            params.getUserDetails().getUserDataOrgUnitIds().stream()
                .map(organisationUnitService::getOrganisationUnit)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet()));
      } else if (orgUnitBoundary == UserOrgUnitType.TEI_SEARCH) {
        params.setOrganisationUnits(
            params.getUserDetails().getUserSearchOrgUnitIds().stream()
                .map(organisationUnitService::getOrganisationUnit)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet()));
      }
    }
  }

  /**
   * Validates the user query parameters to ensure that the user has the necessary permissions to
   * perform the requested operation.
   *
   * @param params the {@link UserQueryParams} to validate.
   * @throws ConflictException if the user does not have the required permissions.
   */
  private void validateUserQueryParams(UserQueryParams params) throws ConflictException {
    if (params.isCanManage()
        && (params.getUserDetails() == null || !hasManagedGroups(params.getUserDetails()))) {
      throw new ConflictException(
          "Cannot get managed users as user does not have any managed groups");
    }
    if (params.isAuthSubset()
        && (params.getUserDetails() == null || !hasAuthorities(params.getUserDetails()))) {
      throw new ConflictException(
          "Cannot get users with authority subset as user does not have any authorities");
    }
    if (params.isDisjointRoles()
        && (params.getUserDetails() == null || !hasUserRoles(params.getUserDetails()))) {
      throw new ConflictException(
          "Cannot get users with disjoint roles as user does not have any user roles");
    }
  }

  private boolean hasManagedGroups(UserDetails userDetails) {
    return userDetails != null && !userDetails.getManagedGroupLongIds().isEmpty();
  }

  private boolean hasAuthorities(UserDetails userDetails) {
    return userDetails != null && !userDetails.getAllAuthorities().isEmpty();
  }

  private boolean hasUserRoles(UserDetails userDetails) {
    return userDetails != null && !userDetails.getUserRoleIds().isEmpty();
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
    return canAddOrUpdateUser(userGroups, CurrentUserUtil.getCurrentUserDetails());
  }

  // TODO: MAS refactor to use user details instead of user
  @Override
  @Transactional(readOnly = true)
  public boolean canAddOrUpdateUser(Collection<String> userGroups, UserDetails currentUser) {
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
  public long addUserRole(UserRole userRole, UserDetails actingUser) {
    userRoleStore.save(userRole, actingUser, false);
    return userRole.getId();
  }

  @Override
  @Transactional
  public void updateUserRole(UserRole userRole) {
    userRoleStore.update(userRole);
  }

  @Override
  @Transactional
  public void updateUserRole(UserRole userRole, UserDetails userDetails) {
    userRoleStore.update(userRole, userDetails);
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
  public List<UID> getRolesCurrentUserCanIssue() {
    UserDetails user = CurrentUserUtil.getCurrentUserDetails();

    boolean canGrantOwnUserRoles = settingsProvider.getCurrentSettings().getCanGrantOwnUserRoles();

    return userRoleStore.getAll().stream()
        .filter(role -> user.canIssueUserRole(role, canGrantOwnUserRoles))
        .map(role -> UID.of(role.getUid()))
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public List<User> getUsersByUsernames(Collection<String> usernames) {
    return userStore.getUserByUsernames(usernames);
  }

  @Override
  @Transactional
  public void encodeAndSetPassword(User user, String rawPassword) {
    if (isEmpty(rawPassword) && !user.isExternalAuth()) {
      return; // Leave unchanged if internal authentication and no password supplied
    }

    if (user.isExternalAuth()) {
      user.setPassword(PW_NO_INTERNAL_LOGIN);

      return; // Set unusable, not-encoded password if external authentication
    }

    boolean isNewPassword =
        StringUtils.isBlank(user.getPassword())
            || !passwordManager.matches(rawPassword, user.getPassword());

    if (isNewPassword) {
      user.setPasswordLastUpdated(new Date());
    }

    // Encode and set password
    Matcher matcher = BCRYPT_PATTERN.matcher(rawPassword);
    if (matcher.matches()) {
      throw new IllegalArgumentException(
          "Raw password looks like bcrypt encoded password, this is most likely a bug");
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
    int credentialsExpires = settingsProvider.getCurrentSettings().getCredentialsExpires();

    if (credentialsExpires == 0) {
      return true;
    }

    if (user.getPasswordLastUpdated() == null) {
      return true;
    }

    int months = DateUtils.monthsBetween(user.getPasswordLastUpdated(), new Date());

    return months < credentialsExpires;
  }

  @Override
  @Transactional(readOnly = true)
  public List<ErrorReport> validateUserCreateOrUpdateAccess(User user, UserDetails currentUser) {

    List<ErrorReport> errors = new ArrayList<>();

    if (user == null || currentUser.isSuper()) {
      return errors;
    }

    User userToChange = userStore.get(user.getId());
    if (userToChange != null && userToChange.isSuper()) {
      errors.add(new ErrorReport(User.class, ErrorCode.E3041, currentUser.getUsername()));
    }

    checkHasAccessToUserRoles(user, currentUser, errors);
    checkHasAccessToUserGroups(user, currentUser, errors);

    checkIsInOrgUnitHierarchy(user.getOrganisationUnits(), currentUser, errors);
    checkIsInOrgUnitHierarchy(user.getDataViewOrganisationUnits(), currentUser, errors);
    checkIsInOrgUnitHierarchy(user.getTeiSearchOrganisationUnits(), currentUser, errors);

    return errors;
  }

  // TODO: MAS. This needs refactoring, can be unnecessary expensive, can be moved to the DB
  private void checkIsInOrgUnitHierarchy(
      Set<OrganisationUnit> organisationUnits, UserDetails currentUser, List<ErrorReport> errors) {
    for (OrganisationUnit orgUnit : organisationUnits) {
      // We have to fetch the org unit in order to get the full path
      OrganisationUnit fetchedOrgUnit =
          organisationUnitService.getOrganisationUnit(orgUnit.getUid());
      boolean inUserHierarchy = fetchedOrgUnit.isDescendant(currentUser.getUserOrgUnitIds());
      if (!inUserHierarchy) {
        errors.add(
            new ErrorReport(
                OrganisationUnit.class,
                ErrorCode.E7617,
                orgUnit.getUid(),
                currentUser.getUsername()));
      }
    }
  }

  /**
   * Checks if the current user has access to the user groups of the user being created or updated.
   * If not, adds an error report to the list of errors.
   *
   * @param user the user being created or updated.
   * @param currentUser the user performing the action.
   * @param errors the list of error reports to add to.
   */
  private void checkHasAccessToUserGroups(
      User user, UserDetails currentUser, List<ErrorReport> errors) {
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

  /**
   * Checks if the current user has access to the user roles of the user being created or updated.
   * If not, adds an error report to the list of errors.
   *
   * @param user the user being created or updated.
   * @param currentUser the user performing the action.
   * @param errors the list of error reports to add to.
   */
  private void checkHasAccessToUserRoles(
      User user, UserDetails currentUser, List<ErrorReport> errors) {
    Set<UserRole> userRoles = user.getUserRoles();

    boolean canGrantOwnUserRoles = settingsProvider.getCurrentSettings().getCanGrantOwnUserRoles();

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
  }

  @Override
  @Transactional(readOnly = true)
  public List<ErrorReport> validateUserRoleCreateOrUpdate(UserRole role, UserDetails currentUser) {

    List<ErrorReport> errors = new ArrayList<>();

    if (role == null) {
      return errors;
    }

    if (!currentUser.isSuper() && role.isSuper()) {
      errors.add(new ErrorReport(UserRole.class, ErrorCode.E3032, currentUser.getUsername()));
    }

    UserRole userRoleBefore = userRoleStore.get(role.getId());
    if (!currentUser.isSuper() && userRoleBefore != null && userRoleBefore.isSuper()) {
      errors.add(new ErrorReport(UserRole.class, ErrorCode.E3032, currentUser.getUsername()));
    }

    return errors;
  }

  @Override
  public List<UserAccountExpiryInfo> getExpiringUserAccounts(int inDays) {
    return userStore.getExpiringUserAccounts(inDays);
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
  public boolean is2FADisableEndpointLocked(String username) {
    return twoFaDisableFailedAttemptCache.get(username).orElse(0) >= LOGIN_MAX_FAILED_ATTEMPTS;
  }

  @Override
  @Transactional
  public int disableUsersInactiveSince(Date inactiveSince) {
    if (ZonedDateTime.ofInstant(inactiveSince.toInstant(), systemDefault())
        .plusMonths(1)
        .isAfter(now())) {
      // Never disable users that have been active during last month
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
  @Transactional(readOnly = true)
  public Map<String, String> getActiveUserGroupUserEmailsByUsername(String userGroupId) {
    return userStore.getActiveUserGroupUserEmailsByUsername(userGroupId);
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
  public UserDetails createUserDetails(String userUid) throws NotFoundException {
    User user = userStore.getByUid(userUid);
    if (user == null) {
      throw new NotFoundException(User.class, userUid);
    }
    return createUserDetails(user);
  }

  @Override
  @Transactional(readOnly = true)
  @CheckForNull
  public UserDetails createUserDetailsSafe(@Nonnull String userUid) {
    User user = userStore.getByUid(userUid);
    if (user == null) {
      return null;
    }
    return createUserDetails(user);
  }

  @Override
  @Transactional(readOnly = true)
  public UserDetails createUserDetails(User user) {
    if (user == null) {
      return null;
    }
    Objects.requireNonNull(user);

    String username = user.getUsername();
    boolean enabled = !user.isDisabled();
    boolean accountNonExpired = user.isAccountNonExpired();
    boolean credentialsNonExpired = userNonExpired(user);
    boolean accountNonLocked = !isLocked(user.getUsername());

    if (ObjectUtils.anyIsFalse(
        enabled, credentialsNonExpired, accountNonLocked, accountNonExpired)) {
      log.info(
          String.format(
              "Login attempt for disabled/locked user: '%s', enabled: %b, account non-expired: %b,"
                  + " user non-expired: %b, account non-locked: %b",
              username, enabled, accountNonExpired, credentialsNonExpired, accountNonLocked));
    }

    List<String> organisationUnitsUidsByUser =
        organisationUnitService.getOrganisationUnitsUidsByUser(user.getUsername());
    List<String> searchOrganisationUnitsUidsByUser =
        organisationUnitService.getSearchOrganisationUnitsUidsByUser(user.getUsername());
    List<String> dataViewOrganisationUnitsUidsByUser =
        organisationUnitService.getDataViewOrganisationUnitsUidsByUser(user.getUsername());

    return UserDetails.createUserDetails(
        user,
        accountNonLocked,
        credentialsNonExpired,
        new HashSet<>(organisationUnitsUidsByUser),
        new HashSet<>(searchOrganisationUnitsUidsByUser),
        new HashSet<>(dataViewOrganisationUnitsUidsByUser));
  }

  @Override
  public CurrentUserGroupInfo getCurrentUserGroupInfo(String userUID) {
    return userStore.getCurrentUserGroupInfo(userUID);
  }

  @Override
  @Transactional(readOnly = true)
  public User getUserByEmail(String email) {
    return userStore.getUserByEmail(email);
  }

  @Override
  @Transactional(readOnly = true)
  public boolean canCurrentUserCanModify(
      UserDetails currentUser, User userToModify, Consumer<ErrorReport> errors) {
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
  public List<SessionInformation> listSessions(String userUID) {
    User user = userStore.getByUid(userUID);
    if (user == null) {
      return List.of();
    }
    return sessionRegistry.getAllSessions(createUserDetails(user), true);
  }

  @Override
  public List<SessionInformation> listSessions(UserDetails principal) {
    return sessionRegistry.getAllSessions(principal, true);
  }

  @Override
  public void invalidateAllSessions() {
    for (Object allPrincipal : sessionRegistry.getAllPrincipals()) {
      for (SessionInformation allSession : sessionRegistry.getAllSessions(allPrincipal, true)) {
        sessionRegistry.removeSessionInformation(allSession.getSessionId());
      }
    }
  }

  @Override
  public void invalidateUserSessions(String username) {
    User user = getUserByUsername(username);
    UserDetails userDetails = createUserDetails(user);
    if (userDetails != null) {
      List<SessionInformation> allSessions = sessionRegistry.getAllSessions(userDetails, false);
      allSessions.forEach(SessionInformation::expireNow);
    }
  }

  @Override
  @Transactional(readOnly = true)
  public ErrorCode validateInvite(User user) {
    if (user == null) {
      return ErrorCode.E6201;
    }

    if (user.getUsername() != null && getUserByUsername(user.getUsername()) != null) {
      log.warn("Could not send invite message as username is already taken.");
      return ErrorCode.E6204;
    }

    if (user.getEmail() == null || !ValidationUtils.emailIsValid(user.getEmail())) {
      log.warn("Could not send restore/invite message as user has no email or email is invalid");
      return ErrorCode.E6202;
    }

    if (!emailMessageSender.isConfigured()) {
      log.warn("Could not send restore/invite message as email is not configured");
      return ErrorCode.E6203;
    }

    return null;
  }

  @Override
  @Transactional
  public boolean sendRestoreOrInviteMessage(
      User user, String rootPath, RestoreOptions restoreOptions) {
    User persistedUser = getUser(user.getUid());
    UserSettings userSettings =
        userSettingsService.getUserSettings(persistedUser.getUsername(), true);
    I18n i18n = i18nManager.getI18n(userSettings.getUserUiLocale());
    String encodedTokens = generateAndPersistTokens(persistedUser, restoreOptions);
    RestoreType restoreType = restoreOptions.getRestoreType();
    String applicationTitle = settingsProvider.getCurrentSettings().getApplicationTitle();
    String restorePath = String.format("%s%s%s", rootPath, RESTORE_PATH, restoreType.getAction());
    rootPath = rootPath.replace("http://", "").replace("https://", "");

    if (isEmpty(applicationTitle)) {
      applicationTitle = DEFAULT_APPLICATION_TITLE;
    }

    Map<String, Object> vars = new HashMap<>();
    vars.put("applicationTitle", applicationTitle);
    vars.put("restorePath", restorePath);
    vars.put("token", encodedTokens);
    vars.put("username", user.getUsername());
    vars.put("email", user.getEmail());
    vars.put("welcomeMessage", persistedUser.getWelcomeMessage());
    vars.put("i18n", i18n);

    String messageBody = new VelocityManager().render(vars, restoreType.getEmailTemplate() + "1");
    String messageSubject = i18n.getString(restoreType.getEmailSubject()) + " " + rootPath;

    emailMessageSender.sendMessage(
        messageSubject, messageBody, null, null, Set.of(persistedUser), true);

    return true;
  }

  @Override
  @Transactional
  public String generateAndPersistTokens(User user, RestoreOptions restoreOptions) {
    RestoreType restoreType = restoreOptions.getRestoreType();

    String restoreToken = restoreOptions.getTokenPrefix() + CodeGenerator.getRandomSecureToken();

    String hashedRestoreToken = passwordManager.encode(restoreToken);

    String idToken = CodeGenerator.getRandomSecureToken();

    Date expiry =
        new Cal()
            .now()
            .add(restoreType.getExpiryIntervalType(), restoreType.getExpiryIntervalCount())
            .time();

    // ID token is not hashed as it is used for lookups
    user.setIdToken(idToken);
    user.setRestoreToken(hashedRestoreToken);
    user.setRestoreExpiry(expiry);

    updateUser(user, new SystemUser());

    return Base64.getUrlEncoder()
        .withoutPadding()
        .encodeToString((idToken + ":" + restoreToken).getBytes());
  }

  @Override
  public String[] decodeEncodedTokens(String encodedTokens) {
    String decodedEmailToken =
        new String(Base64.getUrlDecoder().decode(encodedTokens), StandardCharsets.UTF_8);

    return decodedEmailToken.split(":");
  }

  @Override
  public RestoreOptions getRestoreOptions(String token) {
    return RestoreOptions.getRestoreOptions(token);
  }

  @Override
  @Transactional
  public boolean restore(User user, String token, String newPassword, RestoreType restoreType) {
    if (ObjectUtils.anyIsNull(user, token, newPassword) || !canRestore(user, token, restoreType)) {
      return false;
    }

    user.setRestoreToken(null);
    user.setRestoreExpiry(null);
    user.setIdToken(null);
    user.setInvitation(false);

    encodeAndSetPassword(user, newPassword);
    updateUser(user, new SystemUser());

    return true;
  }

  @Override
  public void registerRecoveryAttempt(String username) {
    if (isNotBlockOnFailedLogins() || username == null) {
      return;
    }

    Integer attempts = userAccountRecoverAttemptCache.get(username).orElse(0);

    userAccountRecoverAttemptCache.put(username, ++attempts);
  }

  @Override
  public boolean isRecoveryLocked(String username) {
    if (isNotBlockOnFailedLogins() || username == null) {
      return false;
    }

    return userAccountRecoverAttemptCache.get(username).orElse(0) > RECOVER_MAX_ATTEMPTS;
  }

  @Override
  public void registerFailedLogin(String username) {
    if (isNotBlockOnFailedLogins() || username == null) {
      return;
    }

    Integer attempts = userFailedLoginAttemptCache.get(username).orElse(0);

    attempts++;

    userFailedLoginAttemptCache.put(username, attempts);
  }

  @Override
  public void registerSuccessfulLogin(String username) {
    if (isNotBlockOnFailedLogins() || username == null) {
      return;
    }

    userFailedLoginAttemptCache.invalidate(username);
  }

  @Override
  public boolean isLocked(String username) {
    if (isNotBlockOnFailedLogins() || username == null) {
      return false;
    }
    return userFailedLoginAttemptCache.get(username).orElse(0) >= LOGIN_MAX_FAILED_ATTEMPTS;
  }

  private boolean isNotBlockOnFailedLogins() {
    return !settingsProvider.getCurrentSettings().getLockMultipleFailedLogins();
  }

  @Override
  public void prepareUserForInvite(User user) {
    Objects.requireNonNull(user, "User object can't be null");

    if (isEmpty(user.getUsername())) {
      String username = "invite" + CodeGenerator.generateUid().toLowerCase();

      user.setUsername(username);
    }

    int minPasswordLength = settingsProvider.getCurrentSettings().getMinPasswordLength();
    char[] plaintextPassword = PasswordGenerator.generateValidPassword(minPasswordLength);

    user.setSurname(isEmpty(user.getSurname()) ? TBD_NAME : user.getSurname());
    user.setFirstName(isEmpty(user.getFirstName()) ? TBD_NAME : user.getFirstName());
    user.setInvitation(true);
    user.setPassword(new String(plaintextPassword));
  }

  @Override
  public ErrorCode validateRestore(User user) {
    if (user == null) {
      log.warn("Could not send restore/invite message as user is null");
      return ErrorCode.E6201;
    }

    if (user.getEmail() == null || !ValidationUtils.emailIsValid(user.getEmail())) {
      log.warn("Could not send restore/invite message as user has no email or email is invalid");
      return ErrorCode.E6202;
    }

    if (user.isExternalAuth()) {
      log.warn("Could reset password, user is using external authentication.");
      return ErrorCode.E6202;
    }

    if (!emailMessageSender.isConfigured()) {
      log.warn("Could not send restore/invite message as email is not configured");
      return ErrorCode.E6203;
    }

    return null;
  }

  @Override
  public boolean canRestore(User user, String token, RestoreType restoreType) {
    ErrorCode code = validateRestore(user, token, restoreType);
    log.info("User account restore outcome: {}", code);
    return code == null;
  }

  /**
   * Verifies all parameters needed for account restore and checks validity of the user supplied
   * token and code. If the restore cannot be verified a descriptive error string is returned.
   *
   * @param user the user.
   * @param token the user supplied token.
   * @param restoreType the restore type.
   * @return null if restore is valid, a descriptive error string otherwise.
   */
  private ErrorCode validateRestore(User user, String token, RestoreType restoreType) {
    if (user.getRestoreToken() == null) {
      return ErrorCode.E6209;
    }

    if (user.getRestoreExpiry() == null) {
      return ErrorCode.E6210;
    }

    if (new Date().after(user.getRestoreExpiry())) {
      return ErrorCode.E6211;
    }

    return validateRestoreToken(user, token, restoreType);
  }

  @Override
  public ErrorCode validateRestoreToken(User user, String restoreToken, RestoreType restoreType) {
    if (user == null) {
      return ErrorCode.E6201;
    }

    if (restoreToken == null) {
      log.warn(
          "Could not send verify restore token; error=token_parameter_is_null; username: {}",
          user.getUsername());
      return ErrorCode.E6205;
    }

    if (restoreType == null) {
      log.warn(
          "Could not send verify restore token; error=restore_type_parameter_is_null; username: {}",
          user.getUsername());
      return ErrorCode.E6206;
    }

    RestoreOptions restoreOptions = RestoreOptions.getRestoreOptions(restoreToken);

    if (restoreOptions == null) {
      log.warn(
          "Could not send verify restore token; error=cannot_parse_restore_options; username: {}",
          user.getUsername());
      return ErrorCode.E6207;
    }

    if (restoreType != restoreOptions.getRestoreType()) {
      log.warn(
          "Could not send verify restore token; error=wrong_prefix_for_restore_type; username: {}",
          user.getUsername());
      return ErrorCode.E6207;
    }

    String hashedRestoreToken = user.getRestoreToken();

    if (hashedRestoreToken == null) {
      log.warn(
          "Could not send verify restore token; error=could_not_verify_token; username: {}",
          user.getUsername());
      return ErrorCode.E6208;
    }

    boolean validToken = passwordManager.matches(restoreToken, hashedRestoreToken);

    if (!validToken) {
      log.warn(
          "Could not verify restore token; error=restore_token_does_not_match_supplied_token;"
              + " username: {}",
          user.getUsername());
      return ErrorCode.E6208;
    }

    return null;
  }

  @Override
  public boolean canCreatePublic(IdentifiableObject identifiableObject) {
    return !aclService.isShareable(identifiableObject)
        || aclService.canMakePublic(CurrentUserUtil.getCurrentUserDetails(), identifiableObject);
  }

  @Override
  public boolean canCreatePublic(String type) {
    Class<? extends IdentifiableObject> klass = aclService.classForType(type);

    return !aclService.isClassShareable(klass)
        || aclService.canMakeClassPublic(CurrentUserUtil.getCurrentUserDetails(), klass);
  }

  @Override
  public boolean canCreatePrivate(IdentifiableObject identifiableObject) {
    return !aclService.isShareable(identifiableObject)
        || aclService.canMakePrivate(CurrentUserUtil.getCurrentUserDetails(), identifiableObject);
  }

  @Override
  public boolean canView(String type) {
    boolean requireAddToView = settingsProvider.getCurrentSettings().getRequireAddToView();

    return !requireAddToView || (canCreatePrivate(type) || canCreatePublic(type));
  }

  @Override
  public boolean canCreatePrivate(String type) {
    Class<? extends IdentifiableObject> klass = aclService.classForType(type);

    return !aclService.isClassShareable(klass)
        || aclService.canMakeClassPrivate(CurrentUserUtil.getCurrentUserDetails(), klass);
  }

  @Override
  public boolean canRead(IdentifiableObject identifiableObject) {
    return !aclService.isSupported(identifiableObject)
        || aclService.canRead(CurrentUserUtil.getCurrentUserDetails(), identifiableObject);
  }

  @Override
  public boolean canWrite(IdentifiableObject identifiableObject) {
    return !aclService.isSupported(identifiableObject)
        || aclService.canWrite(CurrentUserUtil.getCurrentUserDetails(), identifiableObject);
  }

  @Override
  public boolean canUpdate(IdentifiableObject identifiableObject) {
    return !aclService.isSupported(identifiableObject)
        || aclService.canUpdate(CurrentUserUtil.getCurrentUserDetails(), identifiableObject);
  }

  @Override
  public boolean canDelete(IdentifiableObject identifiableObject) {
    return !aclService.isSupported(identifiableObject)
        || aclService.canDelete(CurrentUserUtil.getCurrentUserDetails(), identifiableObject);
  }

  @Override
  public boolean canManage(IdentifiableObject identifiableObject) {
    return !aclService.isShareable(identifiableObject)
        || aclService.canManage(CurrentUserUtil.getCurrentUserDetails(), identifiableObject);
  }

  @Override
  public RecaptchaResponse verifyRecaptcha(String key, String remoteIp) throws IOException {
    MultiValueMap<String, String> params = new LinkedMultiValueMap<>();

    params.add("secret", settingsProvider.getCurrentSettings().getRecaptchaSecret());
    params.add("response", key);
    params.add("remoteip", remoteIp);

    String result = restTemplate.postForObject(RECAPTCHA_VERIFY_URL, params, String.class);

    log.info("Recaptcha result: " + result);

    return result != null ? jsonMapper.readValue(result, RecaptchaResponse.class) : null;
  }

  @Override
  public boolean canDataWrite(IdentifiableObject identifiableObject) {
    return !aclService.isSupported(identifiableObject)
        || aclService.canDataWrite(CurrentUserUtil.getCurrentUserDetails(), identifiableObject);
  }

  @Override
  public boolean canDataRead(IdentifiableObject identifiableObject) {
    return !aclService.isSupported(identifiableObject)
        || aclService.canDataRead(CurrentUserUtil.getCurrentUserDetails(), identifiableObject);
  }

  @Override
  @Transactional
  public String generateAndSetNewEmailVerificationToken(User user) {
    String token = CodeGenerator.getRandomSecureToken();
    String encodedToken = token + "|" + (System.currentTimeMillis() + EMAIL_TOKEN_EXPIRY_MILLIS);
    user.setEmailVerificationToken(encodedToken);
    updateUser(user);
    return token;
  }

  @Override
  public boolean sendEmailVerificationToken(User user, String token, String requestUrl) {
    String applicationTitle = settingsProvider.getCurrentSettings().getApplicationTitle();
    if (isEmpty(applicationTitle)) {
      applicationTitle = DEFAULT_APPLICATION_TITLE;
    }

    I18n i18n =
        i18nManager.getI18n(
            userSettingsService.getUserSettings(user.getUsername(), true).getUserUiLocale());

    Map<String, Object> vars = new HashMap<>();
    vars.put("applicationTitle", applicationTitle);
    vars.put("requestUrl", requestUrl + "/api/account/verifyEmail");
    vars.put("token", token);
    vars.put("username", user.getUsername());
    vars.put("email", user.getEmail());
    vars.put("i18n", i18n);

    VelocityManager vm = new VelocityManager();
    String messageBody = vm.render(vars, "verify_email_body_template_" + "v1");
    String messageSubject = i18n.getString("verify_email_subject");

    OutboundMessageResponse status =
        emailMessageSender.sendMessage(messageSubject, messageBody, null, null, Set.of(user), true);

    return status.getResponseObject() == EmailResponse.SENT;
  }

  @Override
  @Transactional
  public boolean verifyEmail(String token) {
    User user = getUserByEmailVerificationToken(token);
    if (user == null) {
      return false;
    }
    String[] tokenParts = user.getEmailVerificationToken().split("\\|");
    if (tokenParts.length != 2) {
      return false;
    }
    if (System.currentTimeMillis() > Long.parseLong(tokenParts[1])) {
      return false;
    }

    if (getUserByVerifiedEmail(user.getEmail()) != null) {
      return false;
    }

    user.setEmailVerificationToken(null);
    user.setVerifiedEmail(user.getEmail());
    updateUser(user);
    return true;
  }

  @Override
  @Transactional(readOnly = true)
  public User getUserByEmailVerificationToken(String token) {
    return userStore.getUserByEmailVerificationToken(token);
  }

  @Override
  public List<User> getUsersWithOrgUnits(
      @Nonnull UserOrgUnitProperty orgUnitProperty, @Nonnull Set<UID> uids) {
    return userStore.getUsersWithOrgUnit(orgUnitProperty, uids);
  }

  @Override
  public boolean isEmailVerified(User user) {
    return user.isEmailVerified();
  }

  @Override
  @Transactional(readOnly = true)
  public User getUserByVerifiedEmail(String email) {
    return userStore.getUserByVerifiedEmail(email);
  }

  @Transactional
  @Override
  public void setActiveLinkedAccounts(@Nonnull String actingUser, @Nonnull String activeUsername) {
    userStore.setActiveLinkedAccounts(actingUser, activeUsername);
  }

  @Override
  @Transactional
  public User replicateUser(
      User existingUser, String username, String password, UserDetails currentUser)
      throws ConflictException, NotFoundException, BadRequestException {

    if (!ValidationUtils.usernameIsValid(username, false)) {
      throw new ConflictException("Username is not valid");
    }

    if (getUserByUsername(username) != null) {
      throw new ConflictException("Username already taken: " + username);
    }

    Session session = entityManager.unwrap(Session.class);

    User userReplica = new User();
    metadataMergeService.merge(
        new MetadataMergeParams<>(existingUser, userReplica).setMergeMode(MergeMode.REPLACE));
    copyAttributeValues(userReplica);
    userReplica.setId(0);
    userReplica.setUuid(UUID.randomUUID());
    userReplica.setUid(CodeGenerator.generateUid());
    userReplica.setCode(null);
    userReplica.setCreated(new Date());
    userReplica.setCreatedBy(session.getReference(User.class, currentUser.getId()));
    userReplica.setLdapId(null);
    userReplica.setOpenId(null);
    userReplica.setUsername(username);
    userReplica.setLastLogin(null);
    encodeAndSetPassword(userReplica, password);

    addUser(userReplica);

    userGroupService.addUserToGroups(userReplica, getUids(existingUser.getGroups()), currentUser);

    UserSettings settings = userSettingsService.getUserSettings(existingUser.getUsername(), false);

    Map<String, String> map = settings.toMap();
    map.forEach(
        (key, value) -> {
          if (value != null && !value.isBlank()) {
            try {
              map.put(key, value);
            } catch (Exception e) {
              // ignore
            }
          }
        });
    //    userSettingsService.putAll(map, userReplica.getUsername());

    return userReplica;
  }

  private void copyAttributeValues(User userReplica) {
    if (userReplica.getAttributeValues().isEmpty()) return;

    List<String> uniqueAttributeIds =
        attributeService.getAttributesByIds(userReplica.getAttributeValues().keys()).stream()
            .filter(Attribute::isUnique)
            .map(Attribute::getUid)
            .toList();

    userReplica.setAttributeValues(
        userReplica.getAttributeValues().removedAll(uniqueAttributeIds::contains));
  }
}
