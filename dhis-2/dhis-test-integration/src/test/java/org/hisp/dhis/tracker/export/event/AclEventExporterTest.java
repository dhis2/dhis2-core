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
package org.hisp.dhis.tracker.export.event;

import static org.hisp.dhis.common.OrganisationUnitSelectionMode.ACCESSIBLE;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.ALL;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.CAPTURE;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.CHILDREN;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.DESCENDANTS;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.SELECTED;
import static org.hisp.dhis.test.utils.Assertions.assertContainsOnly;
import static org.hisp.dhis.test.utils.Assertions.assertIsEmpty;
import static org.hisp.dhis.tracker.Assertions.assertNoErrors;
import static org.hisp.dhis.tracker.TrackerTestUtils.uids;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.tracker.TrackerTest;
import org.hisp.dhis.tracker.imports.TrackerImportParams;
import org.hisp.dhis.tracker.imports.TrackerImportService;
import org.hisp.dhis.user.User;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.springframework.beans.factory.annotation.Autowired;

class AclEventExporterTest extends TrackerTest {

  @Autowired private EventService eventService;

  @Autowired private TrackerImportService trackerImportService;

  @Autowired private IdentifiableObjectManager manager;

  private OrganisationUnit orgUnit;

  private Program program;

  private EventOperationParams.EventOperationParamsBuilder operationParamsBuilder;

