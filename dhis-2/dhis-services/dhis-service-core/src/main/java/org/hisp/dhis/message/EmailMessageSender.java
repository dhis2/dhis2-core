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
package org.hisp.dhis.message;

import static java.util.Collections.singleton;

import com.google.common.base.Strings;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.mail.DefaultAuthenticator;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.HtmlEmail;
import org.hisp.dhis.common.DeliveryChannel;
import org.hisp.dhis.commons.util.DebugUtils;
import org.hisp.dhis.commons.util.TextUtils;
import org.hisp.dhis.email.EmailConfiguration;
import org.hisp.dhis.email.EmailResponse;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.outboundmessage.OutboundMessageBatch;
import org.hisp.dhis.outboundmessage.OutboundMessageBatchStatus;
import org.hisp.dhis.outboundmessage.OutboundMessageResponse;
import org.hisp.dhis.outboundmessage.OutboundMessageResponseSummary;
import org.hisp.dhis.setting.SystemSettings;
import org.hisp.dhis.setting.SystemSettingsProvider;
import org.hisp.dhis.system.util.ValidationUtils;
import org.hisp.dhis.system.velocity.VelocityManager;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserSettingsService;
import org.hisp.dhis.util.ObjectUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * @author Lars Helge Overland
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class EmailMessageSender implements MessageSender {
  private static final String DEFAULT_APPLICATION_TITLE = "DHIS 2";

  private static final String LB = System.getProperty("line.separator");

  private static final String MESSAGE_EMAIL_TEMPLATE = "message_email";

  private static final String HOST = "Host: ";

  // -------------------------------------------------------------------------
  // Dependencies
  // -------------------------------------------------------------------------

  private final SystemSettingsProvider settingsProvider;

  private final UserSettingsService userSettingsService;

  private final DhisConfigurationProvider configurationProvider;

  // -------------------------------------------------------------------------
  // MessageSender implementation
  // -------------------------------------------------------------------------

  @Override
  public OutboundMessageResponse sendMessage(
      String subject, String text, String footer, User sender, Set<User> users, boolean forceSend) {
    EmailConfiguration emailConfig = getEmailConfiguration();
    OutboundMessageResponse status = new OutboundMessageResponse();

    String errorMessage = "No recipient found";

    if (emailConfig.getHostName() == null) {
      status.setOk(false);
      status.setDescription(EmailResponse.HOST_CONFIG_NOT_FOUND.getResponseMessage());
      status.setResponseObject(EmailResponse.HOST_CONFIG_NOT_FOUND);
      return status;
    }

    String serverBaseUrl = configurationProvider.getServerBaseUrl();
    String plainContent = renderPlainContent(text, sender);
    String htmlContent =
        renderHtmlContent(text, footer, serverBaseUrl != null ? HOST + serverBaseUrl : "", sender);

    try {
      HtmlEmail email =
          getHtmlEmail(
              emailConfig.getHostName(),
              emailConfig.getPort(),
              emailConfig.getUsername(),
              emailConfig.getPassword(),
              emailConfig.isTls(),
              emailConfig.getFrom());

      email.setSubject(getPrefixedSubject(subject));
      email.setTextMsg(plainContent);
      email.setHtmlMsg(htmlContent);

      boolean hasRecipients = false;

      for (User user : users) {
        boolean doSend =
            forceSend
                || userSettingsService
                    .getUserSettings(user.getUsername(), true)
                    .getUserMessageEmailNotification();

        if (doSend && ValidationUtils.emailIsValid(user.getEmail())) {
          if (isEmailValid(user.getEmail())) {
            email.addBcc(user.getEmail());
            hasRecipients = true;

            log.info(
                "Sending email to user: "
                    + user.getUsername()
                    + " with email address: "
                    + user.getEmail());
          } else {
            log.warn(user.getEmail() + " is not a valid email for user: " + user.getUsername());
            errorMessage = "No valid email address found";
          }
        }
      }

      if (hasRecipients) {
        email.send();

        log.info(
            "Email sent using host: "
                + emailConfig.getHostName()
                + ":"
                + emailConfig.getPort()
                + " with TLS: "
                + emailConfig.isTls());
        status = new OutboundMessageResponse("Email sent", EmailResponse.SENT, true);
      } else {
        status = new OutboundMessageResponse(errorMessage, EmailResponse.ABORTED, false);
      }
    } catch (Exception ex) {
      log.error(
          "Error while sending email: " + ex.getMessage() + ", " + DebugUtils.getStackTrace(ex));
      status =
          new OutboundMessageResponse(
              "Email not sent: " + ex.getMessage(), EmailResponse.FAILED, false);
    }

    return status;
  }

  @Async
  @Override
  public Future<OutboundMessageResponse> sendMessageAsync(
      String subject, String text, String footer, User sender, Set<User> users, boolean forceSend) {
    OutboundMessageResponse response = sendMessage(subject, text, footer, sender, users, forceSend);
    return CompletableFuture.completedFuture(response);
  }

  @Override
  public OutboundMessageResponse sendMessage(String subject, String text, Set<String> recipients) {
    EmailConfiguration emailConfig = getEmailConfiguration();
    OutboundMessageResponse status = new OutboundMessageResponse();

    String errorMessage = "No recipient found";
    String serverBaseUrl = configurationProvider.getServerBaseUrl();

    if (emailConfig.getHostName() == null) {
      status.setOk(false);
      status.setDescription(EmailResponse.HOST_CONFIG_NOT_FOUND.getResponseMessage());
      status.setResponseObject(EmailResponse.HOST_CONFIG_NOT_FOUND);
      return status;
    }

    try {
      HtmlEmail email =
          getHtmlEmail(
              emailConfig.getHostName(),
              emailConfig.getPort(),
              emailConfig.getUsername(),
              emailConfig.getPassword(),
              emailConfig.isTls(),
              emailConfig.getFrom());
      email.setSubject(getPrefixedSubject(subject));
      email.setTextMsg(text);
      email.setHtmlMsg(renderHtmlContent(text, null, serverBaseUrl, null));

      boolean hasRecipients = false;

      for (String recipient : recipients) {
        if (isEmailValid(recipient)) {
          email.addBcc(recipient);
          hasRecipients = true;

          log.info("Sending email to : " + recipient);
        } else {
          log.warn(recipient + " is not a valid email");
          errorMessage = "No valid email address found";
        }
      }

      if (hasRecipients) {
        email.send();

        log.info(
            "Email sent using host: "
                + emailConfig.getHostName()
                + ":"
                + emailConfig.getPort()
                + " with TLS: "
                + emailConfig.isTls());
        return new OutboundMessageResponse("Email sent", EmailResponse.SENT, true);
      } else {
        status = new OutboundMessageResponse(errorMessage, EmailResponse.ABORTED, false);
      }
    } catch (Exception ex) {
      log.error(
          "Error while sending email: " + ex.getMessage() + ", " + DebugUtils.getStackTrace(ex));
      status =
          new OutboundMessageResponse(
              "Email not sent: " + ex.getMessage(), EmailResponse.FAILED, false);
    }

    return status;
  }

  @Override
  public OutboundMessageResponse sendMessage(String subject, String text, String recipient) {
    return sendMessage(subject, text, singleton(recipient));
  }

  @Override
  public OutboundMessageResponseSummary sendMessageBatch(OutboundMessageBatch batch) {
    List<OutboundMessageResponse> statuses =
        batch.getMessages().stream()
            .map(m -> sendMessage(m.getSubject(), m.getText(), m.getRecipients()))
            .collect(Collectors.toList());

    return generateSummary(statuses);
  }

  @Override
  public boolean isConfigured() {
    return getEmailConfiguration().isOk();
  }

  // -------------------------------------------------------------------------
  // Supportive methods
  // -------------------------------------------------------------------------

  private HtmlEmail getHtmlEmail(
      String hostName, int port, String username, String password, boolean tls, String sender)
      throws EmailException {
    HtmlEmail email = new HtmlEmail();
    email.setCharset(StandardCharsets.UTF_8.toString());
    email.setHostName(hostName);
    email.setFrom(sender, getEmailName());
    email.setSmtpPort(port);
    email.setStartTLSEnabled(tls);

    if (username != null && password != null) {
      email.setAuthenticator(new DefaultAuthenticator(username, password));
    }

    return email;
  }

  private String renderPlainContent(String text, User sender) {
    String content =
        sender == null
            ? text
            : (text
                + LB
                + LB
                + sender.getName()
                + LB
                + getNullSafe(sender.getOrganisationUnitsName())
                + getNullSafe(sender.getEmail())
                + getNullSafe(sender.getPhoneNumber()));
    return new String(content.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
  }

  private String getNullSafe(String value) {
    return value == null ? StringUtils.EMPTY : value + LB;
  }

  private String renderHtmlContent(String text, String footer, String serverBaseUrl, User sender) {
    Map<String, Object> content = new HashMap<>();

    if (!Strings.isNullOrEmpty(text)) {
      content.put("text", text.replaceAll("\\r?\\n", "<br>"));
    }

    if (!Strings.isNullOrEmpty(footer)) {
      content.put("footer", footer);
    }

    if (!Strings.isNullOrEmpty(serverBaseUrl)) {
      content.put("serverBaseUrl", serverBaseUrl);
    }

    if (sender != null) {
      content.put("senderName", sender.getName());

      if (sender.getOrganisationUnitsName() != null) {
        content.put("organisationUnitsName", sender.getOrganisationUnitsName());
      }

      if (sender.getEmail() != null) {
        content.put("email", sender.getEmail());
      }

      if (sender.getPhoneNumber() != null) {
        content.put("phoneNumber", sender.getPhoneNumber());
      }
    }

    return new VelocityManager().render(content, MESSAGE_EMAIL_TEMPLATE);
  }

  private String getPrefixedSubject(String subject) {
    String title = settingsProvider.getCurrentSettings().getApplicationTitle();
    return "[" + title + "] " + subject;
  }

  private String getEmailName() {
    String appTitle = settingsProvider.getCurrentSettings().getApplicationTitle();
    appTitle =
        ObjectUtils.firstNonNull(
            StringUtils.trimToNull(emailNameEncode(appTitle)), DEFAULT_APPLICATION_TITLE);
    return appTitle + " message [No reply]";
  }

  private String emailNameEncode(String name) {
    return name != null ? TextUtils.removeNewlines(name) : null;
  }

  private boolean isEmailValid(String email) {
    return ValidationUtils.emailIsValid(email);
  }

  private EmailConfiguration getEmailConfiguration() {
    SystemSettings settings = settingsProvider.getCurrentSettings();
    String hostName = settings.getEmailHostName();
    String username = settings.getEmailUsername();
    String password = settings.getEmailPassword();
    String from = settings.getEmailSender();
    int port = settings.getEmailPort();
    boolean tls = settings.getEmailTls();

    return new EmailConfiguration(hostName, username, password, from, port, tls);
  }

  private OutboundMessageResponseSummary generateSummary(List<OutboundMessageResponse> statuses) {
    OutboundMessageResponseSummary summary = new OutboundMessageResponseSummary();

    int total, sent = 0;

    boolean ok = true;

    String errorMessage = StringUtils.EMPTY;

    total = statuses.size();

    for (OutboundMessageResponse status : statuses) {
      if (EmailResponse.SENT.equals(status.getResponseObject())) {
        sent++;
      } else {
        ok = false;

        errorMessage = status.getDescription();
      }
    }

    summary.setTotal(total);
    summary.setChannel(DeliveryChannel.EMAIL);
    summary.setSent(sent);
    summary.setFailed(total - sent);

    if (!ok) {
      summary.setBatchStatus(OutboundMessageBatchStatus.FAILED);
      summary.setErrorMessage(errorMessage);

      log.error(errorMessage);
    } else {
      summary.setBatchStatus(OutboundMessageBatchStatus.COMPLETED);
      summary.setResponseMessage("SENT");

      log.info("EMAIL batch processed successfully");
    }

    return summary;
  }
}
