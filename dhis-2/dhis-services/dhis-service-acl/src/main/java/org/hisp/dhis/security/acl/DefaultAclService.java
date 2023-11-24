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
package org.hisp.dhis.security.acl;

import static org.springframework.util.CollectionUtils.containsAny;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.commons.collection.CollectionUtils;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.hibernate.HibernateProxyUtils;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.security.AuthorityType;
import org.hisp.dhis.security.acl.AccessStringHelper.Permission;
import org.hisp.dhis.user.CurrentUserDetails;
import org.hisp.dhis.user.CurrentUserDetailsImpl;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserRetrievalStore;
import org.hisp.dhis.user.sharing.Sharing;
import org.hisp.dhis.user.sharing.UserAccess;
import org.hisp.dhis.user.sharing.UserGroupAccess;
import org.springframework.stereotype.Service;

/**
 * Default ACL implementation that uses SchemaDescriptors to get authorities / sharing flags.
 *
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@RequiredArgsConstructor
@Service("org.hisp.dhis.security.acl.AclService")
public class DefaultAclService implements AclService {

  private final SchemaService schemaService;
  private final UserRetrievalStore userRetrievalStore;

  private CurrentUserDetails getCurrentUserImpl(String username) {
    String currentUsername = CurrentUserUtil.getCurrentUsername();
    if (currentUsername != null && !currentUsername.equals(username)) {
      User currentUser = userRetrievalStore.getUserByUsername(username);
      return CurrentUserDetailsImpl.fromUser(currentUser);
    }
    return CurrentUserUtil.getCurrentUserDetails();
  }

  @Override
  public boolean isSupported(String type) {
    return schemaService.getSchemaBySingularName(type) != null;
  }

  @Override
  public boolean isSupported(IdentifiableObject object) {
    return schemaService.getSchema(HibernateProxyUtils.getRealClass(object)) != null;
  }

  @Override
  public boolean isShareable(String type) {
    Schema schema = schemaService.getSchemaBySingularName(type);
    return schema != null && schema.isShareable();
  }

  @Override
  public boolean isShareable(IdentifiableObject object) {
    Schema schema = schemaService.getSchema(HibernateProxyUtils.getRealClass(object));
    return schema != null && schema.isShareable();
  }

  @Override
  public <T extends IdentifiableObject> boolean isClassShareable(Class<T> klass) {
    Schema schema = schemaService.getSchema(klass);
    return schema != null && schema.isShareable();
  }

  @Override
  @SuppressWarnings("unchecked")
  public boolean isDataShareable(IdentifiableObject object) {
    return isDataClassShareable(HibernateProxyUtils.getRealClass(object));
  }

  @Override
  public <T extends IdentifiableObject> boolean isDataClassShareable(Class<T> klass) {
    Schema schema = schemaService.getSchema(klass);
    return schema != null && schema.isDataShareable();
  }

  public boolean canRead(User user, IdentifiableObject object) {
    return canRead(user.getUsername(), object);
  }

  @Override
  @SuppressWarnings("unchecked")
  public boolean canRead(String username, IdentifiableObject object) {
    return object == null || canRead(username, object, HibernateProxyUtils.getRealClass(object));
  }

  @Override
  public <T extends IdentifiableObject> boolean canRead(
      String username, T object, Class<? extends T> objType) {
    if (readWriteCommonCheck(username, objType)) {
      return true;
    }
    CurrentUserDetails currentUserImpl = getCurrentUserImpl(username);

    Schema schema = schemaService.getSchema(objType);

    if (canAccess(
        currentUserImpl.getAllAuthorities(), schema.getAuthorityByType(AuthorityType.READ))) {
      if (object instanceof CategoryOptionCombo) {
        return checkOptionComboSharingPermission(username, object, Permission.READ);
      }

      if (!schema.isShareable()
          || object.getSharing().getPublicAccess() == null
          || checkMetadataSharingPermission(username, object, Permission.READ)) {
        return true;
      }
    } else {
      return false;
    }

    return false;
  }

  @Override
  @SuppressWarnings("unchecked")
  public boolean canDataRead(String username, IdentifiableObject object) {
    return object == null
        || canDataRead(username, object, HibernateProxyUtils.getRealClass(object));
  }

  private <T extends IdentifiableObject> boolean canDataRead(
      String username, T object, Class<? extends T> objType) {
    CurrentUserDetails currentUserImpl = getCurrentUserImpl(username);
    if (readWriteCommonCheck(username, objType)) {
      return true;
    }

    Schema schema = schemaService.getSchema(objType);

    if (canAccess(
        currentUserImpl.getAllAuthorities(), schema.getAuthorityByType(AuthorityType.DATA_READ))) {
      if (object instanceof CategoryOptionCombo) {
        return checkOptionComboSharingPermission(username, object, Permission.DATA_READ)
            || checkOptionComboSharingPermission(username, object, Permission.DATA_WRITE);
      }

      if (schema.isDataShareable()
          && (checkSharingPermission(username, object, Permission.DATA_READ)
              || checkSharingPermission(username, object, Permission.DATA_WRITE))) {
        return true;
      }
    }

    return false;
  }

  @Override
  public boolean canDataOrMetadataRead(String username, IdentifiableObject object) {
    Schema schema = schemaService.getSchema(HibernateProxyUtils.getRealClass(object));

    return schema.isDataShareable() ? canDataRead(username, object) : canRead(username, object);
  }

  public boolean canWrite(User user, IdentifiableObject object) {
    return canWrite(user.getUsername(), object);
  }

  @Override
  @SuppressWarnings("unchecked")
  public boolean canWrite(String username, IdentifiableObject object) {
    return object == null || canWrite(username, object, HibernateProxyUtils.getRealClass(object));
  }

  private <T extends IdentifiableObject> boolean canWrite(
      String username, T object, Class<? extends T> objType) {
    if (readWriteCommonCheck(username, objType)) {
      return true;
    }
    CurrentUserDetails currentUserImpl = getCurrentUserImpl(username);

    Schema schema = schemaService.getSchema(objType);

    List<String> anyAuthorities = new ArrayList<>(schema.getAuthorityByType(AuthorityType.CREATE));

    if (anyAuthorities.isEmpty()) {
      anyAuthorities.addAll(schema.getAuthorityByType(AuthorityType.CREATE_PRIVATE));
      anyAuthorities.addAll(schema.getAuthorityByType(AuthorityType.CREATE_PUBLIC));
    }

    if (canAccess(currentUserImpl.getAllAuthorities(), anyAuthorities)) {
      if (object instanceof CategoryOptionCombo) {
        return checkOptionComboSharingPermission(username, object, Permission.WRITE);
      }
      return writeCommonCheck(schema, username, object, objType);
    } else
      return schema.isImplicitPrivateAuthority() && checkSharingAccess(username, object, objType);
  }

  public boolean canDataWrite(User user, IdentifiableObject object) {
    return canDataWrite(user.getUsername(), object);
  }

  @Override
  @SuppressWarnings("unchecked")
  public boolean canDataWrite(String username, IdentifiableObject object) {
    return object == null
        || canDataWrite(username, object, HibernateProxyUtils.getRealClass(object));
  }

  private <T extends IdentifiableObject> boolean canDataWrite(
      String username, T object, Class<? extends T> objType) {
    if (readWriteCommonCheck(username, objType)) {
      return true;
    }
    CurrentUserDetails currentUserImpl = getCurrentUserImpl(username);

    Schema schema = schemaService.getSchema(objType);

    // returned unmodifiable list does not need to be cloned since it is not
    // modified
    List<String> anyAuthorities = schema.getAuthorityByType(AuthorityType.DATA_CREATE);

    if (canAccess(currentUserImpl.getAllAuthorities(), anyAuthorities)) {
      if (object instanceof CategoryOptionCombo) {
        return checkOptionComboSharingPermission(username, object, Permission.DATA_WRITE);
      }

      if (schema.isDataShareable()
          && checkSharingPermission(username, object, Permission.DATA_WRITE)) {
        return true;
      }
    }

    return false;
  }

  public boolean canUpdate(User user, IdentifiableObject object) {
    return canUpdate(user.getUsername(), object);
  }

  @Override
  @SuppressWarnings("unchecked")
  public boolean canUpdate(String username, IdentifiableObject object) {
    return object == null || canUpdate(username, object, HibernateProxyUtils.getRealClass(object));
  }

  private <T extends IdentifiableObject> boolean canUpdate(
      String username, T object, Class<? extends T> objType) {
    if (readWriteCommonCheck(username, objType)) {
      return true;
    }

    CurrentUserDetails currentUserImpl = getCurrentUserImpl(username);
    Schema schema = schemaService.getSchema(objType);

    List<String> anyAuthorities = new ArrayList<>(schema.getAuthorityByType(AuthorityType.UPDATE));

    if (anyAuthorities.isEmpty()) {
      anyAuthorities.addAll(schema.getAuthorityByType(AuthorityType.CREATE));
      anyAuthorities.addAll(schema.getAuthorityByType(AuthorityType.CREATE_PRIVATE));
      anyAuthorities.addAll(schema.getAuthorityByType(AuthorityType.CREATE_PUBLIC));
    }

    if (canAccess(currentUserImpl.getAllAuthorities(), anyAuthorities)) {
      return writeCommonCheck(schema, username, object, objType);
    } else
      return schema.isImplicitPrivateAuthority()
          && checkSharingAccess(username, object, objType)
          && (checkMetadataSharingPermission(username, object, Permission.WRITE));
  }

  @Override
  @SuppressWarnings("unchecked")
  public boolean canDelete(String username, IdentifiableObject object) {
    return object == null || canDelete(username, object, HibernateProxyUtils.getRealClass(object));
  }

  private <T extends IdentifiableObject> boolean canDelete(
      String username, T object, Class<? extends T> objType) {
    if (readWriteCommonCheck(username, objType)) {
      return true;
    }

    CurrentUserDetails currentUserImpl = getCurrentUserImpl(username);
    Schema schema = schemaService.getSchema(objType);

    List<String> anyAuthorities = new ArrayList<>(schema.getAuthorityByType(AuthorityType.DELETE));

    if (anyAuthorities.isEmpty()) {
      anyAuthorities.addAll(schema.getAuthorityByType(AuthorityType.CREATE));
      anyAuthorities.addAll(schema.getAuthorityByType(AuthorityType.CREATE_PRIVATE));
      anyAuthorities.addAll(schema.getAuthorityByType(AuthorityType.CREATE_PUBLIC));
    }

    if (canAccess(currentUserImpl.getAllAuthorities(), anyAuthorities)) {
      if (!schema.isShareable() || object.getSharing().getPublicAccess() == null) {
        return true;
      }

      if (checkSharingAccess(username, object, objType)
          && (checkMetadataSharingPermission(username, object, Permission.WRITE))) {
        return true;
      }
    } else if (schema.isImplicitPrivateAuthority()
        && (checkMetadataSharingPermission(username, object, Permission.WRITE))) {
      return true;
    }

    return false;
  }

  @Override
  public boolean canManage(String username, IdentifiableObject object) {
    return canUpdate(username, object);
  }

  private <T extends IdentifiableObject> boolean canManage(
      String username, T object, Class<? extends T> objType) {
    return canUpdate(username, object, objType);
  }

  @Override
  public <T extends IdentifiableObject> boolean canRead(String username, Class<T> klass) {
    Schema schema = schemaService.getSchema(klass);

    CurrentUserDetails currentUserImpl = getCurrentUserImpl(username);

    return schema == null
        || schema.getAuthorityByType(AuthorityType.READ) == null
        || canAccess(
            currentUserImpl.getAllAuthorities(), schema.getAuthorityByType(AuthorityType.READ));
  }

  @Override
  public <T extends IdentifiableObject> boolean canCreate(String username, Class<T> klass) {
    Schema schema = schemaService.getSchema(klass);

    if (schema == null) {
      return false;
    }

    CurrentUserDetails currentUserImpl = getCurrentUserImpl(username);

    if (!schema.isShareable()) {
      return canAccess(
          currentUserImpl.getAllAuthorities(), schema.getAuthorityByType(AuthorityType.CREATE));
    }

    return canMakeClassPublic(username, klass) || canMakeClassPrivate(username, klass);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T extends IdentifiableObject> boolean canMakePublic(String username, T object) {
    return canMakeClassPublic(username, HibernateProxyUtils.getRealClass(object));
  }

  @Override
  public <T extends IdentifiableObject> boolean canMakeClassPublic(
      String username, Class<T> klass) {
    Schema schema = schemaService.getSchema(klass);

    if (schema == null || !schema.isShareable()) return false;

    CurrentUserDetails currentUserImpl = getCurrentUserImpl(username);
    return canAccess(
        currentUserImpl.getAllAuthorities(),
        schema.getAuthorityByType(AuthorityType.CREATE_PUBLIC));
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T extends IdentifiableObject> boolean canMakePrivate(String username, T object) {
    return canMakeClassPrivate(username, HibernateProxyUtils.getRealClass(object));
  }

  @Override
  public <T extends IdentifiableObject> boolean canMakeClassPrivate(
      String username, Class<T> klass) {
    Schema schema = schemaService.getSchema(klass);
    CurrentUserDetails currentUserImpl = getCurrentUserImpl(username);
    return !(schema == null || !schema.isShareable())
        && canAccess(
            currentUserImpl.getAllAuthorities(),
            schema.getAuthorityByType(AuthorityType.CREATE_PRIVATE));
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T extends IdentifiableObject> boolean canMakeExternal(String username, T object) {
    return canMakeClassExternal(username, HibernateProxyUtils.getRealClass(object));
  }

  @Override
  public <T extends IdentifiableObject> boolean canMakeClassExternal(
      String username, Class<T> klass) {
    Schema schema = schemaService.getSchema(klass);
    CurrentUserDetails currentUserImpl = getCurrentUserImpl(username);
    return !(schema == null || !schema.isShareable())
        && ((!schema.getAuthorityByType(AuthorityType.EXTERNALIZE).isEmpty()
                && haveOverrideAuthority(currentUserImpl))
            || haveAuthority(
                currentUserImpl.getAllAuthorities(),
                schema.getAuthorityByType(AuthorityType.EXTERNALIZE)));
  }

  @Override
  public <T extends IdentifiableObject> boolean defaultPrivate(Class<T> klass) {
    Schema schema = schemaService.getSchema(klass);
    return schema != null && schema.isDefaultPrivate();
  }

  @Override
  @SuppressWarnings({"unchecked"})
  public <T extends IdentifiableObject> boolean defaultPublic(T object) {
    return !defaultPrivate(HibernateProxyUtils.getRealClass(object));
  }

  @Override
  @SuppressWarnings("unchecked")
  public Class<? extends IdentifiableObject> classForType(String type) {
    Schema schema = schemaService.getSchemaBySingularName(type);

    if (schema != null && schema.isIdentifiableObject()) {
      return (Class<? extends IdentifiableObject>) schema.getKlass();
    }

    return null;
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T extends IdentifiableObject> Access getAccess(T object, String username) {
    return object == null
        ? new Access(true)
        : getAccess(object, username, HibernateProxyUtils.getRealClass(object));
  }

  @Override
  public <T extends IdentifiableObject> Access getAccess(
      T object, String username, Class<? extends T> objType) {

    if (username == null || getCurrentUserImpl(username).isSuper()) {
      Access access = new Access(true);

      if (isDataClassShareable(objType)) {
        access.setData(new AccessData(true, true));
      }

      return access;
    }

    Access access = new Access();
    access.setManage(canManage(username, object, objType));
    access.setExternalize(canMakeClassExternal(username, objType));
    access.setWrite(canWrite(username, object, objType));
    access.setRead(canRead(username, object, objType));
    access.setUpdate(canUpdate(username, object, objType));
    access.setDelete(canDelete(username, object, objType));

    if (isDataClassShareable(objType)) {
      AccessData data =
          new AccessData(
              canDataRead(username, object, objType), canDataWrite(username, object, objType));

      access.setData(data);
    }

    return access;
  }

  @Override
  public <T extends IdentifiableObject> void resetSharing(T object, String username) {
    if (object == null || !isShareable(object) || username == null) {
      return;
    }

    CurrentUserDetails currentUserImpl = getCurrentUserImpl(username);

    Sharing sharing = object.getSharing();
    sharing.setPublicAccess(AccessStringHelper.DEFAULT);
    sharing.setExternal(false);

    if (object.getSharing().getOwner() == null) {
      sharing.setOwner(currentUserImpl.getUid());
    }

    if (canMakePublic(username, object) && defaultPublic(object)) {
      sharing.setPublicAccess(AccessStringHelper.READ_WRITE);
    }
    sharing.resetUserAccesses();
    sharing.resetUserGroupAccesses();
  }

  @Override
  public <T extends IdentifiableObject> void clearSharing(T object, String username) {
    if (object == null || !isShareable(object) || username == null) {
      return;
    }

    CurrentUserDetails currentUserImpl = getCurrentUserImpl(username);

    Sharing sharing = object.getSharing();
    sharing.setOwner(currentUserImpl.getUid());
    sharing.setPublicAccess(AccessStringHelper.DEFAULT);
    sharing.setExternal(false);
    sharing.resetUserAccesses();
    sharing.resetUserGroupAccesses();
  }

  @Override
  public <T extends IdentifiableObject> List<ErrorReport> verifySharing(T object, String username) {
    List<ErrorReport> errorReports = new ArrayList<>();

    CurrentUserDetails currentUserImpl = getCurrentUserImpl(username);

    if (object == null || haveOverrideAuthority(currentUserImpl) || !isShareable(object)) {
      return errorReports;
    }

    if (!AccessStringHelper.isValid(object.getSharing().getPublicAccess())) {
      errorReports.add(
          new ErrorReport(
              object.getClass(), ErrorCode.E3010, object.getSharing().getPublicAccess()));
      return errorReports;
    }

    Schema schema = schemaService.getSchema(HibernateProxyUtils.getRealClass(object));

    if (!schema.isDataShareable()) {
      ErrorReport errorReport = null;

      if (object.getSharing().getPublicAccess() != null
          && AccessStringHelper.hasDataSharing(object.getSharing().getPublicAccess())) {
        errorReport = new ErrorReport(object.getClass(), ErrorCode.E3011, object.getClass());
      } else {
        for (UserAccess userAccess : object.getSharing().getUsers().values()) {
          if (AccessStringHelper.hasDataSharing(userAccess.getAccess())) {
            errorReport = new ErrorReport(object.getClass(), ErrorCode.E3011, object.getClass());
            break;
          }
        }

        for (UserGroupAccess userGroupAccess : object.getSharing().getUserGroups().values()) {
          if (AccessStringHelper.hasDataSharing(userGroupAccess.getAccess())) {
            errorReport = new ErrorReport(object.getClass(), ErrorCode.E3011, object.getClass());
            break;
          }
        }
      }

      if (errorReport != null) {
        errorReports.add(errorReport);
      }
    }

    boolean canMakePublic = canMakePublic(username, object);
    boolean canMakePrivate = canMakePrivate(username, object);
    boolean canMakeExternal = canMakeExternal(username, object);

    if (object.getSharing().isExternal()) {
      if (!canMakeExternal) {
        errorReports.add(
            new ErrorReport(object.getClass(), ErrorCode.E3006, username, object.getClass()));
      }
    }

    errorReports.addAll(verifyImplicitSharing(username, object));

    if (AccessStringHelper.DEFAULT.equals(object.getSharing().getPublicAccess())) {
      if (canMakePublic || canMakePrivate) {
        return errorReports;
      }

      errorReports.add(
          new ErrorReport(object.getClass(), ErrorCode.E3009, username, object.getClass()));
    } else {
      if (canMakePublic) {
        return errorReports;
      }

      errorReports.add(
          new ErrorReport(object.getClass(), ErrorCode.E3008, username, object.getClass()));
    }

    return errorReports;
  }

  private <T extends IdentifiableObject> Collection<? extends ErrorReport> verifyImplicitSharing(
      String username, T object) {
    List<ErrorReport> errorReports = new ArrayList<>();
    Schema schema = schemaService.getSchema(HibernateProxyUtils.getRealClass(object));

    if (!schema.isImplicitPrivateAuthority()
        || checkMetadataSharingPermission(username, object, Permission.WRITE)) {
      return errorReports;
    }

    if (AccessStringHelper.DEFAULT.equals(object.getSharing().getPublicAccess())) {
      errorReports.add(
          new ErrorReport(object.getClass(), ErrorCode.E3001, username, object.getClass()));
    }

    return errorReports;
  }

  private boolean haveOverrideAuthority(CurrentUserDetails user) {
    return user == null || user.isSuper();
  }

  private boolean canAccess(Set<String> userAuthorities, Collection<String> anyAuthorities) {
    return userAuthorities.contains("ALL")
        || anyAuthorities.isEmpty()
        || haveAuthority(userAuthorities, anyAuthorities);
  }

  private boolean haveAuthority(Set<String> userAuthorities, Collection<String> anyAuthorities) {
    return containsAny(userAuthorities, anyAuthorities);
  }

  /**
   * Should user be allowed access to this object as the owner.
   *
   * @param username to check against
   * @param object Object to check against
   * @return true/false depending on if access should be allowed
   */
  private boolean checkOwner(String username, IdentifiableObject object) {
    CurrentUserDetails currentUserImpl = getCurrentUserImpl(username);
    return username == null
        || object.getSharing().getOwner() == null
        || currentUserImpl.getUid().equals(object.getSharing().getOwner());
  }

  /**
   * Is the current user allowed to create/update the object given based on its sharing settings.
   *
   * @param username User to check against
   * @param object Object to check against
   * @return true/false depending on if sharing settings are allowed for given user
   */
  private <T extends IdentifiableObject> boolean checkSharingAccess(
      String username, IdentifiableObject object, Class<T> objType) {
    boolean canMakePublic = canMakeClassPublic(username, objType);
    boolean canMakePrivate = canMakeClassPrivate(username, objType);
    boolean canMakeExternal = canMakeClassExternal(username, objType);

    if (AccessStringHelper.DEFAULT.equals(object.getSharing().getPublicAccess())) {
      if (!(canMakePublic || canMakePrivate)) {
        return false;
      }
    } else {
      if (!canMakePublic) {
        return false;
      }
    }

    if (object.getSharing().isExternal() && !canMakeExternal) {
      return false;
    }

    return true;
  }

  /**
   * Check if given user allowed to access given object using the permission given. If user is the
   * owner of the given metadata object then user has both READ and WRITE permission by default.
   *
   * @param username
   * @param object
   * @param permission
   * @return
   */
  private boolean checkMetadataSharingPermission(
      String username, IdentifiableObject object, Permission permission) {
    return checkOwner(username, object) || checkSharingPermission(username, object, permission);
  }

  /**
   * If the given user allowed to access the given object using the permissions given.
   *
   * @param username to check against
   * @param object Object to check against
   * @param permission Permission to check against
   * @return true if user can access object, false otherwise
   */
  private boolean checkSharingPermission(
      String username, IdentifiableObject object, Permission permission) {

    Sharing sharing = object.getSharing();
    if (AccessStringHelper.isEnabled(sharing.getPublicAccess(), permission)) {
      return true;
    }

    CurrentUserDetails currentUserImpl = getCurrentUserImpl(username);

    if (sharing.getUserGroups() != null
        && !CollectionUtils.isEmpty(currentUserImpl.getAllAuthorities())) {
      for (UserGroupAccess userGroupAccess : sharing.getUserGroups().values()) {
        // Check if user is allowed to read this object through group
        // access
        if (AccessStringHelper.isEnabled(userGroupAccess.getAccess(), permission)
            && hasUserGroupAccess(currentUserImpl.getUserGroupIds(), userGroupAccess.getId())) {
          return true;
        }
      }
    }

    if (sharing.getUsers() != null) {
      for (UserAccess userAccess : sharing.getUsers().values()) {
        // Check if user is allowed to read to this object through user
        // access

        if (AccessStringHelper.isEnabled(userAccess.getAccess(), permission)
            && currentUserImpl.getUid().equals(userAccess.getId())) {
          return true;
        }
      }
    }

    return false;
  }

  private boolean checkOptionComboSharingPermission(
      String username, IdentifiableObject object, Permission permission) {
    CategoryOptionCombo optionCombo = (CategoryOptionCombo) object;

    if (optionCombo.isDefault() || optionCombo.getCategoryOptions().isEmpty()) {
      return true;
    }

    List<Long> accessibleOptions = new ArrayList<>();

    for (CategoryOption option : optionCombo.getCategoryOptions()) {
      if (checkSharingPermission(username, option, permission)) {
        accessibleOptions.add(option.getId());
      }
    }

    return accessibleOptions.size() == optionCombo.getCategoryOptions().size();
  }

  private boolean readWriteCommonCheck(String username, Class<?> objType) {
    CurrentUserDetails currentUserImpl = getCurrentUserImpl(username);
    if (haveOverrideAuthority(currentUserImpl)) {
      return true;
    }

    return schemaService.getSchema(objType) == null;
  }

  private <T extends IdentifiableObject> boolean writeCommonCheck(
      Schema schema, String username, T object, Class<? extends T> objType) {
    if (!schema.isShareable()) {
      return true;
    }

    return checkSharingAccess(username, object, objType)
        && (checkMetadataSharingPermission(username, object, Permission.WRITE));
  }

  private boolean hasUserGroupAccess(Set<String> userGroups, String userGroupUid) {
    for (String groupUid : userGroups) {
      if (groupUid.equals(userGroupUid)) {
        return true;
      }
    }

    return false;
  }
}
