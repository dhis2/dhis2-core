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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@ExtendWith(MockitoExtension.class)
class DefaultLoginEventServiceTest {

  @Mock private LoginEventStore loginEventStore;

  @Mock private DhisConfigurationProvider config;

  @BeforeEach
  void setUp() {
    DefaultLoginEventService.clearDedupCache();
  }

  private DefaultLoginEventService service(String excluded) {
    when(config.getProperty(ConfigurationKey.SYSTEM_USER_STATS_EXCLUDED_USERS))
        .thenReturn(excluded);
    return new DefaultLoginEventService(loginEventStore, config);
  }

  @Test
  void excludedUsernameIsNeverRecorded() {
    DefaultLoginEventService service = service("prometheus-scraper, other-bot");

    service.recordLogin("prometheus-scraper", LoginAuthType.API_TOKEN);
    service.recordLogin("other-bot", LoginAuthType.BASIC);

    verify(loginEventStore, never()).save(any());
  }

  @Test
  void normalUsernameIsRecordedOnceAndDeduped() {
    DefaultLoginEventService service = service("prometheus-scraper");

    service.recordLogin("alice", LoginAuthType.FORM);
    service.recordLogin("alice", LoginAuthType.FORM);

    verify(loginEventStore, times(1)).save(any(LoginEvent.class));
  }

  @Test
  void parseExcludedUsernamesHandlesWhitespaceAndEmpty() {
    assertEquals(Set.of(), DefaultLoginEventService.parseExcludedUsernames(null));
    assertEquals(Set.of(), DefaultLoginEventService.parseExcludedUsernames("  "));
    assertEquals(Set.of("a", "b"), DefaultLoginEventService.parseExcludedUsernames(" a , b ,, "));
  }
}
