/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.tracker;

import static org.hisp.dhis.common.OrganisationUnitSelectionMode.ACCESSIBLE;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.ALL;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.CAPTURE;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.CHILDREN;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.DESCENDANTS;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.SELECTED;
import static org.hisp.dhis.tracker.Assertions.assertNoErrors;
import static org.hisp.dhis.utils.Assertions.assertContainsOnly;
import static org.hisp.dhis.utils.Assertions.assertIsEmpty;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dxf2.events.event.Event;
import org.hisp.dhis.dxf2.events.event.EventQueryParams;
import org.hisp.dhis.dxf2.events.event.EventService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class AclEventExporterTest extends TrackerTest {

  @Autowired private EventService eventService;

  @Autowired private TrackerImportService trackerImportService;

  @Autowired private IdentifiableObjectManager manager;

  private OrganisationUnit orgUnit;

  private Program program;

  @Override
  protected void initTest() throws IOException {
    setUpMetadata("tracker/simple_metadata.json");
    User userA = userService.getUser("M5zQapPyTZI");
    assertNoErrors(
        trackerImportService.importTracker(
            fromJson("tracker/event_and_enrollment.json", userA.getUid())));
    orgUnit = get(OrganisationUnit.class, "h4w96yEMlzO");
    ProgramStage programStage = get(ProgramStage.class, "NpsdDv6kKSO");
    program = programStage.getProgram();

    // to test that events are only returned if the user has read access to ALL COs of an events COC
    CategoryOption categoryOption = get(CategoryOption.class, "yMj2MnmNI8L");
    categoryOption.getSharing().setOwner("o1HMTIzBGo7");
    manager.update(categoryOption);

    manager.flush();
  }

  @BeforeEach
  void setUp() {
    // needed as some tests are run using another user (injectSecurityContext) while most tests
    // expect to be run by admin
    injectAdminUser();
  }

  @Test
  void shouldReturnEventsWhenProgramClosedOuModeDescendantsAndOrgUnitInCaptureScope() {
    injectSecurityContext(userService.getUser("FIgVWzUCkpw"));
    EventQueryParams params = new EventQueryParams();
    params.setProgram(get(Program.class, "pcxIanBWlSY"));
    params.setOrgUnit(get(OrganisationUnit.class, "uoNW0E3xXUy"));
    params.setOrgUnitSelectionMode(DESCENDANTS);

    List<Event> events = eventService.getEvents(params).getEvents();

    assertFalse(
        events.isEmpty(),
        "Expected to find events when ou mode descendants and org units in capture scope");
    events.forEach(
        e ->
            assertEquals(
                "uoNW0E3xXUy",
                e.getOrgUnit(),
                "Expected to find descendant org unit uoNW0E3xXUy, but found "
                    + e.getOrgUnit()
                    + " instead"));
  }

  @Test
  void shouldReturnEventsWhenNoProgramSpecifiedOuModeDescendantsAndRootOrgUnitRequested() {
    injectSecurityContext(userService.getUser("FIgVWzUCkpw"));
    EventQueryParams params = new EventQueryParams();
    params.setOrgUnit(get(OrganisationUnit.class, orgUnit.getUid()));
    params.setOrgUnitSelectionMode(DESCENDANTS);

    List<Event> events = eventService.getEvents(params).getEvents();

    assertFalse(
        events.isEmpty(),
        "Expected to find events when no program specified, ou mode descendants and org units in search scope");
    assertContainsOnly(
        events.stream().map(Event::getUid).collect(Collectors.toSet()),
        List.of(
            "YKmfzHdjUDL",
            "jxgFyJEMUPf",
            "D9PbzJY8bJM",
            "pTzf9KYMk72",
            "JaRDIvcEcEx",
            "SbUJzkxKYAG",
            "gvULMgNiAfM"));
  }

  @Test
  void shouldReturnEventsWhenNoProgramSpecifiedOuModeDescendantsAndOrgUnitInSearchScope() {
    injectSecurityContext(userService.getUser("FIgVWzUCkpw"));
    EventQueryParams params = new EventQueryParams();
    params.setOrgUnit(get(OrganisationUnit.class, "uoNW0E3xXUy"));
    params.setOrgUnitSelectionMode(DESCENDANTS);

    List<Event> events = eventService.getEvents(params).getEvents();

    assertFalse(
        events.isEmpty(),
        "Expected to find events when no program specified, ou mode descendants and org units in search scope");
    assertContainsOnly(
        events.stream().map(Event::getUid).collect(Collectors.toSet()),
        List.of("jxgFyJEMUPf", "JaRDIvcEcEx", "SbUJzkxKYAG", "gvULMgNiAfM"));
  }

  @Test
  void shouldReturnEventsWhenProgramClosedOuModeChildrenAndOrgUnitInCaptureScope() {
    injectSecurityContext(userService.getUser("FIgVWzUCkpw"));
    EventQueryParams params = new EventQueryParams();
    params.setProgram(get(Program.class, "pcxIanBWlSY"));
    params.setOrgUnit(get(OrganisationUnit.class, "uoNW0E3xXUy"));
    params.setOrgUnitSelectionMode(CHILDREN);

    List<Event> events = eventService.getEvents(params).getEvents();

    assertFalse(
        events.isEmpty(),
        "Expected to find events when ou mode children and org units in capture scope");
    events.forEach(
        e ->
            assertEquals(
                "uoNW0E3xXUy",
                e.getOrgUnit(),
                "Expected to find children org unit uoNW0E3xXUy, but found "
                    + e.getOrgUnit()
                    + " instead"));
  }

  @Test
  void shouldReturnEventsWhenNoProgramSpecifiedOuModeChildrenAndOrgUnitInSearchScope() {
    injectSecurityContext(userService.getUser("FIgVWzUCkpw"));
    EventQueryParams params = new EventQueryParams();
    params.setOrgUnit(get(OrganisationUnit.class, orgUnit.getUid()));
    params.setOrgUnitSelectionMode(CHILDREN);

    List<Event> events = eventService.getEvents(params).getEvents();

    assertFalse(
        events.isEmpty(),
        "Expected to find events when no program specified, ou mode children and org units in search scope");
    assertContainsOnly(
        List.of("YKmfzHdjUDL", "jxgFyJEMUPf", "JaRDIvcEcEx", "D9PbzJY8bJM", "pTzf9KYMk72"),
        events.stream().map(Event::getUid).collect(Collectors.toSet()));
  }

  @Test
  void shouldReturnEventsWhenProgramClosedOuModeSelectedAndOrgUnitInCaptureScope() {
    injectSecurityContext(userService.getUser("FIgVWzUCkpw"));
    EventQueryParams params = new EventQueryParams();
    params.setProgram(get(Program.class, "pcxIanBWlSY"));
    params.setOrgUnit(get(OrganisationUnit.class, "uoNW0E3xXUy"));
    params.setOrgUnitSelectionMode(SELECTED);

    List<Event> events = eventService.getEvents(params).getEvents();

    assertFalse(
        events.isEmpty(),
        "Expected to find events when ou mode selected and org units in capture scope");
    events.forEach(
        e ->
            assertEquals(
                "uoNW0E3xXUy",
                e.getOrgUnit(),
                "Expected to find selected org unit uoNW0E3xXUy, but found "
                    + e.getOrgUnit()
                    + " instead"));
  }

  @Test
  void shouldReturnEventsWhenNoProgramSpecifiedOuModeSelectedAndOrgUnitInSearchScope() {
    injectSecurityContext(userService.getUser("nIidJVYpQQK"));

    EventQueryParams params = new EventQueryParams();
    params.setOrgUnit(get(OrganisationUnit.class, "DiszpKrYNg8"));
    params.setOrgUnitSelectionMode(SELECTED);

    List<Event> events = eventService.getEvents(params).getEvents();

    assertFalse(
        events.isEmpty(),
        "Expected to find events when no program specified, ou mode descendants and org units in search scope");

    assertContainsOnly(
        List.of("ck7DzdxqLqA", "OTmjvJDn0Fu", "kWjSezkXHVp"),
        events.stream().map(Event::getUid).collect(Collectors.toList()));
  }

  @Test
  void shouldReturnNoEventsWhenProgramOpenOuModeSelectedAndNoProgramEvents() {
    injectSecurityContext(userService.getUser("FIgVWzUCkpw"));

    EventQueryParams params = new EventQueryParams();
    params.setProgram(get(Program.class, "shPjYNifvMK"));
    params.setOrgUnit(get(OrganisationUnit.class, orgUnit.getUid()));
    params.setOrgUnitSelectionMode(SELECTED);

    List<Event> events = eventService.getEvents(params).getEvents();

    assertTrue(events.isEmpty(), "Expected to find no events, but found: " + events.size());
  }

  @Test
  void shouldReturnEventsWhenProgramClosedOuModeAccessible() {
    injectSecurityContext(userService.getUser("FIgVWzUCkpw"));

    EventQueryParams params = new EventQueryParams();
    params.setProgram(get(Program.class, "pcxIanBWlSY"));
    params.setOrgUnitSelectionMode(ACCESSIBLE);

    List<Event> events = eventService.getEvents(params).getEvents();

    assertFalse(
        events.isEmpty(), "Expected to find events when ou mode accessible and program closed");
    events.forEach(
        e ->
            assertEquals(
                "uoNW0E3xXUy",
                e.getOrgUnit(),
                "Expected to find accessible org unit uoNW0E3xXUy, but found "
                    + e.getOrgUnit()
                    + " instead"));
  }

  @Test
  void shouldReturnEventsWhenProgramOpenOuModeAccessible() {
    injectSecurityContext(userService.getUser("FIgVWzUCkpw"));

    EventQueryParams params = new EventQueryParams();
    params.setProgram(get(Program.class, program.getUid()));
    params.setOrgUnitSelectionMode(ACCESSIBLE);

    List<Event> events = eventService.getEvents(params).getEvents();

    assertFalse(
        events.isEmpty(), "Expected to find events when ou mode accessible and program open");
    events.forEach(
        e ->
            assertEquals(
                "h4w96yEMlzO",
                e.getOrgUnit(),
                "Expected to find accessible org unit h4w96yEMlzO, but found "
                    + e.getOrgUnit()
                    + " instead"));
  }

  @Test
  void shouldReturnEventsWhenProgramOpenOuModeCapture() {
    injectSecurityContext(userService.getUser("FIgVWzUCkpw"));

    EventQueryParams params = new EventQueryParams();
    params.setProgram(get(Program.class, "pcxIanBWlSY"));
    params.setOrgUnitSelectionMode(CAPTURE);

    List<Event> events = eventService.getEvents(params).getEvents();

    assertFalse(
        events.isEmpty(), "Expected to find events when ou mode capture and program closed");
    events.forEach(
        e ->
            assertEquals(
                "uoNW0E3xXUy",
                e.getOrgUnit(),
                "Expected to find capture org unit uoNW0E3xXUy, but found "
                    + e.getOrgUnit()
                    + " instead"));
  }

  @Test
  void shouldReturnEmptyCollectionWhenProgramIsClosedAndOrgUnitNotInSearchScope() {
    injectSecurityContext(userService.getUser("FIgVWzUCkpw"));

    EventQueryParams params = new EventQueryParams();
    params.setProgram(get(Program.class, "pcxIanBWlSY"));
    params.setOrgUnit(get(OrganisationUnit.class, "DiszpKrYNg8"));
    params.setOrgUnitSelectionMode(DESCENDANTS);

    assertIsEmpty(eventService.getEvents(params).getEvents());
  }

  @Test
  void shouldReturnAllEventsWhenOrgUnitModeAllAndNoOrgUnitProvided() {
    injectSecurityContext(userService.getUser("lPaILkLkgOM"));
    EventQueryParams params = new EventQueryParams();
    params.setOrgUnitSelectionMode(ALL);
    List<Event> events = eventService.getEvents(params).getEvents();
    assertFalse(
        events.isEmpty(),
        "Expected to find events when ou mode ALL no program specified and no org unit provided");
    assertContainsOnly(
        List.of(
            "lbDXJBlvtZe",
            "uoNW0E3xXUy",
            "RojfDTBhoGC",
            "tSsGrtfRzjY",
            "h4w96yEMlzO",
            "DiszpKrYNg8"),
        events.stream().map(Event::getOrgUnit).collect(Collectors.toSet()));
  }

  @Test
  void shouldIgnoreRequestedOrgUnitAndReturnAllEventsWhenOrgUnitModeAllAndOrgUnitProvided() {
    injectSecurityContext(userService.getUser("lPaILkLkgOM"));
    EventQueryParams params = new EventQueryParams();
    params.setOrgUnit(get(OrganisationUnit.class, "uoNW0E3xXUy"));
    params.setOrgUnitSelectionMode(ALL);
    List<Event> events = eventService.getEvents(params).getEvents();
    assertFalse(
        events.isEmpty(),
        "Expected to find events when ou mode ALL no program specified and org unit provided");
    assertContainsOnly(
        List.of(
            "lbDXJBlvtZe",
            "uoNW0E3xXUy",
            "RojfDTBhoGC",
            "tSsGrtfRzjY",
            "h4w96yEMlzO",
            "DiszpKrYNg8"),
        events.stream().map(Event::getOrgUnit).collect(Collectors.toSet()));
  }

  @Test
  void shouldReturnAllEventsWhenOrgUnitModeAllAndNoOrgUnitProvidedAndUserNull() {
    injectSecurityContext(null);
    EventQueryParams params = new EventQueryParams();
    params.setOrgUnitSelectionMode(ALL);
    List<Event> events = eventService.getEvents(params).getEvents();
    assertFalse(
        events.isEmpty(),
        "Expected to find events when ou mode ALL no program specified and no org unit provided");
    assertContainsOnly(
        List.of(
            "lbDXJBlvtZe",
            "uoNW0E3xXUy",
            "RojfDTBhoGC",
            "tSsGrtfRzjY",
            "h4w96yEMlzO",
            "DiszpKrYNg8"),
        events.stream().map(Event::getOrgUnit).collect(Collectors.toSet()));
  }

  @Test
  void shouldReturnAllOrgUnitEventsWhenOrgUnitModeAllAndNoOrgUnitProvided() {
    injectSecurityContext(userService.getUser("lPaILkLkgOM"));

    EventQueryParams params = new EventQueryParams();
    params.setOrgUnitSelectionMode(ALL);

    List<Event> events = eventService.getEvents(params).getEvents();

    assertFalse(
        events.isEmpty(),
        "Expected to find events when ou mode ALL no program specified and no org unit provided");
    assertContainsOnly(
        List.of(
            "h4w96yEMlzO",
            "uoNW0E3xXUy",
            "DiszpKrYNg8",
            "tSsGrtfRzjY",
            "lbDXJBlvtZe",
            "RojfDTBhoGC"),
        events.stream().map(Event::getOrgUnit).collect(Collectors.toSet()));
  }

  @Test
  void shouldReturnAllOrgUnitEventsWhenOrgUnitModeAllAndOrgUnitProvided() {
    injectSecurityContext(userService.getUser("lPaILkLkgOM"));

    EventQueryParams params = new EventQueryParams();
    params.setOrgUnit(get(OrganisationUnit.class, "uoNW0E3xXUy"));
    params.setOrgUnitSelectionMode(ALL);

    List<Event> events = eventService.getEvents(params).getEvents();

    assertFalse(
        events.isEmpty(),
        "Expected to find events when ou mode ALL no program specified and no org unit provided");
    assertContainsOnly(
        List.of(
            "h4w96yEMlzO",
            "uoNW0E3xXUy",
            "DiszpKrYNg8",
            "tSsGrtfRzjY",
            "lbDXJBlvtZe",
            "RojfDTBhoGC"),
        events.stream().map(Event::getOrgUnit).collect(Collectors.toSet()));
  }

  private <T extends IdentifiableObject> T get(Class<T> type, String uid) {
    T t = manager.get(type, uid);
    assertNotNull(t, () -> String.format("metadata with uid '%s' should have been created", uid));
    return t;
  }
}
