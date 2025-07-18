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
package org.hisp.dhis.tracker.imports.bundle;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.eventdatavalue.EventDataValue;
import org.hisp.dhis.program.TrackerEvent;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.hisp.dhis.tracker.TestSetup;
import org.hisp.dhis.tracker.imports.TrackerImportParams;
import org.hisp.dhis.tracker.imports.TrackerImportService;
import org.hisp.dhis.tracker.imports.TrackerImportStrategy;
import org.hisp.dhis.user.User;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Transactional
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EventDataValueTest extends PostgresIntegrationTestBase {

  @Autowired private TestSetup testSetup;

  @Autowired private TrackerImportService trackerImportService;

  @Autowired private IdentifiableObjectManager manager;

  @BeforeAll
  void setUp() throws IOException {
    testSetup.importMetadata();

    final User userA = userService.getUser("tTgjgobT1oS");
    injectSecurityContextUser(userA);

    testSetup.importTrackerData("tracker/single_te.json");
    testSetup.importTrackerData("tracker/single_enrollment.json");
    manager.flush();
  }

  @Test
  void successWhenEventHasNoProgramAndHasProgramStage() throws IOException {
    testSetup.importTrackerData("tracker/validations/events-with_no_program.json");
  }

  @Test
  void testEventDataValue() throws IOException {
    testSetup.importTrackerData("tracker/event_with_data_values.json");

    List<TrackerEvent> events = manager.getAll(TrackerEvent.class);
    assertEquals(1, events.size());
    TrackerEvent event = events.get(0);
    Set<EventDataValue> eventDataValues = event.getEventDataValues();
    assertEquals(4, eventDataValues.size());
  }

  @Test
  void testEventDataValueUpdate() throws IOException {
    testSetup.importTrackerData("tracker/event_with_data_values.json");

    List<TrackerEvent> events = manager.getAll(TrackerEvent.class);
    assertEquals(1, events.size());
    TrackerEvent event = events.get(0);
    Set<EventDataValue> eventDataValues = event.getEventDataValues();
    assertEquals(4, eventDataValues.size());
    // update

    TrackerImportParams params = new TrackerImportParams();
    params.setImportStrategy(TrackerImportStrategy.CREATE_AND_UPDATE);
    testSetup.importTrackerData("tracker/event_with_updated_data_values.json", params);

    List<TrackerEvent> updatedEvents = manager.getAll(TrackerEvent.class);
    assertEquals(1, updatedEvents.size());
    TrackerEvent updatedEvent = manager.get(TrackerEvent.class, updatedEvents.get(0).getUid());
    assertEquals(3, updatedEvent.getEventDataValues().size());
    List<String> values =
        updatedEvent.getEventDataValues().stream()
            .map(EventDataValue::getValue)
            .collect(Collectors.toList());
    assertThat(values, hasItem("First"));
    assertThat(values, hasItem("Second"));
    assertThat(values, hasItem("Fourth updated"));

    Map<String, EventDataValue> dataValueMap =
        eventDataValues.stream()
            .collect(Collectors.toMap(EventDataValue::getDataElement, ev -> ev));
    Map<String, EventDataValue> updatedDataValueMap =
        updatedEvent.getEventDataValues().stream()
            .collect(Collectors.toMap(EventDataValue::getDataElement, ev -> ev));

    String updatedDataElementId = "DATAEL00004";
    assertEquals(
        dataValueMap.get(updatedDataElementId).getCreated(),
        updatedDataValueMap.get(updatedDataElementId).getCreated());
    assertEquals("Fourth updated", updatedDataValueMap.get(updatedDataElementId).getValue());
  }
}