  @BeforeAll
  void setUp() throws IOException {
    setUpMetadata("tracker/simple_metadata.json");

    User userA = userService.getUser("tTgjgobT1oS");
    injectSecurityContextUser(userA);

    TrackerImportParams params = TrackerImportParams.builder().build();
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
  void setUpUserAndParams() {
    // needed as some tests are run using another user (injectSecurityContext) while most tests
    // expect to be run by admin
    injectAdminIntoSecurityContext();
    operationParamsBuilder = EventOperationParams.builder().eventParams(EventParams.FALSE);
  }

  @Test
  void shouldReturnEventsWhenProgramClosedOuModeDescendantsAndOrgUnitInCaptureScope()
      throws ForbiddenException, BadRequestException {
    injectSecurityContextUser(userService.getUser("FIgVWzUCkpw"));
    EventOperationParams params =
        operationParamsBuilder
            .program(UID.of("pcxIanBWlSY"))
            .orgUnit(orgUnit)
            .orgUnitMode(DESCENDANTS)
            .build();

    List<Event> events = eventService.getEvents(params);

    assertFalse(
        events.isEmpty(),
        "Expected to find events when ou mode descendants and org units in capture scope");
    events.forEach(
        e ->
            assertEquals(
                "uoNW0E3xXUy",
                e.getOrganisationUnit().getUid(),
                "Expected to find descendant org unit uoNW0E3xXUy, but found "
                    + e.getOrganisationUnit().getUid()
                    + " instead"));
  }

  @Test
  void shouldReturnEventsWhenNoProgramSpecifiedOuModeDescendantsAndOrgUnitInSearchScope()
      throws ForbiddenException, BadRequestException {
    injectSecurityContextUser(userService.getUser("FIgVWzUCkpw"));
    EventOperationParams params =
        operationParamsBuilder.orgUnit(orgUnit).orgUnitMode(DESCENDANTS).build();

    List<Event> events = eventService.getEvents(params);

    assertFalse(
        events.isEmpty(),
        "Expected to find events when no program specified, ou mode descendants and org units in search scope");
    assertContainsOnly(
        List.of(
            "YKmfzHdjUDL",
            "jxgFyJEMUPf",
            "D9PbzJY8bJM",
            "pTzf9KYMk72",
            "JaRDIvcEcEx",
            "SbUJzkxKYAG",
            "gvULMgNiAfM"),
        events.stream().map(BaseIdentifiableObject::getUid).collect(Collectors.toSet()));
  }

  @Test
  void shouldReturnEventsWhenProgramClosedOuModeChildrenAndOrgUnitInCaptureScope()
      throws ForbiddenException, BadRequestException {
    injectSecurityContextUser(userService.getUser("FIgVWzUCkpw"));
    EventOperationParams params =
        operationParamsBuilder
            .program(UID.of("pcxIanBWlSY"))
            .orgUnit(orgUnit)
            .orgUnitMode(CHILDREN)
            .build();

    List<Event> events = eventService.getEvents(params);

    assertFalse(
        events.isEmpty(),
        "Expected to find events when ou mode children and org units in capture scope");
    events.forEach(
        e ->
            assertEquals(
                "uoNW0E3xXUy",
                e.getOrganisationUnit().getUid(),
                "Expected to find children org unit uoNW0E3xXUy, but found "
                    + e.getOrganisationUnit().getUid()
                    + " instead"));
  }

  @Test
  void shouldReturnEventsWhenNoProgramSpecifiedOuModeChildrenAndOrgUnitInSearchScope()
      throws ForbiddenException, BadRequestException {
    injectSecurityContextUser(userService.getUser("FIgVWzUCkpw"));
    EventOperationParams params =
        operationParamsBuilder.orgUnit(orgUnit).orgUnitMode(CHILDREN).build();

    List<Event> events = eventService.getEvents(params);

    assertFalse(
        events.isEmpty(),
        "Expected to find events when no program specified, ou mode children and org units in search scope");
    assertContainsOnly(
        List.of("YKmfzHdjUDL", "jxgFyJEMUPf", "JaRDIvcEcEx", "D9PbzJY8bJM", "pTzf9KYMk72"),
        events.stream().map(BaseIdentifiableObject::getUid).collect(Collectors.toSet()));
  }

  @Test
  void shouldFailWhenProgramIsOpenAndOrgUnitNotInSearchScope() {
    injectSecurityContextUser(userService.getUser("FIgVWzUCkpw"));
    EventOperationParams params =
        operationParamsBuilder
            .program(program)
            .orgUnit(UID.of("DiszpKrYNg8"))
            .orgUnitMode(DESCENDANTS)
            .build();

    ForbiddenException exception =
        assertThrows(ForbiddenException.class, () -> eventService.getEvents(params));
    assertEquals(
        "Organisation unit is not part of your search scope: DiszpKrYNg8", exception.getMessage());
  }

  @Test
  void shouldFailWhenProgramIsClosedAndOrgUnitNotInSearchScope() {
    injectSecurityContextUser(userService.getUser("FIgVWzUCkpw"));
    EventOperationParams params =
        operationParamsBuilder
            .program(UID.of("pcxIanBWlSY"))
            .orgUnit(UID.of("DiszpKrYNg8"))
            .orgUnitMode(DESCENDANTS)
            .build();

    ForbiddenException exception =
        assertThrows(ForbiddenException.class, () -> eventService.getEvents(params));
    assertEquals(
        "Organisation unit is not part of your search scope: DiszpKrYNg8", exception.getMessage());
  }

  @Test
  void shouldReturnEventsWhenProgramClosedOuModeSelectedAndOrgUnitInCaptureScope()
      throws ForbiddenException, BadRequestException {
    injectSecurityContextUser(userService.getUser("FIgVWzUCkpw"));
    EventOperationParams params =
        operationParamsBuilder
            .program(UID.of("pcxIanBWlSY"))
            .orgUnit(UID.of("uoNW0E3xXUy"))
            .orgUnitMode(SELECTED)
            .build();

    List<Event> events = eventService.getEvents(params);

    assertFalse(
        events.isEmpty(),
        "Expected to find events when ou mode selected and org units in capture scope");
    events.forEach(
        e ->
            assertEquals(
                "uoNW0E3xXUy",
                e.getOrganisationUnit().getUid(),
                "Expected to find selected org unit uoNW0E3xXUy, but found "
                    + e.getOrganisationUnit().getUid()
                    + " instead"));
  }

  @Test
  void shouldReturnEventsWhenNoProgramSpecifiedOuModeSelectedAndOrgUnitInSearchScope()
      throws ForbiddenException, BadRequestException {
    injectSecurityContextUser(userService.getUser("nIidJVYpQQK"));
    EventOperationParams params =
        operationParamsBuilder.orgUnit(UID.of("DiszpKrYNg8")).orgUnitMode(SELECTED).build();

    List<Event> events = eventService.getEvents(params);

    assertFalse(
        events.isEmpty(),
        "Expected to find events when no program specified, ou mode selected and org units in search scope");

    assertContainsOnly(
        List.of("ck7DzdxqLqA", "OTmjvJDn0Fu", "kWjSezkXHVp", "H0PbzJY8bJG"),
        events.stream().map(BaseIdentifiableObject::getUid).collect(Collectors.toSet()));
  }

  @Test
  void shouldReturnEventsWhenNoProgramSpecifiedOuModeSelectedAndOrgUnitInCaptureScope()
      throws ForbiddenException, BadRequestException {
    injectSecurityContextUser(userService.getUser("FIgVWzUCkpw"));
    EventOperationParams params =
        operationParamsBuilder.orgUnit(UID.of("RojfDTBhoGC")).orgUnitMode(SELECTED).build();

    List<Event> events = eventService.getEvents(params);

    assertFalse(
        events.isEmpty(),
        "Expected to find events when no program specified, ou mode selected and org units in capture scope");

    assertContainsOnly(
        List.of("SbUJzkxKYAG"),
        events.stream().map(BaseIdentifiableObject::getUid).collect(Collectors.toSet()));
  }

  @Test
  void shouldReturnNoEventsWhenProgramOpenOuModeSelectedAndNoProgramEvents()
      throws ForbiddenException, BadRequestException {
    injectSecurityContextUser(userService.getUser("FIgVWzUCkpw"));
    EventOperationParams params =
        operationParamsBuilder
            .program(UID.of("shPjYNifvMK"))
            .orgUnit(orgUnit)
            .orgUnitMode(SELECTED)
            .build();

    List<Event> events = eventService.getEvents(params);

    assertTrue(events.isEmpty(), "Expected to find no events, but found: " + events.size());
  }

  @Test
  void shouldReturnEventsWhenProgramClosedOuModeAccessible()
      throws ForbiddenException, BadRequestException {
    injectSecurityContextUser(userService.getUser("FIgVWzUCkpw"));
    EventOperationParams params =
        operationParamsBuilder.program(UID.of("pcxIanBWlSY")).orgUnitMode(ACCESSIBLE).build();

    List<Event> events = eventService.getEvents(params);

    assertFalse(
        events.isEmpty(), "Expected to find events when ou mode accessible and program closed");
    events.forEach(
        e ->
            assertEquals(
                "uoNW0E3xXUy",
                e.getOrganisationUnit().getUid(),
                "Expected to find accessible org unit uoNW0E3xXUy, but found "
                    + e.getOrganisationUnit().getUid()
                    + " instead"));
  }

  @Test
  void shouldReturnEventsWhenProgramOpenOuModeAccessible()
      throws ForbiddenException, BadRequestException {
    injectSecurityContextUser(userService.getUser("FIgVWzUCkpw"));
    EventOperationParams params =
        operationParamsBuilder.program(program).orgUnitMode(ACCESSIBLE).build();

    List<Event> events = eventService.getEvents(params);

    assertFalse(
        events.isEmpty(), "Expected to find events when ou mode accessible and program open");
    events.forEach(
        e ->
            assertEquals(
                "h4w96yEMlzO",
                e.getOrganisationUnit().getUid(),
                "Expected to find accessible org unit h4w96yEMlzO, but found "
                    + e.getOrganisationUnit().getUid()
                    + " instead"));
  }

  @Test
  void shouldReturnEventsWhenProgramClosedOuModeCapture()
      throws ForbiddenException, BadRequestException {
    injectSecurityContextUser(userService.getUser("FIgVWzUCkpw"));
    EventOperationParams params =
        operationParamsBuilder.program(UID.of("pcxIanBWlSY")).orgUnitMode(CAPTURE).build();

    List<Event> events = eventService.getEvents(params);

    assertFalse(
        events.isEmpty(), "Expected to find events when ou mode capture and program closed");
    events.forEach(
        e ->
            assertEquals(
                "uoNW0E3xXUy",
                e.getOrganisationUnit().getUid(),
                "Expected to find capture org unit uoNW0E3xXUy, but found "
                    + e.getOrganisationUnit().getUid()
                    + " instead"));
  }

  @Test
  void shouldReturnAccessibleOrgUnitEventsWhenNoOrgUnitSpecified()
      throws ForbiddenException, BadRequestException {
    injectSecurityContextUser(userService.getUser("FIgVWzUCkpw"));
    EventOperationParams params =
        operationParamsBuilder.program(UID.of("pcxIanBWlSY")).orgUnitMode(ACCESSIBLE).build();

    List<Event> events = eventService.getEvents(params);

    assertFalse(
        events.isEmpty(),
        "Expected to find accessible org unit events when org unit not specified");
    events.forEach(
        e ->
            assertEquals(
                "uoNW0E3xXUy",
                e.getOrganisationUnit().getUid(),
                "Expected to find accessible org unit uoNW0E3xXUy, but found "
                    + e.getOrganisationUnit().getUid()
                    + " instead"));
  }

  @Test
  void shouldReturnEventsNonSuperUserIsOwnerOrHasUserAccess()
      throws ForbiddenException, BadRequestException {
    // given events have a COC which has a CO which the
    // user owns yMj2MnmNI8L and has user read access to OUUdG3sdOqb
    injectSecurityContextUser(userService.getUser("o1HMTIzBGo7"));

    EventOperationParams params =
        operationParamsBuilder
            .orgUnit(UID.of("DiszpKrYNg8"))
            .orgUnitMode(SELECTED)
            .events(UID.of("lumVtWwwy0O", "cadc5eGj0j7"))
            .build();

    List<Event> events = eventService.getEvents(params);

    assertContainsOnly(List.of("lumVtWwwy0O", "cadc5eGj0j7"), uids(events));
    List<Executable> executables =
        events.stream()
            .map(
                e ->
                    (Executable)
                        () ->
                            assertEquals(
                                2,
                                e.getAttributeOptionCombo().getCategoryOptions().size(),
                                String.format(
                                    "got category options %s",
                                    e.getAttributeOptionCombo().getCategoryOptions())))
            .toList();
    assertAll(
        "all events should have the optionSize set which is the number of COs in the COC",
        executables);
  }

  @Test
  void shouldReturnNoEventsGivenUserHasNoAccess() throws ForbiddenException, BadRequestException {
    // given events have a COC which has a CO (OUUdG3sdOqb/yMj2MnmNI8L) which are not publicly
    // readable, user is not the owner and has no user access
    injectSecurityContextUser(userService.getUser("CYVgFNKCaUS"));

    EventOperationParams params =
        operationParamsBuilder
            .orgUnit(UID.of("DiszpKrYNg8"))
            .orgUnitMode(SELECTED)
            .events(UID.of("lumVtWwwy0O", "cadc5eGj0j7"))
            .build();

    List<String> events = getEvents(params);

    assertIsEmpty(events);
  }

  @Test
  void shouldReturnAllEventsWhenOrgUnitModeAllAndNoOrgUnitProvided()
      throws ForbiddenException, BadRequestException {
    injectSecurityContextUser(userService.getUser("lPaILkLkgOM"));

    EventOperationParams params = operationParamsBuilder.orgUnitMode(ALL).build();

    List<Event> events = eventService.getEvents(params);

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
        events.stream().map(e -> e.getOrganisationUnit().getUid()).collect(Collectors.toSet()));
  }

