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
package org.hisp.dhis.webapi.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;
import java.util.List;
import java.util.Set;
import org.hisp.dhis.common.DeliveryChannel;
import org.hisp.dhis.dataset.notifications.DataSetNotificationService;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.jsontree.JsonList;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.period.MonthlyPeriodType;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.period.PeriodStore;
import org.hisp.dhis.scheduling.JobProgress;
import org.hisp.dhis.test.webapi.PostgresControllerIntegrationTestBase;
import org.hisp.dhis.tracker.program.message.ProgramMessage;
import org.hisp.dhis.tracker.program.message.ProgramMessageService;
import org.hisp.dhis.util.DateUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tests that data set notification templates send their notifications, on data set completion and
 * in the scheduled job, reading the templates back through their mapping. The session is cleared
 * before each send, so the templates and their lazy data sets, delivery channels and recipient user
 * group are loaded from the database rather than taken from the session that imported them.
 */
@Transactional
class DataSetNotificationSendingTest extends PostgresControllerIntegrationTestBase {
  private static final String DATA_SET_A = "DsNotSndDsA";
  private static final String DATA_SET_B = "DsNotSndDsB";
  private static final String ORG_UNIT_A = "DsNotSndOuA";
  private static final String ORG_UNIT_B = "DsNotSndOuB";
  private static final String USER_GROUP = "DsNotSndUgA";

  @Autowired private DataSetNotificationService dataSetNotificationService;

  @Autowired private ProgramMessageService programMessageService;

  @Autowired private PeriodService periodService;

  @Autowired private PeriodStore periodStore;

  @BeforeEach
  void setUp() {
    // the period id cache outlives the rolled back transaction of the previous test
    periodStore.invalidateCache();
    importMetadata(
        """
        {'organisationUnits':[
          {'id':'%1$s','name':'Org unit A','shortName':'OUA','openingDate':'2020-01-01',
           'phoneNumber':'+4790000001','email':'oua@example.org'},
          {'id':'%2$s','name':'Org unit B','shortName':'OUB','openingDate':'2020-01-01',
           'phoneNumber':'+4790000002','email':'oub@example.org'}],
         'dataSets':[
          {'id':'%3$s','name':'Data set A','shortName':'DSA','periodType':'Monthly',
           'organisationUnits':[{'id':'%1$s'},{'id':'%2$s'}]},
          {'id':'%4$s','name':'Data set B','shortName':'DSB','periodType':'Monthly',
           'organisationUnits':[{'id':'%1$s'}]}],
         'userGroups':[{'id':'%5$s','name':'Group A','users':[{'id':'%6$s'}]}]}
        """
            .formatted(ORG_UNIT_A, ORG_UNIT_B, DATA_SET_A, DATA_SET_B, USER_GROUP, ADMIN_USER_UID));
    assertEquals(
        HttpStatus.OK,
        PATCH(
                "/users/" + ADMIN_USER_UID,
                "[{'op':'add','path':'/organisationUnits','value':[{'id':'%s'}]}]"
                    .formatted(ORG_UNIT_A))
            .status());
  }

  @Test
  void shouldSendCompletionMessageToUserGroupOfCompletedDataSetOnly() {
    importMetadata(
        templates(
            completionTemplate("DsNotSndT01", "Completion A", DATA_SET_A, "USER_GROUP", "EMAIL"),
            completionTemplate("DsNotSndT02", "Completion B", DATA_SET_B, "USER_GROUP", "EMAIL"),
            scheduledTemplate(
                "DsNotSndT03", "Scheduled A", "USER_GROUP", "COLLECTIVE_SUMMARY", "EMAIL", -1)));
    startNewRequestSession();

    complete(DATA_SET_A, ORG_UNIT_A, currentPeriod());

    JsonList<JsonObject> conversations = systemConversations();
    assertEquals(1, conversations.size(), conversations::toString);
    JsonObject conversation = conversations.get(0);
    assertEquals("Completed Data set A", conversation.getString("subject").string());
    assertEquals(
        "Data set A completed for Org unit A",
        conversation.getArray("messages").getObject(0).getString("text").string());
  }

  @Test
  void shouldSendCompletionProgramMessageToOrgUnitContactOnAllDeliveryChannels() {
    importMetadata(
        templates(
            completionTemplate(
                "DsNotSndT04",
                "Completion contact",
                DATA_SET_A,
                "ORGANISATION_UNIT_CONTACT",
                "SMS','EMAIL")));
    startNewRequestSession();

    complete(DATA_SET_A, ORG_UNIT_A, currentPeriod());

    List<ProgramMessage> messages = programMessageService.getAllProgramMessages();
    assertEquals(1, messages.size());
    ProgramMessage message = messages.get(0);
    assertEquals("Completed Data set A", message.getSubject());
    assertEquals("Data set A completed for Org unit A", message.getText());
    assertEquals(Set.of(DeliveryChannel.SMS, DeliveryChannel.EMAIL), message.getDeliveryChannels());
    assertEquals(Set.of("+4790000001"), message.getRecipients().getPhoneNumbers());
    assertEquals(Set.of("oua@example.org"), message.getRecipients().getEmailAddresses());
    assertTrue(systemConversations().isEmpty());
  }

