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
package org.hisp.dhis.relationship;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

class RelationshipTypeTest {

  @Test
  void shouldEqualRelationshipTypeWithSameUidCodeAndName() {
    RelationshipType type = createRelationshipType("uid", "code", "name");
    RelationshipType other = createRelationshipType("uid", "code", "name");

    assertEquals(type, other);
    assertEquals(type.hashCode(), other.hashCode());
  }

  @Test
  void shouldNotEqualRelationshipTypeWithDifferentUidCodeOrName() {
    RelationshipType type = createRelationshipType("uid", "code", "name");

    assertNotEquals(type, createRelationshipType("other", "code", "name"));
    assertNotEquals(type, createRelationshipType("uid", "other", "name"));
    assertNotEquals(type, createRelationshipType("uid", "code", "other"));
  }

  @Test
  void shouldEqualRelationshipTypeDifferingOnlyInPropertiesOutsideIdentity() {
    RelationshipType type = createRelationshipType("uid", "code", "name");
    RelationshipType other = createRelationshipType("uid", "code", "name");
    other.setDescription("description");
    other.setFromToName("from to");
    other.setToFromName("to from");
    other.setBidirectional(true);
    other.setReferral(true);
    other.setFromConstraint(new RelationshipConstraint());
    other.addAttributeValue("AttrUid0001", "value");

    assertEquals(type, other);
    assertEquals(type.hashCode(), other.hashCode());
  }

  private static RelationshipType createRelationshipType(String uid, String code, String name) {
    RelationshipType type = new RelationshipType();
    type.setUid(uid);
    type.setCode(code);
    type.setName(name);
    return type;
  }
}
