/*
 * Copyright (c) 2004-2025, University of Oslo
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
package org.hisp.dhis.tracker.imports.programrule.executor.event;

/**
 * @author Zubair Asghar
 */
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.tracker.imports.bundle.TrackerBundle;
import org.hisp.dhis.tracker.imports.domain.MetadataIdentifier;
import org.hisp.dhis.tracker.imports.domain.TrackerEvent;
import org.hisp.dhis.tracker.imports.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.imports.programrule.ProgramRuleIssue;
import org.hisp.dhis.tracker.imports.programrule.engine.ValidationEffect;
import org.hisp.dhis.tracker.imports.validation.ValidationCode;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ScheduleEventExecutorTest {
  private static final UID RULE_UID = UID.generate();
  private static final UID STAGE_UID = UID.generate();

  private ProgramStage programStage;
  private TrackerBundle bundle;
  private TrackerEvent inputEvent;
  private User user;
  private UserDetails userDetails;

  @Mock private AclService aclService;
  @Mock private ValidationEffect validationEffect;
  @Mock private TrackerPreheat preheat;

  private ScheduleEventExecutor executor;

  @BeforeEach
  void setUp() {
    user = new User();
    user.setUsername("username1");
    user.setAutoFields();
    userDetails = UserDetails.fromUser(user);
    bundle = TrackerBundle.builder().preheat(preheat).user(userDetails).build();

    programStage = new ProgramStage();
    inputEvent =
        TrackerEvent.builder()
            .event(UID.generate())
            .status(EventStatus.ACTIVE)
            .enrollment(UID.generate())
            .programStage(MetadataIdentifier.ofUid(STAGE_UID.getValue()))
            .orgUnit(MetadataIdentifier.ofUid(CodeGenerator.generateUid()))
            .program(MetadataIdentifier.ofUid(CodeGenerator.generateUid()))
            .attributeOptionCombo(MetadataIdentifier.ofUid(CodeGenerator.generateUid()))
            .build();

    executor = new ScheduleEventExecutor(validationEffect, aclService);

    when(validationEffect.field()).thenReturn(STAGE_UID);
    when(validationEffect.rule()).thenReturn(RULE_UID);
    when(preheat.getProgramStage(STAGE_UID.getValue())).thenReturn(programStage);
    when(aclService.canWrite(userDetails, programStage)).thenReturn(true);
  }

  @Test
  void shouldScheduleEventSuccessfully() {
    when(validationEffect.data()).thenReturn("2025-12-01");

    Optional<ProgramRuleIssue> result = executor.executeRuleAction(bundle, inputEvent);

    assertTrue(result.isPresent());
    verifyIssueCodeAndRule(ValidationCode.E1320, result.get());
  }

  @Test
  void shouldReturnWarningIfEventAlreadyExists() {
    when(validationEffect.data()).thenReturn("2025-12-01");
    when(preheat.hasProgramStageWithTrackerEvents(any(MetadataIdentifier.class), anyString()))
        .thenReturn(true);

    Optional<ProgramRuleIssue> result = executor.executeRuleAction(bundle, inputEvent);

    assertTrue(result.isPresent());
    verifyIssueCodeAndRule(ValidationCode.E1322, result.get());
  }

  @Test
  void shouldReturnWarningIfScheduledAtDateIsInvalid() {
    when(validationEffect.data()).thenReturn("invalid-date");

    Optional<ProgramRuleIssue> result = executor.executeRuleAction(bundle, inputEvent);

    assertTrue(result.isPresent());
    verifyIssueCodeAndRule(ValidationCode.E1319, result.get());
  }

  @Test
  void shouldReturnWarningIfUserCannotWriteToProgramStage() {
    when(validationEffect.data()).thenReturn("2025-12-01");
    when(aclService.canWrite(userDetails, programStage)).thenReturn(false);

    Optional<ProgramRuleIssue> result = executor.executeRuleAction(bundle, inputEvent);

    assertTrue(result.isPresent());
    verifyIssueCodeAndRule(ValidationCode.E1321, result.get());
  }

  private void verifyIssueCodeAndRule(ValidationCode code, ProgramRuleIssue issue) {
    assertEquals(code, issue.getIssueCode());
    assertEquals(RULE_UID, issue.getRuleUid());
  }
}
