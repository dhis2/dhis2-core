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
package org.hisp.dhis.organisationunit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.jsontree.Text;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link OrgUnitPath} values.
 *
 * @since 2.44
 * @author Jan Bernitt
 */
class OrgUnitPathTest {

  @Test
  void testOfNullable() {
    assertNull(OrgUnitPath.ofNullable(null));
  }

  @Test
  void testOf() {
    // NB. This might look a bit silly but of(...) is parsing the value
    // and toString() is re-composing it so this comparison does test full cycle
    assertEquals("/ou123456789", OrgUnitPath.of("/ou123456789").toString());
    assertEquals("/ou111111111/ou222222222", OrgUnitPath.of("/ou111111111/ou222222222").toString());
    assertEquals(
        "/ou111111111/ou222222222/ou333333333",
        OrgUnitPath.of("/ou111111111/ou222222222/ou333333333").toString());
  }

  @Test
  void testOf_Empty() {
    IllegalArgumentException ex =
        assertThrowsExactly(IllegalArgumentException.class, () -> OrgUnitPath.of(""));
    assertEquals("Path must not be empty", ex.getMessage());
  }

  @Test
  void testOf_NoLeadingSlash() {
    IllegalArgumentException ex =
        assertThrowsExactly(IllegalArgumentException.class, () -> OrgUnitPath.of("ou222222222"));
    assertEquals("Path must start with a slash", ex.getMessage());
  }

  @Test
  void testOf_NotMultipleOf12() {
    IllegalArgumentException ex =
        assertThrowsExactly(IllegalArgumentException.class, () -> OrgUnitPath.of("/ou222222222/"));
    assertEquals("Path must consist of UIDs segments each with a leading slash", ex.getMessage());
  }

  @Test
  void testOf_InvalidUID() {
    IllegalArgumentException ex =
        assertThrowsExactly(IllegalArgumentException.class, () -> OrgUnitPath.of("/ou$22222222"));
    assertEquals("Path id must be a valid UID but was: ou$22222222", ex.getMessage());
  }

  @Test
  void testLength() {
    assertEquals(1, OrgUnitPath.of("/ou123456789").length());
    assertEquals(2, OrgUnitPath.of("/ou111111111/ou222222222").length());
    assertEquals(3, OrgUnitPath.of("/ou111111111/ou222222222/ou333333333").length());
  }

  @Test
  void testToUID() {
    assertEquals(UID.of("ou123456789"), OrgUnitPath.of("/ou123456789").toUID());
    assertEquals(UID.of("ou222222222"), OrgUnitPath.of("/ou111111111/ou222222222").toUID());
    assertEquals(
        UID.of("ou333333333"), OrgUnitPath.of("/ou111111111/ou222222222/ou333333333").toUID());
  }

  @Test
  void testIsParent() {
    assertTrue(OrgUnitPath.of("/ou111111111/ou222222222").isParent(OrgUnitPath.of("/ou111111111")));
    assertTrue(
        OrgUnitPath.of("/ou111111111/ou222222222/ou333333333")
            .isParent(OrgUnitPath.of("/ou111111111")));
    assertTrue(
        OrgUnitPath.of("/ou111111111/ou222222222/ou333333333")
            .isParent(OrgUnitPath.of("/ou111111111/ou222222222")));

    assertFalse(
        OrgUnitPath.of("/ou111111111/ou222222222/ou333333333")
            .isParent(OrgUnitPath.of("/ou222222222")));
    assertFalse(OrgUnitPath.of("/ou111111111").isParent(OrgUnitPath.of("/ou111111111")));
    assertFalse(
        OrgUnitPath.of("/ou111111111").isParent(OrgUnitPath.of("/ou111111111/ou222222222")));
    assertFalse(
        OrgUnitPath.of("/ou111111111/ou222222222").isParent(OrgUnitPath.of("/ouXXXXXXXXX")));
  }

  @Test
  void testIsAncestor() {
    assertTrue(OrgUnitPath.of("/ou111111111/ou222222222").isAncestor("ou111111111"));
    assertTrue(OrgUnitPath.of("/ou111111111/ou222222222/ou333333333").isAncestor("ou111111111"));
    assertTrue(OrgUnitPath.of("/ou111111111/ou222222222/ou333333333").isAncestor("ou222222222"));
    assertFalse(OrgUnitPath.of("/ou111111111/ou222222222/ou333333333").isAncestor("ou333333333"));
    assertFalse(OrgUnitPath.of("/ou111111111/ou222222222").isAncestor("ouXXXXXXXXX"));
  }

