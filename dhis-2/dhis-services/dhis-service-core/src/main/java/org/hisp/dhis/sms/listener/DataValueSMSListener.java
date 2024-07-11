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
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.ValueType;
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
import org.hisp.dhis.user.UserService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component("org.hisp.dhis.sms.listener.DataValueSMSListener")
@Transactional
public class DataValueSMSListener extends CommandSMSListener {
  private static final String DATASET_LOCKED = "Dataset: [%s] is locked for period: [%s]";
  private static final String OU_NOT_IN_DATASET =
      "Organisation unit [%s] is not assigned to dataSet [%s]";
  private static final String DATASET_CANNOT_BE_MARKED_COMPLETE =
      "Dataset cannot be marked as completed";
  private static final String THANK_YOU_VALUES_ENTERED = "Thank you! Values entered: ";
  private static final String MISSING_VALUES_FOR = "Missing values for: ";

  // -------------------------------------------------------------------------
  // Dependencies
  // -------------------------------------------------------------------------

  private final CompleteDataSetRegistrationService registrationService;
  private final DataValueService dataValueService;
  private final SMSCommandService smsCommandService;
  private final DataSetService dataSetService;
  private final DataElementService dataElementService;

  public DataValueSMSListener(
      CategoryService dataElementCategoryService,
      UserService userService,
      IncomingSmsService incomingSmsService,
      @Qualifier("smsMessageSender") MessageSender smsSender,
      CompleteDataSetRegistrationService registrationService,
      DataValueService dataValueService,
      SMSCommandService smsCommandService,
      DataSetService dataSetService,
      DataElementService dataElementService) {
    super(dataElementCategoryService, userService, incomingSmsService, smsSender);
    this.registrationService = registrationService;
    this.dataValueService = dataValueService;
    this.smsCommandService = smsCommandService;
    this.dataSetService = dataSetService;
    this.dataElementService = dataElementService;
  }

  @Override
  protected void postProcess(
      IncomingSms sms, SMSCommand smsCommand, Map<String, String> parsedMessage) {
    String message = sms.getText();
    Date date = SmsUtils.lookForDate(message);
    String senderPhoneNumber = StringUtils.replace(sms.getOriginator(), "+", "");

    OrganisationUnit orgUnit =
        getOrganisationUnits(sms).iterator().hasNext()
            ? getOrganisationUnits(sms).iterator().next()
            : null;

    Period period = getPeriod(smsCommand, date);
    DataSet dataSet = smsCommand.getDataset();

    if (orgUnit == null || !dataSet.hasOrganisationUnit(orgUnit)) {
      sendFeedback(
          String.format(
              OU_NOT_IN_DATASET, orgUnit != null ? orgUnit.getUid() : "unknown", dataSet.getUid()),
          sms.getOriginator(),
          ERROR);
      update(sms, SmsMessageStatus.FAILED, false);
      return;
    }

    if (!isDataSetOpen(dataSet, period, orgUnit)) {
      sendFeedback(
          String.format(DATASET_LOCKED, dataSet.getUid(), period.getName()),
          sms.getOriginator(),
          ERROR);
      update(sms, SmsMessageStatus.FAILED, false);
      return;
    }

    boolean valueStored = processSMSCodes(sms, orgUnit, parsedMessage, smsCommand, date);

    if (parsedMessage.isEmpty()) {
      sendFeedback(
          StringUtils.defaultIfEmpty(
              smsCommand.getDefaultMessage(),
              "No values reported for command '" + smsCommand.getName() + "'"),
          sms.getOriginator(),
          ERROR);
      update(sms, SmsMessageStatus.FAILED, false);
    } else if (!valueStored) {
      sendFeedback(
          StringUtils.defaultIfEmpty(
              smsCommand.getWrongFormatMessage(), SMSCommand.WRONG_FORMAT_MESSAGE),
          sms.getOriginator(),
          ERROR);
      update(sms, SmsMessageStatus.FAILED, false);
    } else if (markCompleteDataSet(sms, orgUnit, smsCommand, date)) {
      sendSuccessFeedback(senderPhoneNumber, smsCommand, parsedMessage, date, orgUnit);
      update(sms, SmsMessageStatus.PROCESSED, true);
    } else {
      sendFeedback(DATASET_CANNOT_BE_MARKED_COMPLETE, sms.getOriginator(), ERROR);
      update(sms, SmsMessageStatus.FAILED, false);
    }
  }

  @Override
  protected SMSCommand getSMSCommand(IncomingSms sms) {
    return smsCommandService.getSMSCommand(
        SmsUtils.getCommandString(sms), ParserType.KEY_VALUE_PARSER);
  }

