/*
 * Copyright (c) 2004-2026, University of Oslo
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
package org.hisp.dhis.notification;

import static org.hisp.dhis.program.notification.NotificationTrigger.ENROLLMENT;
import static org.hisp.dhis.program.notification.NotificationTrigger.SCHEDULED_DAYS_ENROLLMENT_DATE;
import static org.hisp.dhis.tracker.test.TrackerTestBase.createTrackedEntity;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vdurmont.semver4j.Semver;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.configuration.Configuration;
import org.hisp.dhis.configuration.ConfigurationService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.notification.NotificationTrigger;
import org.hisp.dhis.program.notification.ProgramNotificationRecipient;
import org.hisp.dhis.program.notification.ProgramNotificationTemplate;
import org.hisp.dhis.scheduling.JobProgress;
import org.hisp.dhis.system.SystemUpdateNotificationService;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.tracker.model.Enrollment;
import org.hisp.dhis.tracker.model.TrackedEntity;
import org.hisp.dhis.tracker.program.notification.ProgramNotificationService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserGroup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Sends notifications to a recipient user group with a disabled member. Each step runs in its own
 * committed transaction, so the test reads the real {@code usergroupmembers} rows afterwards.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
class NotificationRecipientUserGroupTest extends PostgresIntegrationTestBase {

  @Autowired private IdentifiableObjectManager manager;

  @Autowired private ProgramService programService;

  @Autowired private CategoryService categoryService;

  @Autowired private ConfigurationService configurationService;

  @Autowired private ProgramNotificationService programNotificationService;

  @Autowired private SystemUpdateNotificationService systemUpdateNotificationService;

  @Autowired private TransactionTemplate transactionTemplate;

  @Autowired private JdbcTemplate jdbcTemplate;

  private OrganisationUnit orgUnit;

  private User enabledUser;

  private User disabledUser;

  private UserGroup userGroup;

  @BeforeEach
  void setUp() {
    transactionTemplate.executeWithoutResult(
        status -> {
          orgUnit = createOrganisationUnit('A');
          manager.save(orgUnit);
          enabledUser = createAndAddUser("enabledrecipient", orgUnit);
          disabledUser = createAndAddUser("disabledrecipient", orgUnit);
          disabledUser.setDisabled(true);
          userGroup = createUserGroup('A', new HashSet<>(Set.of(enabledUser, disabledUser)));
          manager.save(userGroup);
        });
  }

  @Test
  void shouldKeepDisabledUserInUserGroupWhenSendingScheduledProgramNotification() {
    Date today = today();
    transactionTemplate.executeWithoutResult(
        status -> createEnrollmentWithUserGroupTemplate(SCHEDULED_DAYS_ENROLLMENT_DATE, today));
    entityManager.clear();

    programNotificationService.sendScheduledNotificationsForDay(today, JobProgress.noop());

    assertOnlyEnabledUserNotifiedAndMembershipKept();
  }

  @Test
  void shouldKeepDisabledUserInUserGroupWhenSendingEnrollmentNotification() {
    Enrollment enrollment =
        transactionTemplate.execute(
            status -> createEnrollmentWithUserGroupTemplate(ENROLLMENT, today()));
    entityManager.clear();

    programNotificationService.sendEnrollmentNotifications(enrollment.getId());

    assertOnlyEnabledUserNotifiedAndMembershipKept();
  }

  @Test
  void shouldKeepDisabledUserInUserGroupWhenSendingSystemUpdateNotification() {
    transactionTemplate.executeWithoutResult(
        status -> {
          Configuration configuration = configurationService.getConfiguration();
          configuration.setSystemUpdateNotificationRecipients(userGroup);
          configurationService.setConfiguration(configuration);
        });
    entityManager.clear();

    systemUpdateNotificationService.sendMessageForEachVersion(
        Map.of(
            new Semver("2.99.1"),
            Map.of(
                "version", "2.99.1",
                "releaseDate", "2026-01-01",
                "downloadUrl", "https://releases.dhis2.org/2.99/dhis2-stable-2.99.1.war")),
        JobProgress.noop());

    assertOnlyEnabledUserNotifiedAndMembershipKept();
  }

  private Enrollment createEnrollmentWithUserGroupTemplate(
      NotificationTrigger trigger, Date enrollmentDate) {
    TrackedEntityType trackedEntityType = createTrackedEntityType('A');
    manager.save(trackedEntityType);
    TrackedEntity trackedEntity = createTrackedEntity(orgUnit, trackedEntityType);
    manager.save(trackedEntity);

    ProgramNotificationTemplate template =
        createProgramNotificationTemplate(
            "user group template", 0, trigger, ProgramNotificationRecipient.USER_GROUP);
    template.setRecipientUserGroup(userGroup);
    manager.save(template);

    Program program = createProgram('A', new HashSet<>(), orgUnit);
    program.setNotificationTemplates(new HashSet<>(Set.of(template)));
    programService.addProgram(program);

    Enrollment enrollment = new Enrollment(enrollmentDate, enrollmentDate, trackedEntity, program);
    enrollment.setOrganisationUnit(orgUnit);
    enrollment.setAttributeOptionCombo(categoryService.getDefaultCategoryOptionCombo());
    manager.save(enrollment);
    return enrollment;
  }

  private void assertOnlyEnabledUserNotifiedAndMembershipKept() {
    assertEquals(1, countUserMessages(enabledUser));
    assertEquals(0, countUserMessages(disabledUser));
    assertTrue(isGroupMember(disabledUser), "disabled user lost its user group membership");
    assertTrue(isGroupMember(enabledUser));
  }

  private static Date today() {
    Calendar calendar = Calendar.getInstance();
    PeriodType.clearTimeOfDay(calendar);
    return calendar.getTime();
  }

  private int countUserMessages(User user) {
    return jdbcTemplate.queryForObject(
        "select count(*) from usermessage where userid = ?", Integer.class, user.getId());
  }

  private boolean isGroupMember(User user) {
    Integer rows =
        jdbcTemplate.queryForObject(
            "select count(*) from usergroupmembers where usergroupid = ? and userid = ?",
            Integer.class,
            userGroup.getId(),
            user.getId());
    return rows == 1;
  }
}
