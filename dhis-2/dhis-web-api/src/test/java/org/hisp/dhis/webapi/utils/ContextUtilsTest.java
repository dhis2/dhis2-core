/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.webapi.utils;

import static org.hisp.dhis.test.TestBase.getDate;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.Date;
import org.hisp.dhis.common.HashUtils;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserDetails;
import org.junit.jupiter.api.Test;

class ContextUtilsTest {
  @Test
  void testGetEtag() {
    Date date = getDate(2022, 03, 10);
    User user = new User();
    user.setUid("kYt56BgfED2");

    // Expected hash is timezone-dependent via TestBase#getDate (Joda LocalDateTime -> Date).
    // Assert against the same formula ContextUtils uses so the test is TZ-stable.
    String expected =
        HashUtils.hashMD5(String.format("%d-%s", date.getTime(), user.getUid()).getBytes());
    assertEquals(expected, ContextUtils.getEtag(date, UserDetails.fromUser(user)));
  }

  @Test
  void testGetEtagDistinguishesSubSecondChanges() {
    User user = new User();
    user.setUid("kYt56BgfED2");
    UserDetails userDetails = UserDetails.fromUser(user);

    Date date = getDate(2022, 03, 10);
    Date oneMilliLater = new Date(date.getTime() + 1);

    assertNotEquals(
        ContextUtils.getEtag(date, userDetails), ContextUtils.getEtag(oneMilliLater, userDetails));
  }

  @Test
  void testQuote() {
    assertEquals("\"2022-03-10T00:00:00\"", ContextUtils.quote("2022-03-10T00:00:00"));
  }
}
