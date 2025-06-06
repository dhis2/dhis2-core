/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.dataset.notifications;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyList;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.DeliveryChannel;
import org.hisp.dhis.configuration.ConfigurationService;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataset.CompleteDataSetRegistration;
import org.hisp.dhis.dataset.CompleteDataSetRegistrationService;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.i18n.I18nFormat;
import org.hisp.dhis.i18n.I18nManager;
import org.hisp.dhis.message.DefaultMessageService;
import org.hisp.dhis.message.MessageConversationStore;
import org.hisp.dhis.message.MessageSender;
import org.hisp.dhis.notification.NotificationMessage;
import org.hisp.dhis.notification.NotificationMessageRenderer;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.outboundmessage.BatchResponseStatus;
import org.hisp.dhis.outboundmessage.OutboundMessageBatchStatus;
import org.hisp.dhis.outboundmessage.OutboundMessageResponseSummary;
import org.hisp.dhis.period.MonthlyPeriodType;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.program.message.ProgramMessage;
import org.hisp.dhis.program.message.ProgramMessageService;
import org.hisp.dhis.setting.SystemSettingsService;
import org.hisp.dhis.test.TestBase;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserSettingsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Zubair Asghar.
 */
@ExtendWith(MockitoExtension.class)
class DataSetNotificationServiceTest extends TestBase {
  public static final String TEMPALTE_A_UID = "smsTemplateA";

  public static final String TEMPALTE_B_UID = "emailTemplateB";

  public static final String DATA_ELEMENT_A_UID = "dataElementA";

  public static final String DATA_ELEMENT_B_UID = "dataElementB";

  public static final String PHONE_NUMBER = "00474";

  private DataSetNotificationTemplate smsTemplateA;

  private DataSetNotificationTemplate emailTemplateB;

  private BatchResponseStatus successStatus;

  private OutboundMessageResponseSummary summary;

  private List<DataSetNotificationTemplate> templates = new ArrayList<>();

  private CompleteDataSetRegistration registrationA;

  private NotificationMessage notificationMessage;

  private OrganisationUnit organisationUnitA;

  private OrganisationUnit organisationUnitB;

  private DataSet dataSetA;

  private Period periodA;

  private DataElement dataElementA;

  private DataElement dataElementB;

  private CategoryOptionCombo categoryOptionCombo;

  @Mock private DataSetNotificationTemplateService dsntService;

  @Mock private MessageConversationStore messageConversationStore;

  @Mock private ConfigurationService configurationService;

  @Mock private UserSettingsService userSettingsService;

  @Mock private SystemSettingsService settingsService;

  @Mock private DhisConfigurationProvider configurationProvider;

  @Mock private List<MessageSender> messageSenders;

  @InjectMocks private DefaultMessageService internalMessageService;

  @Mock private ProgramMessageService externalMessageService;

  @Mock private NotificationMessageRenderer<CompleteDataSetRegistration> renderer;

  @Mock private CompleteDataSetRegistrationService completeDataSetRegistrationService;

  @Mock private PeriodService periodService;

  @Mock private CategoryService categoryService;

  @Mock private I18nManager i18nManager;

  @Mock private OrganisationUnitService organisationUnitService;

  @Mock private MessageSender emailMessageSender;

  @Captor private ArgumentCaptor<CompleteDataSetRegistration> registrationCaptor;

  @Captor private ArgumentCaptor<DataSetNotificationTemplate> templateCaptor;

  @Captor private ArgumentCaptor<ArrayList<ProgramMessage>> programMessageCaptor;

  @Captor private ArgumentCaptor<Set<User>> userRecipientsCaptor;

  private DataSetNotificationService subject;

  @BeforeEach
  public void setUp() {
    this.subject =
        new DefaultDataSetNotificationService(
            dsntService,
            internalMessageService,
            externalMessageService,
            renderer,
            completeDataSetRegistrationService,
            periodService,
            categoryService,
            i18nManager,
            organisationUnitService);

    setUpConfigurations();
  }

