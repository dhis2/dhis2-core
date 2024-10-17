/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.dxf2.events.event;

import static org.hisp.dhis.utils.Assertions.assertContainsOnly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hisp.dhis.dxf2.events.event.persistence.EventPersistenceService;
import org.hisp.dhis.dxf2.events.importer.Checker;
import org.hisp.dhis.dxf2.events.importer.EventManager;
import org.hisp.dhis.dxf2.events.importer.EventProcessorExecutor;
import org.hisp.dhis.dxf2.events.importer.EventProcessorPhase;
import org.hisp.dhis.dxf2.events.importer.context.WorkContext;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.trackedentitydatavalue.TrackedEntityDataValueAuditService;
import org.hisp.dhis.user.CurrentUserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Zubair Asghar
 */
@ExtendWith(MockitoExtension.class)
class EventManagerTest {

  @Mock EventPersistenceService eventPersistenceService;

  @Mock Map<EventProcessorPhase, EventProcessorExecutor> executorsByPhase;

  @Mock EventProcessorExecutor eventProcessorExecutor;

  @Mock TrackedEntityDataValueAuditService entityDataValueAuditService;

  @Mock WorkContext workContext;

  @Mock List<Checker> checkersRunOnDelete;

  @Mock CurrentUserService currentUserService;

  @Captor ArgumentCaptor<EventProcessorPhase> eventProcessorPhaseArgumentCaptor;

  @InjectMocks EventManager subject;

  @Test
  void shouldTriggerUpdatePostProcessorsWhenEventDataValuesUpdated()
      throws JsonProcessingException {

    Event event = new Event();
    event.setUid("id");
    doNothing()
        .when(eventPersistenceService)
        .updateTrackedEntityInstances(any(WorkContext.class), anyList());

    doNothing().when(eventProcessorExecutor).execute(any(WorkContext.class), anyList());

    when(executorsByPhase.get(any(EventProcessorPhase.class))).thenReturn(eventProcessorExecutor);
    when(workContext.getPersistedProgramStageInstanceMap())
        .thenReturn(Map.of("id", new ProgramStageInstance()));
    subject.updateEventDataValues(event, Set.of(), workContext);

    verify(eventPersistenceService, times(1))
        .updateTrackedEntityInstances(any(WorkContext.class), anyList());
    verify(executorsByPhase, times(2)).get(eventProcessorPhaseArgumentCaptor.capture());

    List<EventProcessorPhase> phases = eventProcessorPhaseArgumentCaptor.getAllValues();

    assertContainsOnly(
        List.of(EventProcessorPhase.UPDATE_PRE, EventProcessorPhase.UPDATE_POST), phases);
  }
}
