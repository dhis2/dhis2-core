package org.hisp.dhis.organisationunit;

import org.hisp.dhis.common.UID;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
}