  private Period getPeriod(SMSCommand command, Date date) {
    Period period = command.getDataset().getPeriodType().createPeriod();
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

  private boolean isDataSetOpen(DataSet dataSet, Period period, OrganisationUnit orgUnit) {
    return dataSetService
        .getLockStatus(
            dataSet, period, orgUnit, dataElementCategoryService.getDefaultCategoryOptionCombo())
        .isOpen();
  }

  private boolean processSMSCodes(
      IncomingSms sms,
      OrganisationUnit orgUnit,
      Map<String, String> parsedMessage,
      SMSCommand smsCommand,
      Date date) {
    boolean valueStored = false;

    for (SMSCode code : smsCommand.getCodes()) {
      if (parsedMessage.containsKey(code.getCode())) {
        valueStored = storeDataValue(sms, orgUnit, parsedMessage, code, smsCommand, date);
      }
    }

    return valueStored;
  }

  private boolean storeDataValue(
      IncomingSms sms,
      OrganisationUnit orgUnit,
      Map<String, String> parsedMessage,
      SMSCode code,
      SMSCommand command,
      Date date) {
    String sender = sms.getOriginator();
    String storedBy =
        SmsUtils.getUser(sender, command, Collections.singletonList(getUser(sms))).getUsername();

    if (StringUtils.isBlank(storedBy)) {
      storedBy = "[unknown] from [" + sender + "]";
    }

    CategoryOptionCombo optionCombo =
        dataElementCategoryService.getCategoryOptionCombo(code.getOptionId().getId());
    Period period = getPeriod(command, date);
    DataValue dv =
        dataValueService.getDataValue(code.getDataElement(), period, orgUnit, optionCombo);
    String value = parsedMessage.get(code.getCode());

    value = replaceSpecialCharacters(command.getSpecialCharacters(), value);

    if (!StringUtils.isEmpty(value)) {
      boolean newDataValue = false;

      if (dv == null) {
        dv = new DataValue();
        dv.setCategoryOptionCombo(optionCombo);
        dv.setSource(orgUnit);
        dv.setDataElement(code.getDataElement());
        dv.setPeriod(period);
        dv.setComment("");
        newDataValue = true;
      }

      value = validateAndConvertValue(dv.getDataElement().getValueType(), value);

      dv.setValue(value);
      dv.setLastUpdated(new Date());
      dv.setStoredBy(storedBy);

      if (newDataValue) {
        dataValueService.addDataValue(dv);
      } else {
        dataValueService.updateDataValue(dv);
      }

      processFormula(code.getFormula(), period, orgUnit, value, storedBy);
    }

    return true;
  }

  private String replaceSpecialCharacters(
      Set<SMSSpecialCharacter> specialCharacters, String value) {
    for (SMSSpecialCharacter each : specialCharacters) {
      if (each.getName().equalsIgnoreCase(value)) {
        return each.getValue();
      }
    }
    return value;
  }

  private String validateAndConvertValue(ValueType valueType, String value) {
    if (ValueType.BOOLEAN == valueType) {
      if ("Y".equalsIgnoreCase(value) || "YES".equalsIgnoreCase(value)) {
        return "true";
      } else if ("N".equalsIgnoreCase(value) || "NO".equalsIgnoreCase(value)) {
        return "false";
      }
    } else if (valueType.isInteger()) {
      try {
        Integer.parseInt(value);
      } catch (NumberFormatException e) {
        log.error("Invalid integer value: {}", value, e);
        throw e;
      }
    }
    return value;
  }

  private void processFormula(
      String formula, Period period, OrganisationUnit orgUnit, String value, String storedBy) {
    if (formula != null) {
      try {
        String targetDataElementId = formula.substring(1);
        String operation = String.valueOf(formula.charAt(0));
        DataElement targetDataElement =
            dataElementService.getDataElement(Integer.parseInt(targetDataElementId));

        if (targetDataElement == null) {
          return;
        }

        DataValue targetDataValue =
            dataValueService.getDataValue(
                targetDataElement,
                period,
                orgUnit,
                dataElementCategoryService.getDefaultCategoryOptionCombo());

        int targetValue = 0;
        boolean newTargetDataValue = false;

        if (targetDataValue == null) {
          targetDataValue = new DataValue();
          targetDataValue.setCategoryOptionCombo(
              dataElementCategoryService.getDefaultCategoryOptionCombo());
          targetDataValue.setSource(orgUnit);
          targetDataValue.setDataElement(targetDataElement);
          targetDataValue.setPeriod(period);
          targetDataValue.setComment("");
          newTargetDataValue = true;
        } else {
          targetValue = Integer.parseInt(targetDataValue.getValue());
        }

        targetValue = updateTargetValue(operation, value, targetValue);

        targetDataValue.setValue(String.valueOf(targetValue));
        targetDataValue.setLastUpdated(new Date());
        targetDataValue.setStoredBy(storedBy);

        if (newTargetDataValue) {
          dataValueService.addDataValue(targetDataValue);
        } else {
          dataValueService.updateDataValue(targetDataValue);
        }

      } catch (Exception e) {
        log.error("Error processing formula: {}", formula, e);
      }
    }
  }

  private int updateTargetValue(String operation, String value, int targetValue) {
    if (operation.equals("+")) {
      return targetValue + Integer.parseInt(value);
    } else if (operation.equals("-")) {
      return targetValue - Integer.parseInt(value);
    }
    return targetValue;
  }

  private boolean markCompleteDataSet(
      IncomingSms sms, OrganisationUnit orgUnit, SMSCommand command, Date date) {
    String sender = sms.getOriginator();

    int numberOfEmptyValue = 0;
    Period period = null;
    for (SMSCode code : command.getCodes()) {
      CategoryOptionCombo optionCombo =
          dataElementCategoryService.getCategoryOptionCombo(code.getOptionId().getId());
      period = getPeriod(command, date);

      DataValue dv =
          dataValueService.getDataValue(code.getDataElement(), period, orgUnit, optionCombo);
      if (dv == null && !StringUtils.isEmpty(code.getCode())) {
        numberOfEmptyValue++;
      }
    }

    if (!isDataSetComplete(command, numberOfEmptyValue)) {
      return false;
    }

    String storedBy =
        SmsUtils.getUser(sender, command, Collections.singletonList(getUser(sms))).getUsername();

    if (StringUtils.isBlank(storedBy)) {
      storedBy = "[unknown] from [" + sender + "]";
    }

    deregisterCompleteDataSet(command.getDataset(), period, orgUnit);
    registerCompleteDataSet(command.getDataset(), period, orgUnit, storedBy);

    return true;
  }

  private boolean isDataSetComplete(SMSCommand command, int numberOfEmptyValue) {
    if (command.getCompletenessMethod() == CompletenessMethod.ALL_DATAVALUE
        && numberOfEmptyValue > 0) {
      return false;
    } else if (command.getCompletenessMethod() == CompletenessMethod.AT_LEAST_ONE_DATAVALUE
        && numberOfEmptyValue == command.getCodes().size()) {
      return false;
    } else if (command.getCompletenessMethod() == CompletenessMethod.DO_NOT_MARK_COMPLETE) {
      return false;
    }
    return true;
  }

  protected void sendSuccessFeedback(
      String sender,
      SMSCommand command,
      Map<String, String> parsedMessage,
      Date date,
      OrganisationUnit orgUnit) {
    StringBuilder reportBack = new StringBuilder(THANK_YOU_VALUES_ENTERED);
    StringBuilder notInReport = new StringBuilder(MISSING_VALUES_FOR);

    Period period;
    Map<String, DataValue> codesWithDataValues = new TreeMap<>();
    List<String> codesWithoutDataValues = new ArrayList<>();

    for (SMSCode code : command.getCodes()) {
      CategoryOptionCombo optionCombo =
          dataElementCategoryService.getCategoryOptionCombo(code.getOptionId().getId());
      period = getPeriod(command, date);

      DataValue dv =
          dataValueService.getDataValue(code.getDataElement(), period, orgUnit, optionCombo);
      if (dv == null && !StringUtils.isEmpty(code.getCode())) {
        codesWithoutDataValues.add(code.getCode());
      } else if (dv != null) {
        codesWithDataValues.put(code.getCode(), dv);
      }
    }

    appendDataValues(reportBack, codesWithDataValues);
    appendMissingValues(notInReport, codesWithoutDataValues);

    if (smsSender.isConfigured()) {
      if (StringUtils.isNotEmpty(command.getSuccessMessage())) {
        smsSender.sendMessage(null, command.getSuccessMessage(), sender);
      } else {
        smsSender.sendMessage(null, reportBack.toString(), sender);
      }
    } else {
      log.info("No sms configuration found.");
    }
  }

  private void appendDataValues(
      StringBuilder reportBack, Map<String, DataValue> codesWithDataValues) {
    for (String key : codesWithDataValues.keySet()) {
      DataValue dv = codesWithDataValues.get(key);
      String value = dv.getValue();

      if (ValueType.BOOLEAN == dv.getDataElement().getValueType()) {
        value = "true".equals(value) ? "Yes" : "false".equals(value) ? "No" : value;
      }
      reportBack.append(key).append("=").append(value).append(" ");
    }
  }

  private void appendMissingValues(StringBuilder notInReport, List<String> codesWithoutDataValues) {
    Collections.sort(codesWithoutDataValues);
    for (String key : codesWithoutDataValues) {
      notInReport.append(key).append(",");
    }
    notInReport.setLength(notInReport.length() - 1); // remove last comma
  }

  private void registerCompleteDataSet(
      DataSet dataSet, Period period, OrganisationUnit organisationUnit, String storedBy) {
    CategoryOptionCombo optionCombo = dataElementCategoryService.getDefaultCategoryOptionCombo();

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
    CategoryOptionCombo optionCombo = dataElementCategoryService.getDefaultCategoryOptionCombo();

    CompleteDataSetRegistration registration =
        registrationService.getCompleteDataSetRegistration(
            dataSet, period, organisationUnit, optionCombo);

    if (registration != null) {
      registrationService.deleteCompleteDataSetRegistration(registration);
    }
  }
}
