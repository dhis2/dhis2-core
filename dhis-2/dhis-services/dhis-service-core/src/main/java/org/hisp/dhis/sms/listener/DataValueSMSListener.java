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

import static java.lang.Integer.parseInt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import javax.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.dataset.CompleteDataSetRegistration;
import org.hisp.dhis.dataset.CompleteDataSetRegistrationService;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.datavalue.DataEntryService;
import org.hisp.dhis.datavalue.DataEntryValue;
import org.hisp.dhis.datavalue.DataValue;
import org.hisp.dhis.datavalue.DataValueService;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.message.MessageSender;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.sms.command.CompletenessMethod;
import org.hisp.dhis.sms.command.SMSCommand;
import org.hisp.dhis.sms.command.SMSCommandService;
import org.hisp.dhis.sms.command.SMSSpecialCharacter;
import org.hisp.dhis.sms.command.code.SMSCode;
import org.hisp.dhis.sms.incoming.IncomingSms;
import org.hisp.dhis.sms.incoming.IncomingSmsService;
import org.hisp.dhis.sms.incoming.SmsMessageStatus;
import org.hisp.dhis.sms.parse.ParserType;
import org.hisp.dhis.system.util.SmsUtils;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.user.UserService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component("org.hisp.dhis.sms.listener.DataValueSMSListener")
@Transactional
public class DataValueSMSListener extends CommandSMSListener {

  private final CompleteDataSetRegistrationService registrationService;

  private final DataValueService dataValueService;

  private final CategoryService dataElementCategoryService;

  private final SMSCommandService smsCommandService;

  private final DataEntryService dataEntryService;

  private final DataElementService dataElementService;

  public DataValueSMSListener(
      UserService userService,
      IncomingSmsService incomingSmsService,
      MessageSender smsMessageSender,
      CompleteDataSetRegistrationService registrationService,
      DataValueService dataValueService,
      CategoryService dataElementCategoryService1,
      SMSCommandService smsCommandService,
      DataEntryService dataEntryService,
      DataElementService dataElementService) {
    super(userService, incomingSmsService, smsMessageSender);
    this.registrationService = registrationService;
    this.dataValueService = dataValueService;
    this.dataElementCategoryService = dataElementCategoryService1;
    this.smsCommandService = smsCommandService;
    this.dataEntryService = dataEntryService;
    this.dataElementService = dataElementService;
  }

  @Override
  protected void postProcess(
      @Nonnull IncomingSms sms,
      @Nonnull UserDetails smsCreatedBy,
      @Nonnull SMSCommand smsCommand,
      @Nonnull Map<String, String> codeValues) {

    if (codeValues.isEmpty()) {
      sendFeedback(
          org.apache.commons.lang3.StringUtils.defaultIfEmpty(
              smsCommand.getDefaultMessage(),
              "No values reported for command '" + smsCommand.getName() + "'"),
          sms.getOriginator(),
          ERROR);

      update(sms, SmsMessageStatus.FAILED, false);
      return;
    }

    String message = sms.getText();

    Date date = SmsUtils.lookForDate(message);
    String senderPhoneNumber = StringUtils.replace(sms.getOriginator(), "+", "");

    OrganisationUnit orgUnit = null;

    if (getOrganisationUnits(sms).iterator().hasNext()) {
      orgUnit = getOrganisationUnits(sms).iterator().next();
    }

    for (SMSCode code : smsCommand.getCodes()) {
      if (codeValues.containsKey(code.getCode())) {
        try {
          storeDataValue(orgUnit, codeValues, code, smsCommand, date);
        } catch (Exception ex) {
          sendFeedback(
              StringUtils.defaultIfEmpty(
                  smsCommand.getWrongFormatMessage(), SMSCommand.WRONG_FORMAT_MESSAGE),
              sms.getOriginator(),
              ERROR);

          update(sms, SmsMessageStatus.FAILED, false);
          return;
        }
      }
    }

    if (markCompleteDataSet(sms, smsCreatedBy, orgUnit, smsCommand, date)) {
      sendSuccessFeedback(senderPhoneNumber, smsCommand, date, orgUnit);

      update(sms, SmsMessageStatus.PROCESSED, true);
    } else {
      sendFeedback("Dataset cannot be marked as completed", sms.getOriginator(), ERROR);

      update(sms, SmsMessageStatus.FAILED, false);
    }
  }

