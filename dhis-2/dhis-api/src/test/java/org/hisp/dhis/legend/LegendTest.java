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
package org.hisp.dhis.legend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class LegendTest {

  @Test
  void testEqualsWithSameObject() {
    Legend legend = createLegend("uid1", "code1", "name1");
    assertEquals(legend, legend);
  }

  @Test
  void testEqualsWithEqualObjects() {
    Legend a = createLegend("uid1", "code1", "name1");
    Legend b = createLegend("uid1", "code1", "name1");
    assertEquals(a, b);
    assertEquals(b, a);
  }

  @Test
  void testEqualsWithDifferentUid() {
    Legend a = createLegend("uid1", "code1", "name1");
    Legend b = createLegend("uid2", "code1", "name1");
    assertNotEquals(a, b);
  }

  @Test
  void testEqualsWithDifferentCode() {
    Legend a = createLegend("uid1", "code1", "name1");
    Legend b = createLegend("uid1", "code2", "name1");
    assertNotEquals(a, b);
  }

  @Test
  void testEqualsWithDifferentName() {
    Legend a = createLegend("uid1", "code1", "name1");
    Legend b = createLegend("uid1", "code1", "name2");
    assertNotEquals(a, b);
  }

  @Test
  void testEqualsWithNull() {
    Legend legend = createLegend("uid1", "code1", "name1");
    assertNotEquals(null, legend);
  }

  @Test
  void testEqualsWithDifferentType() {
    Legend legend = createLegend("uid1", "code1", "name1");
    assertNotEquals("not a legend", legend);
  }

  @Test
  void testEqualsWithNullFields() {
    Legend a = createLegend(null, null, null);
    Legend b = createLegend(null, null, null);
    assertEquals(a, b);
  }

  @Test
  void testHashCodeConsistency() {
    Legend legend = createLegend("uid1", "code1", "name1");
    int hash1 = legend.hashCode();
    int hash2 = legend.hashCode();
    assertEquals(hash1, hash2);
  }

  @Test
  void testHashCodeEqualObjects() {
    Legend a = createLegend("uid1", "code1", "name1");
    Legend b = createLegend("uid1", "code1", "name1");
    assertEquals(a.hashCode(), b.hashCode());
  }

  @Test
  void testHashCodeDifferentObjects() {
    Legend a = createLegend("uid1", "code1", "name1");
    Legend b = createLegend("uid2", "code2", "name2");
    assertNotEquals(a.hashCode(), b.hashCode());
  }

  @Test
  void testHashCodeWithNullFields() {
    Legend legend = createLegend(null, null, null);
    assertEquals(0, legend.hashCode());
  }

  @Test
  void testTypedEqualsWithNull() {
    Legend legend = createLegend("uid1", "code1", "name1");
    assertFalse(legend.typedEquals(null));
  }

  @Test
  void testTypedEqualsWithMatchingObject() {
    Legend a = createLegend("uid1", "code1", "name1");
    Legend b = createLegend("uid1", "code1", "name1");
    assertTrue(a.typedEquals(b));
  }

  @Test
  void testTypedEqualsWithDifferentUid() {
    Legend a = createLegend("uid1", "code1", "name1");
    Legend b = createLegend("uid2", "code1", "name1");
    assertFalse(a.typedEquals(b));
  }

  private Legend createLegend(String uid, String code, String name) {
    Legend legend = new Legend();
    legend.setUid(uid);
    legend.setCode(code);
    legend.setName(name);
    return legend;
  }
}