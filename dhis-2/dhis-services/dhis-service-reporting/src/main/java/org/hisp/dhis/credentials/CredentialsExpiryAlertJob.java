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
package org.hisp.dhis.credentials;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.message.MessageSender;
import org.hisp.dhis.scheduling.Job;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.scheduling.JobProgress;
import org.hisp.dhis.scheduling.JobType;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.util.DateUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/** Created by zubair on 29.03.17. */
@Slf4j
@Component("credentialsExpiryAlertJob")
public class CredentialsExpiryAlertJob implements Job {
  private static final String SUBJECT = "Password Expiry Alert";

  private static final String TEXT =
      "Dear %s, Please change your password. It will expire in %d days.";

  private static final String KEY_TASK = "credentialsExpiryAlertTask";

  // -------------------------------------------------------------------------
  // Dependencies
  // -------------------------------------------------------------------------

  private UserService userService;

  private MessageSender emailMessageSender;

  private SystemSettingManager systemSettingManager;

  public CredentialsExpiryAlertJob(
      UserService userService,
      @Qualifier("emailMessageSender") MessageSender emailMessageSender,
      SystemSettingManager systemSettingManager) {
    checkNotNull(userService);
    checkNotNull(emailMessageSender);
    checkNotNull(systemSettingManager);

    this.userService = userService;
    this.emailMessageSender = emailMessageSender;
    this.systemSettingManager = systemSettingManager;
  }

  // -------------------------------------------------------------------------
  // Implementation
  // -------------------------------------------------------------------------

  @Override
  public JobType getJobType() {
    return JobType.CREDENTIALS_EXPIRY_ALERT;
  }

  @Override
  public void execute(JobConfiguration jobConfiguration, JobProgress progress) {
    boolean isExpiryAlertEnabled =
        systemSettingManager.getBoolSetting(SettingKey.CREDENTIALS_EXPIRY_ALERT);

    if (!isExpiryAlertEnabled) {
      log.info(String.format("%s aborted. Expiry alerts are disabled", KEY_TASK));

      return;
    }

    log.info(String.format("%s has started", KEY_TASK));

    List<User> users = userService.getExpiringUsers();

    Map<String, String> content = new HashMap<>();

    for (User user : users) {
      if (user.getEmail() != null) {
        content.put(user.getEmail(), createText(user));
      }
    }

    log.info(String.format("Users added for alert: %d", content.size()));

    sendExpiryAlert(content);
  }

  @Override
  public ErrorReport validate() {
    if (!emailMessageSender.isConfigured()) {
      return new ErrorReport(
          CredentialsExpiryAlertJob.class,
          ErrorCode.E7010,
          "EMAIL gateway configuration does not exist");
    }

    return Job.super.validate();
  }

  private void sendExpiryAlert(Map<String, String> content) {
    if (emailMessageSender.isConfigured()) {
      for (String email : content.keySet()) {
        emailMessageSender.sendMessage(SUBJECT, content.get(email), email);
      }
    } else {
      log.error("Email service is not configured");
    }
  }

  private String createText(User user) {
    return String.format(TEXT, user.getUsername(), getRemainingDays(user));
  }

  private int getRemainingDays(User user) {
    int daysBeforeChangeRequired =
        systemSettingManager.getIntSetting(SettingKey.CREDENTIALS_EXPIRES) * 30;

    Date passwordLastUpdated = user.getPasswordLastUpdated();

    return (daysBeforeChangeRequired - DateUtils.daysBetween(passwordLastUpdated, new Date()));
  }
}
