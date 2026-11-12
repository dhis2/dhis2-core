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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

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
    AtomicLong now = new AtomicLong(1_000_000L);
    UserDetailsChangeTracker tracker =
        new InMemoryUserDetailsChangeTracker(Duration.ofSeconds(60), now::get);

    tracker.markChanged(List.of("alice"));
    now.addAndGet(59_999L);
    assertNotNull(tracker.getChangeToken("alice"));
    now.incrementAndGet();

    assertNull(tracker.getChangeToken("alice"));
  }

  /**
   * A session timeout that is not positive does not expire the sessions in time, so their changes
   * must not expire either. Before, such a timeout forgot every change at once.
   */
  @ParameterizedTest
  @ValueSource(strings = {"0", "-1", "1h"})
  void changesAreKeptWhenTheSessionTimeoutIsNotPositive(String sessionTimeout) {
    DhisConfigurationProvider config = mock(DhisConfigurationProvider.class);
    when(config.getProperty(ConfigurationKey.SYSTEM_SESSION_TIMEOUT)).thenReturn(sessionTimeout);
    UserDetailsChangeTracker tracker = new InMemoryUserDetailsChangeTracker(config);

    tracker.markChanged(List.of("alice"));

    assertNotNull(tracker.getChangeToken("alice"));
    assertEquals(Duration.ZERO, tracker.getChangeTokenTimeToLive());
  }

  private static UserDetailsChangeTracker trackerWithSessionTimeout(int seconds) {
    DhisConfigurationProvider config = mock(DhisConfigurationProvider.class);
    when(config.getProperty(ConfigurationKey.SYSTEM_SESSION_TIMEOUT))
        .thenReturn(String.valueOf(seconds));
    return new InMemoryUserDetailsChangeTracker(config);
  }
}
