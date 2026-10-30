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
package org.hisp.dhis.schema.introspection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.HashMap;
import java.util.Map;
import org.hisp.dhis.schema.Property;
import org.hisp.dhis.schema.annotation.Property.Access;
import org.hisp.dhis.user.UserRole;
import org.junit.jupiter.api.Test;

/**
 * Uses the real {@link JacksonPropertyIntrospector} pipeline (not a hand-built {@link Property}) to
 * reproduce the exact reflection-based state that {@link PropertyPropertyIntrospector} sees in
 * production, since a hand-built {@code Property} with {@code setPropertyTransformer(...)} called
 * directly would bypass the bug entirely.
 */
class PropertyPropertyIntrospectorTest {

  @Test
  void detectsPropertyTransformerOnReadOnlyDerivedGetter() {
    Map<String, Property> properties = new HashMap<>();
    new JacksonPropertyIntrospector().introspect(UserRole.class, properties);

    Property users = properties.get("users");

    // UserRole.getUsers() is computed from the "members" field and has no matching setUsers(),
    // so it must be read-only.
    assertFalse(users.isWritable());

    new PropertyPropertyIntrospector().introspect(UserRole.class, properties);

    // ... yet it still carries @PropertyTransformer(UserPropertyTransformer.class) (DHIS2-21872).
    assertTrue(users.hasPropertyTransformer());
  }

  @Test
  void detectsPropertyAnnotationOnReadOnlyProperty() {
    Map<String, Property> properties = new HashMap<>();
    new JacksonPropertyIntrospector().introspect(ReadOnlyAliasedFixture.class, properties);

    Property displayAlias = properties.get("displayAlias");

    // No setDisplayAlias(...), so this is read-only.
    assertFalse(displayAlias.isWritable());

    new PropertyPropertyIntrospector().introspect(ReadOnlyAliasedFixture.class, properties);

    // ... yet persistedAs() still aliases the field name, since it describes the property, not
    // whether it can be written to (DHIS2-21872).
    assertEquals("internalName", displayAlias.getFieldName());
  }

  @Test
  void rangeDefaultsUnaffectedWhenPropertyAnnotationOverridesWritableProperty() {
    Map<String, Property> properties = new HashMap<>();
    new JacksonPropertyIntrospector().introspect(ReadOnlyOverrideFixture.class, properties);

    Property computed = properties.get("computed");

    // Has a real setter, so writable prior to the @Property(access = READ_ONLY) override below.
    assertTrue(computed.isWritable());

    new PropertyPropertyIntrospector().introspect(ReadOnlyOverrideFixture.class, properties);

    // The annotation flips writability for API purposes ...
    assertFalse(computed.isWritable());
    // ... but range/min-max defaults are still applied, since that decision is snapshotted from
    // before the annotation-driven override, matching pre-existing behavior for properties like
    // Attribute.getObjectTypes() (real setter + @Property(access = READ_ONLY)).
    assertNotNull(computed.getMin());
    assertNotNull(computed.getMax());
  }

  private static class ReadOnlyAliasedFixture {
    @JsonProperty
    @org.hisp.dhis.schema.annotation.Property(persistedAs = "internalName")
    public String getDisplayAlias() {
      return "computed";
    }
  }

  private static class ReadOnlyOverrideFixture {
    @JsonProperty
    @org.hisp.dhis.schema.annotation.Property(access = Access.READ_ONLY)
    public String getComputed() {
      return "computed";
    }

    public void setComputed(String value) {
      // real setter; @Property(access = READ_ONLY) overrides this for API purposes only
    }
  }
}
