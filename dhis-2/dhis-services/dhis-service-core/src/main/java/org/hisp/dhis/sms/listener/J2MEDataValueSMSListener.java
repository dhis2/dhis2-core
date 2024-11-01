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

import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataset.CompleteDataSetRegistration;
import org.hisp.dhis.dataset.CompleteDataSetRegistrationService;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.datavalue.DataValue;
import org.hisp.dhis.datavalue.DataValueService;
import org.hisp.dhis.message.MessageSender;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.DailyPeriodType;
import org.hisp.dhis.period.MonthlyPeriodType;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.period.QuarterlyPeriodType;
import org.hisp.dhis.period.WeeklyPeriodType;
import org.hisp.dhis.period.YearlyPeriodType;
import org.hisp.dhis.sms.command.SMSCommand;
import org.hisp.dhis.sms.command.SMSCommandService;
import org.hisp.dhis.sms.command.code.SMSCode;
import org.hisp.dhis.sms.incoming.IncomingSms;
import org.hisp.dhis.sms.incoming.IncomingSmsService;
import org.hisp.dhis.sms.parse.ParserType;
import org.hisp.dhis.sms.parse.SMSParserException;
import org.hisp.dhis.system.util.SmsUtils;
import org.hisp.dhis.system.util.ValidationUtils;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.util.DateUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component("org.hisp.dhis.sms.listener.J2MEDataValueSMSListener")
@Transactional
public class J2MEDataValueSMSListener extends CommandSMSListener {

  private final DataValueService dataValueService;

  private final SMSCommandService smsCommandService;

  private final CompleteDataSetRegistrationService registrationService;

  private final CategoryService dataElementCategoryService;

  public J2MEDataValueSMSListener(
      UserService userService,
      IncomingSmsService incomingSmsService,
      @Qualifier("smsMessageSender") MessageSender smsSender,
      DataValueService dataValueService,
      SMSCommandService smsCommandService,
      CompleteDataSetRegistrationService registrationService,
      CategoryService dataElementCategoryService) {
    super(userService, incomingSmsService, smsSender);
    this.dataValueService = dataValueService;
    this.smsCommandService = smsCommandService;
    this.registrationService = registrationService;
    this.dataElementCategoryService = dataElementCategoryService;
  }

  @Transactional
  @Override
  public boolean accept(@Nonnull IncomingSms sms) {
    return smsCommandService.getSMSCommand(SmsUtils.getCommandString(sms), ParserType.J2ME_PARSER)
        != null;
  }

  @Transactional
  @Override
  public void receive(@Nonnull IncomingSms sms, @Nonnull UserDetails smsCreatedBy) {
    String message = sms.getText();

    SMSCommand smsCommand =
        smsCommandService.getSMSCommand(SmsUtils.getCommandString(sms), ParserType.J2ME_PARSER);

    String token[] = message.split("!");
    Map<String, String> parsedMessage = this.parse(token[1], smsCommand);

    String senderPhoneNumber = StringUtils.replace(sms.getOriginator(), "+", "");
    Collection<OrganisationUnit> orgUnits = getOrganisationUnits(sms);

    if (orgUnits == null || orgUnits.isEmpty()) {
      throw new SMSParserException(smsCommand.getNoUserMessage());
    }

    OrganisationUnit orgUnit = SmsUtils.selectOrganisationUnit(orgUnits, parsedMessage, smsCommand);
    Period period = this.getPeriod(token[0].trim(), smsCommand.getDataset().getPeriodType());
    boolean valueStored = false;

    for (SMSCode code : smsCommand.getCodes()) {
      if (parsedMessage.containsKey(code.getCode())) {
        storeDataValue(sms, smsCreatedBy, orgUnit, parsedMessage, code, period);
        valueStored = true;
      }
    }

    if (parsedMessage.isEmpty() || !valueStored) {
      if (StringUtils.isEmpty(smsCommand.getDefaultMessage())) {
        throw new SMSParserException(
            "No values reported for command '" + smsCommand.getName() + "'");
      } else {
        throw new SMSParserException(smsCommand.getDefaultMessage());
      }
    }

    this.registerCompleteDataSet(smsCommand.getDataset(), period, orgUnit, "mobile");

    this.sendSuccessFeedback(senderPhoneNumber, smsCommand, period, orgUnit);
  }

  @Override
  protected SMSCommand getSMSCommand(@Nonnull IncomingSms sms) {
    return null;
  }

  @Override
  protected void postProcess(
      @Nonnull IncomingSms sms,
      @Nonnull UserDetails smsCreatedBy,
      @Nonnull SMSCommand smsCommand,
      @Nonnull Map<String, String> codeValues) {}

  private Map<String, String> parse(String sms, SMSCommand smsCommand) {
    String[] keyValuePairs;

    if (sms.contains("#")) {
      keyValuePairs = sms.split("#");
    } else {
      keyValuePairs = new String[1];
      keyValuePairs[0] = sms;
    }

    Map<String, String> keyValueMap = new HashMap<>();
    for (String keyValuePair : keyValuePairs) {
      String[] token = keyValuePair.split(Pattern.quote(smsCommand.getSeparator()));
      keyValueMap.put(token[0], token[1]);
    }

    return keyValueMap;
  }

