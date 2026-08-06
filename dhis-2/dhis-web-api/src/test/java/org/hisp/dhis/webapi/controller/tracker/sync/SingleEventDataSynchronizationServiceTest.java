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
package org.hisp.dhis.webapi.controller.tracker.sync;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.program.ProgramStageDataElementService;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.setting.SystemSettingsService;
import org.hisp.dhis.tracker.TrackerType;
import org.hisp.dhis.tracker.export.singleevent.SingleEventService;
import org.hisp.dhis.webapi.controller.tracker.view.Event;
import org.hisp.dhis.webapi.controller.tracker.view.Relationship;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
class SingleEventDataSynchronizationServiceTest {

  @Mock private SingleEventService singleEventService;
  @Mock private ProgramStageDataElementService programStageDataElementService;
  @Mock private SystemSettingsService systemSettingsService;
  @Mock private RestTemplate restTemplate;
  @Mock private RenderService renderService;

  private SingleEventDataSynchronizationService service;

  @BeforeEach
  void setUp() {
    service =
        new SingleEventDataSynchronizationService(
            singleEventService,
            systemSettingsService,
            programStageDataElementService,
            restTemplate,
            renderService);
  }

  @Test
  void shouldStripDeletedRelationshipsFromActiveEvent() {
    Relationship deletedRelationship =
        Relationship.builder().relationship(UID.of("Rel00000001")).deleted(true).build();
    Relationship activeRelationship =
        Relationship.builder().relationship(UID.of("Rel00000002")).deleted(false).build();

    Event event =
        Event.builder()
            .event(UID.of("Event000001"))
            .relationships(List.of(deletedRelationship, activeRelationship))
            .build();

    BaseDataSynchronizationWithPaging.NestedDeletion<Event> result =
        service.splitDeletedChildren(List.of(event));

    assertEquals(1, result.deletedCountByType().get(TrackerType.RELATIONSHIP));
    assertEquals(List.of(activeRelationship), event.getRelationships());
    assertEquals(
        Set.of(deletedRelationship.getRelationship()),
        result.deletedChildUidsByParent().get(event.getEvent()));
  }

  @Test
  void shouldReportFailedChildWhenNestedRelationshipFailed() {
    Relationship relationship = Relationship.builder().relationship(UID.of("Rel00000001")).build();
    Event event =
        Event.builder().event(UID.of("Event000001")).relationships(List.of(relationship)).build();

    boolean hasFailedChild =
        service.hasFailedChild(
            event, Map.of(TrackerType.RELATIONSHIP, Set.of(UID.of("Rel00000001"))));

    assertTrue(hasFailedChild);
  }

  @Test
  void shouldNotReportFailedChildWhenNoRelationshipFailed() {
    Relationship relationship = Relationship.builder().relationship(UID.of("Rel00000001")).build();
    Event event =
        Event.builder().event(UID.of("Event000001")).relationships(List.of(relationship)).build();

    boolean hasFailedChild =
        service.hasFailedChild(
            event, Map.of(TrackerType.RELATIONSHIP, Set.of(UID.of("SomeOtherOn"))));

    assertFalse(hasFailedChild);
  }
}