  private void setUpConfigurations() {
    categoryOptionCombo = new CategoryOptionCombo();

    organisationUnitA = createOrganisationUnit('A');
    organisationUnitB = createOrganisationUnit('B');
    organisationUnitA.setPhoneNumber(PHONE_NUMBER);
    organisationUnitB.setPhoneNumber(PHONE_NUMBER);

    periodA = createPeriod(new MonthlyPeriodType(), getDate(2000, 1, 1), getDate(2000, 1, 31));

    dataElementA = createDataElement('A');
    dataElementB = createDataElement('B');
    dataElementA.setUid(DATA_ELEMENT_A_UID);
    dataElementB.setUid(DATA_ELEMENT_B_UID);

    dataSetA = createDataSet('A', new MonthlyPeriodType());

    dataSetA.addDataSetElement(dataElementA);
    dataSetA.addDataSetElement(dataElementB);

    dataSetA.getSources().add(organisationUnitA);
    dataSetA.getSources().add(organisationUnitB);

    smsTemplateA = new DataSetNotificationTemplate();
    smsTemplateA.setUid(TEMPALTE_A_UID);
    smsTemplateA.setDataSetNotificationTrigger(DataSetNotificationTrigger.DATA_SET_COMPLETION);
    smsTemplateA.setDeliveryChannels(Sets.newHashSet(DeliveryChannel.SMS));
    smsTemplateA.setNotificationRecipient(DataSetNotificationRecipient.ORGANISATION_UNIT_CONTACT);
    smsTemplateA.getDataSets().add(dataSetA);

    emailTemplateB = new DataSetNotificationTemplate();
    emailTemplateB.setUid(TEMPALTE_B_UID);
    emailTemplateB.setDataSetNotificationTrigger(DataSetNotificationTrigger.DATA_SET_COMPLETION);
    emailTemplateB.setDeliveryChannels(Sets.newHashSet(DeliveryChannel.EMAIL));
    emailTemplateB.setNotificationRecipient(DataSetNotificationRecipient.ORGANISATION_UNIT_CONTACT);
    emailTemplateB.getDataSets().add(dataSetA);

    templates.add(smsTemplateA);
    registrationA =
        new CompleteDataSetRegistration(
            dataSetA,
            periodA,
            organisationUnitA,
            categoryOptionCombo,
            new Date(),
            "",
            new Date(),
            "",
            true);
    notificationMessage = new NotificationMessage("subject", "message");
    summary = new OutboundMessageResponseSummary();
    summary.setBatchStatus(OutboundMessageBatchStatus.COMPLETED);
    summary.setChannel(DeliveryChannel.SMS);
    successStatus = new BatchResponseStatus(Arrays.asList(summary));
  }

  @Test
  void testShouldReturnNullIfRegistrationIsNull() {
    subject.sendCompleteDataSetNotifications(null);

    verify(dsntService, times(0)).getCompleteNotifications(any(DataSet.class));
  }

  @Test
  void testIfNotTemplateFoundForDataSet() {
    when(dsntService.getCompleteNotifications(any(DataSet.class))).thenReturn(null);

    subject.sendCompleteDataSetNotifications(registrationA);

    verify(dsntService, times(1)).getCompleteNotifications(any(DataSet.class));
    verify(renderer, times(0))
        .render(any(CompleteDataSetRegistration.class), any(DataSetNotificationTemplate.class));
  }

  @Test
  void testSendCompletionSMSNotification() {
    when(renderer.render(
            any(CompleteDataSetRegistration.class), any(DataSetNotificationTemplate.class)))
        .thenReturn(notificationMessage);
    when(externalMessageService.sendMessages(anyList())).thenReturn(successStatus);
    when(dsntService.getCompleteNotifications(any(DataSet.class))).thenReturn(templates);
    I18nFormat format = Mockito.mock(I18nFormat.class);
    when(i18nManager.getI18nFormat()).thenReturn(format);
    when(format.formatPeriod(any())).thenReturn("2000-1-1");

    subject.sendCompleteDataSetNotifications(registrationA);

    verify(renderer).render(registrationCaptor.capture(), templateCaptor.capture());

    assertEquals(registrationA, registrationCaptor.getValue());
    assertEquals(smsTemplateA, templateCaptor.getValue());

    verify(externalMessageService).sendMessages(programMessageCaptor.capture());

    assertEquals(1, programMessageCaptor.getValue().size());
    assertTrue(
        programMessageCaptor.getValue().get(0).getDeliveryChannels().contains(DeliveryChannel.SMS));
    assertEquals("subject", programMessageCaptor.getValue().get(0).getSubject());
    assertTrue(
        programMessageCaptor
            .getValue()
            .get(0)
            .getRecipients()
            .getPhoneNumbers()
            .contains(PHONE_NUMBER));
  }

