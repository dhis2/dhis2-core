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
package org.hisp.dhis.dataApproval;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.hisp.dhis.dataapproval.DataApprovalLevel;
import org.junit.jupiter.api.Test;

class DataApprovalLevelTest {

  @Test
  void testEqualsWithSameObject() {
    DataApprovalLevel level = createLevel("uid1", "name1");
    assertEquals(level, level);
  }

  @Test
  void testEqualsWithEqualObjects() {
    DataApprovalLevel a = createLevel("uid1", "name1");
    DataApprovalLevel b = createLevel("uid1", "name1");
    assertEquals(a, b);
    assertEquals(b, a);
  }

  @Test
  void testEqualsWithDifferentUid() {
    DataApprovalLevel a = createLevel("uid1", "name1");
    DataApprovalLevel b = createLevel("uid2", "name1");
    assertNotEquals(a, b);
  }

  @Test
  void testEqualsWithDifferentName() {
    DataApprovalLevel a = createLevel("uid1", "name1");
    DataApprovalLevel b = createLevel("uid1", "name2");
    assertNotEquals(a, b);
  }

  @Test
  void testEqualsWithNull() {
    DataApprovalLevel level = createLevel("uid1", "name1");
    assertNotEquals(null, level);
  }

  @Test
  void testEqualsWithDifferentType() {
    DataApprovalLevel level = createLevel("uid1", "name1");
    assertNotEquals("not a level", level);
  }

  @Test
  void testEqualsWithNullFields() {
    DataApprovalLevel a = createLevel(null, null);
    DataApprovalLevel b = createLevel(null, null);
    assertEquals(a, b);
  }

  @Test
  void testHashCodeConsistency() {
    DataApprovalLevel level = createLevel("uid1", "name1");
    int hash1 = level.hashCode();
    int hash2 = level.hashCode();
    assertEquals(hash1, hash2);
  }

  @Test
  void testHashCodeEqualObjects() {
    DataApprovalLevel a = createLevel("uid1", "name1");
    DataApprovalLevel b = createLevel("uid1", "name1");
    assertEquals(a.hashCode(), b.hashCode());
  }

  @Test
  void testHashCodeDifferentObjects() {
    DataApprovalLevel a = createLevel("uid1", "name1");
    DataApprovalLevel b = createLevel("uid2", "name2");
    assertNotEquals(a.hashCode(), b.hashCode());
  }

  @Test
  void testHashCodeWithNullFields() {
    DataApprovalLevel level = createLevel(null, null);
    assertEquals(0, level.hashCode());
  }

  @Test
  void testTypedEqualsWithNull() {
    DataApprovalLevel level = createLevel("uid1", "name1");
    assertFalse(level.typedEquals(null));
  }

  @Test
  void testTypedEqualsWithMatchingObject() {
    DataApprovalLevel a = createLevel("uid1", "name1");
    DataApprovalLevel b = createLevel("uid1", "name1");
    assertTrue(a.typedEquals(b));
  }

  @Test
  void testTypedEqualsWithDifferentUid() {
    DataApprovalLevel a = createLevel("uid1", "name1");
    DataApprovalLevel b = createLevel("uid2", "name1");
    assertFalse(a.typedEquals(b));
  }

  private DataApprovalLevel createLevel(String uid, String name) {
    DataApprovalLevel level = new DataApprovalLevel();
    level.setUid(uid);
    level.setName(name);
    return level;
  }
}
