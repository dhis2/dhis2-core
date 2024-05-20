/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.webapi.controller.tracker.export;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Set;
import java.util.stream.Stream;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dxf2.events.event.EventService;
import org.hisp.dhis.dxf2.events.event.Events;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.security.acl.AccessStringHelper;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.sharing.UserAccess;
import org.hisp.dhis.web.HttpStatus;
import org.hisp.dhis.webapi.DhisControllerConvenienceTest;
import org.hisp.dhis.webapi.utils.ContextUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(classes = TrackerEventExportTestConfiguration.class)
@ActiveProfiles("TrackerEventExport")
class TrackerEventsExportControllerTest extends DhisControllerConvenienceTest {
  @Autowired private IdentifiableObjectManager manager;

  @Autowired private EventService eventService;

  private User user;

  @BeforeEach
  void setUp() {
    User owner = makeUser("owner");

    OrganisationUnit orgUnit = createOrganisationUnit('A');
    orgUnit.getSharing().setOwner(owner);
    manager.save(orgUnit, false);

    OrganisationUnit anotherOrgUnit = createOrganisationUnit('B');
    anotherOrgUnit.getSharing().setOwner(owner);
    manager.save(anotherOrgUnit, false);

    user = createUserWithId("tester", CodeGenerator.generateUid());
    user.addOrganisationUnit(orgUnit);
    user.setTeiSearchOrganisationUnits(Set.of(orgUnit));
    this.userService.updateUser(user);

    Program program = createProgram('A');
    program.addOrganisationUnit(orgUnit);
    program.getSharing().setOwner(owner);
    program.getSharing().addUserAccess(userAccess());
    manager.save(program, false);

    ProgramStage programStage = createProgramStage('A', program);
    programStage.getSharing().setOwner(owner);
    programStage.getSharing().addUserAccess(userAccess());
    manager.save(programStage, false);

    DataElement de = createDataElement('A');
    de.getSharing().setOwner(owner);
    manager.save(de, false);
  }

  static Stream<Arguments>
      shouldMatchContentTypeAndAttachmentWhenEndpointForCompressedEventJsonIsInvoked() {
    return Stream.of(
        arguments(
            "/tracker/events.json.zip",
            "application/json+zip",
            "attachment; filename=events.json.zip",
            "binary"),
        arguments(
            "/tracker/events.json.gz",
            "application/json+gzip",
            "attachment; filename=events.json.gz",
            "binary"),
        arguments(
            "/tracker/events.csv",
            "application/csv; charset=UTF-8",
            "attachment; filename=events.csv",
            null),
        arguments(
            "/tracker/events.csv.gz",
            "application/csv+gzip",
            "attachment; filename=events.csv.gz",
            "binary"),
        arguments(
            "/tracker/events.csv.zip",
            "application/csv+zip",
            "attachment; filename=events.csv.zip",
            "binary"));
  }

  @ParameterizedTest
  @MethodSource
  void shouldMatchContentTypeAndAttachmentWhenEndpointForCompressedEventJsonIsInvoked(
      String url, String expectedContentType, String expectedAttachment, String encoding) {

    when(eventService.getEvents(any())).thenReturn(new Events());
    injectSecurityContext(user);

    HttpResponse res = GET(url);
    assertEquals(HttpStatus.OK, res.status());
    assertEquals(expectedContentType, res.header("Content-Type"));
    assertEquals(expectedAttachment, res.header(ContextUtils.HEADER_CONTENT_DISPOSITION));
    assertEquals(encoding, res.header(ContextUtils.HEADER_CONTENT_TRANSFER_ENCODING));
  }

  private UserAccess userAccess() {
    UserAccess a = new UserAccess();
    a.setUser(user);
    a.setAccess(AccessStringHelper.FULL);
    return a;
  }
}
