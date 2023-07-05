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
import org.hisp.dhis.jsontree.JsonDate;
import org.hisp.dhis.jsontree.JsonList;

/**
 * Web API equivalent of a {@link org.hisp.dhis.organisationunit.OrganisationUnit}.
 *
 * @author Jan Bernitt
 */
public interface JsonOrganisationUnit extends JsonIdentifiableObject {
  default boolean isLeaf() {
    return getBoolean("leaf").booleanValue();
  }

  default int getLevel() {
    return getNumber("level").intValue();
  }

  default JsonPath getPath() {
    return get("path", JsonPath.class);
  }

  default LocalDateTime getOpeningDate() {
    return get("openingDate", JsonDate.class).date();
  }

  default LocalDateTime getClosedDate() {
    return get("closedDate", JsonDate.class).date();
  }

  default JsonOrganisationUnit getParent() {
    return get("parent", JsonOrganisationUnit.class);
  }

  default JsonList<JsonOrganisationUnit> getChildren() {
    return getList("children", JsonOrganisationUnit.class);
  }

  default JsonList<JsonOrganisationUnit> getAncestors() {
    return getList("ancestors", JsonOrganisationUnit.class);
  }

  default JsonList<JsonUser> getUsers() {
    return getList("users", JsonUser.class);
  }
}
