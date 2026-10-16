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
package org.hisp.dhis.sms.outbound;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Set;
import org.hisp.dhis.common.Locale;
import org.hisp.dhis.translation.Translation;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.sharing.Sharing;
import org.junit.jupiter.api.Test;

class OutboundSmsTest {

  @Test
  void testEqualsAndHashCodeCompareUidAndRealClass() {
    OutboundSms a = sms("outSmsUidA1", "Message A");
    OutboundSms sameUid = sms("outSmsUidA1", "Other message");
    OutboundSms otherUid = sms("outSmsUidB1", "Message A");
    OutboundSms subclass = new OutboundSms() {};
    subclass.setUid("outSmsUidA1");

    assertEquals(a, sameUid);
    assertEquals(a.hashCode(), sameUid.hashCode());
    assertNotEquals(a, otherUid);
    assertNotEquals(a, subclass);
  }

  @Test
  void testDefaultConstructorSetsUidAndTimestamps() {
    OutboundSms sms = new OutboundSms();

    assertNotNull(sms.getUid());
    assertNotNull(sms.getCreated());
    assertNotNull(sms.getLastUpdated());
    assertEquals(OutboundSmsStatus.OUTBOUND, sms.getStatus());
  }

  @Test
  void testUnsupportedPropertiesIgnoreWrites() {
    OutboundSms sms = sms("outSmsUidA1", "Message");
    Sharing sharing = new Sharing();
    sharing.setOwner("ownerUid123");

    sms.setCode("CODE");
    sms.setName("Name");
    sms.setSharing(sharing);
    sms.setOwner("ownerUid123");
    sms.setCreatedBy(new User());
    sms.setUser(new User());
    sms.setLastUpdatedBy(new User());

    assertNull(sms.getCode());
    assertNull(sms.getName());
    assertNull(sms.getDisplayName());
    assertNull(sms.getCreatedBy());
    assertNull(sms.getUser());
    assertNull(sms.getLastUpdatedBy());
    assertNull(sms.getSharing().getOwner());
    assertNotSame(sms.getSharing(), sms.getSharing());
  }

  @Test
  void testTranslationsAreKeptInMemory() {
    OutboundSms sms = sms("outSmsUidA1", "Message");
    Set<Translation> translations = Set.of(new Translation(Locale.of("fr"), "NAME", "x"));

    sms.setTranslations(translations);

    assertEquals(translations, sms.getTranslations());
  }

  /** The JSON body of POST /api/sms/outbound, including properties the entity does not store. */
  @Test
  void testDeserializeSendRequestBody() throws Exception {
    String json =
        """
        {"message": "Hello", "recipients": ["+4711111111", "+4722222222"],
         "code": "CODE", "name": "Name", "attributeValues": [],
         "sharing": {"owner": "ownerUid123", "public": "rw------"},
         "translations": [{"locale": "fr", "property": "NAME", "value": "x"}]}
        """;

    OutboundSms sms = new ObjectMapper().readValue(json, OutboundSms.class);

    assertEquals("Hello", sms.getMessage());
    assertEquals(Set.of("+4711111111", "+4722222222"), sms.getRecipients());
    assertNull(sms.getCode());
    assertNull(sms.getName());
    assertNull(sms.getSharing().getOwner());
    assertEquals(1, sms.getTranslations().size());
  }

  private static OutboundSms sms(String uid, String message) {
    OutboundSms sms = new OutboundSms("Subject", message, Set.of("+4711111111"));
    sms.setUid(uid);
    return sms;
  }
}
