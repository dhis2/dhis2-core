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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.message.MessageSender;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.sms.command.SMSCommand;
import org.hisp.dhis.sms.command.code.SMSCode;
import org.hisp.dhis.sms.incoming.IncomingSms;
import org.hisp.dhis.sms.incoming.IncomingSmsService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.user.UserService;
import org.springframework.transaction.annotation.Transactional;

/** Created by zubair@dhis2.org on 11.08.17. */
@Slf4j
@Transactional
public abstract class CommandSMSListener extends BaseSMSListener {
  private static final String DEFAULT_PATTERN = "([^\\s|=]+)\\s*\\=\\s*([^|=]+)\\s*(\\=|$)*+\\s*";

  protected static final int INFO = 1;

  protected static final int WARNING = 2;

  protected static final int ERROR = 3;

  protected final UserService userService;

  public CommandSMSListener(
      UserService userService, IncomingSmsService incomingSmsService, MessageSender smsSender) {
    super(incomingSmsService, smsSender);
    this.userService = userService;
  }

  @Override
  public boolean accept(@Nonnull IncomingSms sms) {
    return getSMSCommand(sms) != null;
  }

  @Override
  public void receive(@Nonnull IncomingSms sms, @Nonnull UserDetails smsCreatedBy)
      throws ConflictException {
    // we cannot annotate getSMSCommand itself with Nonnull as it can return null but
    // receive is only called when accept returned true, which is if there is a non-null command
    SMSCommand smsCommand = getSMSCommand(sms);

    Map<String, String> codeValues = parseCodeValuePairs(sms, smsCommand);

    if (!hasCorrectFormat(sms, smsCommand)
        || !validateInputValues(sms, smsCreatedBy, smsCommand, codeValues)) {
      return;
    }

    try {
      postProcess(sms, smsCreatedBy, smsCommand, codeValues);
    } catch (ConflictException ex) {
      log.error("Failed to handle SMS", ex);
    }
  }

  protected abstract void postProcess(
      @Nonnull IncomingSms sms,
      @Nonnull UserDetails smsCreatedBy,
      @Nonnull SMSCommand smsCommand,
      @Nonnull Map<String, String> codeValues)
      throws ConflictException;

  protected abstract SMSCommand getSMSCommand(@Nonnull IncomingSms sms);

  protected boolean hasCorrectFormat(@Nonnull IncomingSms sms, @Nonnull SMSCommand smsCommand) {
    String regexp = DEFAULT_PATTERN;

    if (smsCommand.getSeparator() != null && !smsCommand.getSeparator().trim().isEmpty()) {
      regexp = regexp.replaceAll("=", smsCommand.getSeparator());
    }

    Pattern pattern = Pattern.compile(regexp);

    Matcher matcher = pattern.matcher(sms.getText());

    if (!matcher.find()) {
      sendFeedback(
          StringUtils.defaultIfEmpty(
              smsCommand.getWrongFormatMessage(), SMSCommand.WRONG_FORMAT_MESSAGE),
          sms.getOriginator(),
          ERROR);
      return false;
    }

    return true;
  }

  protected Set<OrganisationUnit> getOrganisationUnits(IncomingSms sms) {
    User user = userService.getUser(sms.getCreatedBy().getUid());

    if (user == null) {
      return new HashSet<>();
    }

    return user.getOrganisationUnits();
  }

  private boolean validateInputValues(
      @Nonnull IncomingSms sms,
      @Nonnull UserDetails smsCreatedBy,
      @Nonnull SMSCommand smsCommand,
      @Nonnull Map<String, String> commandValuePairs) {
    if (!hasMandatoryCodes(smsCommand.getCodes(), commandValuePairs.keySet())) {
      sendFeedback(
          StringUtils.defaultIfEmpty(smsCommand.getDefaultMessage(), SMSCommand.PARAMETER_MISSING),
          sms.getOriginator(),
          ERROR);

      return false;
    }

    if (!hasOrganisationUnit(smsCreatedBy)) {
      sendFeedback(smsCommand.getNoUserMessage(), sms.getOriginator(), ERROR);

      return false;
    }

    if (hasMultipleOrganisationUnits(smsCreatedBy)) {
      sendFeedback(smsCommand.getMoreThanOneOrgUnitMessage(), sms.getOriginator(), ERROR);

      return false;
    }

    return true;
  }

  /**
   * Parses the code value pairs of an SMS command. For example SMS {@code visit bcgd=1,opvd=2} with
   * command name {@code visit} will lead to a map of SMS code names to values {@code
   * {bcgd=1,opvd=2}}.
   */
  private @Nonnull Map<String, String> parseCodeValuePairs(
      @Nonnull IncomingSms sms, @Nonnull SMSCommand smsCommand) {
    HashMap<String, String> result = new HashMap<>();

    Pattern pattern = Pattern.compile(DEFAULT_PATTERN);

    if (!StringUtils.isBlank(smsCommand.getSeparator())) {
      String regex = DEFAULT_PATTERN.replaceAll("=", smsCommand.getSeparator());

      pattern = Pattern.compile(regex);
    }

    Matcher matcher = pattern.matcher(sms.getText());
    while (matcher.find()) {
      String key = matcher.group(1).trim();
      String value = matcher.group(2).trim();

      if (!StringUtils.isEmpty(key) && !StringUtils.isEmpty(value)) {
        result.put(key, value);
      }
    }

    return result;
  }

  private boolean hasMandatoryCodes(Set<SMSCode> configuredCodes, Set<String> actualCodes) {
    for (SMSCode configuredCode : configuredCodes) {
      if (configuredCode.isCompulsory() && !actualCodes.contains(configuredCode.getCode())) {
        return false;
      }
    }

    return true;
  }

  private static boolean hasOrganisationUnit(UserDetails smsCreatedBy) {
    return !smsCreatedBy.getUserOrgUnitIds().isEmpty();
  }

  private static boolean hasMultipleOrganisationUnits(UserDetails smsCreatedBy) {
    return smsCreatedBy.getUserOrgUnitIds().size() > 1;
  }
}
