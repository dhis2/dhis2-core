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
package org.hisp.dhis.program.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.security.Authorities;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.tracker.imports.job.NotificationSender;
import org.hisp.dhis.tracker.imports.programrule.engine.Notification;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserGroup;
import org.hisp.dhis.util.DateUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Zubair Asghar
 */
@Transactional
class ProgramNotificationInstanceServiceTest extends PostgresIntegrationTestBase {
  private static final int TEST_USER_COUNT = 60;
  private static final Date DATE = DateUtils.parseDate("2025-01-01");
  private static final int EXPECTED_NOTIFICATIONS = 20;

  @Autowired private ProgramNotificationInstanceService programNotificationInstanceService;
  @Autowired private IdentifiableObjectManager manager;
  @Autowired private NotificationSender notificationSender;

  private OrganisationUnit organisationUnit;
  private UserGroup userGroup;
  private int expectedNotifications;

  @BeforeEach
  void setUp() {
    organisationUnit = createOrganisationUnit('O');
    manager.save(organisationUnit);
    Program program = createProgram('P', Set.of(), organisationUnit);
    manager.save(program);
    userGroup = createUserGroup('U', new HashSet<>());
    manager.save(userGroup);
    createAndAddUsersToUserGroupAndOrgUnit();

    TrackedEntityType trackedEntityType = createTrackedEntityType('P');
    manager.save(trackedEntityType);
    TrackedEntity trackedEntity = createTrackedEntity(organisationUnit, trackedEntityType);
    manager.save(trackedEntity);
    Enrollment enrollment = createEnrollment(program, trackedEntity, organisationUnit);
    manager.save(enrollment);

    List<Notification> notifications = getNotifications();
    expectedNotifications = notifications.size();
    notifications.forEach(n -> notificationSender.send(n, enrollment));
  }

  @Test
  @DisplayName("Should fetch scheduled notifications within timeout")
  void shouldReturnProgramNotificationInstancesForGivenDate() {
    ProgramNotificationInstanceParam param =
        ProgramNotificationInstanceParam.builder().scheduledAt(DATE).build();

    List<ProgramNotificationInstance> instances =
        programNotificationInstanceService.getProgramNotificationInstances(param);

    assertEquals(
        expectedNotifications,
        instances.size(),
        () ->
            String.format(
                "Expected %d notification instances, but got %d. No instances were found for scheduledAt: %s. Instances: %s",
                expectedNotifications, instances.size(), DATE, instances));

    instances.forEach(
        instance ->
            assertEquals(
                DATE,
                instance.getScheduledAt(),
                () ->
                    String.format(
                        "Expected scheduledAt=%s but got %s in instance: %s",
                        DATE, instance.getScheduledAt(), instance)));
  }

  private String createNotification() {
    ProgramNotificationTemplate pnt = new ProgramNotificationTemplate();
    pnt.setName("Test-PNT-Schedule");
    pnt.setMessageTemplate("message_template");
    pnt.setDeliveryChannels(Set.of());
    pnt.setSubjectTemplate("subject_template");
    pnt.setRecipientUserGroup(userGroup);
    pnt.setNotificationRecipient(ProgramNotificationRecipient.USER_GROUP);
    pnt.setNotificationTrigger(NotificationTrigger.PROGRAM_RULE);
    pnt.setAutoFields();
    manager.save(pnt);
    return pnt.getUid();
  }

  private void createAndAddUsersToUserGroupAndOrgUnit() {
    Set<User> users = new HashSet<>();
    for (int i = 1; i <= TEST_USER_COUNT; i++) {
      User user = createAndAddUser("user" + i, organisationUnit, Authorities.ALL.toString());
      user.setPhoneNumber(String.valueOf(i));
      user.setEmail("email" + i + "@email.com");
      users.add(user);
    }
    userGroup.getMembers().addAll(users);
    organisationUnit.getUsers().addAll(users);
    manager.save(userGroup);
    manager.save(organisationUnit);
  }

  List<Notification> getNotifications() {
    return Stream.generate(() -> new Notification(UID.of(createNotification()), DATE))
        .limit(EXPECTED_NOTIFICATIONS)
        .toList();
  }
}
