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
package org.hisp.dhis.constant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.hisp.dhis.attribute.AttributeValues;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.user.User;
import org.junit.jupiter.api.Test;

class ConstantTest {

  @Test
  void testEqualConstantsHaveEqualHashCodes() {
    Constant a = constant("Pi", "Ratio");
    Constant b = constant("Pi", "Ratio");

    assertEquals(a, b);
    assertEquals(a.hashCode(), b.hashCode());
  }

  @Test
  void testConstantsWithDifferentShortNamesAreNotEqual() {
    Constant a = constant("Pi", "Ratio");
    Constant b = constant("Pi", "Ratio");
    b.setShortName("Other");

    assertNotEquals(a, b);
  }

  @Test
  void testConstantsWithDifferentDescriptionsAreNotEqual() {
    assertNotEquals(constant("Pi", "Ratio"), constant("Pi", "Other ratio"));
  }

  @Test
  void testFormNameIsNotStoredAndDisplayFormNameFallsBackToName() {
    Constant constant = constant("Pi", "Ratio");
    constant.setFormName("Form name");

    assertNull(constant.getFormName());
    assertEquals("Pi", constant.getDisplayFormName());
  }

  @Test
  void testSetTranslationsToNullKeepsAnEmptySet() {
    Constant constant = constant("Pi", "Ratio");
    constant.setTranslations(null);

    assertTrue(constant.getTranslations().isEmpty());
  }

  @Test
  void testPropertyValueByAttributeIdScheme() {
    Constant constant = constant("Pi", "Ratio");
    constant.setAttributeValues(AttributeValues.of(Map.of("AttrUid0001", "attribute value")));

    assertEquals(
        "attribute value", constant.getPropertyValue(IdScheme.from("ATTRIBUTE:AttrUid0001")));
  }

  @Test
  void testSetUserSetsCreatorAndSharingOwner() {
    User user = new User();
    user.setUid("UserUid0001");
    Constant constant = constant("Pi", "Ratio");
    constant.setUser(user);

    assertEquals(user, constant.getCreatedBy());
    assertEquals("UserUid0001", constant.getSharing().getOwner());
  }

  private static Constant constant(String name, String description) {
    Constant constant = new Constant(name, 3.14);
    constant.setUid("ConstantUi1");
    constant.setCode("CODE");
    constant.setDescription(description);
    return constant;
  }
}