  @Test
  void shouldIgnoreRequestedOrgUnitAndReturnAllEventsWhenOrgUnitModeAllAndOrgUnitProvided()
      throws ForbiddenException, BadRequestException {
    injectSecurityContextUser(userService.getUser("lPaILkLkgOM"));

    EventOperationParams params =
        operationParamsBuilder.orgUnit(UID.of("uoNW0E3xXUy")).orgUnitMode(ALL).build();

    List<Event> events = eventService.getEvents(params);

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
        events.stream().map(e -> e.getOrganisationUnit().getUid()).collect(Collectors.toSet()));
  }

  @Test
  void
      shouldReturnOnlyVisibleEventsInSearchAndCaptureScopeWhenNoProgramPresentOrgUnitModeAccessible()
          throws ForbiddenException, BadRequestException {
    injectSecurityContextUser(userService.getUser("nIidJVYpQQK"));

    EventOperationParams params = operationParamsBuilder.orgUnitMode(ACCESSIBLE).build();

    List<Event> events = eventService.getEvents(params);

    assertFalse(
        events.isEmpty(), "Expected to find events when ou mode ACCESSIBLE and events visible");
    assertContainsOnly(
        List.of(
            "ck7DzdxqLqA",
            "OTmjvJDn0Fu",
            "kWjSezkXHVp",
            "jxgFyJEMUPf",
            "JaRDIvcEcEx",
            "YKmfzHdjUDL",
            "SbUJzkxKYAG",
            "gvULMgNiAfM",
            "H0PbzJY8bJG"),
        events.stream().map(BaseIdentifiableObject::getUid).collect(Collectors.toSet()));
  }

  private <T extends IdentifiableObject> T get(Class<T> type, String uid) {
    T t = manager.get(type, uid);
    assertNotNull(t, () -> String.format("metadata with uid '%s' should have been created", uid));
    return t;
  }

  private List<String> getEvents(EventOperationParams params)
      throws ForbiddenException, BadRequestException {
    return uids(eventService.getEvents(params));
  }
}