  @Test
  void testOfMissingAncestors() {
    assertEquals(Set.of(), OrgUnitPath.ofMissingAncestors(Set.of()));
    assertEquals(Set.of(), OrgUnitPath.ofMissingAncestors(Set.of(OrgUnitPath.of("/ou123456789"))));
    assertEquals(
        Set.of(OrgUnitPath.of("/ou111111111")),
        OrgUnitPath.ofMissingAncestors(Set.of(OrgUnitPath.of("/ou111111111/ou222222222"))));
    assertEquals(
        Set.of(OrgUnitPath.of("/ou111111111"), OrgUnitPath.of("/ou111111111/ou222222222")),
        OrgUnitPath.ofMissingAncestors(
            Set.of(OrgUnitPath.of("/ou111111111/ou222222222/ou333333333"))));
    assertEquals(
        Set.of(OrgUnitPath.of("/ou111111111/ou222222222")),
        OrgUnitPath.ofMissingAncestors(
            Set.of(
                OrgUnitPath.of("/ou111111111"),
                OrgUnitPath.of("/ou111111111/ou222222222/ou333333333"))));
  }

  // --------------------- AI generated tests -------------------------------------

  private static final String UID_A = "ou123456789";
  private static final String UID_B = "ou234567890";
  private static final String UID_C = "ou345678901";
  private static final String UID_D = "ou456789012";

  private static String pathString(String... uids) {
    StringBuilder sb = new StringBuilder();
    for (String uid : uids) {
      sb.append('/').append(uid);
    }
    return sb.toString();
  }

  private static OrgUnitPath path(String... uids) {
    return OrgUnitPath.of(pathString(uids));
  }

  @Test
  void testOf_singleSegment() {
    OrgUnitPath path = OrgUnitPath.of("/" + UID_A);

    assertNull(path.parent());
    assertEquals(Text.of(UID_A), path.uid());
    assertEquals(1, path.length());
  }

  @Test
  void testOf_multipleSegments() {
    OrgUnitPath path = OrgUnitPath.of(pathString(UID_A, UID_B, UID_C));

    assertEquals(path(UID_A, UID_B), path.parent());
    assertEquals(Text.of(UID_C), path.uid());
    assertEquals(3, path.length());
  }

  @Test
  void testOf_emptyPathThrows() {
    assertThrows(IllegalArgumentException.class, () -> OrgUnitPath.of(""));
  }

  @Test
  void testOf_pathWithoutLeadingSlashThrows() {
    assertThrows(IllegalArgumentException.class, () -> OrgUnitPath.of(UID_A));
  }

  @Test
  void testOf_wrongLengthThrows() {
    assertThrows(IllegalArgumentException.class, () -> OrgUnitPath.of("/" + UID_A + "/"));
  }

  @Test
  void testOf_invalidUidSegmentThrows() {
    assertThrows(IllegalArgumentException.class, () -> OrgUnitPath.of("/12345678901"));
  }

  @Test
  void testOf_nullThrows() {
    assertThrows(NullPointerException.class, () -> OrgUnitPath.of(null));
  }

  @Test
  void testOfNullable_null() {
    assertNull(OrgUnitPath.ofNullable(null));
  }

  @Test
  void testOfNullable_validPath() {
    assertEquals(path(UID_A, UID_B), OrgUnitPath.ofNullable(pathString(UID_A, UID_B)));
  }

  @Test
  void testOfNullable_emptyPathThrows() {
    assertThrows(IllegalArgumentException.class, () -> OrgUnitPath.ofNullable(""));
  }

  @Test
  void testOfMissingAncestors_empty() {
    assertEquals(Set.of(), OrgUnitPath.ofMissingAncestors(List.of()));
  }

  @Test
  void testOfMissingAncestors_singlePath() {
    assertEquals(
        Set.of(path(UID_A), path(UID_A, UID_B)),
        OrgUnitPath.ofMissingAncestors(List.of(path(UID_A, UID_B, UID_C))));
  }

  @Test
  void testOfMissingAncestors_someAncestorsPresent() {
    assertEquals(
        Set.of(path(UID_A, UID_B)),
        OrgUnitPath.ofMissingAncestors(
            List.of(path(UID_A), path(UID_A, UID_B, UID_C))));
  }

  @Test
  void testOfMissingAncestors_allAncestorsPresent() {
    assertEquals(
        Set.of(),
        OrgUnitPath.ofMissingAncestors(
            List.of(path(UID_A), path(UID_A, UID_B), path(UID_A, UID_B, UID_C))));
  }

  @Test
  void testConstructor_root() {
    OrgUnitPath path = new OrgUnitPath(null, Text.of(UID_A));

    assertNull(path.parent());
    assertEquals(Text.of(UID_A), path.uid());
    assertEquals(1, path.length());
  }

  @Test
  void testConstructor_nested() {
    OrgUnitPath parent = path(UID_A);
    OrgUnitPath path = new OrgUnitPath(parent, Text.of(UID_B));

    assertEquals(parent, path.parent());
    assertEquals(Text.of(UID_B), path.uid());
    assertEquals(2, path.length());
  }

