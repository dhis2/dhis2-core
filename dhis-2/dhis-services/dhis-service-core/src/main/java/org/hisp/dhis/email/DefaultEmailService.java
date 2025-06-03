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
package org.hisp.dhis.email;

import com.google.common.collect.Sets;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.message.MessageSender;
import org.hisp.dhis.outboundmessage.OutboundMessageResponse;
import org.hisp.dhis.setting.SystemSettings;
import org.hisp.dhis.setting.SystemSettingsProvider;
import org.hisp.dhis.system.util.ValidationUtils;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Halvdan Hoem Grelland <halvdanhg@gmail.com>
 */
@Transactional // TODO do we need transactions at all here?
@RequiredArgsConstructor
@Service("org.hisp.dhis.email.EmailService")
public class DefaultEmailService implements EmailService {
  private static final String TEST_EMAIL_SUBJECT = "Test email from DHIS 2";

  private static final String TEST_EMAIL_TEXT = "This is an automatically generated email from ";

  // -------------------------------------------------------------------------
  // Dependencies
  // -------------------------------------------------------------------------

  private final MessageSender emailMessageSender;

  private final UserService userService;

  private final SystemSettingsProvider settingsProvider;

  // -------------------------------------------------------------------------
  // EmailService implementation
  // -------------------------------------------------------------------------

  @Override
  public boolean emailConfigured() {
    return settingsProvider.getCurrentSettings().isEmailConfigured();
  }

  @Override
  public OutboundMessageResponse sendEmail(String subject, String message, Set<String> recipients) {
    return emailMessageSender.sendMessage(subject, message, recipients);
  }

  @Override
  public OutboundMessageResponse sendSystemEmail(Email email) {
    OutboundMessageResponse response = new OutboundMessageResponse();

    SystemSettings settings = settingsProvider.getCurrentSettings();
    String recipient = settings.getSystemNotificationsEmail();
    String appTitle = settings.getApplicationTitle();

    if (recipient == null || !ValidationUtils.emailIsValid(recipient)) {
      response.setOk(false);
      response.setDescription("No recipient found");

      return response;
    }

    User user = new User();
    user.setEmail(recipient);

    User sender = new User();
    sender.setFirstName(StringUtils.trimToEmpty(appTitle));
    sender.setSurname(recipient);

    return emailMessageSender.sendMessage(
        email.getSubject(), email.getText(), null, sender, Sets.newHashSet(user), true);
  }

  @Override
  public OutboundMessageResponse sendTestEmail() {
    String instanceName = settingsProvider.getCurrentSettings().getApplicationTitle();

    User currentUser = userService.getUserByUsername(CurrentUserUtil.getCurrentUsername());

    Email email =
        new Email(
            TEST_EMAIL_SUBJECT, TEST_EMAIL_TEXT + instanceName, null, Sets.newHashSet(currentUser));

    return emailMessageSender.sendMessage(
        email.getSubject(), email.getText(), null, email.getSender(), email.getRecipients(), true);
  }
}
