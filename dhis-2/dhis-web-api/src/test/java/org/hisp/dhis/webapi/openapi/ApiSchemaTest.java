/*
 * Copyright (c) 2004-2025, University of Oslo
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
package org.hisp.dhis.webapi.openapi;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;
import java.util.Set;
import org.hisp.dhis.attribute.AttributeValues;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.security.acl.Access;
import org.hisp.dhis.translation.Translation;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.user.sharing.Sharing;
import org.junit.jupiter.api.Test;

class ApiSchemaTest {

  private static class SomeClass {}

  private static class NonIdentifiable {}

  private static class Identifiable implements IdentifiableObject {
    @Override
    public String getCode() {
      return "";
    }

    @Override
    public String getName() {
      return "";
    }

    @Override
    public String getDisplayName() {
      return "";
    }

    @Override
    public Date getCreated() {
      return null;
    }

    @Override
    public Date getLastUpdated() {
      return null;
    }

    @Override
    public User getLastUpdatedBy() {
      return null;
    }

    @Override
    public AttributeValues getAttributeValues() {
      return null;
    }

    @Override
    public void setAttributeValues(AttributeValues attributeValues) {}

    @Override
    public void addAttributeValue(String attributeUid, String value) {}

    @Override
    public void removeAttributeValue(String attributeId) {}

    @Override
    public Set<Translation> getTranslations() {
      return Set.of();
    }

    @Override
    public void setAccess(Access access) {}

    @Override
    public Set<String> getFavorites() {
      return Set.of();
    }

    @Override
    public boolean isFavorite() {
      return false;
    }

    @Override
    public boolean setAsFavorite(UserDetails user) {
      return false;
    }

    @Override
    public boolean removeAsFavorite(UserDetails user) {
      return false;
    }

    @Override
    public User getCreatedBy() {
      return null;
    }

    @Override
    public User getUser() {
      return null;
    }

    @Override
    public void setCreatedBy(User createdBy) {}

    @Override
    public void setUser(User user) {}

    @Override
    public Access getAccess() {
      return null;
    }

    @Override
    public Sharing getSharing() {
      return null;
    }

    @Override
    public void setSharing(Sharing sharing) {}

    @Override
    public String getPropertyValue(IdScheme idScheme) {
      return "";
    }

    @Override
    public String getDisplayPropertyValue(IdScheme idScheme) {
      return "";
    }

    @Override
    public String getHref() {
      return "";
    }

    @Override
    public void setHref(String link) {}

    @Override
    public long getId() {
      return 0;
    }

    @Override
    public String getUid() {
      return "";
    }

    @Override
    public int compareTo(IdentifiableObject o) {
      return 0;
    }
  }

  @Test
  void testAddPropertyLeavesSchemaBidirectionalWhenAddingNonIdentifiableObject() {
    Api.Schema nonIdentifiableSchema = Api.Schema.ofObject(NonIdentifiable.class);
    Api.Property nonIdentifiableProperty =
        new Api.Property(null, "components", true, nonIdentifiableSchema);
    Api.Schema schema = Api.Schema.ofObject(SomeClass.class).addProperty(nonIdentifiableProperty);
    assertTrue(schema.isBidirectional());
  }

  @Test
  void testAddPropertyTurnsSchemaUnidirectionalWhenAddingIdentifiableObject() {
    Api.Schema identifiableSchema = Api.Schema.ofObject(Identifiable.class);
    Api.Property identifiableProperty =
        new Api.Property(null, "components", true, identifiableSchema);
    Api.Schema schema = Api.Schema.ofObject(SomeClass.class).addProperty(identifiableProperty);
    assertFalse(schema.isBidirectional());
  }
}