  @Test
  void testConstructor_nullUidThrows() {
    assertThrows(NullPointerException.class, () -> new OrgUnitPath(null, null));
  }

  @Test
  void testConstructor_invalidUidThrows() {
    assertThrows(
        IllegalArgumentException.class, () -> new OrgUnitPath(null, Text.of("12345678901")));
  }

  @Test
  void testParent_rootReturnsNull() {
    assertNull(path(UID_A).parent());
  }

  @Test
  void testParent_nestedReturnsParent() {
    assertEquals(path(UID_A), path(UID_A, UID_B).parent());
    assertEquals(path(UID_A, UID_B), path(UID_A, UID_B, UID_C).parent());
  }

  @Test
  void testUid_returnsTailUid() {
    assertEquals(Text.of(UID_C), path(UID_A, UID_B, UID_C).uid());
  }

  @Test
  void testLength_root() {
    assertEquals(1, path(UID_A).length());
  }

  @Test
  void testLength_nested() {
    assertEquals(2, path(UID_A, UID_B).length());
    assertEquals(3, path(UID_A, UID_B, UID_C).length());
  }

  @Test
  void testIsParent_directParent() {
    assertTrue(path(UID_A, UID_B).isParent(path(UID_A)));
  }

  @Test
  void testIsParent_distantParent() {
    assertTrue(path(UID_A, UID_B, UID_C).isParent(path(UID_A)));
  }

  @Test
  void testIsParent_self() {
    assertFalse(path(UID_A, UID_B).isParent(path(UID_A, UID_B)));
  }

  @Test
  void testIsParent_child() {
    assertFalse(path(UID_A).isParent(path(UID_A, UID_B)));
  }

  @Test
  void testIsParent_root() {
    assertFalse(path(UID_A).isParent(path(UID_A, UID_B, UID_C)));
  }

  @Test
  void testIsAncestor_uid_directParent() {
    assertTrue(path(UID_A, UID_B).isAncestor(UID.of(UID_A)));
  }

  @Test
  void testIsAncestor_uid_distantParent() {
    assertTrue(path(UID_A, UID_B, UID_C).isAncestor(UID.of(UID_A)));
    assertTrue(path(UID_A, UID_B, UID_C).isAncestor(UID.of(UID_B)));
  }

  @Test
  void testIsAncestor_uid_ownUid() {
    assertFalse(path(UID_A, UID_B, UID_C).isAncestor(UID.of(UID_C)));
  }

  @Test
  void testIsAncestor_uid_unrelated() {
    assertFalse(path(UID_A, UID_B).isAncestor(UID.of(UID_D)));
  }

  @Test
  void testIsAncestor_uid_root() {
    assertFalse(path(UID_A).isAncestor(UID.of(UID_A)));
  }

  @Test
  void testIsAncestor_charSequence_directParent() {
    assertTrue(path(UID_A, UID_B).isAncestor(UID_A));
  }

  @Test
  void testIsAncestor_charSequence_distantParent() {
    assertTrue(path(UID_A, UID_B, UID_C).isAncestor(UID_A));
    assertTrue(path(UID_A, UID_B, UID_C).isAncestor(UID_B));
  }

  @Test
  void testIsAncestor_charSequence_ownUid() {
    assertFalse(path(UID_A, UID_B, UID_C).isAncestor(UID_C));
  }

  @Test
  void testIsAncestor_charSequence_unrelated() {
    assertFalse(path(UID_A, UID_B).isAncestor(UID_D));
  }

  @Test
  void testIsAncestor_charSequence_root() {
    assertFalse(path(UID_A).isAncestor(UID_A));
  }

  @Test
  void testEquals_equalPaths() {
    assertEquals(path(UID_A, UID_B, UID_C), path(UID_A, UID_B, UID_C));
  }

  @Test
  void testEquals_differentPath() {
    assertNotEquals(path(UID_A, UID_B, UID_C), path(UID_A, UID_B));
    assertNotEquals(path(UID_A, UID_B), path(UID_A));
  }

  @Test
  void testEquals_nullAndOtherType() {
    assertNotEquals(path(UID_A), null);
    assertNotEquals(path(UID_A), pathString(UID_A));
  }

  @Test
  void testHashCode_equalPaths() {
    assertEquals(path(UID_A, UID_B, UID_C).hashCode(), path(UID_A, UID_B, UID_C).hashCode());
  }

  @Test
  void testToString_root() {
    assertEquals("/" + UID_A, path(UID_A).toString());
  }

  @Test
  void testToString_nested() {
    assertEquals(
        pathString(UID_A, UID_B, UID_C), path(UID_A, UID_B, UID_C).toString());
  }

  @Test
  void testToUID_returnsTailUid() {
    assertEquals(UID.of(UID_C), path(UID_A, UID_B, UID_C).toUID());
  }
}