  @Test
  @SuppressWarnings("unchecked")
  void sendCompleteDataSetNotificationsTest() {
    // setup
    org.hisp.dhis.user.User userEnabled = makeUser("testUserEnabled");
    userEnabled.setDisabled(false);
    userEnabled.setEmail("enabled@example.com");
    userEnabled.addOrganisationUnit(organisationUnitA);

    org.hisp.dhis.user.UserGroup userGroup = createUserGroup('A', Sets.newHashSet(userEnabled));

    List<DataSetNotificationTemplate> emailTemplates = new ArrayList<>();
    DataSetNotificationTemplate emailTemplateInternal = new DataSetNotificationTemplate();
    emailTemplateInternal.setDataSetNotificationTrigger(
        DataSetNotificationTrigger.DATA_SET_COMPLETION);
    emailTemplateInternal.setDeliveryChannels(Sets.newHashSet(DeliveryChannel.EMAIL));
    emailTemplateInternal.setNotificationRecipient(DataSetNotificationRecipient.USER_GROUP);
    emailTemplateInternal.getDataSets().add(dataSetA);
    emailTemplateInternal.setRecipientUserGroup(userGroup);

    emailTemplates.add(emailTemplateInternal);

    CompleteDataSetRegistration registration = new CompleteDataSetRegistration();
    registration.setSource(organisationUnitA);
    when(dsntService.getCompleteNotifications(any())).thenReturn(emailTemplates);

    I18nFormat format = Mockito.mock(I18nFormat.class);
    when(i18nManager.getI18nFormat()).thenReturn(format);
    when(format.formatPeriod(any())).thenReturn("2000-1-1");

    when(renderer.render(
            any(CompleteDataSetRegistration.class), any(DataSetNotificationTemplate.class)))
        .thenReturn(notificationMessage);

    when(configurationProvider.getServerBaseUrl()).thenReturn("https://dhis2.org");

    // mock an email sender so its args can be inspected
    Iterator<MessageSender> itr = Mockito.mock(Iterator.class);
    when(messageSenders.iterator()).thenReturn(itr);
    when(itr.hasNext()).thenReturn(true, false);
    when(itr.next()).thenReturn(emailMessageSender);

    ArgumentCaptor<String> footerCaptor = ArgumentCaptor.forClass(String.class);

    // condition
    subject.sendCompleteDataSetNotifications(registration);

    // checks
    verify(emailMessageSender)
        .sendMessageAsync(any(), any(), footerCaptor.capture(), any(), any(), anyBoolean());
    String footer = footerCaptor.getValue();
    assertTrue(footer.contains("https://dhis2.org/dhis-web-messaging/#/SYSTEM/"));
  }

  @Test
  void testSendCompleteDataSetNotifications_shouldNotSendToDisabledUsers() {
    // Setup
    org.hisp.dhis.user.User userDisabled = makeUser("testUserDisabled");
    userDisabled.setDisabled(true);
    userDisabled.setEmail("disabled@example.com");
    userDisabled.addOrganisationUnit(organisationUnitA);

    org.hisp.dhis.user.User userEnabled = makeUser("testUserEnabled");
    userEnabled.setDisabled(false);
    userEnabled.setEmail("enabled@example.com");
    userEnabled.addOrganisationUnit(organisationUnitA);

    org.hisp.dhis.user.UserGroup userGroup =
        createUserGroup('A', Sets.newHashSet(userDisabled, userEnabled));

    DataSetNotificationTemplate emailTemplateForUserGroup = new DataSetNotificationTemplate();
    emailTemplateForUserGroup.setUid("emailTemplateUserGroup");
    emailTemplateForUserGroup.setDataSetNotificationTrigger(
        DataSetNotificationTrigger.DATA_SET_COMPLETION);
    emailTemplateForUserGroup.setDeliveryChannels(Sets.newHashSet(DeliveryChannel.EMAIL));
    emailTemplateForUserGroup.setNotificationRecipient(DataSetNotificationRecipient.USER_GROUP);
    emailTemplateForUserGroup.setRecipientUserGroup(userGroup);
    emailTemplateForUserGroup.getDataSets().add(dataSetA);

    List<DataSetNotificationTemplate> testTemplates = new ArrayList<>();
    testTemplates.add(emailTemplateForUserGroup);

    when(dsntService.getCompleteNotifications(dataSetA)).thenReturn(testTemplates);
    when(renderer.render(
            any(CompleteDataSetRegistration.class), any(DataSetNotificationTemplate.class)))
        .thenReturn(notificationMessage);

    I18nFormat format = Mockito.mock(I18nFormat.class);
    when(i18nManager.getI18nFormat()).thenReturn(format);
    when(format.formatPeriod(any())).thenReturn("2000-1-1");

    Iterator<MessageSender> itr = Mockito.mock(Iterator.class);
    when(messageSenders.iterator()).thenReturn(itr);
    when(itr.hasNext()).thenReturn(true, false);
    when(itr.next()).thenReturn(emailMessageSender);

    subject.sendCompleteDataSetNotifications(registrationA);

    verify(emailMessageSender, times(1))
        .sendMessageAsync(
            any(String.class),
            any(String.class),
            any(String.class),
            any(),
            userRecipientsCaptor.capture(),
            anyBoolean());

    Set<org.hisp.dhis.user.User> capturedUsers = userRecipientsCaptor.getValue();
    assertEquals(1, capturedUsers.size(), "Only one user should receive the email.");
    assertTrue(
        capturedUsers.contains(userEnabled), "The enabled user should be in the recipient list.");
    assertTrue(
        !capturedUsers.contains(userDisabled),
        "The disabled user should NOT be in the recipient list.");
  }
}
