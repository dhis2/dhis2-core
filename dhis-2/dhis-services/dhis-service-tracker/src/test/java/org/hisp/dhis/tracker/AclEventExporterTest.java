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
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.CAPTURE;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.CHILDREN;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.DESCENDANTS;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.SELECTED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dxf2.events.event.Event;
import org.hisp.dhis.dxf2.events.event.EventSearchParams;
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
    assertNoImportErrors(
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
    // injectSecurityContext(createAdminUser("ALL"));
  }

  @Test
  void shouldReturnEventsWhenProgramClosedOuModeDescendantsAndOrgUnitInCaptureScope() {
    injectSecurityContext(userService.getUser("FIgVWzUCkpw"));
    EventSearchParams params = new EventSearchParams();
    params.setProgram(get(Program.class, "pcxIanBWlSY"));
    params.setAccessibleOrgUnits(List.of(get(OrganisationUnit.class, "uoNW0E3xXUy")));
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
  void shouldReturnEventsWhenNoProgramSpecifiedOuModeDescendantsAndOrgUnitInSearchScope() {
    injectSecurityContext(userService.getUser("FIgVWzUCkpw"));
    EventSearchParams params = new EventSearchParams();
    params.setAccessibleOrgUnits(List.of(get(OrganisationUnit.class, orgUnit.getUid())));
    params.setOrgUnitSelectionMode(DESCENDANTS);

    List<Event> events = eventService.getEvents(params).getEvents();

    assertFalse(
        events.isEmpty(),
        "Expected to find events when no program specified, ou mode descendants and org units in search scope");
    assertCollectionsAreSame(
        events.stream().map(Event::getOrgUnit).collect(Collectors.toSet()),
        List.of("uoNW0E3xXUy", "h4w96yEMlzO", "tSsGrtfRzjY"));
  }

  @Test
  void shouldReturnEventsWhenNoProgramSpecifiedOuModeDescenadantsAndOrgUnitInSearchScope() {
    injectSecurityContext(userService.getUser("FIgVWzUCkpw"));
    EventSearchParams params = new EventSearchParams();
    params.setAccessibleOrgUnits(List.of(get(OrganisationUnit.class, "uoNW0E3xXUy")));
    params.setOrgUnitSelectionMode(DESCENDANTS);

    List<Event> events = eventService.getEvents(params).getEvents();

    assertFalse(
        events.isEmpty(),
        "Expected to find events when no program specified, ou mode descendants and org units in search scope");
    assertCollectionsAreSame(
        events.stream().map(Event::getOrgUnit).collect(Collectors.toSet()),
        List.of("uoNW0E3xXUy", "tSsGrtfRzjY"));
  }

  @Test
  void shouldReturnEventsWhenProgramClosedOuModeChildrenAndOrgUnitInCaptureScope() {
    injectSecurityContext(userService.getUser("FIgVWzUCkpw"));
    EventSearchParams params = new EventSearchParams();
    params.setProgram(get(Program.class, "pcxIanBWlSY"));
    params.setAccessibleOrgUnits(List.of(get(OrganisationUnit.class, "uoNW0E3xXUy")));
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
    EventSearchParams params = new EventSearchParams();
    params.setAccessibleOrgUnits(
        List.of(
            get(OrganisationUnit.class, orgUnit.getUid()),
            get(OrganisationUnit.class, "uoNW0E3xXUy")));
    params.setOrgUnitSelectionMode(CHILDREN);

    List<Event> events = eventService.getEvents(params).getEvents();

    assertFalse(
        events.isEmpty(),
        "Expected to find events when no program specified, ou mode children and org units in search scope");
    assertCollectionsAreSame(
        events.stream().map(Event::getOrgUnit).collect(Collectors.toSet()),
        List.of("uoNW0E3xXUy", "h4w96yEMlzO"));
  }

  @Test
  void shouldReturnEventsWhenProgramClosedOuModeSelectedAndOrgUnitInCaptureScope() {
    injectSecurityContext(userService.getUser("FIgVWzUCkpw"));
    EventSearchParams params = new EventSearchParams();
    params.setProgram(get(Program.class, "pcxIanBWlSY"));
    params.setAccessibleOrgUnits(List.of(get(OrganisationUnit.class, "uoNW0E3xXUy")));
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
    injectSecurityContext(userService.getUser("FIgVWzUCkpw"));

    EventSearchParams params = new EventSearchParams();
    params.setAccessibleOrgUnits(List.of(get(OrganisationUnit.class, orgUnit.getUid())));
    params.setOrgUnitSelectionMode(SELECTED);

    List<Event> events = eventService.getEvents(params).getEvents();

    assertFalse(
        events.isEmpty(),
        "Expected to find events when no program specified, ou mode descendants and org units in search scope");

    events.forEach(
        e ->
            assertEquals(
                "h4w96yEMlzO",
                e.getOrgUnit(),
                "Expected to find selected org unit h4w96yEMlzO, but found "
                    + e.getOrgUnit()
                    + " instead"));
  }

  @Test
  void shouldReturnNoEventsWhenProgramOpenOuModeSelectedAndNoProgramEvents() {
    injectSecurityContext(userService.getUser("FIgVWzUCkpw"));

    EventSearchParams params = new EventSearchParams();
    params.setProgram(get(Program.class, "shPjYNifvMK"));
    params.setAccessibleOrgUnits(List.of(get(OrganisationUnit.class, orgUnit.getUid())));
    params.setOrgUnitSelectionMode(SELECTED);

    List<Event> events = eventService.getEvents(params).getEvents();

    assertTrue(events.isEmpty(), "Expected to find no events, but found: " + events.size());
  }

  @Test
  void shouldReturnEventsWhenProgramClosedOuModeAccessible() {
    injectSecurityContext(userService.getUser("FIgVWzUCkpw"));

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
    injectSecurityContext(userService.getUser("FIgVWzUCkpw"));

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
    injectSecurityContext(userService.getUser("FIgVWzUCkpw"));

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

  private <T extends IdentifiableObject> T get(Class<T> type, String uid) {
    T t = manager.get(type, uid);
    assertNotNull(t, () -> String.format("metadata with uid '%s' should have been created", uid));
    return t;
  }

  private void assertCollectionsAreSame(Collection<String> expected, Collection<String> actual) {
    assertTrue(
        expected.containsAll(actual),
        "Expected list: " + expected + " does not contain values: " + actual);
    assertTrue(
        actual.containsAll(expected),
        "Actual list: " + actual + " does not contain values: " + expected);

    assertEquals(
        expected.size(),
        actual.size(),
        "List size don't match, expected " + expected.size() + " but found " + actual.size());
  }
}
