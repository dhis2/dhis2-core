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
package org.hisp.dhis.gist;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Collections.unmodifiableSet;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.AllArgsConstructor;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.PrimaryKeyObject;
import org.hisp.dhis.gist.GistQuery.Comparison;
import org.hisp.dhis.gist.GistQuery.Field;
import org.hisp.dhis.gist.GistQuery.Filter;
import org.hisp.dhis.query.JpaQueryUtils;
import org.hisp.dhis.schema.annotation.Gist.Transform;
import org.hisp.dhis.security.acl.Access;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.user.sharing.Sharing;

/**
 * Encapsulates all access control related logic of gist API request processing.
 *
 * <p>An instance is always related to the current {@link User} of the currently processed gist API
 * request.
 *
 * @author Jan Bernitt
 */
@AllArgsConstructor
public class DefaultGistAccessControl implements GistAccessControl {

  private static final Set<String> PUBLIC_USER_PROPERTY_PATHS =
      unmodifiableSet(
          new HashSet<>(
              asList("id", "code", "displayName", "name", "surname", "firstName", "username")));

  private static final Set<String> PUBLIC_PROPERTY_PATHS =
      unmodifiableSet(new HashSet<>(asList("sharing", "access", "translations")));

  private final User currentUser;

  private final AclService aclService;

  private final UserService userService;

  private final GistService gistService;

  @Override
  public String getCurrentUserUid() {
    return currentUser.getUid();
  }

  @Override
  public boolean isSuperuser() {
    return currentUser != null && currentUser.isSuper();
  }

  @Override
  public boolean canReadHQL() {
    return isSuperuser() || currentUser.isAuthorized("F_METADATA_EXPORT");
  }

  @Override
  public boolean canRead(Class<? extends PrimaryKeyObject> type) {
    if (!IdentifiableObject.class.isAssignableFrom(type)) {
      return true;
    }
    @SuppressWarnings("unchecked")
    Class<? extends IdentifiableObject> ioType = (Class<? extends IdentifiableObject>) type;
    return aclService.canRead(currentUser, ioType);
  }

  @Override
  public boolean canReadObject(Class<? extends PrimaryKeyObject> type, String uid) {
    if (!IdentifiableObject.class.isAssignableFrom(type)) {
      return true;
    }
    @SuppressWarnings("unchecked")
    Class<? extends IdentifiableObject> ioType = (Class<? extends IdentifiableObject>) type;
    if (!aclService.isClassShareable(ioType)) {
      return aclService.canRead(currentUser, ioType);
    }
    List<?> res =
        gistService.gist(
            GistQuery.builder()
                .elementType(ioType)
                .autoType(GistAutoType.M)
                .fields(singletonList(new Field("sharing", Transform.NONE)))
                .filters(singletonList(new Filter("id", Comparison.EQ, uid)))
                .build());
    Sharing sharing = res.isEmpty() ? new Sharing() : (Sharing) res.get(0);
    BaseIdentifiableObject object = new BaseIdentifiableObject();
    object.setSharing(sharing);
    return aclService.canRead(currentUser, object, ioType);
  }

  @Override
  public boolean canRead(Class<? extends PrimaryKeyObject> type, String path) {
    if (!IdentifiableObject.class.isAssignableFrom(type)) {
      return true;
    }
    boolean isUserField = type == User.class;
    if (isUserField && PUBLIC_USER_PROPERTY_PATHS.contains(path)) {
      return true;
    }

    @SuppressWarnings("unchecked")
    Class<? extends IdentifiableObject> ioType = (Class<? extends IdentifiableObject>) type;
    return PUBLIC_PROPERTY_PATHS.contains(path) || aclService.canRead(currentUser, ioType);
  }

  @Override
  public boolean canFilterByAccessOfUser(String userUid) {
    User user = getCurrentUserUid().equals(userUid) ? currentUser : userService.getUser(userUid);
    return user != null && aclService.canRead(currentUser, user);
  }

  @Override
  public Access asAccess(Class<? extends IdentifiableObject> type, Sharing value) {
    BaseIdentifiableObject object = new BaseIdentifiableObject();
    object.setSharing(value);
    return aclService.getAccess(object, currentUser, type);
  }

  @Override
  public String createAccessFilterHQL(String tableName) {
    return JpaQueryUtils.generateHqlQueryForSharingCheck(
        tableName, currentUser, AclService.LIKE_READ_METADATA);
  }
}