  @Override
  protected SMSCommand getSMSCommand(@Nonnull IncomingSms sms) {
    return smsCommandService.getSMSCommand(
        SmsUtils.getCommandString(sms), ParserType.KEY_VALUE_PARSER);
  }

  private Period getPeriod(SMSCommand command, Date date) {
    Period period;
    period = command.getDataset().getPeriodType().createPeriod();
    PeriodType periodType = period.getPeriodType();

    if (command.isCurrentPeriodUsedForReporting()) {
      period = periodType.createPeriod(new Date());
    } else {
      period = periodType.getPreviousPeriod(period);
    }

    if (date != null) {
      period = periodType.createPeriod(date);
    }

    return period;
  }

  // -------------------------------------------------------------------------
  // Supportive methods
  // -------------------------------------------------------------------------

  private void storeDataValue(
      OrganisationUnit orgUnit,
      Map<String, String> message,
      SMSCode code,
      SMSCommand command,
      Date date)
      throws ConflictException, BadRequestException {

    String value = message.get(code.getCode());
    Set<SMSSpecialCharacter> specialCharacters = command.getSpecialCharacters();
    for (SMSSpecialCharacter each : specialCharacters) {
      if (each.getName().equalsIgnoreCase(value)) {
        value = each.getValue();
        break;
      }
    }

    Period period = getPeriod(command, date);

    if (!StringUtils.isEmpty(value)) {
      UID ds = UID.of(command.getDataset());
      String de = getUid(code.getDataElement());
      String ou = getUid(orgUnit);
      String coc = getUid(code.getOptionId());
      String pe = period.getIsoDate();
      DataEntryValue.Input dv =
          new DataEntryValue.Input(de, ou, coc, null, null, pe, value, null, null, null);
      dataEntryService.upsertValue(false, ds, dataEntryService.decodeValue(ds, dv));
    }

    String formula = code.getFormula();
    if (formula == null) return;

    String targetDataElementId = formula.substring(1);
    String operation = String.valueOf(formula.charAt(0));

    DataElement targetDataElement =
        dataElementService.getDataElement(parseInt(targetDataElementId));

    if (targetDataElement == null) {
      return;
    }

    DataValue curValue =
        dataValueService.getDataValue(
            targetDataElement,
            period,
            orgUnit,
            dataElementCategoryService.getDefaultCategoryOptionCombo());

    int val = curValue == null ? 0 : parseInt(curValue.getValue());

    if (operation.equals("+")) {
      val += parseInt(value);
    } else if (operation.equals("-")) {
      val -= parseInt(value);
    }

    UID ds = UID.of(command.getDataset());
    String de = getUid(targetDataElement);
    String ou = getUid(orgUnit);
    String pe = period.getIsoDate();
    DataEntryValue.Input dv =
        new DataEntryValue.Input(de, ou, null, null, null, pe, "" + val, null, null, null);
    dataEntryService.upsertValue(false, ds, dataEntryService.decodeValue(ds, dv));
  }

  private static String getUid(IdentifiableObject object) {
    return object == null ? null : object.getUid();
  }

