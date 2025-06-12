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
package org.hisp.dhis.tracker.imports.validation.validator.event;

import static org.hisp.dhis.test.utils.Assertions.assertIsEmpty;
import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E1118;
import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E1120;
import static org.hisp.dhis.tracker.imports.validation.validator.AssertValidations.assertHasError;
import static org.hisp.dhis.tracker.imports.validation.validator.AssertValidations.assertHasWarning;

import com.google.common.collect.Sets;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.test.TestBase;
import org.hisp.dhis.tracker.TrackerIdSchemeParams;
import org.hisp.dhis.tracker.imports.bundle.TrackerBundle;
import org.hisp.dhis.tracker.imports.domain.Event;
import org.hisp.dhis.tracker.imports.domain.MetadataIdentifier;
import org.hisp.dhis.tracker.imports.domain.TrackerEvent;
import org.hisp.dhis.tracker.imports.domain.User;
import org.hisp.dhis.tracker.imports.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.imports.validation.Reporter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Enrico Colasante
 */
@ExtendWith(MockitoExtension.class)
class AssignedUserValidatorTest extends TestBase {

  private static final String USER_NAME = "Username";

  private static final String NOT_VALID_USERNAME = "not_valid_username";

  private static final String PROGRAM_STAGE = "ProgramStage";

  private AssignedUserValidator validator;

  private TrackerBundle bundle;

  private Reporter reporter;

  private ProgramStage programStage;

  private static final User VALID_USER = User.builder().username(USER_NAME).build();

  private static final User INVALID_USER = User.builder().username(NOT_VALID_USERNAME).build();

  @BeforeEach
  public void setUp() {
    validator = new AssignedUserValidator();

    bundle = TrackerBundle.builder().build();
    TrackerPreheat preheat = new TrackerPreheat();
    org.hisp.dhis.user.User user = makeUser("A");
    user.setUsername(USER_NAME);
    preheat.addUsers(Sets.newHashSet(user));
    bundle.setPreheat(preheat);

    programStage = new ProgramStage();
    programStage.setUid(PROGRAM_STAGE);
    programStage.setEnableUserAssignment(true);
    preheat.put(programStage);

    TrackerIdSchemeParams idSchemes = TrackerIdSchemeParams.builder().build();
    reporter = new Reporter(idSchemes);
  }

  @Test
  void testAssignedUserIsValid() {
    // given
    Event event =
        TrackerEvent.builder()
            .event(UID.generate())
            .assignedUser(VALID_USER)
            .programStage(MetadataIdentifier.ofUid(PROGRAM_STAGE))
            .build();

    // when
    validator.validate(reporter, bundle, event);

    // then
    assertIsEmpty(reporter.getErrors());
  }

  @Test
  void testAssignedUserIsNull() {
    // given
    Event event =
        TrackerEvent.builder()
            .event(UID.generate())
            .assignedUser(null)
            .programStage(MetadataIdentifier.ofUid(PROGRAM_STAGE))
            .build();

    // when
    validator.validate(reporter, bundle, event);

    // then
    assertIsEmpty(reporter.getErrors());
  }

  @Test
  void testAssignedUserIsEmpty() {
    // given
    Event event =
        TrackerEvent.builder()
            .event(UID.generate())
            .assignedUser(User.builder().build())
            .programStage(MetadataIdentifier.ofUid(PROGRAM_STAGE))
            .build();

    // when
    validator.validate(reporter, bundle, event);

    // then
    assertIsEmpty(reporter.getErrors());
  }

  @Test
  void testEventWithNotValidUsername() {
    // given
    Event event =
        TrackerEvent.builder()
            .event(UID.generate())
            .assignedUser(INVALID_USER)
            .programStage(MetadataIdentifier.ofUid(PROGRAM_STAGE))
            .build();

    // when
    validator.validate(reporter, bundle, event);

    // then
    assertHasError(reporter, event, E1118);
  }

  @Test
  void testEventWithUserNotPresentInPreheat() {
    // given
    Event event =
        TrackerEvent.builder()
            .event(UID.generate())
            .assignedUser(VALID_USER)
            .programStage(MetadataIdentifier.ofUid(PROGRAM_STAGE))
            .build();

    // when
    TrackerPreheat preheat = new TrackerPreheat();
    preheat.put(programStage);
    bundle = TrackerBundle.builder().preheat(preheat).build();

    validator.validate(reporter, bundle, event);

    // then
    assertHasError(reporter, event, E1118);
  }

  @Test
  void testEventWithNotEnabledUserAssignment() {
    // given
    Event event =
        TrackerEvent.builder()
            .event(UID.generate())
            .assignedUser(VALID_USER)
            .programStage(MetadataIdentifier.ofUid(PROGRAM_STAGE))
            .build();

    // when
    programStage.setEnableUserAssignment(false);

    validator.validate(reporter, bundle, event);

    // then
    assertIsEmpty(reporter.getErrors());
    assertHasWarning(reporter, event, E1120);
  }

  @Test
  void testEventWithNullEnabledUserAssignment() {
    // given
    Event event =
        TrackerEvent.builder()
            .event(UID.generate())
            .assignedUser(VALID_USER)
            .programStage(MetadataIdentifier.ofUid(PROGRAM_STAGE))
            .build();

    // when
    programStage.setEnableUserAssignment(null);

    validator.validate(reporter, bundle, event);

    // then
    assertIsEmpty(reporter.getErrors());
    assertHasWarning(reporter, event, E1120);
  }
}
