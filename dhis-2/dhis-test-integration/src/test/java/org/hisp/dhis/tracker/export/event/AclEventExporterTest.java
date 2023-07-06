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
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.CAPTURE;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.CHILDREN;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.DESCENDANTS;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.SELECTED;
import static org.hisp.dhis.tracker.Assertions.assertNoErrors;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.commons.collection.CollectionUtils;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.tracker.TrackerTest;
import org.hisp.dhis.tracker.imports.TrackerImportService;
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

    injectSecurityContext(userService.getUser("M5zQapPyTZI"));
  }

  @BeforeEach
  void setUp() {
    // needed as some tests are run using another user (injectSecurityContext) while most tests
    // expect to be run by admin
    injectAdminUser();
  }

  @Test
  void shouldReturnEventsWhenProgramClosedOuModeDescendantsAndOrgUnitInCaptureScope()
      throws ForbiddenException, BadRequestException {
    injectSecurityContext(userService.getUser("FIgVWzUCkpw"));
    EventOperationParams params =
        EventOperationParams.builder()
            .programUid("pcxIanBWlSY")
            .orgUnitUid(orgUnit.getUid())
            .orgUnitSelectionMode(DESCENDANTS)
            .build();

    List<Event> events = eventService.getEvents(params).getEvents();

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
    injectSecurityContext(userService.getUser("FIgVWzUCkpw"));
    EventOperationParams params =
        EventOperationParams.builder()
            .orgUnitUid(orgUnit.getUid())
            .orgUnitSelectionMode(DESCENDANTS)
            .build();

    List<Event> events = eventService.getEvents(params).getEvents();

    assertFalse(
        events.isEmpty(),
        "Expected to find events when no program specified, ou mode descendants and org units in search scope");
    assertContainsOnly(
        events.stream().map(e -> e.getOrganisationUnit().getUid()).toList(),
        List.of("uoNW0E3xXUy", "h4w96yEMlzO", "tSsGrtfRzjY"));
  }

  @Test
  void shouldReturnEventsWhenProgramClosedOuModeChildrenAndOrgUnitInCaptureScope()
      throws ForbiddenException, BadRequestException {
    injectSecurityContext(userService.getUser("FIgVWzUCkpw"));
    EventOperationParams params =
        EventOperationParams.builder()
            .programUid("pcxIanBWlSY")
            .orgUnitUid(orgUnit.getUid())
            .orgUnitSelectionMode(CHILDREN)
            .build();

    List<Event> events = eventService.getEvents(params).getEvents();

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
    injectSecurityContext(userService.getUser("FIgVWzUCkpw"));
    EventOperationParams params =
        EventOperationParams.builder()
            .orgUnitUid(orgUnit.getUid())
            .orgUnitSelectionMode(CHILDREN)
            .build();

    List<Event> events = eventService.getEvents(params).getEvents();

    assertFalse(
        events.isEmpty(),
        "Expected to find events when no program specified, ou mode children and org units in search scope");
    assertContainsOnly(
        events.stream().map(e -> e.getOrganisationUnit().getUid()).toList(),
        List.of("uoNW0E3xXUy", "h4w96yEMlzO"));
  }

  @Test
  void shouldFailWhenProgramIsOpenAndOrgUnitNotInSearchScope() {
    injectSecurityContext(userService.getUser("FIgVWzUCkpw"));
    EventOperationParams params =
        EventOperationParams.builder()
            .programUid(program.getUid())
            .orgUnitUid("DiszpKrYNg8")
            .orgUnitSelectionMode(DESCENDANTS)
            .build();

    ForbiddenException exception =
        assertThrows(ForbiddenException.class, () -> eventService.getEvents(params));
    assertEquals("User does not have access to orgUnit: DiszpKrYNg8", exception.getMessage());
  }

  @Test
  void shouldFailWhenProgramIsClosedAndOrgUnitNotInCaptureScope() {
    injectSecurityContext(userService.getUser("FIgVWzUCkpw"));
    EventOperationParams params =
        EventOperationParams.builder()
            .programUid("pcxIanBWlSY")
            .orgUnitUid("DiszpKrYNg8")
            .orgUnitSelectionMode(DESCENDANTS)
            .build();

    ForbiddenException exception =
        assertThrows(ForbiddenException.class, () -> eventService.getEvents(params));
    assertEquals("User does not have access to orgUnit: DiszpKrYNg8", exception.getMessage());
  }

  @Test
  void shouldReturnEventsWhenProgramClosedOuModeSelectedAndOrgUnitInCaptureScope()
      throws ForbiddenException, BadRequestException {
    injectSecurityContext(userService.getUser("FIgVWzUCkpw"));
    EventOperationParams params =
        EventOperationParams.builder()
            .programUid("pcxIanBWlSY")
            .orgUnitUid("uoNW0E3xXUy")
            .orgUnitSelectionMode(SELECTED)
            .build();

    List<Event> events = eventService.getEvents(params).getEvents();

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
    injectSecurityContext(userService.getUser("FIgVWzUCkpw"));
    EventOperationParams params =
        EventOperationParams.builder()
            .orgUnitUid(orgUnit.getUid())
            .orgUnitSelectionMode(SELECTED)
            .build();

    List<Event> events = eventService.getEvents(params).getEvents();

    assertFalse(
        events.isEmpty(),
        "Expected to find events when no program specified, ou mode descendants and org units in search scope");

    events.forEach(
        e ->
            assertEquals(
                "h4w96yEMlzO",
                e.getOrganisationUnit().getUid(),
                "Expected to find selected org unit h4w96yEMlzO, but found "
                    + e.getOrganisationUnit().getUid()
                    + " instead"));
  }

  @Test
  void shouldReturnNoEventsWhenProgramOpenOuModeSelectedAndNoProgramEvents()
      throws ForbiddenException, BadRequestException {
    injectSecurityContext(userService.getUser("FIgVWzUCkpw"));
    EventOperationParams params =
        EventOperationParams.builder()
            .programUid("shPjYNifvMK")
            .orgUnitUid(orgUnit.getUid())
            .orgUnitSelectionMode(SELECTED)
            .build();

    List<Event> events = eventService.getEvents(params).getEvents();

    assertTrue(events.isEmpty(), "Expected to find no events, but found: " + events.size());
  }

  @Test
  void shouldReturnEventsWhenProgramClosedOuModeAccessible()
      throws ForbiddenException, BadRequestException {
    injectSecurityContext(userService.getUser("FIgVWzUCkpw"));
    EventOperationParams params =
        EventOperationParams.builder()
            .programUid("pcxIanBWlSY")
            .orgUnitUid("uoNW0E3xXUy")
            .orgUnitSelectionMode(ACCESSIBLE)
            .build();

    List<Event> events = eventService.getEvents(params).getEvents();

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
    injectSecurityContext(userService.getUser("FIgVWzUCkpw"));
    EventOperationParams params =
        EventOperationParams.builder()
            .programUid(program.getUid())
            .orgUnitSelectionMode(ACCESSIBLE)
            .build();

    List<Event> events = eventService.getEvents(params).getEvents();

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
  void shouldReturnEventsWhenProgramOpenOuModeCapture()
      throws ForbiddenException, BadRequestException {
    injectSecurityContext(userService.getUser("FIgVWzUCkpw"));
    EventOperationParams params =
        EventOperationParams.builder()
            .programUid("pcxIanBWlSY")
            .orgUnitUid("uoNW0E3xXUy")
            .orgUnitSelectionMode(CAPTURE)
            .build();

    List<Event> events = eventService.getEvents(params).getEvents();

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
  void shouldReturnSelectedOrgUnitEventsWhenNoOuModeSpecifiedAndUserHasAccessToOrgUnit()
      throws ForbiddenException, BadRequestException {
    injectSecurityContext(userService.getUser("FIgVWzUCkpw"));
    EventOperationParams params =
        EventOperationParams.builder().programUid("pcxIanBWlSY").orgUnitUid("uoNW0E3xXUy").build();

    List<Event> events = eventService.getEvents(params).getEvents();

    assertFalse(
        events.isEmpty(), "Expected to find selected org unit events when ou mode not specified");
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
  void shouldFailWhenNoOuModeSpecifiedAndUserHasNoAccessToOrgUnit() {
    injectSecurityContext(userService.getUser("FIgVWzUCkpw"));
    EventOperationParams params =
        EventOperationParams.builder().programUid("pcxIanBWlSY").orgUnitUid("DiszpKrYNg8").build();

    ForbiddenException exception =
        assertThrows(ForbiddenException.class, () -> eventService.getEvents(params));
    assertEquals("User does not have access to orgUnit: DiszpKrYNg8", exception.getMessage());
  }

  @Test
  void shouldReturnAccessibleOrgUnitEventsWhenNoOrgUnitSpecifiedNorOuModeSpecified()
      throws ForbiddenException, BadRequestException {
    injectSecurityContext(userService.getUser("FIgVWzUCkpw"));
    EventOperationParams params = EventOperationParams.builder().programUid("pcxIanBWlSY").build();

    List<Event> events = eventService.getEvents(params).getEvents();

    assertFalse(
        events.isEmpty(), "Expected to find accessible org unit events when ou mode not specified");
    events.forEach(
        e ->
            assertEquals(
                "uoNW0E3xXUy",
                e.getOrganisationUnit().getUid(),
                "Expected to find accessible org unit uoNW0E3xXUy, but found "
                    + e.getOrganisationUnit().getUid()
                    + " instead"));
  }

  private void assertContainsOnly(List<String> actualOrgUnits, List<String> expectedOrgUnits) {
    List<String> missing = CollectionUtils.difference(expectedOrgUnits, actualOrgUnits);
    List<String> extra = CollectionUtils.difference(actualOrgUnits, expectedOrgUnits);

    assertAll(
        "assertContainsAllOrgUnits found mismatch",
        () ->
            assertTrue(
                missing.isEmpty(),
                () -> String.format("Expected %s to be in %s", missing, actualOrgUnits)),
        () ->
            assertTrue(
                extra.isEmpty(),
                () -> String.format("Expected %s NOT to be in %s", extra, actualOrgUnits)));
  }

  private <T extends IdentifiableObject> T get(Class<T> type, String uid) {
    T t = manager.get(type, uid);
    assertNotNull(t, () -> String.format("metadata with uid '%s' should have been created", uid));
    return t;
  }
}
