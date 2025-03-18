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
package org.hisp.dhis.webapi.controller;

import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.conflict;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.error;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.ok;
import static org.hisp.dhis.security.Authorities.F_SEND_EMAIL;
import static org.hisp.dhis.system.util.ValidationUtils.emailIsValid;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.dxf2.webmessage.WebMessage;
import org.hisp.dhis.email.Email;
import org.hisp.dhis.email.EmailResponse;
import org.hisp.dhis.email.EmailService;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.outboundmessage.OutboundMessageResponse;
import org.hisp.dhis.security.RequiresAuthority;
import org.hisp.dhis.setting.SystemSettings;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @author Halvdan Hoem Grelland <halvdanhg@gmail.com>
 */
@OpenApi.Document(
    entity = Email.class,
    classifiers = {"team:platform", "purpose:support"})
@Controller
@RequestMapping("/api/email")
@ApiVersion({DhisApiVersion.DEFAULT, DhisApiVersion.ALL})
@RequiredArgsConstructor
public class EmailController {

  private static final String SMTP_ERROR = "SMTP server not configured";

  @Autowired private final EmailService emailService;
  @Autowired private final UserService userService;

  @PostMapping("/test")
  @ResponseBody
  public WebMessage sendTestEmail() throws ConflictException {
    checkEmailSettings();

    User currentUser = userService.getUserByUsername(CurrentUserUtil.getCurrentUsername());
    if (!currentUser.hasEmail()) {
      return conflict("Could not send test email, no email configured for current user");
    }

    OutboundMessageResponse emailResponse = emailService.sendTestEmail();

    return emailResponseHandler(emailResponse);
  }

  @PostMapping(value = "/notification", consumes = APPLICATION_JSON_VALUE)
  @ResponseBody
  public WebMessage sendSystemNotificationEmail(@RequestBody Email email, SystemSettings settings)
      throws ConflictException {
    checkEmailSettings();

    if (!emailIsValid(settings.getSystemNotificationsEmail()))
      throw new ConflictException(
          "Could not send email, system notification email address not set or not valid");

    OutboundMessageResponse emailResponse = emailService.sendSystemEmail(email);

    return emailResponseHandler(emailResponse);
  }

  @RequiresAuthority(anyOf = F_SEND_EMAIL)
  @PostMapping(value = "/notification", produces = APPLICATION_JSON_VALUE)
  @ResponseBody
  public WebMessage sendEmailNotification(
      @RequestParam Set<String> recipients,
      @RequestParam String message,
      @RequestParam(defaultValue = "DHIS 2") String subject)
      throws ConflictException {
    checkEmailSettings();

    OutboundMessageResponse emailResponse = emailService.sendEmail(subject, message, recipients);

    return emailResponseHandler(emailResponse);
  }

  // ---------------------------------------------------------------------
  // Supportive methods
  // ---------------------------------------------------------------------

  private WebMessage emailResponseHandler(OutboundMessageResponse emailResponse) {
    if (emailResponse.isOk()) {
      String msg =
          !StringUtils.isEmpty(emailResponse.getDescription())
              ? emailResponse.getDescription()
              : EmailResponse.SENT.getResponseMessage();
      return ok(msg);
    }
    String msg =
        !StringUtils.isEmpty(emailResponse.getDescription())
            ? emailResponse.getDescription()
            : EmailResponse.FAILED.getResponseMessage();
    return error(msg);
  }

  private void checkEmailSettings() throws ConflictException {
    if (!emailService.emailConfigured()) {
      throw new ConflictException(SMTP_ERROR);
    }
  }
}
