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
package org.hisp.dhis.webapi.controller;

import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.conflict;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.error;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.ok;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.dxf2.webmessage.WebMessage;
import org.hisp.dhis.dxf2.webmessage.WebMessageException;
import org.hisp.dhis.email.Email;
import org.hisp.dhis.email.EmailResponse;
import org.hisp.dhis.email.EmailService;
import org.hisp.dhis.outboundmessage.OutboundMessageResponse;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @author Halvdan Hoem Grelland <halvdanhg@gmail.com>
 */
@Controller
@RequestMapping(value = EmailController.RESOURCE_PATH)
@ApiVersion({DhisApiVersion.DEFAULT, DhisApiVersion.ALL})
public class EmailController {
  public static final String RESOURCE_PATH = "/email";

  private static final String SMTP_ERROR = "SMTP server not configured";

  // --------------------------------------------------------------------------
  // Dependencies
  // --------------------------------------------------------------------------

  @Autowired private EmailService emailService;

  @Autowired private CurrentUserService currentUserService;

  @Autowired private SystemSettingManager systemSettingManager;

  @PostMapping("/test")
  @ResponseBody
  public WebMessage sendTestEmail() throws WebMessageException {
    checkEmailSettings();

    if (!currentUserService.getCurrentUser().hasEmail()) {
      return conflict("Could not send test email, no email configured for current user");
    }

    OutboundMessageResponse emailResponse = emailService.sendTestEmail();

    return emailResponseHandler(emailResponse);
  }

  @PostMapping(value = "/notification", consumes = APPLICATION_JSON_VALUE)
  @ResponseBody
  public WebMessage sendSystemNotificationEmail(@RequestBody Email email)
      throws WebMessageException {
    checkEmailSettings();

    boolean systemNotificationEmailValid = systemSettingManager.systemNotificationEmailValid();

    if (!systemNotificationEmailValid) {
      return conflict(
          "Could not send email, system notification email address not set or not valid");
    }

    OutboundMessageResponse emailResponse = emailService.sendSystemEmail(email);

    return emailResponseHandler(emailResponse);
  }

  @PreAuthorize("hasRole('ALL') or hasRole('F_SEND_EMAIL')")
  @PostMapping(value = "/notification", produces = APPLICATION_JSON_VALUE)
  @ResponseBody
  public WebMessage sendEmailNotification(
      @RequestParam Set<String> recipients,
      @RequestParam String message,
      @RequestParam(defaultValue = "DHIS 2") String subject)
      throws WebMessageException {
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

  private void checkEmailSettings() throws WebMessageException {
    if (!emailService.emailConfigured()) {
      throw new WebMessageException(conflict(SMTP_ERROR));
    }
  }
}
