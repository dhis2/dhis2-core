/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.webapi.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.hisp.dhis.test.webapi.H2ControllerIntegrationTestBase;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.UserDetails;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author david mackessy
 */
@Transactional
class CurrentUserUtilTest extends H2ControllerIntegrationTestBase {

  @Test
  void testCurrentUserDetailsIsSuper() {
    switchToNewUser("newSuperuser", "ALL");
    UserDetails newSuperuser = CurrentUserUtil.getCurrentUserDetails();
    assertNotNull(newSuperuser);
    assertEquals("newSuperuser", newSuperuser.getUsername());
    assertTrue(newSuperuser.isSuper());
  }

  @Test
  void testCurrentUserDetailsIsNotSuper() {
    switchToNewUser("basicUser", "NONE");
    UserDetails basicUser = CurrentUserUtil.getCurrentUserDetails();
    assertNotNull(basicUser);
    assertEquals("basicUser", basicUser.getUsername());
    assertFalse(basicUser.isSuper());
  }

  @Test
  void testCurrentUserDetailsThrowsException() {
    clearSecurityContext();
    RuntimeException exception =
        assertThrows(RuntimeException.class, CurrentUserUtil::getCurrentUserDetails);
    assertEquals("No authentication found", exception.getMessage());
  }

  @Test
  void testCurrentUsernameThrowsException() {
    clearSecurityContext();
    RuntimeException exception =
        assertThrows(RuntimeException.class, CurrentUserUtil::getCurrentUsername);
    assertEquals("No authentication found", exception.getMessage());
  }
}
