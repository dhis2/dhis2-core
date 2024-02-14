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
package org.hisp.dhis.tracker.export.changelog;

import static org.hisp.dhis.tracker.Assertions.assertNoErrors;
import static org.hisp.dhis.utils.Assertions.assertNotEmpty;
import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.util.List;
import org.hisp.dhis.changelog.EventDataValueChangeLog;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.tracker.TrackerTest;
import org.hisp.dhis.tracker.imports.TrackerImportParams;
import org.hisp.dhis.tracker.imports.TrackerImportService;
import org.hisp.dhis.tracker.imports.domain.TrackerObjects;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ChangeLogServiceTest extends TrackerTest {

  @Autowired private ChangeLogService changeLogService;

  @Autowired private TrackerImportService trackerImportService;

  @Autowired private IdentifiableObjectManager manager;

  @Autowired protected UserService _userService;

  private User importUser;

  private TrackerImportParams importParams;

  @Override
  protected void initTest() throws IOException {
    userService = _userService;
    setUpMetadata("tracker/simple_metadata.json");
    importUser = userService.getUser("M5zQapPyTZI");
    importParams = TrackerImportParams.builder().userId(importUser.getUid()).build();
    assertNoErrors(
        trackerImportService.importTracker(
            importParams, fromJson("tracker/event_and_enrollment.json")));
  }

  @Test
  void shouldFailWhenEventDoesNotExist() {
    assertThrows(
        NotFoundException.class,
        () -> changeLogService.getEventDataValueChangeLog(UID.of(CodeGenerator.generateUid())));
  }

  @Test
  void shouldFailWhenEventOrgUnitIsNotAccessible() {
    injectSecurityContextUser(manager.get(User.class, "o1HMTIzBGo7"));

    assertThrows(
        NotFoundException.class,
        () -> changeLogService.getEventDataValueChangeLog(UID.of("D9PbzJY8bJM")));
  }

  @Test
  void shouldFailWhenEventProgramIsNotAccessible() {
    injectSecurityContextUser(manager.get(User.class, "o1HMTIzBGo7"));

    assertThrows(
        NotFoundException.class,
        () -> changeLogService.getEventDataValueChangeLog(UID.of("G9PbzJY8bJG")));
  }

  @Test
  void shouldFailWhenEventDataElementIsNotAccessible() {
    /*    injectSecurityContextUser(manager.get(User.class, "FIgVWzUCkpw"));

    assertThrows(
        NotFoundException.class,
        () -> changeLogService.getEventDataValueChangeLog(UID.of("pTzf9KYMk72")));*/
    fail();
  }

  @Test
  void shouldFailWhenProgramTrackedEntityTypeIsNotAccessible() {
    fail();
  }

  @Test
  void shouldReturnChangeLogListWhenDataValueIsCreated() {
    fail();
  }

  @Test
  void shouldReturnChangeLogListWhenDataValueIsDeleted() {
    fail();
  }

  @Test
  void shouldReturnChangeLogListWhenDataValueIsUpdated() throws NotFoundException, IOException {
    injectSecurityContextUser(manager.get(User.class, "M5zQapPyTZI"));

    Event event = manager.get(Event.class, "D9PbzJY8bJM");
    assertNotNull(event);
    String dataElementUid = event.getEventDataValues().iterator().next().getDataElement();
    DataElement dataElement = manager.get(DataElement.class, dataElementUid);
    assertNotNull(dataElement);

    TrackerObjects trackerObjects = fromJson("tracker/event_and_enrollment.json");
    trackerObjects.getEvents().stream()
        .filter(e -> e.getEvent().equalsIgnoreCase(event.getUid()))
        .findFirst()
        .flatMap(
            e ->
                e.getDataValues().stream()
                    .filter(
                        dv -> dv.getDataElement().getIdentifier().equalsIgnoreCase(dataElementUid))
                    .findFirst())
        .ifPresent(dv -> dv.setValue("new value"));
    assertNoErrors(trackerImportService.importTracker(importParams, trackerObjects));

    List<EventDataValueChangeLog> changeLogList =
        changeLogService.getEventDataValueChangeLog(UID.of("D9PbzJY8bJM"));

    assertAll(
        () -> {
          assertNotEmpty(changeLogList);
          assertEquals(importUser.getUsername(), changeLogList.get(0).updatedBy().getUsername());
          assertEquals(importUser.getFirstName(), changeLogList.get(0).updatedBy().getFirstName());
          assertEquals(importUser.getSurname(), changeLogList.get(0).updatedBy().getSurname());
          assertEquals(importUser.getUid(), changeLogList.get(0).updatedBy().getUid());
          assertEquals("new value", changeLogList.get(0).change().dataValue().currentValue());
          assertEquals("value00002", changeLogList.get(0).change().dataValue().previousValue());
          assertEquals(dataElementUid, changeLogList.get(0).change().dataValue().dataElement());
        });
  }

  @Test
  void shouldReturnChangeLogListWhenDataValueIsCreatedUpdatedAndDeleted() {
    fail();
  }
}
