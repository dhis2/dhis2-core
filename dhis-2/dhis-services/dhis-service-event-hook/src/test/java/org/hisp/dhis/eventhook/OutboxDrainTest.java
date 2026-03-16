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
package org.hisp.dhis.eventhook;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Properties;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.leader.election.LeaderManager;
import org.hisp.dhis.test.TestBase;
import org.hisp.dhis.test.config.TestDhisConfigurationProvider;
import org.junit.jupiter.api.Test;

class OutboxDrainTest extends TestBase {

  @Test
  void testDrainOutboxesDoesNotRunWhenEventHooksEnabledConfigurationKeyIsFalse() {
    Properties properties = new Properties();
    properties.setProperty(ConfigurationKey.EVENT_HOOKS_ENABLED.getKey(), "false");

    EventHookService mockEventHookService = mock(EventHookService.class);
    when(mockEventHookService.getEventHookTargets()).thenReturn(Collections.emptyList());

    OutboxDrain outboxDrain =
        new OutboxDrain(
            new LeaderManager() {
              @Override
              public void renewLeader(int ttlSeconds) {}

              @Override
              public void electLeader(int ttlSeconds) {}

              @Override
              public boolean isLeader() {
                return true;
              }

              @Override
              public String getCurrentNodeUuid() {
                return "";
              }

              @Override
              public String getLeaderNodeUuid() {
                return "";
              }

              @Override
              public String getLeaderNodeId() {
                return "";
              }
            },
            null,
            mockEventHookService,
            new TestDhisConfigurationProvider(properties),
            null);
    outboxDrain.drainOutboxes();

    verifyNoInteractions(mockEventHookService);
  }

  @Test
  void testDrainOutboxesDoesNotRunWhenIsNotLeader() {
    Properties properties = new Properties();
    properties.setProperty(ConfigurationKey.EVENT_HOOKS_ENABLED.getKey(), "true");

    EventHookService mockEventHookService = mock(EventHookService.class);
    when(mockEventHookService.getEventHookTargets()).thenReturn(Collections.emptyList());

    OutboxDrain outboxDrain =
        new OutboxDrain(
            new LeaderManager() {
              @Override
              public void renewLeader(int ttlSeconds) {}

              @Override
              public void electLeader(int ttlSeconds) {}

              @Override
              public boolean isLeader() {
                return false;
              }

              @Override
              public String getCurrentNodeUuid() {
                return "";
              }

              @Override
              public String getLeaderNodeUuid() {
                return "";
              }

              @Override
              public String getLeaderNodeId() {
                return "";
              }
            },
            null,
            null,
            new TestDhisConfigurationProvider(properties),
            null);
    outboxDrain.drainOutboxes();

    verifyNoInteractions(mockEventHookService);
  }
}
