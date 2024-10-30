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
package org.hisp.dhis.dxf2.metadata.objectbundle.hooks;

import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.adapter.BaseIdentifiableObject_;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundle;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.fileresource.FileResource;
import org.hisp.dhis.fileresource.FileResourceService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.preheat.PreheatIdentifier;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.system.util.ValidationUtils;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserRole;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.user.UserSettingsService;
import org.springframework.stereotype.Component;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Component
@AllArgsConstructor
public class UserObjectBundleHook extends AbstractObjectBundleHook<User> {
  public static final String USERNAME = "username";
  public static final String INVALIDATE_SESSIONS_KEY = "shouldInvalidateUserSessions";
  public static final String PRE_UPDATE_USER_KEY = "preUpdateUser";

  private final UserService userService;

  private final FileResourceService fileResourceService;

  private final AclService aclService;

  private final UserSettingsService userSettingsService;

  private final DhisConfigurationProvider dhisConfig;

  @Override
  public void validate(User user, ObjectBundle bundle, Consumer<ErrorReport> addReports) {
    if (bundle.getImportMode().isCreate() && !ValidationUtils.isValidUid(user.getUid())) {
      addReports.accept(
          new ErrorReport(User.class, ErrorCode.E4014, user.getUid(), "uid")
              .setErrorProperty("uid"));
    }

    if (bundle.getImportMode().isCreate()
        && !ValidationUtils.usernameIsValid(user.getUsername(), user.isInvitation())) {
      addReports.accept(
          new ErrorReport(User.class, ErrorCode.E4049, USERNAME, user.getUsername())
              .setErrorProperty(USERNAME));
    }

    boolean usernameExists = userService.getUserByUsername(user.getUsername()) != null;
    if ((bundle.getImportMode().isCreate() && usernameExists)) {
      addReports.accept(
          new ErrorReport(User.class, ErrorCode.E4054, USERNAME, user.getUsername())
              .setErrorProperty(USERNAME));
    }

    User existingUser = userService.getUser(user.getUid());
    if (bundle.getImportMode().isUpdate()
        && existingUser != null
        && user.getUsername() != null
        && !user.getUsername().equals(existingUser.getUsername())) {
      addReports.accept(
          new ErrorReport(User.class, ErrorCode.E4056, USERNAME, user.getUsername())
              .setErrorProperty(USERNAME));
    }

    User existingUserWithMatchingOpenID = userService.getUserByOpenId(user.getOpenId());
    boolean linkedAccountsDisabled =
        dhisConfig.isDisabled(ConfigurationKey.LINKED_ACCOUNTS_ENABLED);
    if (linkedAccountsDisabled
        && (existingUserWithMatchingOpenID != null
            && !existingUserWithMatchingOpenID.getUid().equals(user.getUid()))) {
      addReports.accept(
          new ErrorReport(User.class, ErrorCode.E4054, "OIDC mapping value", user.getOpenId())
              .setErrorProperty(USERNAME));
    }

    if (user.getUserRoles() == null || user.getUserRoles().isEmpty()) {
      addReports.accept(
          new ErrorReport(User.class, ErrorCode.E4055, USERNAME, user.getUsername())
              .setErrorProperty(USERNAME));
    }

    if (user.getWhatsApp() != null && !ValidationUtils.validateWhatsApp(user.getWhatsApp())) {
      addReports.accept(
          new ErrorReport(User.class, ErrorCode.E4027, user.getWhatsApp(), "whatsApp")
              .setErrorProperty("whatsApp"));
    }
  }

  @Override
  public void preCreate(User user, ObjectBundle bundle) {
    if (user == null) return;

    User currentUser = userService.getUserByUsername(CurrentUserUtil.getCurrentUsername());

    if (currentUser != null) {
      user.getCogsDimensionConstraints().addAll(currentUser.getCogsDimensionConstraints());
      user.getCatDimensionConstraints().addAll(currentUser.getCatDimensionConstraints());
    }
  }

  @Override
  public void postCreate(User user, ObjectBundle bundle) {
    if (!StringUtils.isEmpty(user.getPassword())) {
      userService.encodeAndSetPassword(user, user.getPassword());
    }

    if (user.getAvatar() != null) {
      FileResource fileResource = fileResourceService.getFileResource(user.getAvatar().getUid());
      fileResource.setAssigned(true);
      fileResourceService.updateFileResource(fileResource);
    }

    preheatService.connectReferences(user, bundle.getPreheat(), bundle.getPreheatIdentifier());
    getSession().update(user);
    updateUserSettings(user);
  }

  @Override
  public void preUpdate(User user, User persisted, ObjectBundle bundle) {
    if (user == null) return;

    bundle.putExtras(user, PRE_UPDATE_USER_KEY, user);
    bundle.putExtras(persisted, INVALIDATE_SESSIONS_KEY, userRolesUpdated(user, persisted));

    if (persisted.getAvatar() != null
        && (user.getAvatar() == null
            || !persisted.getAvatar().getUid().equals(user.getAvatar().getUid()))) {
      FileResource fileResource =
          fileResourceService.getFileResource(persisted.getAvatar().getUid());
      fileResourceService.updateFileResource(fileResource);

      if (user.getAvatar() != null) {
        fileResource = fileResourceService.getFileResource(user.getAvatar().getUid());
        fileResource.setAssigned(true);
        fileResourceService.updateFileResource(fileResource);
      }
    }
  }

