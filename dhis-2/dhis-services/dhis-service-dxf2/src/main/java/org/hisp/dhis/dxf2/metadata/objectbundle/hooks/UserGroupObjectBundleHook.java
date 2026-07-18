/*
 * Copyright (c) 2004-2026, University of Oslo
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
package org.hisp.dhis.dxf2.metadata.objectbundle.hooks;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundle;
import org.hisp.dhis.user.UserGroup;
import org.hisp.dhis.user.authz.AuthzService;
import org.springframework.stereotype.Component;

/**
 * Metadata import hook for {@link UserGroup}. Group membership affects the sharing/ACL snapshot
 * inside {@code UserDetails} ({@code getUserGroupIds}), so membership changes soft-bump affected
 * users. Group metadata-only changes (name, etc.) do not touch members.
 *
 * <p>{@code UserGroup.members} is the owning side of the membership relation; this is the import
 * path that can mutate group membership.
 *
 * @author Morten Svanæs
 */
@Component
@AllArgsConstructor
public class UserGroupObjectBundleHook extends AbstractObjectBundleHook<UserGroup> {

  public static final String MEMBER_DELTA_KEY = "userGroupMemberDelta";

  private final AuthzService authzService;

  @Override
  public void preUpdate(UserGroup object, UserGroup persistedObject, ObjectBundle bundle) {
    handleCreatedUserProperty(object, persistedObject, bundle);

    Set<String> before = uids(persistedObject.getMembers());
    Set<String> after = uids(object.getMembers());
    Set<String> delta = symmetricDifference(before, after);
    bundle.putExtras(object, MEMBER_DELTA_KEY, delta);
  }

  @Override
  @SuppressWarnings("unchecked")
  public void postUpdate(UserGroup persistedObject, ObjectBundle bundle) {
    Set<String> delta = (Set<String>) bundle.getExtras(persistedObject, MEMBER_DELTA_KEY);
    if (delta != null && !delta.isEmpty()) {
      authzService.bumpUsers(delta);
    }
    bundle.removeExtras(persistedObject, MEMBER_DELTA_KEY);
  }

  @Override
  public void postCreate(UserGroup persistedObject, ObjectBundle bundle) {
    bumpAllMembers(persistedObject);
  }

  @Override
  public void preDelete(UserGroup persistedObject, ObjectBundle bundle) {
    bumpAllMembers(persistedObject);
  }

  private void bumpAllMembers(UserGroup group) {
    Set<String> memberUids = uids(group.getMembers());
    if (!memberUids.isEmpty()) {
      authzService.bumpUsers(memberUids);
    }
  }

  /**
   * As User property of UserGroup is marked with @JsonIgnore ( see {@link UserGroup} ), the new
   * object will always has User = NULL. So we need to get this from persisted UserGroup, otherwise
   * it will always be set to current User when updating.
   */
  private void handleCreatedUserProperty(
      UserGroup userGroup, UserGroup persistedUserGroup, ObjectBundle bundle) {
    userGroup.setCreatedBy(persistedUserGroup.getCreatedBy());
    bundle.getPreheat().put(bundle.getPreheatIdentifier(), persistedUserGroup.getCreatedBy());
  }

  private static Set<String> uids(Collection<? extends IdentifiableObject> objects) {
    if (objects == null) {
      return Set.of();
    }
    return objects.stream()
        .filter(Objects::nonNull)
        .map(IdentifiableObject::getUid)
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());
  }

  private static Set<String> symmetricDifference(Set<String> before, Set<String> after) {
    Set<String> added = new HashSet<>(after);
    added.removeAll(before);
    Set<String> removed = new HashSet<>(before);
    removed.removeAll(after);
    Set<String> delta = new HashSet<>(added);
    delta.addAll(removed);
    return delta;
  }
}
