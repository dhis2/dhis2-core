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
package org.hisp.dhis.dxf2.deprecated.tracker;

import static org.hisp.dhis.common.OrganisationUnitSelectionMode.ACCESSIBLE;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.ALL;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.CAPTURE;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.CHILDREN;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.DESCENDANTS;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.SELECTED;
import static org.hisp.dhis.tracker.Assertions.assertNoErrors;
import static org.hisp.dhis.utils.Assertions.assertContainsOnly;
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
import org.hisp.dhis.dxf2.deprecated.tracker.event.Event;
import org.hisp.dhis.dxf2.deprecated.tracker.event.EventSearchParams;
import org.hisp.dhis.dxf2.deprecated.tracker.event.EventService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.tracker.TrackerTest;
import org.hisp.dhis.tracker.imports.TrackerImportParams;
import org.hisp.dhis.tracker.imports.TrackerImportService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class AclEventExporterTest extends TrackerTest {

  @Autowired private EventService eventService;

  @Autowired private TrackerImportService trackerImportService;

  @Autowired private IdentifiableObjectManager manager;
  @Autowired protected UserService _userService;
  private OrganisationUnit orgUnit;

  private Program program;

  @Override
  protected void initTest() throws IOException {
    userService = _userService;
    setUpMetadata("tracker/simple_metadata.json");
    User userA = userService.getUser("M5zQapPyTZI");
    TrackerImportParams params = TrackerImportParams.builder().userId(userA.getUid()).build();
    assertNoErrors(
        trackerImportService.importTracker(params, fromJson("tracker/event_and_enrollment.json")));
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
    injectSecurityContextUser(userService.getUser("FIgVWzUCkpw"));
    EventSearchParams params = new EventSearchParams();
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
    injectSecurityContextUser(userService.getUser("FIgVWzUCkpw"));
    EventSearchParams params = new EventSearchParams();
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
    injectSecurityContextUser(userService.getUser("FIgVWzUCkpw"));
    EventSearchParams params = new EventSearchParams();
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
    injectSecurityContextUser(userService.getUser("FIgVWzUCkpw"));
    EventSearchParams params = new EventSearchParams();
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
    injectSecurityContextUser(userService.getUser("FIgVWzUCkpw"));
    EventSearchParams params = new EventSearchParams();
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
    injectSecurityContextUser(userService.getUser("FIgVWzUCkpw"));
    EventSearchParams params = new EventSearchParams();
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
    injectSecurityContextUser(userService.getUser("nIidJVYpQQK"));

    EventSearchParams params = new EventSearchParams();
    params.setOrgUnit(get(OrganisationUnit.class, "DiszpKrYNg8"));
    params.setOrgUnitSelectionMode(SELECTED);

    List<Event> events = eventService.getEvents(params).getEvents();

    assertFalse(
        events.isEmpty(),
        "Expected to find events when no program specified, ou mode descendants and org units in search scope");

    assertContainsOnly(
        List.of("ck7DzdxqLqA", "OTmjvJDn0Fu", "kWjSezkXHVp", "H0PbzJY8bJG"),
        events.stream().map(Event::getUid).collect(Collectors.toList()));
  }

  @Test
  void shouldReturnNoEventsWhenProgramOpenOuModeSelectedAndNoProgramEvents() {
    injectSecurityContextUser(userService.getUser("FIgVWzUCkpw"));

    EventSearchParams params = new EventSearchParams();
    params.setProgram(get(Program.class, "shPjYNifvMK"));
    params.setOrgUnit(get(OrganisationUnit.class, orgUnit.getUid()));
    params.setOrgUnitSelectionMode(SELECTED);

    List<Event> events = eventService.getEvents(params).getEvents();

    assertTrue(events.isEmpty(), "Expected to find no events, but found: " + events.size());
  }

  @Test
  void shouldReturnEventsWhenProgramClosedOuModeAccessible() {
    injectSecurityContextUser(userService.getUser("FIgVWzUCkpw"));

    EventSearchParams params = new EventSearchParams();
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
    injectSecurityContextUser(userService.getUser("FIgVWzUCkpw"));

    EventSearchParams params = new EventSearchParams();
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
    injectSecurityContextUser(userService.getUser("FIgVWzUCkpw"));

    EventSearchParams params = new EventSearchParams();
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
  void shouldReturnAllEventsWhenOrgUnitModeAllAndNoOrgUnitProvided() {
    injectSecurityContextUser(userService.getUser("lPaILkLkgOM"));

    EventSearchParams params = new EventSearchParams();
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
            "DiszpKrYNg8",
            "g4w96yEMlzO"),
        events.stream().map(Event::getOrgUnit).collect(Collectors.toSet()));
  }

  @Test
  void shouldIgnoreRequestedOrgUnitAndReturnAllEventsWhenOrgUnitModeAllAndOrgUnitProvided() {
    injectSecurityContextUser(userService.getUser("lPaILkLkgOM"));

    EventSearchParams params = new EventSearchParams();
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
            "DiszpKrYNg8",
            "g4w96yEMlzO"),
        events.stream().map(Event::getOrgUnit).collect(Collectors.toSet()));
  }

  @Test
  void shouldReturnAllEventsWhenOrgUnitModeAllAndNoOrgUnitProvidedAndUserNull() {
    clearSecurityContext();

    EventSearchParams params = new EventSearchParams();
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
            "DiszpKrYNg8",
            "g4w96yEMlzO"),
        events.stream().map(Event::getOrgUnit).collect(Collectors.toSet()));
  }

  private <T extends IdentifiableObject> T get(Class<T> type, String uid) {
    T t = manager.get(type, uid);
    assertNotNull(t, () -> String.format("metadata with uid '%s' should have been created", uid));
    return t;
  }
}