  private void storeDataValue(
      IncomingSms sms,
      UserDetails smsCreatedBy,
      OrganisationUnit orgUnit,
      Map<String, String> parsedMessage,
      SMSCode code,
      Period period) {
    validateUserOrgUnits(smsCreatedBy);
    String upperCaseCode = code.getCode().toUpperCase();
    String sender = sms.getOriginator();
    String storedBy = smsCreatedBy.getUsername();

    if (StringUtils.isBlank(storedBy)) {
      storedBy = "[unknown] from [" + sender + "]";
    }

    CategoryOptionCombo optionCombo =
        dataElementCategoryService.getCategoryOptionCombo(code.getOptionId().getId());

    DataValue dv =
        dataValueService.getDataValue(code.getDataElement(), period, orgUnit, optionCombo);

    String value = parsedMessage.get(upperCaseCode);
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

      if (ValueType.BOOLEAN == dv.getDataElement().getValueType()) {
        if ("Y".equalsIgnoreCase(value) || "YES".equalsIgnoreCase(value)) {
          value = "true";
        } else if ("N".equalsIgnoreCase(value) || "NO".equalsIgnoreCase(value)) {
          value = "false";
        }
      }

      dv.setValue(value);
      dv.setLastUpdated(new java.util.Date());
      dv.setStoredBy(storedBy);

      if (ValidationUtils.valueIsValid(value, dv.getDataElement()) != null) {
        return; // not a valid value for data element
      }

      if (newDataValue) {
        dataValueService.addDataValue(dv);
      } else {
        dataValueService.updateDataValue(dv);
      }
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
      registration.setPeriodName(registration.getPeriod().toString());

      registrationService.saveCompleteDataSetRegistration(registration);
    }
  }

  private void sendSuccessFeedback(
      String sender, SMSCommand command, Period period, OrganisationUnit orgunit) {
    String reportBack = "Thank you! Values entered: ";
    String notInReport = "Missing values for: ";
    boolean missingElements = false;

    for (SMSCode code : command.getCodes()) {
      CategoryOptionCombo optionCombo =
          dataElementCategoryService.getCategoryOptionCombo(code.getOptionId().getId());

      DataValue dv =
          dataValueService.getDataValue(code.getDataElement(), period, orgunit, optionCombo);

      if (dv == null && !StringUtils.isEmpty(code.getCode())) {
        notInReport += code.getCode() + ",";
        missingElements = true;
      } else if (dv != null) {
        String value = dv.getValue();

        if (ValueType.BOOLEAN == dv.getDataElement().getValueType()) {
          if ("true".equals(value)) {
            value = "Yes";
          } else if ("false".equals(value)) {
            value = "No";
          }
        }

        reportBack += code.getCode() + "=" + value + " ";
      }
    }

    notInReport = notInReport.substring(0, notInReport.length() - 1);

    if (missingElements) {
      reportBack += notInReport;
    }

    reportBack = command.getSuccessMessage();
    smsMessageSender.sendMessage(null, reportBack, sender);
  }

  public Period getPeriod(String periodName, PeriodType periodType)
      throws IllegalArgumentException {
    if (periodType instanceof DailyPeriodType) {
      return periodType.createPeriod(DateUtils.toMediumDate(periodName));
    }

    if (periodType instanceof WeeklyPeriodType) {
      return periodType.createPeriod(DateUtils.toMediumDate(periodName));
    }

    if (periodType instanceof MonthlyPeriodType) {
      int dashIndex = periodName.indexOf('-');

      if (dashIndex < 0) {
        return null;
      }

      int month = Integer.parseInt(periodName.substring(0, dashIndex));
      int year = Integer.parseInt(periodName.substring(dashIndex + 1));

      Calendar cal = Calendar.getInstance();
      cal.set(Calendar.YEAR, year);
      cal.set(Calendar.MONTH, month);

      return periodType.createPeriod(cal.getTime());
    }

    if (periodType instanceof YearlyPeriodType) {
      Calendar cal = Calendar.getInstance();
      cal.set(Calendar.YEAR, Integer.parseInt(periodName));

      return periodType.createPeriod(cal.getTime());
    }

    if (periodType instanceof QuarterlyPeriodType) {
      Calendar cal = Calendar.getInstance();

      int month = 0;

      switch (periodName.substring(0, periodName.indexOf(" "))) {
        case "Jan":
          month = 1;
          break;
        case "Apr":
          month = 4;
          break;
        case "Jul":
          month = 6;
          break;
        case "Oct":
          month = 10;
          break;
      }

      int year = Integer.parseInt(periodName.substring(periodName.lastIndexOf(" ") + 1));

      cal.set(Calendar.MONTH, month);
      cal.set(Calendar.YEAR, year);

      if (month != 0) {
        return periodType.createPeriod(cal.getTime());
      }
    }

    throw new IllegalArgumentException(
        "Couldn't make a period of type " + periodType.getName() + " and name " + periodName);
  }
}
