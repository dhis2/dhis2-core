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
package org.hisp.dhis.system.util;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.commons.util.TextUtils;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.sms.command.SMSCommand;
import org.hisp.dhis.sms.incoming.IncomingSms;
import org.hisp.dhis.sms.parse.SMSParserException;
import org.hisp.dhis.user.User;

/**
 * @author Zubair <rajazubair.asghar@gmail.com>
 */
@Slf4j
public class SmsUtils {

  private static final String COMMAND_PATTERN = "([A-Za-z])\\w+";

  public static String getCommandString(IncomingSms sms) {
    return getCommandString(sms.getText());
  }

  public static String getCommandString(String text) {
    String commandString = null;

    Pattern pattern = Pattern.compile(COMMAND_PATTERN);

    Matcher matcher = pattern.matcher(text);

    if (matcher.find()) {
      commandString = matcher.group();
      commandString = commandString.trim();
    }

    return commandString;
  }

  public static boolean isBase64(IncomingSms sms) {
    return isBase64(sms.getText());
  }

  public static boolean isBase64(String text) {
    try {
      Base64.getDecoder().decode(text);
      return true;
    } catch (IllegalArgumentException e) {
      return false;
    }
  }

  public static byte[] getBytes(IncomingSms sms) {
    try {
      return Base64.getDecoder().decode(sms.getText());
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  public static String encode(String value) {
    if (StringUtils.isBlank(value)) {
      return value;
    }

    return URLEncoder.encode(value, StandardCharsets.UTF_8);
  }

  public static Date lookForDate(String message) {
    if (!message.contains(" ")) {
      return null;
    }

    Date date = null;
    String[] messageSplit = message.trim().split(" ");
    // The first element in the split is the sms command. If there are only
    // two elements
    // in the split assume the 2nd is data values, not date.
    if (messageSplit.length <= 2) {
      return null;
    }
    String dateString = messageSplit[1];
    SimpleDateFormat format = new SimpleDateFormat("ddMM");

    try {
      Calendar cal = Calendar.getInstance();
      date = format.parse(dateString);
      cal.setTime(date);
      int year = Calendar.getInstance().get(Calendar.YEAR);
      int month = Calendar.getInstance().get(Calendar.MONTH);

      if (cal.get(Calendar.MONTH) <= month) {
        cal.set(Calendar.YEAR, year);
      } else {
        cal.set(Calendar.YEAR, year - 1);
      }

      date = cal.getTime();
    } catch (Exception e) {
      // no date found
    }

    return date;
  }

  public static Set<String> getRecipientsPhoneNumber(Collection<User> users) {
    return users.parallelStream()
        .filter(u -> u.getPhoneNumber() != null && !u.getPhoneNumber().isEmpty())
        .map(User::getPhoneNumber)
        .collect(Collectors.toSet());
  }

  public static OrganisationUnit selectOrganisationUnit(
      Collection<OrganisationUnit> orgUnits,
      Map<String, String> parsedMessage,
      SMSCommand smsCommand) {
    OrganisationUnit orgUnit = null;

    for (OrganisationUnit o : orgUnits) {
      if (orgUnits.size() == 1) {
        orgUnit = o;
      }
      if (parsedMessage.containsKey("ORG") && o.getCode().equals(parsedMessage.get("ORG"))) {
        orgUnit = o;
        break;
      }
    }

    if (orgUnit == null && orgUnits.size() > 1) {
      String messageListingOrgUnits = smsCommand.getMoreThanOneOrgUnitMessage();

      for (Iterator<OrganisationUnit> i = orgUnits.iterator(); i.hasNext(); ) {
        OrganisationUnit o = i.next();
        messageListingOrgUnits += TextUtils.SPACE + o.getName() + ":" + o.getCode();

        if (i.hasNext()) {
          messageListingOrgUnits += ",";
        }
      }

      throw new SMSParserException(messageListingOrgUnits);
    }

    return orgUnit;
  }

  public static String removePhoneNumberPrefix(String number) {
    if (number == null) {
      return null;
    }

    if (number.startsWith("00")) {
      number = number.substring(2);
    } else if (number.startsWith("+")) {
      number = number.substring(1);
    }

    return number;
  }
}
