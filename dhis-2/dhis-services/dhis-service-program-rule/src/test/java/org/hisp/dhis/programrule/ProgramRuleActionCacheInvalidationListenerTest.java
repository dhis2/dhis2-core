/*
 * Copyright (c) 2004-2026, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
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
package org.hisp.dhis.programrule;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.hisp.dhis.program.Program;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProgramRuleActionCacheInvalidationListenerTest {

  @Mock private DefaultProgramRuleService programRuleService;

  @Mock private org.hibernate.event.spi.PostInsertEvent postInsertEvent;

  @Mock private org.hibernate.event.spi.PostUpdateEvent postUpdateEvent;

  @Mock private org.hibernate.event.spi.PostDeleteEvent postDeleteEvent;

  private ProgramRuleActionCacheInvalidationListener listener;

  @BeforeEach
  void setUp() {
    listener = new ProgramRuleActionCacheInvalidationListener(programRuleService);
  }

  @Test
  void invalidatesOnProgramRuleActionInsert() {
    when(postInsertEvent.getEntity()).thenReturn(new ProgramRuleAction());

    listener.onPostInsert(postInsertEvent);

    verify(programRuleService).invalidateProgramRuleActionCaches();
  }

  @Test
  void invalidatesOnProgramRuleActionUpdate() {
    when(postUpdateEvent.getEntity()).thenReturn(new ProgramRuleAction());

    listener.onPostUpdate(postUpdateEvent);

    verify(programRuleService).invalidateProgramRuleActionCaches();
  }

  @Test
  void invalidatesOnProgramRuleActionDelete() {
    when(postDeleteEvent.getEntity()).thenReturn(new ProgramRuleAction());

    listener.onPostDelete(postDeleteEvent);

    verify(programRuleService).invalidateProgramRuleActionCaches();
  }

  @Test
  void ignoresUnrelatedEntities() {
    when(postInsertEvent.getEntity()).thenReturn(new Program());

    listener.onPostInsert(postInsertEvent);

    verify(programRuleService, never()).invalidateProgramRuleActionCaches();
  }
}
