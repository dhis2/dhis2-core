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
package org.hisp.dhis.user;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.junit.jupiter.api.Test;

/**
 * Unit test of {@link InMemoryUserDetailsChangeTracker}.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
class InMemoryUserDetailsChangeTrackerTest {

  @Test
  void everyChangeOfAUserGetsANewToken() {
    UserDetailsChangeTracker tracker = trackerWithSessionTimeout(3600);

    tracker.markChanged(List.of("alice"));
    String first = tracker.getChangeToken("alice");
    tracker.markChanged(List.of("alice"));

    assertNotNull(first);
    assertNotEquals(first, tracker.getChangeToken("alice"));
  }

  @Test
  void changesAreForgottenAfterTheSessionTimeout() {
    UserDetailsChangeTracker tracker = trackerWithSessionTimeout(0);

    tracker.markChanged(List.of("alice"));

    assertNull(tracker.getChangeToken("alice"));
  }

  private static UserDetailsChangeTracker trackerWithSessionTimeout(int seconds) {
    DhisConfigurationProvider config = mock(DhisConfigurationProvider.class);
    when(config.getIntProperty(ConfigurationKey.SYSTEM_SESSION_TIMEOUT)).thenReturn(seconds);
    return new InMemoryUserDetailsChangeTracker(config);
  }
}
