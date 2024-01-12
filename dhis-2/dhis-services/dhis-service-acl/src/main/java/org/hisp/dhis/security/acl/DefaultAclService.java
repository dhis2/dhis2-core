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
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserDetails;
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

  @Override
  @SuppressWarnings("unchecked")
  public boolean canRead(UserDetails userDetails, IdentifiableObject object) {
    return object == null || canRead(userDetails, object, HibernateProxyUtils.getRealClass(object));
  }

  @Override
  public boolean canRead(User user, IdentifiableObject object) {
    return canRead(UserDetails.fromUser(user), object);
  }

  @Override
  public <T extends IdentifiableObject> boolean canRead(
      UserDetails userDetails, T object, Class<? extends T> objType) {

    if (readWriteCommonCheck(userDetails, objType)) {
      return true;
    }

    Schema schema = schemaService.getSchema(objType);

    if (canAccess(userDetails, schema.getAuthorityByType(AuthorityType.READ))) {
      if (object instanceof CategoryOptionCombo) {
        return checkOptionComboSharingPermission(userDetails, object, Permission.READ);
      }

      return !schema.isShareable()
          || object.getSharing().getPublicAccess() == null
          || checkMetadataSharingPermission(userDetails, object, Permission.READ);
    } else {
      return false;
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public boolean canDataRead(UserDetails userDetails, IdentifiableObject object) {
    return object == null
        || canDataRead(userDetails, object, HibernateProxyUtils.getRealClass(object));
  }

  private <T extends IdentifiableObject> boolean canDataRead(
      UserDetails userDetails, T object, Class<? extends T> objType) {
    if (readWriteCommonCheck(userDetails, objType)) {
      return true;
    }

    Schema schema = schemaService.getSchema(objType);

    if (canAccess(userDetails, schema.getAuthorityByType(AuthorityType.DATA_READ))) {

      if (object instanceof CategoryOptionCombo) {
        return checkOptionComboSharingPermission(userDetails, object, Permission.DATA_READ)
            || checkOptionComboSharingPermission(userDetails, object, Permission.DATA_WRITE);
      } else {

        return schema.isDataShareable()
            && (checkSharingPermission(userDetails, object, Permission.DATA_READ)
                || checkSharingPermission(userDetails, object, Permission.DATA_WRITE));
      }
    }

    return false;
  }

  @Override
  public boolean canDataOrMetadataRead(User user, IdentifiableObject object) {
    return canDataOrMetadataRead(UserDetails.fromUser(user), object);
  }

  @Override
  public boolean canDataOrMetadataRead(UserDetails userDetails, IdentifiableObject object) {
    Schema schema = schemaService.getSchema(HibernateProxyUtils.getRealClass(object));

    return schema.isDataShareable()
        ? canDataRead(userDetails, object)
        : canRead(userDetails, object);
  }

  @Override
  public boolean canWrite(User user, IdentifiableObject object) {
    return canWrite(UserDetails.fromUser(user), object);
  }

  @Override
  @SuppressWarnings("unchecked")
  public boolean canWrite(UserDetails userDetails, IdentifiableObject object) {
    return object == null
        || canWrite(userDetails, object, HibernateProxyUtils.getRealClass(object));
  }

  private <T extends IdentifiableObject> boolean canWrite(
      UserDetails userDetails, T object, Class<? extends T> objType) {
    if (readWriteCommonCheck(userDetails, objType)) {
      return true;
    }

    Schema schema = schemaService.getSchema(objType);

    List<String> anyAuthorities = new ArrayList<>(schema.getAuthorityByType(AuthorityType.CREATE));

    if (anyAuthorities.isEmpty()) {
      anyAuthorities.addAll(schema.getAuthorityByType(AuthorityType.CREATE_PRIVATE));
      anyAuthorities.addAll(schema.getAuthorityByType(AuthorityType.CREATE_PUBLIC));
    }

    if (canAccess(userDetails, anyAuthorities)) {
      if (object instanceof CategoryOptionCombo) {
        return checkOptionComboSharingPermission(userDetails, object, Permission.WRITE);
      }
      return writeCommonCheck(schema, userDetails, object, objType);
    } else
      return schema.isImplicitPrivateAuthority()
          && checkSharingAccess(userDetails, object, objType);
  }

  @Override
  public boolean canDataWrite(User user, IdentifiableObject object) {
    return canDataWrite(UserDetails.fromUser(user), object);
  }

  @Override
  @SuppressWarnings("unchecked")
  public boolean canDataWrite(UserDetails userDetails, IdentifiableObject object) {
    return object == null
        || canDataWrite(userDetails, object, HibernateProxyUtils.getRealClass(object));
  }

  private <T extends IdentifiableObject> boolean canDataWrite(
      UserDetails userDetails, T object, Class<? extends T> objType) {

    if (readWriteCommonCheck(userDetails, objType)) {
      return true;
    }

    Schema schema = schemaService.getSchema(objType);

    // returned unmodifiable list does not need to be cloned since it is not
    // modified
    List<String> anyAuthorities = schema.getAuthorityByType(AuthorityType.DATA_CREATE);

    if (canAccess(userDetails, anyAuthorities)) {
      if (object instanceof CategoryOptionCombo) {
        return checkOptionComboSharingPermission(userDetails, object, Permission.DATA_WRITE);
      }

      return schema.isDataShareable()
          && checkSharingPermission(userDetails, object, Permission.DATA_WRITE);
    }

    return false;
  }

  @Override
  @SuppressWarnings("unchecked")
  public boolean canUpdate(User user, IdentifiableObject object) {
    return canUpdate(UserDetails.fromUser(user), object);
  }

  @Override
  @SuppressWarnings("unchecked")
  public boolean canUpdate(UserDetails userDetails, IdentifiableObject object) {
    return object == null
        || canUpdate(userDetails, object, HibernateProxyUtils.getRealClass(object));
  }

  private <T extends IdentifiableObject> boolean canUpdate(
      UserDetails userDetails, T object, Class<? extends T> objType) {
    if (readWriteCommonCheck(userDetails, objType)) {
      return true;
    }

    Schema schema = schemaService.getSchema(objType);

    List<String> anyAuthorities = new ArrayList<>(schema.getAuthorityByType(AuthorityType.UPDATE));

    if (anyAuthorities.isEmpty()) {
      anyAuthorities.addAll(schema.getAuthorityByType(AuthorityType.CREATE));
      anyAuthorities.addAll(schema.getAuthorityByType(AuthorityType.CREATE_PRIVATE));
      anyAuthorities.addAll(schema.getAuthorityByType(AuthorityType.CREATE_PUBLIC));
    }

    if (canAccess(userDetails, anyAuthorities)) {
      return writeCommonCheck(schema, userDetails, object, objType);
    } else
      return schema.isImplicitPrivateAuthority()
          && checkSharingAccess(userDetails, object, objType)
          && (checkMetadataSharingPermission(userDetails, object, Permission.WRITE));
  }

  @Override
  public boolean canDelete(User user, IdentifiableObject object) {
    return canDelete(UserDetails.fromUser(user), object);
  }

  @Override
  @SuppressWarnings("unchecked")
  public boolean canDelete(UserDetails userDetails, IdentifiableObject object) {
    return object == null
        || canDelete(userDetails, object, HibernateProxyUtils.getRealClass(object));
  }

  private <T extends IdentifiableObject> boolean canDelete(
      UserDetails userDetails, T object, Class<? extends T> objType) {
    if (readWriteCommonCheck(userDetails, objType)) {
      return true;
    }

    Schema schema = schemaService.getSchema(objType);

    List<String> anyAuthorities = new ArrayList<>(schema.getAuthorityByType(AuthorityType.DELETE));

    if (anyAuthorities.isEmpty()) {
      anyAuthorities.addAll(schema.getAuthorityByType(AuthorityType.CREATE));
      anyAuthorities.addAll(schema.getAuthorityByType(AuthorityType.CREATE_PRIVATE));
      anyAuthorities.addAll(schema.getAuthorityByType(AuthorityType.CREATE_PUBLIC));
    }

    if (canAccess(userDetails, anyAuthorities)) {
      if (!schema.isShareable() || object.getSharing().getPublicAccess() == null) {
        return true;
      }

      if (checkSharingAccess(userDetails, object, objType)
          && (checkMetadataSharingPermission(userDetails, object, Permission.WRITE))) {
        return true;
      }
    } else if (schema.isImplicitPrivateAuthority()
        && (checkMetadataSharingPermission(userDetails, object, Permission.WRITE))) {
      return true;
    }

    return false;
  }

  @Override
  public boolean canManage(User user, IdentifiableObject object) {
    return canManage(UserDetails.fromUser(user), object);
  }

  @Override
  public boolean canManage(UserDetails userDetails, IdentifiableObject object) {
    return canUpdate(userDetails, object);
  }

  private <T extends IdentifiableObject> boolean canManage(
      UserDetails userDetails, T object, Class<? extends T> objType) {
    return canUpdate(userDetails, object, objType);
  }

  @Override
  public <T extends IdentifiableObject> boolean canRead(UserDetails userDetails, Class<T> klass) {
    Schema schema = schemaService.getSchema(klass);
    return schema == null
        || schema.getAuthorityByType(AuthorityType.READ) == null
        || canAccess(userDetails, schema.getAuthorityByType(AuthorityType.READ));
  }

  @Override
  public boolean canDataRead(User user, IdentifiableObject object) {
    // TODO: MAS UserDetails.fromUser(user) needs further refactoring
    return canDataRead(UserDetails.fromUser(user), object);
  }

  @Override
  public <T extends IdentifiableObject> boolean canCreate(User user, Class<T> klass) {
    return canCreate(UserDetails.fromUser(user), klass);
  }

  @Override
  public <T extends IdentifiableObject> boolean canCreate(UserDetails userDetails, Class<T> klass) {
    Schema schema = schemaService.getSchema(klass);

    if (schema == null) {
      return false;
    }

    if (!schema.isShareable()) {
      return canAccess(userDetails, schema.getAuthorityByType(AuthorityType.CREATE));
    }

    return canMakeClassPublic(userDetails, klass) || canMakeClassPrivate(userDetails, klass);
  }

  @Override
  public <T extends IdentifiableObject> boolean canMakePublic(User user, T object) {
    return canMakePublic(UserDetails.fromUser(user), object);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T extends IdentifiableObject> boolean canMakePublic(UserDetails userDetails, T object) {
    return canMakeClassPublic(userDetails, HibernateProxyUtils.getRealClass(object));
  }

  @Override
  public <T extends IdentifiableObject> boolean canMakeClassPublic(User user, Class<T> klass) {
    return canMakeClassPublic(UserDetails.fromUser(user), klass);
  }

  @Override
  public <T extends IdentifiableObject> boolean canMakeClassPublic(
      UserDetails userDetails, Class<T> klass) {
    Schema schema = schemaService.getSchema(klass);

    if (schema == null || !schema.isShareable()) return false;

    return canAccess(userDetails, schema.getAuthorityByType(AuthorityType.CREATE_PUBLIC));
  }

  @Override
  public <T extends IdentifiableObject> boolean canMakePrivate(User user, T object) {
    return canMakePrivate(UserDetails.fromUser(user), object);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T extends IdentifiableObject> boolean canMakePrivate(UserDetails userDetails, T object) {
    return canMakeClassPrivate(userDetails, HibernateProxyUtils.getRealClass(object));
  }

  @Override
  public <T extends IdentifiableObject> boolean canMakeClassPrivate(User user, Class<T> klass) {
    return canMakeClassPrivate(UserDetails.fromUser(user), klass);
  }

  @Override
  public <T extends IdentifiableObject> boolean canMakeClassPrivate(
      UserDetails userDetails, Class<T> klass) {
    Schema schema = schemaService.getSchema(klass);

    return !(schema == null || !schema.isShareable())
        && canAccess(userDetails, schema.getAuthorityByType(AuthorityType.CREATE_PRIVATE));
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T extends IdentifiableObject> boolean canMakeExternal(UserDetails userDetails, T object) {
    return canMakeClassExternal(userDetails, HibernateProxyUtils.getRealClass(object));
  }

  @Override
  public <T extends IdentifiableObject> boolean canMakeClassExternal(
      UserDetails userDetails, Class<T> klass) {
    Schema schema = schemaService.getSchema(klass);

    return !(schema == null || !schema.isShareable())
        && ((!schema.getAuthorityByType(AuthorityType.EXTERNALIZE).isEmpty()
                && haveOverrideAuthority(userDetails))
            || haveAuthority(
                userDetails.getAllAuthorities(),
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
  public <T extends IdentifiableObject> Access getAccess(T object, User user) {
    return getAccess(object, UserDetails.fromUser(user));
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T extends IdentifiableObject> Access getAccess(T object, UserDetails userDetails) {
    return object == null
        ? new Access(true)
        : getAccess(object, userDetails, HibernateProxyUtils.getRealClass(object));
  }

  @Override
  public <T extends IdentifiableObject> Access getAccess(
      T object, UserDetails userDetails, Class<? extends T> objType) {

    if (userDetails == null || isSuper(userDetails)) {
      Access access = new Access(true);

      if (isDataClassShareable(objType)) {
        access.setData(new AccessData(true, true));
      }

      return access;
    }

    Access access = new Access();
    access.setManage(canManage(userDetails, object, objType));
    access.setExternalize(canMakeClassExternal(userDetails, objType));
    access.setWrite(canWrite(userDetails, object, objType));
    access.setRead(canRead(userDetails, object, objType));
    access.setUpdate(canUpdate(userDetails, object, objType));
    access.setDelete(canDelete(userDetails, object, objType));

    if (isDataClassShareable(objType)) {
      AccessData data =
          new AccessData(
              canDataRead(userDetails, object, objType),
              canDataWrite(userDetails, object, objType));

      access.setData(data);
    }

    return access;
  }

  @Override
  public <T extends IdentifiableObject> void resetSharing(T object, User user) {
    resetSharing(object, UserDetails.fromUser(user));
  }

  @Override
  public <T extends IdentifiableObject> void resetSharing(T object, UserDetails userDetails) {
    // TODO: MAS do not allow userDetails to be NULL here
    if (object == null || !isShareable(object) || userDetails == null) {
      return;
    }

    Sharing sharing = object.getSharing();
    sharing.setPublicAccess(AccessStringHelper.DEFAULT);
    sharing.setExternal(false);

    if (object.getSharing().getOwner() == null) {
      sharing.setOwner(userDetails.getUid());
    }

    if (canMakePublic(userDetails, object) && defaultPublic(object)) {
      sharing.setPublicAccess(AccessStringHelper.READ_WRITE);
    }
    sharing.resetUserAccesses();
    sharing.resetUserGroupAccesses();
  }

  @Override
  public <T extends IdentifiableObject> void clearSharing(T object, UserDetails userDetails) {
    if (object == null || !isShareable(object) || userDetails == null) {
      return;
    }
    Sharing sharing = object.getSharing();
    sharing.setOwner(userDetails.getUid());
    sharing.setPublicAccess(AccessStringHelper.DEFAULT);
    sharing.setExternal(false);
    sharing.resetUserAccesses();
    sharing.resetUserGroupAccesses();
  }

  @Override
  public <T extends IdentifiableObject> List<ErrorReport> verifySharing(T object, User user) {
    return verifySharing(object, UserDetails.fromUser(user));
  }

  @Override
  public <T extends IdentifiableObject> List<ErrorReport> verifySharing(
      T object, UserDetails userDetails) {
    List<ErrorReport> errorReports = new ArrayList<>();

    if (object == null || haveOverrideAuthority(userDetails) || !isShareable(object)) {
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

    boolean canMakePublic = canMakePublic(userDetails, object);
    boolean canMakePrivate = canMakePrivate(userDetails, object);
    boolean canMakeExternal = canMakeExternal(userDetails, object);

    if (object.getSharing().isExternal() && !canMakeExternal) {
      errorReports.add(
          new ErrorReport(object.getClass(), ErrorCode.E3006, userDetails, object.getClass()));
    }

    errorReports.addAll(verifyImplicitSharing(userDetails, object));

    if (AccessStringHelper.DEFAULT.equals(object.getSharing().getPublicAccess())) {
      if (canMakePublic || canMakePrivate) {
        return errorReports;
      }

      errorReports.add(
          new ErrorReport(object.getClass(), ErrorCode.E3009, userDetails, object.getClass()));
    } else {
      if (canMakePublic) {
        return errorReports;
      }

      errorReports.add(
          new ErrorReport(object.getClass(), ErrorCode.E3008, userDetails, object.getClass()));
    }

    return errorReports;
  }

  private <T extends IdentifiableObject> Collection<? extends ErrorReport> verifyImplicitSharing(
      UserDetails userDetails, T object) {
    List<ErrorReport> errorReports = new ArrayList<>();
    Schema schema = schemaService.getSchema(HibernateProxyUtils.getRealClass(object));

    if (!schema.isImplicitPrivateAuthority()
        || checkMetadataSharingPermission(userDetails, object, Permission.WRITE)) {
      return errorReports;
    }

    if (AccessStringHelper.DEFAULT.equals(object.getSharing().getPublicAccess())) {
      errorReports.add(
          new ErrorReport(object.getClass(), ErrorCode.E3001, userDetails, object.getClass()));
    }

    return errorReports;
  }

  private boolean haveOverrideAuthority(UserDetails user) {
    return user == null || user.isSuper();
  }

  private boolean isSuper(UserDetails user) {
    return user == null || user.isSuper();
  }

  private boolean canAccess(UserDetails user, Collection<String> anyAuthorities) {
    return haveOverrideAuthority(user)
        || anyAuthorities.isEmpty()
        || haveAuthority(user.getAllAuthorities(), anyAuthorities);
  }

  private boolean haveAuthority(Set<String> userAuthorities, Collection<String> anyAuthorities) {
    return containsAny(userAuthorities, anyAuthorities);
  }

  /**
   * Should user be allowed access to this object as the owner.
   *
   * @param userDetails to check against
   * @param object Object to check against
   * @return true/false depending on if access should be allowed
   */
  private boolean checkOwner(UserDetails userDetails, IdentifiableObject object) {
    return userDetails == null
        || object.getSharing().getOwner() == null
        || userDetails.getUid().equals(object.getSharing().getOwner());
  }

  /**
   * Is the current user allowed to create/update the object given based on its sharing settings.
   *
   * @param userDetails User to check against
   * @param object Object to check against
   * @return true/false depending on if sharing settings are allowed for given user
   */
  private <T extends IdentifiableObject> boolean checkSharingAccess(
      UserDetails userDetails, IdentifiableObject object, Class<T> objType) {
    boolean canMakePublic = canMakeClassPublic(userDetails, objType);
    boolean canMakePrivate = canMakeClassPrivate(userDetails, objType);
    boolean canMakeExternal = canMakeClassExternal(userDetails, objType);

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
   * @param userDetails
   * @param object
   * @param permission
   * @return
   */
  private boolean checkMetadataSharingPermission(
      UserDetails userDetails, IdentifiableObject object, Permission permission) {
    return checkOwner(userDetails, object)
        || checkSharingPermission(userDetails, object, permission);
  }

  /**
   * If the given user allowed to access the given object using the permissions given.
   *
   * @param userDetails to check against
   * @param object Object to check against
   * @param permission Permission to check against
   * @return true if user can access object, false otherwise
   */
  private boolean checkSharingPermission(
      UserDetails userDetails, IdentifiableObject object, Permission permission) {
    // throw null pointer if userDetails is null
    if (userDetails == null) {
      throw new IllegalArgumentException("userDetails is null");
    }

    Sharing sharing = object.getSharing();
    if (AccessStringHelper.isEnabled(sharing.getPublicAccess(), permission)) {
      return true;
    }

    if (sharing.getUserGroups() != null
        && !CollectionUtils.isEmpty(userDetails.getUserGroupIds())) {
      for (UserGroupAccess userGroupAccess : sharing.getUserGroups().values()) {
        // Check if user is allowed to read this object through group
        // access
        if (AccessStringHelper.isEnabled(userGroupAccess.getAccess(), permission)
            && hasUserGroupAccess(userDetails.getUserGroupIds(), userGroupAccess.getId())) {
          return true;
        }
      }
    }

    if (sharing.getUsers() != null) {
      for (UserAccess userAccess : sharing.getUsers().values()) {
        // Check if user is allowed to read to this object through user
        // access

        if (AccessStringHelper.isEnabled(userAccess.getAccess(), permission)
            && userDetails.getUid().equals(userAccess.getId())) {
          return true;
        }
      }
    }

    return false;
  }

  private boolean checkOptionComboSharingPermission(
      UserDetails userDetails, IdentifiableObject object, Permission permission) {
    CategoryOptionCombo optionCombo = (CategoryOptionCombo) object;

    if (optionCombo.isDefault() || optionCombo.getCategoryOptions().isEmpty()) {
      return true;
    }

    List<Long> accessibleOptions = new ArrayList<>();

    for (CategoryOption option : optionCombo.getCategoryOptions()) {
      if (checkSharingPermission(userDetails, option, permission)) {
        accessibleOptions.add(option.getId());
      }
    }

    return accessibleOptions.size() == optionCombo.getCategoryOptions().size();
  }

  private boolean readWriteCommonCheck(UserDetails userDetails, Class<?> objType) {
    if (haveOverrideAuthority(userDetails)) {
      return true;
    }

    return schemaService.getSchema(objType) == null;
  }

  private <T extends IdentifiableObject> boolean writeCommonCheck(
      Schema schema, UserDetails userDetails, T object, Class<? extends T> objType) {
    if (!schema.isShareable()) {
      return true;
    }

    return checkSharingAccess(userDetails, object, objType)
        && (checkMetadataSharingPermission(userDetails, object, Permission.WRITE));
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
