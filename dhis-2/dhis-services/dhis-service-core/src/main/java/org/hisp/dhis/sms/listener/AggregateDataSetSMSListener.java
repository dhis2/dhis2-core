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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.dataset.CompleteDataSetRegistration;
import org.hisp.dhis.dataset.CompleteDataSetRegistrationService;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.DataSetService;
import org.hisp.dhis.datavalue.DataValue;
import org.hisp.dhis.datavalue.DataValueService;
import org.hisp.dhis.message.MessageSender;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.program.EventService;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.sms.incoming.IncomingSms;
import org.hisp.dhis.sms.incoming.IncomingSmsService;
import org.hisp.dhis.smscompression.SmsConsts.SubmissionType;
import org.hisp.dhis.smscompression.SmsResponse;
import org.hisp.dhis.smscompression.models.AggregateDatasetSmsSubmission;
import org.hisp.dhis.smscompression.models.SmsDataValue;
import org.hisp.dhis.smscompression.models.SmsSubmission;
import org.hisp.dhis.smscompression.models.Uid;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.trackedentity.TrackedEntityTypeService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component("org.hisp.dhis.sms.listener.AggregateDatasetSMSListener")
@Transactional
public class AggregateDataSetSMSListener extends CompressionSMSListener {
  private final DataSetService dataSetService;

  private final DataValueService dataValueService;

  private final CompleteDataSetRegistrationService registrationService;

  public AggregateDataSetSMSListener(
      IncomingSmsService incomingSmsService,
      @Qualifier("smsMessageSender") MessageSender smsSender,
      UserService userService,
      TrackedEntityTypeService trackedEntityTypeService,
      TrackedEntityAttributeService trackedEntityAttributeService,
      ProgramService programService,
      OrganisationUnitService organisationUnitService,
      CategoryService categoryService,
      DataElementService dataElementService,
      EventService eventService,
      DataSetService dataSetService,
      DataValueService dataValueService,
      CompleteDataSetRegistrationService registrationService,
      IdentifiableObjectManager identifiableObjectManager) {
    super(
        incomingSmsService,
        smsSender,
        userService,
        trackedEntityTypeService,
        trackedEntityAttributeService,
        programService,
        organisationUnitService,
        categoryService,
        dataElementService,
        eventService,
        identifiableObjectManager);

    this.dataSetService = dataSetService;
    this.dataValueService = dataValueService;
    this.registrationService = registrationService;
  }

  @Override
  protected SmsResponse postProcess(IncomingSms sms, SmsSubmission submission)
      throws SMSProcessingException {
    AggregateDatasetSmsSubmission subm = (AggregateDatasetSmsSubmission) submission;

    Uid ouid = subm.getOrgUnit();
    Uid dsid = subm.getDataSet();
    String per = subm.getPeriod();
    Uid aocid = subm.getAttributeOptionCombo();

    OrganisationUnit orgUnit = organisationUnitService.getOrganisationUnit(ouid.getUid());
    User user = userService.getUser(subm.getUserId().getUid());

    DataSet dataSet = dataSetService.getDataSet(dsid.getUid());

    if (dataSet == null) {
      throw new SMSProcessingException(SmsResponse.INVALID_DATASET.set(dsid));
    }

    Period period = PeriodType.getPeriodFromIsoString(per);
    if (period == null) {
      throw new SMSProcessingException(SmsResponse.INVALID_PERIOD.set(per));
    }

    CategoryOptionCombo aoc = categoryService.getCategoryOptionCombo(aocid.getUid());

    if (aoc == null) {
      throw new SMSProcessingException(SmsResponse.INVALID_AOC.set(aocid));
    }

    if (!dataSet.hasOrganisationUnit(orgUnit)) {
      throw new SMSProcessingException(SmsResponse.OU_NOTIN_DATASET.set(ouid, dsid));
    }

    if (!dataSetService.getLockStatus(dataSet, period, orgUnit, aoc).isOpen()) {
      throw new SMSProcessingException(SmsResponse.DATASET_LOCKED.set(dsid, per));
    }

    List<Object> errorElems = submitDataValues(subm.getValues(), period, orgUnit, aoc, user);

    if (subm.isComplete()) {
      CompleteDataSetRegistration existingReg =
          registrationService.getCompleteDataSetRegistration(dataSet, period, orgUnit, aoc);
      if (existingReg != null) {
        registrationService.deleteCompleteDataSetRegistration(existingReg);
      }
      Date now = new Date();
      String username = user.getUsername();
      CompleteDataSetRegistration newReg =
          new CompleteDataSetRegistration(
              dataSet, period, orgUnit, aoc, now, username, now, username, true);
      registrationService.saveCompleteDataSetRegistration(newReg);
    }

    if (!errorElems.isEmpty()) {
      return SmsResponse.WARN_DVERR.setList(errorElems);
    } else if (subm.getValues() == null || subm.getValues().isEmpty()) {
      // TODO: Should we save if there are no data values?
      return SmsResponse.WARN_DVEMPTY;
    }

    return SmsResponse.SUCCESS;
  }

  private List<Object> submitDataValues(
      List<SmsDataValue> values,
      Period period,
      OrganisationUnit orgUnit,
      CategoryOptionCombo aoc,
      User user) {
    ArrayList<Object> errorElems = new ArrayList<>();

    if (values == null) {
      return errorElems;
    }

    for (SmsDataValue smsdv : values) {
      Uid deid = smsdv.getDataElement();
      Uid cocid = smsdv.getCategoryOptionCombo();
      String combid = deid + "-" + cocid;

      DataElement de = dataElementService.getDataElement(deid.getUid());

      if (de == null) {
        log.warn(
            String.format("Data element [%s] does not exist. Continuing with submission...", deid));
        errorElems.add(combid);
        continue;
      }

      CategoryOptionCombo coc = categoryService.getCategoryOptionCombo(cocid.getUid());

      if (coc == null) {
        log.warn(
            String.format(
                "Category Option Combo [%s] does not exist. Continuing with submission...", cocid));
        errorElems.add(combid);
        continue;
      }

      String val = smsdv.getValue();
      if (val == null || StringUtils.isEmpty(val)) {
        log.warn(
            String.format(
                "Value for [%s]  is null or empty. Continuing with submission...", combid));
        continue;
      }

      DataValue dv = dataValueService.getDataValue(de, period, orgUnit, coc, aoc);

      boolean newDataValue = false;
      if (dv == null) {
        dv = new DataValue();
        dv.setCategoryOptionCombo(coc);
        dv.setSource(orgUnit);
        dv.setDataElement(de);
        dv.setPeriod(period);
        dv.setComment("");
        newDataValue = true;
      }

      dv.setValue(val);
      dv.setLastUpdated(new java.util.Date());
      dv.setStoredBy(user.getUsername());

      if (newDataValue) {
        boolean addedDataValue = dataValueService.addDataValue(dv);
        if (!addedDataValue) {
          log.warn(
              String.format(
                  "Failed to submit data value [%s]. Continuing with submission...", combid));
          errorElems.add(combid);
        }
      } else {
        dataValueService.updateDataValue(dv);
      }
    }

    return errorElems;
  }

  @Override
  protected boolean handlesType(SubmissionType type) {
    return (type == SubmissionType.AGGREGATE_DATASET);
  }
}