  private boolean markCompleteDataSet(
      IncomingSms sms,
      UserDetails smsCreatedBy,
      OrganisationUnit orgunit,
      SMSCommand command,
      Date date) {
    String sender = sms.getOriginator();

    Period period = null;
    int numberOfEmptyValue = 0;
    for (SMSCode code : command.getCodes()) {

      CategoryOptionCombo optionCombo =
          dataElementCategoryService.getCategoryOptionCombo(code.getOptionId().getId());

      period = getPeriod(command, date);

      DataValue dv =
          dataValueService.getDataValue(code.getDataElement(), period, orgunit, optionCombo);

      if (dv == null && !StringUtils.isEmpty(code.getCode())) {
        numberOfEmptyValue++;
      }
    }

    // Check completeness method
    if (command.getCompletenessMethod() == CompletenessMethod.ALL_DATAVALUE) {
      if (numberOfEmptyValue > 0) {
        return false;
      }
    } else if (command.getCompletenessMethod() == CompletenessMethod.AT_LEAST_ONE_DATAVALUE) {
      if (numberOfEmptyValue == command.getCodes().size()) {
        return false;
      }
    } else if (command.getCompletenessMethod() == CompletenessMethod.DO_NOT_MARK_COMPLETE) {
      return false;
    }

    // Go through the complete process
    String storedBy = smsCreatedBy.getUsername();

    if (StringUtils.isBlank(storedBy)) {
      storedBy = "[unknown] from [" + sender + "]";
    }

    // If new values are submitted re-register as complete
    deregisterCompleteDataSet(command.getDataset(), period, orgunit);
    registerCompleteDataSet(command.getDataset(), period, orgunit, storedBy);

    return true;
  }

  protected void sendSuccessFeedback(
      String sender, SMSCommand command, Date date, OrganisationUnit orgunit) {
    String reportBack = "Thank you! Values entered: ";
    String notInReport = "Missing values for: ";

    Period period;

    Map<String, DataValue> codesWithDataValues = new TreeMap<>();
    List<String> codesWithoutDataValues = new ArrayList<>();

    for (SMSCode code : command.getCodes()) {

      CategoryOptionCombo optionCombo =
          dataElementCategoryService.getCategoryOptionCombo(code.getOptionId().getId());

      period = getPeriod(command, date);

      DataValue dv =
          dataValueService.getDataValue(code.getDataElement(), period, orgunit, optionCombo);

      if (dv == null && !StringUtils.isEmpty(code.getCode())) {
        codesWithoutDataValues.add(code.getCode());
      } else if (dv != null) {
        codesWithDataValues.put(code.getCode(), dv);
      }
    }

    for (String key : codesWithDataValues.keySet()) {
      DataValue dv = codesWithDataValues.get(key);
      String value = dv.getValue();

      if (ValueType.BOOLEAN == dv.getDataElement().getValueType()) {
        if ("true".equals(value)) {
          value = "Yes";
        } else if ("false".equals(value)) {
          value = "No";
        }
      }
      reportBack += key + "=" + value + " ";
    }

    Collections.sort(codesWithoutDataValues);

    for (String key : codesWithoutDataValues) {
      notInReport += key + ",";
    }

    notInReport = notInReport.substring(0, notInReport.length() - 1);

    if (smsMessageSender.isConfigured()) {
      if (command.getSuccessMessage() != null) {
        smsMessageSender.sendMessage(null, command.getSuccessMessage(), sender);
      } else {
        smsMessageSender.sendMessage(null, reportBack, sender);
      }
    } else {
      log.info("No sms configuration found.");
    }
  }

  private void registerCompleteDataSet(
      DataSet dataSet, Period period, OrganisationUnit organisationUnit, String storedBy) {
    CategoryOptionCombo optionCombo =
        dataElementCategoryService.getDefaultCategoryOptionCombo(); // TODO

    if (registrationService.getCompleteDataSetRegistration(
            dataSet, period, organisationUnit, optionCombo)
        == null) {
      Date now = new Date();
      CompleteDataSetRegistration registration =
          new CompleteDataSetRegistration(
              dataSet, period, organisationUnit, optionCombo, now, storedBy, now, storedBy, true);

      registrationService.saveCompleteDataSetRegistration(registration);
    }
  }

  private void deregisterCompleteDataSet(
      DataSet dataSet, Period period, OrganisationUnit organisationUnit) {
    CategoryOptionCombo optionCombo =
        dataElementCategoryService.getDefaultCategoryOptionCombo(); // TODO

    CompleteDataSetRegistration registration =
        registrationService.getCompleteDataSetRegistration(
            dataSet, period, organisationUnit, optionCombo);

    if (registration != null) {
      registrationService.deleteCompleteDataSetRegistration(registration);
    }
  }
}
