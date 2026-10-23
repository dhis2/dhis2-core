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
package org.hisp.dhis.option;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.hisp.dhis.common.DisplayProperty;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.sharing.Sharing;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link Option}. */
class OptionTest {

  @Test
  void testEqualsAndHashCodeUseUidCodeNameAndDescription() {
    Option option = option("OptionUid01", "A", "Anna", "desc");
    Option same = option("OptionUid01", "A", "Anna", "desc");

    assertEquals(option, same);
    assertEquals(option.hashCode(), same.hashCode());
    assertNotEquals(option, option("OptionUid02", "A", "Anna", "desc"));
    assertNotEquals(option, option("OptionUid01", "B", "Anna", "desc"));
    assertNotEquals(option, option("OptionUid01", "A", "Betta", "desc"));
    assertNotEquals(option, option("OptionUid01", "A", "Anna", "other"));
  }

  @Test
  void testEqualsIgnoresSortOrderFormNameAndOptionSet() {
    Option option = option("OptionUid01", "A", "Anna", "desc");
    Option other = option("OptionUid01", "A", "Anna", "desc");
    other.setSortOrder(5);
    other.setFormName("form");
    other.setOptionSet(new OptionSet());

    assertEquals(option, other);
  }

  @Test
  void testNotEqualToSubclassWithSameIdentity() {
    Option option = option("OptionUid01", "A", "Anna", "desc");
    Option subclass = new Option() {};
    subclass.setUid("OptionUid01");
    subclass.setCode("A");
    subclass.setName("Anna");
    subclass.setDescription("desc");

    assertNotEquals(option, subclass);
  }

  @Test
  void testUnsupportedPropertiesIgnoreWrites() {
    Option option = new Option("Anna", "A");
    User user = new User();
    option.setShortName("short");
    option.setCreatedBy(user);
    option.setUser(user);
    option.setLastUpdatedBy(user);
    option.setSharing(Sharing.builder().publicAccess("rw------").build());
    option.setOwner("OwnerUid001");

    assertNull(option.getShortName());
    assertNull(option.getCreatedBy());
    assertNull(option.getUser());
    assertNull(option.getLastUpdatedBy());
    assertNull(option.getSharing().getPublicAccess());
    assertNull(option.getSharing().getOwner());
    assertTrue(option.getSharing().getUsers().isEmpty());
  }

  @Test
  void testSharingIsANewMutableInstanceOnEveryCall() {
    Option option = new Option("Anna", "A");
    option.getSharing().setPublicAccess("rw------");

    assertNull(option.getSharing().getPublicAccess());
  }

  @Test
  void testDisplayPropertiesFallBack() {
    Option option = new Option("Anna", "A");

    assertEquals("Anna", option.getDisplayProperty(DisplayProperty.SHORTNAME));
    assertEquals("Anna", option.getFormNameFallback());
    assertEquals("Anna", option.getDisplayFormName());
    option.setFormName("Form Anna");
    assertEquals("Form Anna", option.getDisplayFormName());
  }

  @Test
  void testPropertyValueByIdScheme() {
    Option option = option("OptionUid01", "A", "Anna", null);
    option.addAttributeValue("AttrUid0001", "attribute value");

    assertEquals("OptionUid01", option.getPropertyValue(IdScheme.UID));
    assertEquals("A", option.getPropertyValue(IdScheme.CODE));
    assertEquals("Anna", option.getPropertyValue(IdScheme.NAME));
    assertEquals(
        "attribute value", option.getPropertyValue(IdScheme.from("ATTRIBUTE:AttrUid0001")));
  }

  private static Option option(String uid, String code, String name, String description) {
    Option option = new Option(name, code);
    option.setUid(uid);
    option.setDescription(description);
    return option;
  }
}
