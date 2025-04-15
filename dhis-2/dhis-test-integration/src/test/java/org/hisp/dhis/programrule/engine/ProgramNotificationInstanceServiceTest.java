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
package org.hisp.dhis.programrule.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramInstanceService;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.notification.NotificationTrigger;
import org.hisp.dhis.program.notification.ProgramNotificationInstance;
import org.hisp.dhis.program.notification.ProgramNotificationInstanceParam;
import org.hisp.dhis.program.notification.ProgramNotificationInstanceService;
import org.hisp.dhis.program.notification.ProgramNotificationRecipient;
import org.hisp.dhis.program.notification.ProgramNotificationService;
import org.hisp.dhis.program.notification.ProgramNotificationTemplate;
import org.hisp.dhis.program.notification.ProgramNotificationTemplateService;
import org.hisp.dhis.programrule.ProgramRule;
import org.hisp.dhis.programrule.ProgramRuleAction;
import org.hisp.dhis.programrule.ProgramRuleActionType;
import org.hisp.dhis.scheduling.NoopJobProgress;
import org.hisp.dhis.test.integration.IntegrationTestBase;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserGroup;
import org.hisp.dhis.user.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Zubair Asghar
 */
class ProgramNotificationInstanceServiceTest extends IntegrationTestBase {
  private static final int TEST_USER_COUNT = 30;

  @Autowired private ProgramInstanceService programInstanceService;
  @Autowired private ProgramNotificationService programNotificationService;
  @Autowired private ProgramService programService;
  @Autowired private TrackedEntityInstanceService trackedEntityInstanceService;
  @Autowired private ProgramNotificationTemplateService programNotificationTemplateService;
  @Autowired private ProgramNotificationInstanceService programNotificationInstanceService;
  @Autowired private OrganisationUnitService organisationUnitService;
  @Autowired private IdentifiableObjectManager manager;
  @Autowired private ProgramRuleEngineService programRuleEngineService;
  @Autowired private UserService _userService;

  private Program program;
  private ProgramRule programRule;
  private ProgramRuleAction programRuleAction1;
  private ProgramRuleAction programRuleAction2;
  private ProgramNotificationTemplate programNotificationTemplateScheduledForToday;
  private ProgramNotificationTemplate programNotificationTemplateScheduledForTomorrow;
  private OrganisationUnit organisationUnit;
  private TrackedEntityInstance trackedEntityInstance;
  private ProgramInstance programInstance;
  private ProgramInstance programInstanceB;
  private UserGroup userGroup;

  private String today = "'" + LocalDate.now() + "'";
  private String tomorrow = "'" + LocalDate.now().plusDays(1) + "'";

  @Override
  protected void setUpTest() throws Exception {
    userService = _userService;
    organisationUnit = createOrganisationUnit('O');
    organisationUnitService.addOrganisationUnit(organisationUnit);
    program = createProgram('P');
    programService.addProgram(program);
    trackedEntityInstance = createTrackedEntityInstance('T', organisationUnit);
    trackedEntityInstanceService.addTrackedEntityInstance(trackedEntityInstance);
    programRule = createProgramRule('R', program);
    programRule.setCondition("true");
    manager.save(programRule);

    userGroup = createUserGroup('U', new HashSet<>());
    manager.save(userGroup);

    createAndAddUsersToUserGroupAndOrgUnit();

    programNotificationTemplateScheduledForToday =
        createProgramNotificationTemplate(
            "test", 1, NotificationTrigger.PROGRAM_RULE, ProgramNotificationRecipient.USER_GROUP);
    programNotificationTemplateScheduledForToday.setRecipientUserGroup(userGroup);
    programNotificationTemplateScheduledForToday.setAutoFields();
    programNotificationTemplateService.save(programNotificationTemplateScheduledForToday);

    programNotificationTemplateScheduledForTomorrow =
        createProgramNotificationTemplate(
            "test", 1, NotificationTrigger.PROGRAM_RULE, ProgramNotificationRecipient.USER_GROUP);
    programNotificationTemplateScheduledForTomorrow.setRecipientUserGroup(userGroup);
    programNotificationTemplateScheduledForTomorrow.setAutoFields();
    programNotificationTemplateService.save(programNotificationTemplateScheduledForTomorrow);

    programRuleAction1 = createProgramRuleAction('A');
    programRuleAction1.setProgramRuleActionType(ProgramRuleActionType.SCHEDULEMESSAGE);
    programRuleAction1.setTemplateUid(programNotificationTemplateScheduledForToday.getUid());
    programRuleAction1.setData(today);
    manager.save(programRuleAction1);
    programRule.getProgramRuleActions().add(programRuleAction1);

    programRuleAction2 = createProgramRuleAction('B');
    programRuleAction2.setProgramRuleActionType(ProgramRuleActionType.SCHEDULEMESSAGE);
    programRuleAction2.setTemplateUid(programNotificationTemplateScheduledForTomorrow.getUid());
    programRuleAction2.setData(tomorrow);
    manager.save(programRuleAction2);
    programRule.getProgramRuleActions().add(programRuleAction2);

    manager.update(programRule);
    programInstance = createProgramInstance(program, trackedEntityInstance, organisationUnit);
    programInstanceService.addProgramInstance(programInstance);
    programInstanceB = createProgramInstance(program, trackedEntityInstance, organisationUnit);
    programInstanceService.addProgramInstance(programInstanceB);

    programRuleEngineService.evaluateEnrollmentAndRunEffects(programInstance.getId());
  }

