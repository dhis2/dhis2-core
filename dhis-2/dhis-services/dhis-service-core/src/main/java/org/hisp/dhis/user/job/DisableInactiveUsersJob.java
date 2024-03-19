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
import static java.time.ZoneId.systemDefault;
import static org.hisp.dhis.scheduling.JobProgress.FailurePolicy.SKIP_STAGE;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.AllArgsConstructor;
import org.hisp.dhis.email.EmailResponse;
import org.hisp.dhis.email.EmailService;
import org.hisp.dhis.i18n.I18n;
import org.hisp.dhis.i18n.I18nManager;
import org.hisp.dhis.i18n.locale.LocaleManager;
import org.hisp.dhis.outboundmessage.OutboundMessageResponse;
import org.hisp.dhis.scheduling.Job;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.scheduling.JobProgress;
import org.hisp.dhis.scheduling.JobType;
import org.hisp.dhis.scheduling.parameters.DisableInactiveUsersJobParameters;
import org.hisp.dhis.user.UserService;
import org.springframework.stereotype.Component;

/**
 * Disables users that have been inactive for too long as well as sending emails to those users that
 * soon would become inactive on certain days before this happens.
 *
 * @author Jan Bernitt
 */
@AllArgsConstructor
@Component("disableInactiveUsersJob")
public class DisableInactiveUsersJob implements Job {
  private final UserService userService;

  private final EmailService emailService;

  private final LocaleManager localeManager;

  private final I18nManager i18nManager;

  @Override
  public JobType getJobType() {
    return JobType.DISABLE_INACTIVE_USERS;
  }

  @Override
  public void execute(JobConfiguration jobConfiguration, JobProgress progress) {
    progress.startingProcess("Disable inactive users");
    DisableInactiveUsersJobParameters parameters =
        (DisableInactiveUsersJobParameters) jobConfiguration.getJobParameters();
    LocalDate today = LocalDate.now();
    LocalDate since = today.minusMonths(parameters.getInactiveMonths());
    Date nMonthsAgo = Date.from(since.atStartOfDay(systemDefault()).toInstant());

    progress.startingStage("Disabling inactive users", SKIP_STAGE);
    progress.runStage(
        0,
        count ->
            format(
                "Disabled %d users with %d months of inactivity",
                count, parameters.getInactiveMonths()),
        () -> userService.disableUsersInactiveSince(nMonthsAgo));

    Integer reminderDaysBefore = parameters.getReminderDaysBefore();
    if (reminderDaysBefore == null) {
      progress.completedProcess("Skipping reminder emails. Done.");
      return; // done
    }
    int daysUntilDisable = reminderDaysBefore;
    do {
      sendReminderEmail(since, daysUntilDisable, progress);
      daysUntilDisable = daysUntilDisable / 2;
    } while (daysUntilDisable > 0);
    progress.completedProcess(null);
  }

  private void sendReminderEmail(LocalDate since, int daysUntilDisable, JobProgress progress) {
    LocalDate referenceDay = since.plusDays(daysUntilDisable);
    ZonedDateTime nDaysPriorToDisabling = referenceDay.atStartOfDay(systemDefault());
    ZonedDateTime nDaysPriorToDisablingEod = referenceDay.plusDays(1).atStartOfDay(systemDefault());

    progress.startingStage(
        format("Fetching users for reminder, %d days until disable", daysUntilDisable), SKIP_STAGE);
    Map<String, Optional<Locale>> receivers =
        progress.runStage(
            Map.of(),
            map -> format("Found %d receivers", map.size()),
            () ->
                userService.findNotifiableUsersWithLastLoginBetween(
                    Date.from(nDaysPriorToDisabling.toInstant()),
                    Date.from(nDaysPriorToDisablingEod.toInstant())));
    if (receivers.isEmpty()) {
      return;
    }
    // send reminders by language
    Locale fallback = localeManager.getFallbackLocale();
    Map<Locale, Set<String>> receiversByLocale = new HashMap<>();
    for (Map.Entry<String, Optional<Locale>> e : receivers.entrySet()) {
      receiversByLocale
          .computeIfAbsent(e.getValue().orElse(fallback), key -> new HashSet<>())
          .add(e.getKey());
    }
    progress.startingStage(
        format("Sending reminder for %d days until disable", daysUntilDisable),
        receiversByLocale.size(),
        JobProgress.FailurePolicy.SKIP_ITEM);
    progress.runStage(
        receiversByLocale.entrySet().stream(),
        e ->
            format(
                "Sending email to %d user(s) in %s",
                e.getValue().size(), e.getKey().getDisplayLanguage()),
        OutboundMessageResponse::getDescription,
        e -> sendReminderEmailInLanguage(e.getKey(), e.getValue(), daysUntilDisable),
        null);
  }

  private OutboundMessageResponse sendReminderEmailInLanguage(
      Locale language, Set<String> receiverEmails, int daysUntilDisable) {
    I18n i18n = i18nManager.getI18n(language);
    String subject =
        i18n.getString(
            "notification.user_inactive.subject", "Your DHIS2 account gets disabled soon");
    String body =
        i18n.getString(
            "notification.user_inactive.body",
            "Your DHIS2 user account was inactive for a while. Login during the next {0} days to prevent your account from being disabled.");
    OutboundMessageResponse response =
        emailService.sendEmail(
            subject, format(body.replace("{0}", "%d"), daysUntilDisable), receiverEmails);

    if (response.getResponseObject() != EmailResponse.SENT) {
      throw new UncheckedIOException(response.getDescription(), new IOException());
    }
    return response;
  }
}
