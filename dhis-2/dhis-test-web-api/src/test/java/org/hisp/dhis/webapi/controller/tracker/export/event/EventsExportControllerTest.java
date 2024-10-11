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
package org.hisp.dhis.webapi.controller.tracker.export.event;

import static org.hisp.dhis.test.utils.Assertions.assertStartsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.security.acl.AccessStringHelper;
import org.hisp.dhis.test.webapi.H2ControllerIntegrationTestBase;
import org.hisp.dhis.tracker.export.event.EventService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.sharing.UserAccess;
import org.hisp.dhis.webapi.controller.tracker.export.event.EventsExportControllerTest.Config;
import org.hisp.dhis.webapi.utils.ContextUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;

@ContextConfiguration(classes = Config.class)
@Transactional
class EventsExportControllerTest extends H2ControllerIntegrationTestBase {

  static class Config {
    @Bean
    public EventService eventService() {
      EventService eventService = mock(EventService.class);
      // Orderable fields are checked within the controller constructor
      when(eventService.getOrderableFields())
          .thenReturn(new HashSet<>(EventMapper.ORDERABLE_FIELDS.values()));
      return eventService;
    }
  }

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

  @Test
  void getEventsFailsIfGivenAttributeCategoryOptionsAndDeprecatedAttributeCos() {
    assertStartsWith(
        "Only one parameter of 'attributeCos' (deprecated",
        GET("/tracker/events?attributeCategoryOptions=Hq3Kc6HK4OZ&attributeCos=Hq3Kc6HK4OZ")
            .error(HttpStatus.BAD_REQUEST)
            .getMessage());
  }

  @Test
  void getEventsFailsIfGivenAttributeCcAndAttributeCategoryCombo() {
    assertStartsWith(
        "Only one parameter of 'attributeCc' and 'attributeCategoryCombo'",
        GET("/tracker/events?attributeCc=FQnYqKlIHxd&attributeCategoryCombo=YApXsOpwiXk")
            .error(HttpStatus.BAD_REQUEST)
            .getMessage());
  }

  static Stream<Arguments>
      shouldMatchContentTypeAndAttachment_whenEndpointForCompressedEventJsonIsInvoked() {
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
  void shouldMatchContentTypeAndAttachment_whenEndpointForCompressedEventJsonIsInvoked(
      String url, String expectedContentType, String expectedAttachment, String encoding)
      throws ForbiddenException, BadRequestException, NotFoundException {

    when(eventService.getEvents(any())).thenReturn(List.of());
    injectSecurityContextUser(user);

    HttpResponse res = GET(url);
    assertEquals(HttpStatus.OK, res.status());
    assertEquals(expectedContentType, res.header("Content-Type"));
    assertEquals(expectedAttachment, res.header(ContextUtils.HEADER_CONTENT_DISPOSITION));
    assertEquals(encoding, res.header(ContextUtils.HEADER_CONTENT_TRANSFER_ENCODING));
    assertNotNull(res.content(expectedContentType));
  }

  private UserAccess userAccess() {
    UserAccess a = new UserAccess();
    a.setUser(user);
    a.setAccess(AccessStringHelper.FULL);
    return a;
  }
}
