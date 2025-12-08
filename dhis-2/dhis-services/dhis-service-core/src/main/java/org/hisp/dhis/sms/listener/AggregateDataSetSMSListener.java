/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.sms.listener;

import static org.hisp.dhis.scheduling.RecordingJobProgress.transitory;
import static org.hisp.dhis.util.DateUtils.toMediumDate;

import java.util.Date;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.datavalue.DataEntryGroup;
import org.hisp.dhis.datavalue.DataEntryService;
import org.hisp.dhis.datavalue.DataEntryValue;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.feedback.DataEntrySummary;
import org.hisp.dhis.message.MessageSender;
import org.hisp.dhis.sms.incoming.IncomingSms;
import org.hisp.dhis.sms.incoming.IncomingSmsService;
import org.hisp.dhis.smscompression.SmsConsts.SubmissionType;
import org.hisp.dhis.smscompression.SmsResponse;
import org.hisp.dhis.smscompression.models.AggregateDatasetSmsSubmission;
import org.hisp.dhis.smscompression.models.SmsDataValue;
import org.hisp.dhis.smscompression.models.SmsSubmission;
import org.hisp.dhis.user.UserDetails;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component("org.hisp.dhis.sms.listener.AggregateDatasetSMSListener")
@Transactional
public class AggregateDataSetSMSListener extends CompressionSMSListener {

  private final DataEntryService dataEntryService;

  public AggregateDataSetSMSListener(
      IncomingSmsService incomingSmsService,
      @Qualifier("smsMessageSender") MessageSender smsSender,
      DataEntryService dataEntryService,
      IdentifiableObjectManager identifiableObjectManager) {
    super(incomingSmsService, smsSender, identifiableObjectManager);
    this.dataEntryService = dataEntryService;
  }

  @Override
  protected SmsResponse postProcess(
      IncomingSms sms, SmsSubmission submission, UserDetails smsCreatedBy)
      throws SMSProcessingException, ConflictException {

    return importDataValues((AggregateDatasetSmsSubmission) submission);
  }

  private SmsResponse importDataValues(AggregateDatasetSmsSubmission sub) {
    // Note that the user passed to postProcess is the current user
    // so no need to pass it along explicitly
    String ou = sub.getOrgUnit().getUid();
    String ds = sub.getDataSet().getUid();
    String pe = sub.getPeriod();
    String aoc = sub.getAttributeOptionCombo().getUid();

    DataEntryGroup.Options options = new DataEntryGroup.Options();
    List<DataEntryValue.Input> values =
        sub.getValues() == null
            ? List.of()
            : sub.getValues().stream().map(AggregateDataSetSMSListener::toDataEntryValue).toList();
    if (values.isEmpty()) return SmsResponse.WARN_DVEMPTY;
    String completionDate = sub.isComplete() ? toMediumDate(new Date()) : null;
    DataEntryGroup.Input input =
        new DataEntryGroup.Input(null, ds, completionDate, null, ou, pe, aoc, null, values);
    try {
      DataEntryGroup group = dataEntryService.decodeGroup(input);
      DataEntrySummary result = dataEntryService.upsertGroup(options, group, transitory());
      if (!result.errors().isEmpty())
        return SmsResponse.WARN_DVERR.setList(
            result.errors().stream()
                .map(e -> toIdentifier(group.values().get(e.value().index())))
                .toList());
      return SmsResponse.SUCCESS;
    } catch (ConflictException | BadRequestException ex) {
      switch (ex.getCode()) {
        case E8002, E8003, E8004, E8005:
          throw new SMSProcessingException(SmsResponse.INVALID_DATASET.set(ds));
        case E8021:
          throw new SMSProcessingException(SmsResponse.INVALID_PERIOD.set(pe));
        case E8023:
          throw new SMSProcessingException(SmsResponse.INVALID_AOC.set(aoc));
        case E8022, E8025:
          throw new SMSProcessingException(SmsResponse.OU_NOTIN_DATASET.set(ou, ds));
        case E8030, E8033:
          throw new SMSProcessingException(SmsResponse.DATASET_LOCKED.set(ds, pe));
        default:
          throw new SMSProcessingException(SmsResponse.UNKNOWN_ERROR);
      }
    }
  }

  private static DataEntryValue.Input toDataEntryValue(SmsDataValue value) {
    String de = value.getDataElement().getUid();
    String coc = value.getCategoryOptionCombo().getUid();
    return new DataEntryValue.Input(
        de, null, coc, null, null, null, null, null, value.getValue(), null, null, null);
  }

  private static Object toIdentifier(DataEntryValue dv) {
    return dv.dataElement() + "-" + dv.categoryOptionCombo();
  }

  @Override
  protected boolean handlesType(SubmissionType type) {
    return (type == SubmissionType.AGGREGATE_DATASET);
  }
}
