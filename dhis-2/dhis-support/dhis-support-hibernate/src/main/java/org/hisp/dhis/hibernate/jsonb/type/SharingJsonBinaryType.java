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
package org.hisp.dhis.hibernate.jsonb.type;

import java.util.Map;
import java.util.Objects;
import org.hisp.dhis.common.collection.CollectionUtils;
import org.hisp.dhis.sharing.AccessObject;
import org.hisp.dhis.user.sharing.Sharing;

public class SharingJsonBinaryType extends JsonBinaryType {
  @Override
  protected Object convertJsonToObject(String content) {
    Sharing sharing = (Sharing) super.convertJsonToObject(content);
    sharing.setUsers(CollectionUtils.emptyIfNull(sharing.getUsers()));
    sharing.setUserGroups(CollectionUtils.emptyIfNull(sharing.getUserGroups()));
    return sharing;
  }

  /**
   * {@link Sharing} does not implement {@code equals}, and the snapshot Hibernate keeps for dirty
   * checking is a deep copy, so the inherited check would serialise and parse both values on every
   * flush of every entity with sharing. Compare the persisted fields directly first, and only fall
   * back to the content based comparison when they differ.
   */
  @Override
  public boolean equals(Object x, Object y) {
    if (x instanceof Sharing a && y instanceof Sharing b && sameFields(a, b)) {
      return true;
    }
    return super.equals(x, y);
  }

  private static boolean sameFields(Sharing a, Sharing b) {
    return Objects.equals(a.getOwner(), b.getOwner())
        && Objects.equals(a.getPublicAccess(), b.getPublicAccess())
        && sameAccess(a.getUsers(), b.getUsers())
        && sameAccess(a.getUserGroups(), b.getUserGroups());
  }

  private static boolean sameAccess(
      Map<String, ? extends AccessObject> a, Map<String, ? extends AccessObject> b) {
    if (a == null || b == null) {
      return a == b;
    }
    if (a.size() != b.size()) {
      return false;
    }
    for (Map.Entry<String, ? extends AccessObject> e : a.entrySet()) {
      AccessObject other = b.get(e.getKey());
      AccessObject access = e.getValue();
      if (access == null
          || other == null
          || !Objects.equals(access.getId(), other.getId())
          || !Objects.equals(access.getAccess(), other.getAccess())
          || !Objects.equals(access.getDisplayName(), other.getDisplayName())) {
        return false;
      }
    }
    return true;
  }
}
