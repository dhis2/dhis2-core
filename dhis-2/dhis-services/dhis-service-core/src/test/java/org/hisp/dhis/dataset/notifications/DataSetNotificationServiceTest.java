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
package org.hisp.dhis.dataset.notifications;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyList;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import org.hisp.dhis.DhisConvenienceTest;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.DeliveryChannel;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataset.CompleteDataSetRegistration;
import org.hisp.dhis.dataset.CompleteDataSetRegistrationService;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.i18n.I18nManager;
import org.hisp.dhis.message.MessageService;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Zubair Asghar.
 */
@ExtendWith(MockitoExtension.class)
class DataSetNotificationServiceTest extends DhisConvenienceTest {
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

  @Mock private MessageService internalMessageService;

  @Mock private ProgramMessageService externalMessageService;

  @Mock private NotificationMessageRenderer<CompleteDataSetRegistration> renderer;

  @Mock private CompleteDataSetRegistrationService completeDataSetRegistrationService;

  @Mock private PeriodService periodService;

  @Mock private CategoryService categoryService;

  @Mock private I18nManager i18nManager;

  @Mock private OrganisationUnitService organisationUnitService;

  @Captor private ArgumentCaptor<CompleteDataSetRegistration> registrationCaptor;

  @Captor private ArgumentCaptor<DataSetNotificationTemplate> templateCaptor;

  @Captor private ArgumentCaptor<ArrayList<ProgramMessage>> programMessageCaptor;

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
}
