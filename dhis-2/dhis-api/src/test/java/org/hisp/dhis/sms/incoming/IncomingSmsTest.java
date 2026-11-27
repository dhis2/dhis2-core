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
package org.hisp.dhis.sms.incoming;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.Set;
import org.hisp.dhis.common.Locale;
import org.hisp.dhis.translation.Translation;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.sharing.Sharing;
import org.junit.jupiter.api.Test;

class IncomingSmsTest {

  @Test
  void testEqualsAndHashCodeCompareUidAndRealClass() {
    IncomingSms a = sms("inSmsUidA01", "Text A");
    IncomingSms sameUid = sms("inSmsUidA01", "Other text");
    IncomingSms otherUid = sms("inSmsUidB01", "Text A");
    IncomingSms subclass = new IncomingSms() {};
    subclass.setUid("inSmsUidA01");

    assertEquals(a, sameUid);
    assertEquals(a.hashCode(), sameUid.hashCode());
    assertNotEquals(a, otherUid);
    assertNotEquals(a, subclass);
  }

  @Test
  void testDefaultConstructorSetsUidTimestampsAndDefaults() {
    IncomingSms sms = new IncomingSms();

    assertNotNull(sms.getUid());
    assertNotNull(sms.getCreated());
    assertNotNull(sms.getLastUpdated());
    assertSame(SmsMessageEncoding.ENC7BIT, sms.getEncoding());
    assertSame(SmsMessageStatus.INCOMING, sms.getStatus());
    assertFalse(sms.isParsed());
  }

  @Test
  void testUserIsTheCreatedByUser() {
    User sender = new User();
    User other = new User();
    IncomingSms sms = sms("inSmsUidA01", "Text");

    sms.setUser(sender);
    sms.setUser(other);

    assertSame(sender, sms.getCreatedBy());
    assertSame(sender, sms.getUser());
  }

  @Test
  void testUnsupportedPropertiesIgnoreWrites() {
    IncomingSms sms = sms("inSmsUidA01", "Text");
    Sharing sharing = new Sharing();
    sharing.setOwner("ownerUid123");

    sms.setCode("CODE");
    sms.setName("Name");
    sms.setSharing(sharing);
    sms.setOwner("ownerUid123");
    sms.setLastUpdatedBy(new User());
    sms.getSharing().setPublicAccess("rw------");

    assertNull(sms.getCode());
    assertNull(sms.getName());
    assertNull(sms.getDisplayName());
    assertNull(sms.getLastUpdatedBy());
    assertNull(sms.getSharing().getOwner());
    assertNull(sms.getSharing().getPublicAccess());
  }

  @Test
  void testTranslationsAreKeptInMemory() {
    IncomingSms sms = sms("inSmsUidA01", "Text");
    Set<Translation> translations = Set.of(new Translation(Locale.of("fr"), "NAME", "x"));

    sms.setTranslations(translations);

    assertEquals(translations, sms.getTranslations());
  }

  private static IncomingSms sms(String uid, String text) {
    IncomingSms sms = new IncomingSms();
    sms.setUid(uid);
    sms.setText(text);
    return sms;
  }
}
