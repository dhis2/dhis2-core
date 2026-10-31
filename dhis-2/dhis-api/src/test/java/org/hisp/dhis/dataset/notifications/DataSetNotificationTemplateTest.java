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
package org.hisp.dhis.dataset.notifications;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.notification.SendStrategy;
import org.hisp.dhis.program.notification.ProgramNotificationTemplate;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.sharing.Sharing;
import org.junit.jupiter.api.Test;

class DataSetNotificationTemplateTest {

  @Test
  void shouldEqualTemplateWithSameUidCodeAndName() {
    DataSetNotificationTemplate template = template("TemplateA01", "CODE_A", "Template A");
    DataSetNotificationTemplate same = template("TemplateA01", "CODE_A", "Template A");

    assertEquals(template, same);
    assertEquals(template.hashCode(), same.hashCode());
  }

  @Test
  void shouldNotEqualTemplateWithDifferentUidCodeOrName() {
    DataSetNotificationTemplate template = template("TemplateA01", "CODE_A", "Template A");

    assertNotEquals(template, template("TemplateB01", "CODE_A", "Template A"));
    assertNotEquals(template, template("TemplateA01", "CODE_B", "Template A"));
    assertNotEquals(template, template("TemplateA01", "CODE_A", "Template B"));
  }

  @Test
  void shouldNotEqualOtherTypeWithSameUidCodeAndName() {
    ProgramNotificationTemplate other = new ProgramNotificationTemplate();
    other.setUid("TemplateA01");
    other.setCode("CODE_A");
    other.setName("Template A");

    assertNotEquals(template("TemplateA01", "CODE_A", "Template A"), other);
  }

  @Test
  void shouldDefaultSendStrategyOnAutoFields() {
    DataSetNotificationTemplate template = new DataSetNotificationTemplate();
    template.setSendStrategy(null);

    template.setAutoFields();

    assertEquals(SendStrategy.SINGLE_NOTIFICATION, template.getSendStrategy());
    assertNotNull(template.getUid());
    assertNotNull(template.getCreated());
    assertEquals(template.getCreated(), template.getLastUpdated());
  }

  @Test
  void shouldReturnPropertyValueForIdScheme() {
    DataSetNotificationTemplate template = template("TemplateA01", "CODE_A", "Template A");
    template.setId(42);

    assertEquals("TemplateA01", template.getPropertyValue(IdScheme.UID));
    assertEquals("CODE_A", template.getPropertyValue(IdScheme.CODE));
    assertEquals("Template A", template.getPropertyValue(IdScheme.NAME));
    assertEquals("42", template.getPropertyValue(IdScheme.ID));
  }

  @Test
  void shouldStubSharingCreatedByAndAttributeValues() {
    DataSetNotificationTemplate template = new DataSetNotificationTemplate();
    Sharing sharing = template.getSharing();
    sharing.setOwner("ownerUid01");
    template.setCreatedBy(new User());

    assertNotSame(sharing, template.getSharing());
    assertNull(template.getSharing().getOwner());
    assertNull(template.getCreatedBy());
    assertTrue(template.getAttributeValues().isEmpty());
  }

  private static DataSetNotificationTemplate template(String uid, String code, String name) {
    DataSetNotificationTemplate template = new DataSetNotificationTemplate();
    template.setUid(uid);
    template.setCode(code);
    template.setName(name);
    return template;
  }
}
