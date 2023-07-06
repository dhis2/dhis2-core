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
package org.hisp.dhis.user.job;

import static java.lang.String.format;
import static org.hisp.dhis.scheduling.JobProgress.FailurePolicy.SKIP_ITEM_OUTLIER;

import java.util.List;
import lombok.AllArgsConstructor;
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
import org.hisp.dhis.user.UserAccountExpiryInfo;
import org.hisp.dhis.user.UserService;
import org.springframework.stereotype.Component;

/**
 * Sends an email alert to all users that are soon to expire due to an account expire date being set
 * in {@link User#getAccountExpiry()} that is within the next {@link
 * SettingKey#ACCOUNT_EXPIRES_IN_DAYS} interval.
 *
 * <p>The job only works when enabled via {@link SettingKey#ACCOUNT_EXPIRY_ALERT}.
 *
 * @author Jan Bernitt
 */
@Slf4j
@Component
@AllArgsConstructor
public class AccountExpiryAlertJob implements Job {
  @Override
  public JobType getJobType() {
    return JobType.ACCOUNT_EXPIRY_ALERT;
  }

  private final UserService userService;

  private final MessageSender emailMessageSender;

  private final SystemSettingManager systemSettingManager;

  @Override
  public ErrorReport validate() {
    if (!emailMessageSender.isConfigured()) {
      return new ErrorReport(
          AccountExpiryAlertJob.class,
          ErrorCode.E7010,
          "EMAIL gateway configuration does not exist");
    }
    return null;
  }

  @Override
  public void execute(JobConfiguration jobConfiguration, JobProgress progress) {
    if (!systemSettingManager.getBoolSetting(SettingKey.ACCOUNT_EXPIRY_ALERT)) {
      log.info(format("%s aborted. Expiry alerts are disabled", getJobType().name()));
      return;
    }

    progress.startingProcess("Notify expiring account users");
    int inDays = systemSettingManager.getIntSetting(SettingKey.ACCOUNT_EXPIRES_IN_DAYS);
    List<UserAccountExpiryInfo> soonExpiring = userService.getExpiringUserAccounts(inDays);

    if (soonExpiring.isEmpty()) {
      progress.completedProcess("No expiring users");
      return;
    }

    progress.startingStage(
        "Notify user accounts that expire within next " + inDays + " days",
        soonExpiring.size(),
        SKIP_ITEM_OUTLIER);
    progress.runStage(
        soonExpiring.stream(),
        UserAccountExpiryInfo::getUsername,
        user ->
            emailMessageSender.sendMessage(
                "Account Expiry Alert", computeEmailMessage(user), user.getEmail()),
        AccountExpiryAlertJob::computeStageSummary);
    progress.completedProcess(null);
  }

  private static String computeEmailMessage(UserAccountExpiryInfo user) {
    return format(
        "Dear %s, your account is about to expire on %2$tY-%2$tm-%2$te. "
            + "If your use of the account needs to continue, get in touch with your system administrator.",
        user.getUsername(), user.getAccountExpiry());
  }

  private static String computeStageSummary(int successful, int failed) {
    log.info(
        format("%d user accounts have been notified about their expiring accounts", successful));
    if (failed == 0) {
      return null;
    }
    String summary =
        format(
            "%d user accounts were not notified about their expiring accounts due to errors while sending the email",
            failed);
    log.warn(summary);
    return summary;
  }
}