  @Test
  void testGetProgramNotificationInstance() {
    List<ProgramNotificationInstance> programNotificationInstances =
        programNotificationInstanceService.getProgramNotificationInstances(
            ProgramNotificationInstanceParam.builder().programInstance(programInstance).build());
    assertFalse(programNotificationInstances.isEmpty());
    assertSame(programInstance, programNotificationInstances.get(0).getProgramInstance());
    ProgramNotificationInstanceParam param =
        ProgramNotificationInstanceParam.builder().programInstance(programInstance).build();
    List<ProgramNotificationInstance> instances =
        programNotificationInstanceService.getProgramNotificationInstances(param);
    assertFalse(instances.isEmpty());
  }

  @Test
  void testGetProgramNotificationInstanceScheduledForToday() throws ParseException {
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
    String formattedDate = sdf.format(new Date());

    Date todayDate = sdf.parse(formattedDate);
    ProgramNotificationInstanceParam param =
        ProgramNotificationInstanceParam.builder().scheduledAt(todayDate).build();

    List<ProgramNotificationInstance> instancesForToday =
        programNotificationInstanceService.getProgramNotificationInstances(param);

    assertFalse(instancesForToday.isEmpty());

    // Only one instance is scheduled for today's date
    assertEquals(1, instancesForToday.size());
    ProgramNotificationInstance instanceForToday = instancesForToday.get(0);
    assertEquals(instanceForToday.getScheduledAt(), todayDate);
  }

  @Test
  @Timeout(value = 20, unit = TimeUnit.SECONDS)
  void testShouldGetAndSendScheduledNotificationInstanceWithoutTimeout() {
    List<ProgramNotificationInstance> instances = programNotificationInstanceService.getAll();
    assertEquals(
        2, instances.size(), "Expected 2 notifications for scheduled messages " + instances);

    programNotificationService.sendScheduledNotifications(NoopJobProgress.INSTANCE);
  }

  @Test
  void testDeleteProgramNotificationInstance() {
    programRuleEngineService.evaluateEnrollmentAndRunEffects(programInstanceB.getId());
    List<ProgramNotificationInstance> programNotificationInstances =
        programNotificationInstanceService.getProgramNotificationInstances(
            ProgramNotificationInstanceParam.builder().programInstance(programInstanceB).build());
    assertFalse(programNotificationInstances.isEmpty());
    assertSame(programInstanceB, programNotificationInstances.get(0).getProgramInstance());
    programNotificationInstanceService.delete(programNotificationInstances.get(0));
    programNotificationInstanceService.delete(programNotificationInstances.get(1));
    List<ProgramNotificationInstance> instances =
        programNotificationInstanceService.getProgramNotificationInstances(
            ProgramNotificationInstanceParam.builder().programInstance(programInstanceB).build());
    assertTrue(instances.isEmpty());
  }

  private void createAndAddUsersToUserGroupAndOrgUnit() {
    for (int i = 1; i <= TEST_USER_COUNT; i++) {
      User user = createAndAddUser("user" + i, organisationUnit, "ALL");
      userGroup.getMembers().add(user);
      organisationUnit.getUsers().add(user);
    }
  }
}
