/*
 * Copyright (c) 2004-2025, University of Oslo
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
package org.hisp.dhis.program.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.text.ParseException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramInstanceService;
import org.hisp.dhis.programrule.ProgramRule;
import org.hisp.dhis.programrule.ProgramRuleAction;
import org.hisp.dhis.programrule.ProgramRuleActionService;
import org.hisp.dhis.programrule.ProgramRuleActionType;
import org.hisp.dhis.programrule.ProgramRuleService;
import org.hisp.dhis.programrule.engine.ProgramRuleEngineService;
import org.hisp.dhis.rules.models.RuleEffect;
import org.hisp.dhis.scheduling.NoopJobProgress;
import org.hisp.dhis.test.integration.SingleSetupIntegrationTestBase;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserGroup;
import org.hisp.dhis.user.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Zubair Asghar
 */
class ProgramNotificationServiceIntegrationTest extends SingleSetupIntegrationTestBase {
  private static final String PI_UID = "PI_UID";
  private static final int TEST_USER_COUNT = 30;

  @Autowired private IdentifiableObjectManager manager;
  @Autowired private ProgramNotificationService programNotificationService;
  @Autowired private ProgramInstanceService programInstanceService;
  @Autowired private ProgramRuleEngineService programRuleEngineService;
  @Autowired private ProgramRuleService programRuleService;
  @Autowired private ProgramRuleActionService programRuleActionService;
  @Autowired protected UserService _userService;

  private Program program;
  private OrganisationUnit organisationUnit;
  private ProgramRule programRule;
  private ProgramInstance programInstance;
  private TrackedEntityInstance trackedEntityInstance;
  private UserGroup userGroup;

  @Override
  public void setUpTest() throws ParseException {
    userService = _userService;
    organisationUnit = createOrganisationUnit('O');
    manager.save(organisationUnit);
    program = createProgram('P', Set.of(), organisationUnit);
    manager.save(program);
    programRule = createProgramRule('R', program);
    programRule.setCondition("true");
    manager.save(programRule);

    userGroup = createUserGroup('U', new HashSet<>());
    manager.save(userGroup);

    trackedEntityInstance = createTrackedEntityInstance('T', organisationUnit);
    manager.save(trackedEntityInstance);
    programInstance = createProgramInstance(program, trackedEntityInstance, organisationUnit);
    programInstance.setUid(PI_UID);
    manager.save(programInstance);

    createAndAddUsersToUserGroupAndOrgUnit();

    setUpScheduleMessage("V{current_date}", 'A');
    setUpScheduleMessage("V{current_date}", 'B');
    setUpScheduleMessage("V{current_date}", 'C');
    setUpScheduleMessage("'2025-01-01'", 'E');

    ProgramInstance pi = programInstanceService.getProgramInstance(PI_UID);
    List<RuleEffect> ruleEffects =
        programRuleEngineService.evaluateEnrollmentAndRunEffects(pi.getId());
    assertEquals(
        4, ruleEffects.size(), "Expected 3 rule effects for scheduled messages " + ruleEffects);
  }

  @Test
  @Timeout(value = 30, unit = TimeUnit.SECONDS)
  void testScheduledNotificationShouldBeTriggeredBeforeTimeout() {
    /* No assertions are made here because this test is intended to verify that
    scheduled notifications are fetched and processed within the specified time limit
    (performance check only) */
    programNotificationService.sendScheduledNotifications(NoopJobProgress.INSTANCE);
  }

  private void setUpScheduleMessage(String scheduledAt, char ch) {
    ProgramNotificationTemplate pnt = createNotification();
    pnt.setUid("PNT_UID" + ch);
    manager.save(pnt);
    ProgramRuleAction programRuleActionForScheduleMessage =
        createProgramRuleAction(ch, programRule);
    programRuleActionForScheduleMessage.setProgramRuleActionType(
        ProgramRuleActionType.SCHEDULEMESSAGE);
    programRuleActionForScheduleMessage.setTemplateUid(pnt.getUid());
    programRuleActionForScheduleMessage.setContent("STATIC-TEXT-SCHEDULE");
    programRuleActionForScheduleMessage.setData(scheduledAt);
    programRuleActionService.addProgramRuleAction(programRuleActionForScheduleMessage);
    programRule.getProgramRuleActions().add(programRuleActionForScheduleMessage);
    programRuleService.updateProgramRule(programRule);
  }

  private ProgramNotificationTemplate createNotification() {
    ProgramNotificationTemplate pnt = new ProgramNotificationTemplate();
    pnt.setName("Test-PNT-Schedule");
    pnt.setMessageTemplate("message_template");
    pnt.setDeliveryChannels(Set.of());
    pnt.setSubjectTemplate("subject_template");
    pnt.setRecipientUserGroup(userGroup);
    pnt.setNotificationRecipient(ProgramNotificationRecipient.USER_GROUP);
    pnt.setNotificationTrigger(NotificationTrigger.PROGRAM_RULE);
    return pnt;
  }

  private void createAndAddUsersToUserGroupAndOrgUnit() {
    for (int i = 1; i <= TEST_USER_COUNT; i++) {
      User user = createAndAddUser("user" + i, organisationUnit, "ALL");
      userGroup.getMembers().add(user);
      organisationUnit.getUsers().add(user);
    }
  }
}
