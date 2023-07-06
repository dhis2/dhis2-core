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
package org.hisp.dhis.sms.listener;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Sets;
import java.util.ArrayList;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.dataset.CompleteDataSetRegistrationService;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.DataSetService;
import org.hisp.dhis.dataset.LockStatus;
import org.hisp.dhis.datavalue.DataValueService;
import org.hisp.dhis.message.MessageSender;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.outboundmessage.OutboundMessageResponse;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramStageInstanceService;
import org.hisp.dhis.sms.incoming.IncomingSms;
import org.hisp.dhis.sms.incoming.IncomingSmsService;
import org.hisp.dhis.smscompression.SmsCompressionException;
import org.hisp.dhis.smscompression.models.AggregateDatasetSmsSubmission;
import org.hisp.dhis.smscompression.models.SmsDataValue;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.trackedentity.TrackedEntityTypeService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@MockitoSettings(strictness = Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
class AggregateDataSetSMSListenerTest extends CompressionSMSListenerTest {

  @Mock private UserService userService;

  @Mock private IncomingSmsService incomingSmsService;

  @Mock private MessageSender smsSender;

  @Mock private DataElementService dataElementService;

  @Mock private TrackedEntityTypeService trackedEntityTypeService;

  @Mock private TrackedEntityAttributeService trackedEntityAttributeService;

  @Mock private ProgramService programService;

  @Mock private OrganisationUnitService organisationUnitService;

  @Mock private CategoryService categoryService;

  @Mock private ProgramStageInstanceService programStageInstanceService;

  // Needed for this test

  @Mock private DataSetService dataSetService;

  @Mock private CompleteDataSetRegistrationService registrationService;

  @Mock private DataValueService dataValueService;

  @Mock private IdentifiableObjectManager identifiableObjectManager;

  private AggregateDataSetSMSListener subject;

  // Needed for all

  private User user;

  private OutboundMessageResponse response = new OutboundMessageResponse();

  private IncomingSms updatedIncomingSms;

  private String message = "";

  // Needed for this test

  private IncomingSms incomingSmsAggregate;

  private IncomingSms incomingSmsAggregateNoValues;

  private OrganisationUnit organisationUnit;

  private CategoryOptionCombo categoryOptionCombo;

  private DataElement dataElement;

  private DataSet dataSet;

  @BeforeEach
  public void initTest() throws SmsCompressionException {
    subject =
        new AggregateDataSetSMSListener(
            incomingSmsService,
            smsSender,
            userService,
            trackedEntityTypeService,
            trackedEntityAttributeService,
            programService,
            organisationUnitService,
            categoryService,
            dataElementService,
            programStageInstanceService,
            dataSetService,
            dataValueService,
            registrationService,
            identifiableObjectManager);

    setUpInstances();

    when(userService.getUser(anyString())).thenReturn(user);
    when(smsSender.isConfigured()).thenReturn(true);
    when(smsSender.sendMessage(any(), any(), anyString()))
        .thenAnswer(
            invocation -> {
              message = (String) invocation.getArguments()[1];
              return response;
            });

    when(organisationUnitService.getOrganisationUnit(anyString())).thenReturn(organisationUnit);
    when(dataSetService.getDataSet(anyString())).thenReturn(dataSet);
    when(dataSetService.getLockStatus(any(), any(DataSet.class), any(), any(), any(), any()))
        .thenReturn(LockStatus.OPEN);
    when(dataValueService.addDataValue(any())).thenReturn(true);
    when(categoryService.getCategoryOptionCombo(anyString())).thenReturn(categoryOptionCombo);
    when(dataElementService.getDataElement(anyString())).thenReturn(dataElement);

    doAnswer(
            invocation -> {
              updatedIncomingSms = (IncomingSms) invocation.getArguments()[0];
              return updatedIncomingSms;
            })
        .when(incomingSmsService)
        .update(any());
  }

  @Test
  void testAggregateDatasetListener() {
    subject.receive(incomingSmsAggregate);

    assertNotNull(updatedIncomingSms);
    assertTrue(updatedIncomingSms.isParsed());
    assertEquals(SUCCESS_MESSAGE, message);

    verify(incomingSmsService, times(1)).update(any());
  }

  @Test
  void testAggregateDatasetListenerRepeat() {
    subject.receive(incomingSmsAggregate);
    subject.receive(incomingSmsAggregate);

    assertNotNull(updatedIncomingSms);
    assertTrue(updatedIncomingSms.isParsed());
    assertEquals(SUCCESS_MESSAGE, message);

    verify(incomingSmsService, times(2)).update(any());
  }

  @Test
  void testAggregateDatasetListenerNoValues() {
    subject.receive(incomingSmsAggregateNoValues);

    assertNotNull(updatedIncomingSms);
    assertTrue(updatedIncomingSms.isParsed());
    assertEquals(NOVALUES_MESSAGE, message);

    verify(incomingSmsService, times(1)).update(any());
  }

  private void setUpInstances() throws SmsCompressionException {
    organisationUnit = createOrganisationUnit('O');
    user = createUser('U');
    user.setPhoneNumber(ORIGINATOR);
    user.setOrganisationUnits(Sets.newHashSet(organisationUnit));
    dataSet = createDataSet('D');
    dataSet.getSources().add(organisationUnit);
    categoryOptionCombo = createCategoryOptionCombo('C');
    dataElement = createDataElement('D');

    incomingSmsAggregate = createSMSFromSubmission(createAggregateDatasetSubmission());

    AggregateDatasetSmsSubmission subm = createAggregateDatasetSubmission();

    subm.setValues(null);
    incomingSmsAggregateNoValues = createSMSFromSubmission(subm);
  }

  private AggregateDatasetSmsSubmission createAggregateDatasetSubmission() {
    AggregateDatasetSmsSubmission subm = new AggregateDatasetSmsSubmission();

    subm.setUserId(user.getUid());
    subm.setOrgUnit(organisationUnit.getUid());
    subm.setDataSet(dataSet.getUid());
    subm.setComplete(true);
    subm.setAttributeOptionCombo(categoryOptionCombo.getUid());
    subm.setPeriod("2019W16");
    ArrayList<SmsDataValue> values = new ArrayList<>();
    values.add(new SmsDataValue(categoryOptionCombo.getUid(), dataElement.getUid(), "12345678"));
    subm.setValues(values);
    subm.setSubmissionId(1);

    return subm;
  }
}
