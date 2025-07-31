/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.message;

import static org.hisp.dhis.test.TestBase.injectSecurityContextNoSettings;
import static org.hisp.dhis.test.utils.Assertions.assertStartsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.List;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.TrackerEvent;
import org.hisp.dhis.program.message.ProgramMessageOperationParamMapper;
import org.hisp.dhis.program.message.ProgramMessageOperationParams;
import org.hisp.dhis.program.message.ProgramMessageQueryParams;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * @author Zubair Asghar
 */
@MockitoSettings(strictness = Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
class ProgramMessageOperationParamsMapperTest {
  private static final UID ENROLLMENT = UID.generate();
  private static final UID EVENT = UID.generate();

  @Mock private IdentifiableObjectManager manager;
  @Mock private ProgramService programService;
  @InjectMocks private ProgramMessageOperationParamMapper subject;

  private User user;
  private Enrollment enrollment;
  private TrackerEvent event;
  private Program program;

  @BeforeEach
  void setUp() {
    user = new User();
    user.setUsername("admin");

    injectSecurityContextNoSettings(UserDetails.fromUser(user));

    program = new Program();
    program.setName("TB-Program");
    program.setAutoFields();

    enrollment = new Enrollment();
    enrollment.setUid(ENROLLMENT.getValue());
    enrollment.setProgram(program);

    event = new TrackerEvent();
    event.setUid(EVENT.getValue());
    event.setEnrollment(enrollment);

    when(manager.get(eq(Enrollment.class), anyString())).thenReturn(enrollment);
    when(manager.get(eq(TrackerEvent.class), anyString())).thenReturn(event);
    when(programService.getCurrentUserPrograms()).thenReturn(List.of(program));
  }

  @Test
  void shouldMapEnrollmentUIDToEnrollmentObject() throws NotFoundException {
    ProgramMessageQueryParams queryParams =
        subject.map(ProgramMessageOperationParams.builder().enrollment(ENROLLMENT).build());

    assertEquals(enrollment, queryParams.getEnrollment());
  }

  @Test
  void shouldMapEventUIDToEnrollmentObject() throws NotFoundException {
    ProgramMessageQueryParams queryParams =
        subject.map(ProgramMessageOperationParams.builder().event(EVENT).build());

    assertEquals(event, queryParams.getEvent());
  }

  @Test
  void shouldFailWhenEnrollmentNotFound() {
    UID invalidEnrollment = UID.generate();
    when(manager.get(eq(Enrollment.class), anyString())).thenReturn(null);

    NotFoundException exception =
        assertThrows(
            NotFoundException.class,
            () ->
                subject.map(
                    ProgramMessageOperationParams.builder().enrollment(invalidEnrollment).build()));

    assertStartsWith(
        String.format(
            "%s: %s does not exist.", Enrollment.class.getSimpleName(), invalidEnrollment),
        exception.getMessage());
  }

  @Test
  void shouldFailWhenEventNotFound() {
    UID invalidEvent = UID.generate();
    when(manager.get(eq(TrackerEvent.class), anyString())).thenReturn(null);

    NotFoundException exception =
        assertThrows(
            NotFoundException.class,
            () -> subject.map(ProgramMessageOperationParams.builder().event(invalidEvent).build()));

    assertStartsWith(
        String.format("%s: %s does not exist.", TrackerEvent.class.getSimpleName(), invalidEvent),
        exception.getMessage());
  }

  @Test
  void shouldFailWhenUserHasNoAccessToProgram() {
    when(programService.getCurrentUserPrograms()).thenReturn(List.of());

    IllegalQueryException exception =
        assertThrows(
            IllegalQueryException.class,
            () ->
                subject.map(
                    ProgramMessageOperationParams.builder().enrollment(ENROLLMENT).build()));

    assertStartsWith(
        String.format(
            "User:%s does not have access to the required program:%s",
            user.getUsername(), program.getName()),
        exception.getMessage());
  }
}
