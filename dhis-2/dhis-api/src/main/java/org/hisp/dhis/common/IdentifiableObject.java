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
package org.hisp.dhis.common;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.io.Serializable;
import java.util.Date;
import java.util.Set;
import javax.annotation.Nonnull;
import org.hisp.dhis.attribute.AttributeValues;
import org.hisp.dhis.security.acl.Access;
import org.hisp.dhis.translation.Translation;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.sharing.Sharing;

/**
 * @author Lars Helge Overland
 */
@OpenApi.Kind("IdentifiableObject")
public interface IdentifiableObject
    extends PrimaryKeyObject, LinkableObject, Comparable<IdentifiableObject>, Serializable {
  String getCode();

  String getName();

  String getDisplayName();

  Date getCreated();

  Date getLastUpdated();

  User getLastUpdatedBy();

  AttributeValues getAttributeValues();

  void setAttributeValues(AttributeValues attributeValues);

  void addAttributeValue(String attributeUid, String value);

  void removeAttributeValue(String attributeId);

  Set<Translation> getTranslations();

  void setAccess(Access access);

  // -----------------------------------------------------------------------------
  // Sharing
  // -----------------------------------------------------------------------------

  /** Return User who created this object This field is immutable and must not be updated */
  User getCreatedBy();

  /**
   * @deprecated This method is replaced by {@link #getCreatedBy()} Currently it is only used for
   *     web api backward compatibility
   */
  @Deprecated
  User getUser();

  void setCreatedBy(User createdBy);

  /**
   * @deprecated This method is replaced by {@link #setCreatedBy(User)} ()} Currently it is only
   *     used for web api backward compatibility
   */
  @Deprecated
  void setUser(User user);

  Access getAccess();

  /** Return all sharing settings of current object */
  Sharing getSharing();

  void setSharing(Sharing sharing);

  // -----------------------------------------------------------------------------
  // Utility methods
  // -----------------------------------------------------------------------------

  @JsonIgnore
  String getPropertyValue(IdScheme idScheme);

  @JsonIgnore
  String getDisplayPropertyValue(IdScheme idScheme);

  default boolean hasSharing() {
    return getSharing() != null;
  }

  // -----------------------------------------------------------------------------
  // Temporary setters until hibernate upgrade is complete
  // -----------------------------------------------------------------------------

  void setId(long id);

  void setUid(String uid);

  void setName(String name);

  void setCode(String code);

  void setOwner(String owner);

  void setTranslations(Set<Translation> translations);

  void setLastUpdated(Date lastUpdated);

  void setLastUpdatedBy(User user);

  void setCreated(Date created);

  default void setAutoFields() {
    if (getUid() == null || getUid().isEmpty()) {
      setUid(CodeGenerator.generateUid());
    }

    Date date = new Date();

    if (getCreated() == null) {
      setCreated(date);
    }

    setLastUpdated(date);
  }

  // TODO check for usage and remove if not needed
  default int compareTo(@Nonnull IdentifiableObject o) {
    if (this.getDisplayName() == null) {
      return o.getDisplayName() == null ? 0 : 1;
    }

    return o.getDisplayName() == null
        ? -1
        : this.getDisplayName().compareToIgnoreCase(o.getDisplayName());
  }
}