  private Boolean userRolesUpdated(User preUpdateUser, User persistedUser) {
    Set<String> before =
        preUpdateUser.getUserRoles().stream().map(UserRole::getUid).collect(Collectors.toSet());
    Set<String> after =
        persistedUser.getUserRoles().stream().map(UserRole::getUid).collect(Collectors.toSet());

    return !Objects.equals(before, after);
  }

  @Override
  public void postUpdate(User persistedUser, ObjectBundle bundle) {
    final User preUpdateUser = (User) bundle.getExtras(persistedUser, PRE_UPDATE_USER_KEY);
    final Boolean invalidateSessions =
        (Boolean) bundle.getExtras(persistedUser, INVALIDATE_SESSIONS_KEY);

    if (!StringUtils.isEmpty(preUpdateUser.getPassword())) {
      userService.encodeAndSetPassword(persistedUser, preUpdateUser.getPassword());
      getSession().update(persistedUser);
    }

    updateUserSettings(persistedUser);

    if (Boolean.TRUE.equals(invalidateSessions)) {
      userService.invalidateUserSessions(persistedUser.getUid());
    }

    bundle.removeExtras(persistedUser, PRE_UPDATE_USER_KEY);
    bundle.removeExtras(persistedUser, INVALIDATE_SESSIONS_KEY);
  }

  @Override
  @SuppressWarnings("unchecked")
  public void postCommit(ObjectBundle bundle) {
    Iterable<User> objects = bundle.getObjects(User.class);
    Map<String, Map<String, Object>> userReferences = bundle.getObjectReferences(User.class);

    if (userReferences == null || userReferences.isEmpty()) {
      return;
    }

    for (User identifiableObject : objects) {
      User user = identifiableObject;

      user = bundle.getPreheat().get(bundle.getPreheatIdentifier(), user);

      Map<String, Object> userReferenceMap = userReferences.get(identifiableObject.getUid());

      if (user == null || userReferenceMap == null || userReferenceMap.isEmpty()) {
        continue;
      }

      Set<UserRole> userRoles = (Set<UserRole>) userReferenceMap.get("userRoles");
      user.setUserRoles(Objects.requireNonNullElseGet(userRoles, HashSet::new));

      Set<OrganisationUnit> organisationUnits =
          (Set<OrganisationUnit>) userReferenceMap.get("organisationUnits");
      user.setOrganisationUnits(organisationUnits);

      Set<OrganisationUnit> dataViewOrganisationUnits =
          (Set<OrganisationUnit>) userReferenceMap.get("dataViewOrganisationUnits");
      user.setDataViewOrganisationUnits(dataViewOrganisationUnits);

      Set<OrganisationUnit> teiSearchOrganisationUnits =
          (Set<OrganisationUnit>) userReferenceMap.get("teiSearchOrganisationUnits");
      user.setTeiSearchOrganisationUnits(teiSearchOrganisationUnits);

      user.setCreatedBy((User) userReferenceMap.get(BaseIdentifiableObject_.CREATED_BY));

      if (user.getCreatedBy() == null) {
        user.setCreatedBy(bundle.getUser());
      }

      user.setLastUpdatedBy(bundle.getUser());

      preheatService.connectReferences(user, bundle.getPreheat(), bundle.getPreheatIdentifier());

      handleNoAccessRoles(user, bundle, userRoles);
      getSession().update(user);
    }
  }

  /**
   * If currentUser doesn't have read access to a UserRole, and it is included in the payload, then
   * that UserRole should not be removed from updating User.
   *
   * @param user the updating User.
   * @param bundle the ObjectBundle.
   */
  private void handleNoAccessRoles(User user, ObjectBundle bundle, Set<UserRole> userRoles) {
    Set<UserRole> roles = user.getUserRoles();
    Set<String> currentRoles =
        roles.stream().map(IdentifiableObject::getUid).collect(Collectors.toSet());

    if (userRoles != null) {
      userRoles.stream()
          .filter(role -> !currentRoles.contains(role.getUid()))
          .forEach(
              role -> {
                UserRole persistedRole = bundle.getPreheat().get(PreheatIdentifier.UID, role);

                if (persistedRole == null) {
                  persistedRole = manager.getNoAcl(UserRole.class, role.getUid());
                }

                if (!aclService.canRead(bundle.getUser(), persistedRole)) {
                  roles.add(persistedRole);
                }
              });
    }
  }

  private void updateUserSettings(User user) {
    Map<String, String> settings = user.getSettings();
    if (settings == null) return;
    try {
      userSettingsService.putAll(settings, user.getUsername());
    } catch (NotFoundException | ConflictException | BadRequestException ex) {
      // this should never happen as this key-value combination should be valid and the user does
      // exist
      throw new IllegalArgumentException(ex);
    }
  }
}
