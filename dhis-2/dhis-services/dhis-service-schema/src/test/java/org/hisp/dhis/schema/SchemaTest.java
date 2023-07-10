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
package org.hisp.dhis.schema;

import static java.util.Collections.singletonMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;
import org.hisp.dhis.common.MetadataObject;
import org.hisp.dhis.common.SecondaryMetadataObject;
import org.hisp.dhis.security.Authority;
import org.hisp.dhis.security.AuthorityType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link Schema}.
 *
 * @author Volker Schmidt
 */
class SchemaTest {

  private List<Authority> authorities;

  @BeforeEach
  void setUp() {
    authorities = new ArrayList<>();
    authorities.add(new Authority(AuthorityType.CREATE, Arrays.asList("x1", "x2")));
    authorities.add(new Authority(AuthorityType.CREATE, Arrays.asList("y1", "y2")));
    authorities.add(new Authority(AuthorityType.DELETE, Arrays.asList("z1", "z2")));
  }

  @Test
  void isSecondaryMetadataObject() {
    assertTrue(new Schema(SecondaryMetadata.class, "singular", "plural").isSecondaryMetadata());
  }

  @Test
  void isSecondaryMetadataObjectMetadata() {
    assertTrue(new Schema(SecondaryMetadata.class, "singular", "plural").isMetadata());
  }

  @Test
  void isSecondaryMetadataObjectNot() {
    assertFalse(new Schema(Metadata.class, "singular", "plural").isSecondaryMetadata());
  }

  @Test
  void testAuthorityByType() {
    final Schema schema = new Schema(SecondaryMetadata.class, "singular", "plural");
    authorities.forEach(schema::add);
    List<String> list1 = schema.getAuthorityByType(AuthorityType.CREATE);
    assertThat(list1, contains("x1", "x2", "y1", "y2"));
    List<String> list2 = schema.getAuthorityByType(AuthorityType.CREATE);
    assertThat(list2, contains("x1", "x2", "y1", "y2"));
    assertSame(list1, list2);
  }

  @Test
  void testAuthorityByTypeDifferent() {
    final Schema schema = new Schema(SecondaryMetadata.class, "singular", "plural");
    authorities.forEach(schema::add);
    List<String> list1 = schema.getAuthorityByType(AuthorityType.CREATE);
    assertThat(list1, contains("x1", "x2", "y1", "y2"));
    List<String> list3 = schema.getAuthorityByType(AuthorityType.DELETE);
    assertThat(list3, contains("z1", "z2"));
    List<String> list2 = schema.getAuthorityByType(AuthorityType.CREATE);
    assertThat(list2, contains("x1", "x2", "y1", "y2"));
    assertSame(list1, list2);
  }

  @Test
  void testAuthorityByTypeNotFound() {
    final Schema schema = new Schema(SecondaryMetadata.class, "singular", "plural");
    authorities.forEach(schema::add);
    List<String> list1 = schema.getAuthorityByType(AuthorityType.CREATE_PRIVATE);
    assertTrue(list1.isEmpty());
    List<String> list2 = schema.getAuthorityByType(AuthorityType.CREATE_PRIVATE);
    assertTrue(list2.isEmpty());
    assertSame(list1, list2);
  }

  @Test
  void testAuthorityByTypeReset() {
    final Schema schema = new Schema(SecondaryMetadata.class, "singular", "plural");
    authorities.forEach(schema::add);
    List<String> list1 = schema.getAuthorityByType(AuthorityType.CREATE);
    assertThat(list1, contains("x1", "x2", "y1", "y2"));
    schema.add(new Authority(AuthorityType.CREATE, Arrays.asList("a1", "a2")));
    List<String> list2 = schema.getAuthorityByType(AuthorityType.CREATE);
    assertThat(list2, contains("x1", "x2", "y1", "y2", "a1", "a2"));
    assertNotSame(list1, list2);
  }

  @Test
  void testGetEmbeddedObjectProperties() {
    Schema schema = new Schema(Metadata.class, "singular", "plural");
    Property a = createProperty('A', Property::setEmbeddedObject, true);
    Property b = createProperty('B', Property::setEmbeddedObject, false);
    schema.addProperty(a);
    schema.addProperty(b);
    assertEquals(singletonMap("PropertyA", a), schema.getEmbeddedObjectProperties());
  }

  @Test
  void testGetAnalyticalObjectProperties() {
    Schema schema = new Schema(Metadata.class, "singular", "plural");
    Property a = createProperty('A', Property::setAnalyticalObject, true);
    Property b = createProperty('B', Property::setAnalyticalObject, false);
    schema.addProperty(a);
    schema.addProperty(b);
    assertEquals(singletonMap("PropertyA", a), schema.getAnalyticalObjectProperties());
  }

  @Test
  void testGetNonPersistedProperties() {
    Schema schema = new Schema(Metadata.class, "singular", "plural");
    Property a = createProperty('A', Property::setPersisted, true);
    Property b = createProperty('B', Property::setPersisted, false);
    schema.addProperty(a);
    schema.addProperty(b);
    assertEquals(singletonMap("PropertyB", b), schema.getNonPersistedProperties());
  }

  @Test
  void testGetPersistedProperties() {
    Schema schema = new Schema(Metadata.class, "singular", "plural");
    Property a = createProperty('A', Property::setPersisted, true);
    Property b = createProperty('B', Property::setPersisted, false);
    schema.addProperty(a);
    schema.addProperty(b);
    assertEquals(singletonMap("PropertyA", a), schema.getPersistedProperties());
  }

  @Test
  void testGetReadableProperties() {
    Schema schema = new Schema(Metadata.class, "singular", "plural");
    Property a = createProperty('A', Property::setReadable, true);
    Property b = createProperty('B', Property::setReadable, false);
    schema.addProperty(a);
    schema.addProperty(b);
    assertEquals(singletonMap("PropertyA", a), schema.getReadableProperties());
  }

  private static <T> Property createProperty(
      char uniqueCharacter, BiConsumer<Property, T> setter, T value) {
    Property p = new Property();
    p.setName("Property" + uniqueCharacter);
    setter.accept(p, value);
    return p;
  }

  private static class SecondaryMetadata implements SecondaryMetadataObject {}

  private static class Metadata implements MetadataObject {}
}
