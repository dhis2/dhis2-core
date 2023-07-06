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
package org.hisp.dhis.webapi.json.domain;

import java.time.LocalDateTime;
import java.util.List;
import org.hisp.dhis.jsontree.JsonDate;
import org.hisp.dhis.jsontree.JsonList;
import org.hisp.dhis.jsontree.JsonObject;

/**
 * Web API equivalent of a {@link org.hisp.dhis.common.BaseIdentifiableObject}.
 *
 * @author Jan Bernitt
 */
public interface JsonIdentifiableObject extends JsonObject {
  default String getId() {
    return getString("id").string();
  }

  default String getName() {
    return getString("name").string();
  }

  default String getDisplayName() {
    return getString("displayName").string();
  }

  default String getHref() {
    return getString("href").string();
  }

  default String getCode() {
    return getString("code").string();
  }

  default JsonUser getLastUpdatedBy() {
    return get("lastUpdatedBy", JsonUser.class);
  }

  default LocalDateTime getLastUpdated() {
    return get("lastUpdated", JsonDate.class).date();
  }

  default JsonUser getCreatedBy() {
    return get("createdBy", JsonUser.class);
  }

  default LocalDateTime getCreated() {
    return get("created", JsonDate.class).date();
  }

  default boolean getExternalAccess() {
    return getBoolean("externalAccess").booleanValue();
  }

  default List<String> getFavorites() {
    return getArray("favorites").stringValues();
  }

  default boolean isFavorite() {
    return getBoolean("favorite").booleanValue();
  }

  default JsonSharing getSharing() {
    return get("sharing", JsonSharing.class);
  }

  default JsonList<JsonAttributeValue> getAttributeValues() {
    return getList("attributeValues", JsonAttributeValue.class);
  }
}
