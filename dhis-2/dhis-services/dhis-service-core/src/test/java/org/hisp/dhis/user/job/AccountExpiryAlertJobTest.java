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
package org.hisp.dhis.user.job;

import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Date;
import org.hisp.dhis.message.EmailMessageSender;
import org.hisp.dhis.message.MessageSender;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.scheduling.JobProgress;
import org.hisp.dhis.setting.SystemSettings;
import org.hisp.dhis.setting.SystemSettingsProvider;
import org.hisp.dhis.user.UserAccountExpiryInfo;
import org.hisp.dhis.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * Tests the {@link AccountExpiryAlertJob} using mocks to only test the logic of the job.
 *
 * @author Jan Bernitt
 */
class AccountExpiryAlertJobTest {

  private final UserService userService = mock(UserService.class);

  private final MessageSender messageSender = mock(EmailMessageSender.class);

  private final SystemSettingsProvider settingsProvider = mock(SystemSettingsProvider.class);
  private final SystemSettings settings = mock(SystemSettings.class);

  private final AccountExpiryAlertJob job =
      new AccountExpiryAlertJob(userService, messageSender, settingsProvider);

  @BeforeEach
  void setUp() {
    // mock normal run conditions
    when(settingsProvider.getCurrentSettings()).thenReturn(settings);
    when(settings.getAccountExpiryAlert()).thenReturn(true);
    when(settings.getAccountExpiresInDays()).thenReturn(7);
  }

  @Test
  void testDisabledJobDoesNotExecute() {
    when(settings.getAccountExpiryAlert()).thenReturn(false);
    job.execute(new JobConfiguration(), JobProgress.noop());
    verify(userService, never()).getExpiringUserAccounts(anyInt());
  }

  @Test
  void testEnabledJobSendsEmail() {
    when(messageSender.isConfigured()).thenReturn(true);
    when(userService.getExpiringUserAccounts(anyInt()))
        .thenReturn(
            singletonList(
                new UserAccountExpiryInfo(
                    "username", "email@example.com", Date.valueOf("2021-08-23"))));
    job.execute(new JobConfiguration(), JobProgress.noop());
    ArgumentCaptor<String> subject = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> text = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> recipient = ArgumentCaptor.forClass(String.class);
    verify(messageSender).sendMessage(subject.capture(), text.capture(), recipient.capture());
    assertEquals("Account Expiry Alert", subject.getValue());
    assertEquals(
        "Dear username, your account is about to expire on 2021-08-23. If your use of the account needs to continue, get in touch with your system administrator.",
        text.getValue());
    assertEquals("email@example.com", recipient.getValue());
  }

  @Test
  void testValidate_Error() {
    when(messageSender.isConfigured()).thenReturn(false);
    job.execute(new JobConfiguration(), JobProgress.noop());
    verify(messageSender, never()).sendMessage(anyString(), anyString(), anyString());
  }
}