  @Test
  void shouldSendScheduledSummaryOfPendingOrgUnitsToUserGroup() {
    Period period = currentPeriod();
    int daysToDueDate = DateUtils.daysBetween(new Date(), period.getEndDate());
    importMetadata(
        templates(
            scheduledTemplate(
                "DsNotSndT05",
                "Summary",
                "USER_GROUP",
                "COLLECTIVE_SUMMARY",
                "EMAIL",
                -daysToDueDate)));
    complete(DATA_SET_A, ORG_UNIT_A, period);
    startNewRequestSession();

    dataSetNotificationService.sendScheduledDataSetNotificationsForDay(
        new Date(), JobProgress.noop());

    JsonList<JsonObject> conversations = systemConversations();
    assertEquals(1, conversations.size(), conversations::toString);
    JsonObject conversation = conversations.get(0);
    assertEquals(
        (daysToDueDate > 0 ? "Pending" : "Overdue") + " DataSet Summary",
        conversation.getString("subject").string());
    String text = conversation.getArray("messages").getObject(0).getString("text").string();
    assertTrue(text.contains("Organisation units : 1"), text);
    assertTrue(text.contains("DataSet : Data set A"), text);
  }

  @Test
  void shouldSendScheduledSmsToContactsOfOrgUnitsThatHaveNotCompleted() {
    Period period = currentPeriod();
    int daysToDueDate = DateUtils.daysBetween(new Date(), period.getEndDate());
    importMetadata(
        templates(
            scheduledTemplate(
                "DsNotSndT06",
                "Reminder",
                "ORGANISATION_UNIT_CONTACT",
                "SINGLE_NOTIFICATION",
                "SMS",
                -daysToDueDate)));
    complete(DATA_SET_A, ORG_UNIT_A, period);
    startNewRequestSession();

    dataSetNotificationService.sendScheduledDataSetNotificationsForDay(
        new Date(), JobProgress.noop());

    List<ProgramMessage> messages = programMessageService.getAllProgramMessages();
    assertEquals(1, messages.size());
    ProgramMessage message = messages.get(0);
    assertEquals("Reminder Data set A", message.getSubject());
    assertEquals(Set.of(DeliveryChannel.SMS), message.getDeliveryChannels());
    assertEquals(Set.of("+4790000001", "+4790000002"), message.getRecipients().getPhoneNumbers());
  }

  private Period currentPeriod() {
    return periodService.reloadPeriod(new MonthlyPeriodType().createPeriod());
  }

  private void complete(String dataSet, String orgUnit, Period period) {
    assertEquals(
        HttpStatus.OK,
        POST(
                "/dataEntry/dataSetCompletion",
                """
                {'dataSet':'%s','period':'%s','orgUnit':'%s','attribute':{},'completed':true}
                """
                    .formatted(dataSet, period.getIsoDate(), orgUnit))
            .status());
  }

  private JsonList<JsonObject> systemConversations() {
    return GET("/messageConversations?fields=subject,messageType,messages[text]&filter=messageType:eq:SYSTEM")
        .content(HttpStatus.OK)
        .getList("messageConversations", JsonObject.class);
  }

  private static String templates(String... templates) {
    return "{'dataSetNotificationTemplates':[" + String.join(",", templates) + "]}";
  }

  private static String completionTemplate(
      String uid, String name, String dataSet, String recipient, String deliveryChannels) {
    return """
        {'id':'%s','name':'%s','dataSetNotificationTrigger':'DATA_SET_COMPLETION',
         'subjectTemplate':'Completed V{data_set_name}',
         'messageTemplate':'V{data_set_name} completed for V{registration_ou}',
         'notificationRecipient':'%s','recipientUserGroup':{'id':'%s'},
         'dataSets':[{'id':'%s'}],'deliveryChannels':['%s']}
        """
        .formatted(uid, name, recipient, USER_GROUP, dataSet, deliveryChannels);
  }

  private static String scheduledTemplate(
      String uid,
      String name,
      String recipient,
      String sendStrategy,
      String deliveryChannels,
      int relativeScheduledDays) {
    return """
        {'id':'%s','name':'%s','dataSetNotificationTrigger':'SCHEDULED_DAYS',
         'relativeScheduledDays':%d,'sendStrategy':'%s',
         'subjectTemplate':'%s V{data_set_name}',
         'messageTemplate':'V{data_set_name} is due',
         'notificationRecipient':'%s','recipientUserGroup':{'id':'%s'},
         'dataSets':[{'id':'%s'}],'deliveryChannels':['%s']}
        """
        .formatted(
            uid,
            name,
            relativeScheduledDays,
            sendStrategy,
            name,
            recipient,
            USER_GROUP,
            DATA_SET_A,
            deliveryChannels);
  }

  private void importMetadata(String metadata) {
    HttpResponse response = POST("/metadata", metadata);
    assertEquals(HttpStatus.OK, response.status(), () -> response.contentUnchecked().toJson());
    assertEquals("OK", response.content().getString("status").string());
  }
}
