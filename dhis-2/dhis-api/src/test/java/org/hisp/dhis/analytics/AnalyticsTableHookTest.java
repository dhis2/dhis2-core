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
package org.hisp.dhis.analytics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Set;
import org.hisp.dhis.attribute.AttributeValues;
import org.hisp.dhis.common.Locale;
import org.hisp.dhis.resourcetable.ResourceTableType;
import org.hisp.dhis.translation.Translation;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.sharing.Sharing;
import org.junit.jupiter.api.Test;

class AnalyticsTableHookTest {

  @Test
  void testEqualsComparesUidCodeAndNameOnly() {
    AnalyticsTableHook hook = hook("hookUid0001", "CODE", "Name");
    AnalyticsTableHook sameIdentity = hook("hookUid0001", "CODE", "Name");
    sameIdentity.setPhase(AnalyticsTablePhase.ANALYTICS_TABLE_POPULATED);
    sameIdentity.setSql("select 2");

    assertEquals(hook, sameIdentity);
    assertEquals(hook.hashCode(), sameIdentity.hashCode());
    assertNotEquals(hook, hook("hookUid0002", "CODE", "Name"));
    assertNotEquals(hook, hook("hookUid0001", "OTHER", "Name"));
    assertNotEquals(hook, hook("hookUid0001", "CODE", "Other"));
  }

  @Test
  void testNotEqualToSubclassWithSameIdentity() {
    AnalyticsTableHook subclass = new AnalyticsTableHook() {};
    subclass.setUid("hookUid0001");
    subclass.setCode("CODE");
    subclass.setName("Name");

    assertNotEquals(hook("hookUid0001", "CODE", "Name"), subclass);
  }

  @Test
  void testHashCodeOfEmptyHookIsZero() {
    assertEquals(0, new AnalyticsTableHook().hashCode());
  }

  @Test
  void testUnsupportedPropertiesStayEmpty() {
    AnalyticsTableHook hook = hook("hookUid0001", "CODE", "Name");
    User user = new User();
    hook.setCreatedBy(user);
    hook.setUser(user);
    hook.setOwner("ownerUid001");
    hook.setSharing(Sharing.builder().owner("ownerUid001").publicAccess("rw------").build());
    hook.setAttributeValues(AttributeValues.of(Map.of("attrUid0001", "x")));
    hook.addAttributeValue("attrUid0002", "y");
    hook.removeAttributeValue("attrUid0001");

    assertNull(hook.getCreatedBy());
    assertNull(hook.getUser());
    assertNull(hook.getSharing().getOwner());
    assertNull(hook.getSharing().getPublicAccess());
    assertTrue(hook.getAttributeValues().isEmpty());
  }

  @Test
  void testTranslationsAreKeptInMemoryAndNotUsedForDisplayName() {
    AnalyticsTableHook hook = hook("hookUid0001", "CODE", "Name");
    Translation translation = new Translation(new Locale("fr"), "NAME", "Nom");
    hook.setTranslations(Set.of(translation));

    assertEquals(Set.of(translation), hook.getTranslations());
    assertEquals("Name", hook.getDisplayName());

    hook.setTranslations(null);
    assertTrue(hook.getTranslations().isEmpty());
  }

  private static AnalyticsTableHook hook(String uid, String code, String name) {
    AnalyticsTableHook hook =
        new AnalyticsTableHook(
            name,
            AnalyticsTablePhase.RESOURCE_TABLE_POPULATED,
            ResourceTableType.ORG_UNIT_STRUCTURE,
            "select 1");
    hook.setUid(uid);
    hook.setCode(code);
    return hook;
  }
}
