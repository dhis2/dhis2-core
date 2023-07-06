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

import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Date;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.message.MessageSender;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.scheduling.NoopJobProgress;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
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

  private final MessageSender messageSender = mock(MessageSender.class);

  private final SystemSettingManager settingManager = mock(SystemSettingManager.class);

  private final AccountExpiryAlertJob job =
      new AccountExpiryAlertJob(userService, messageSender, settingManager);

  @BeforeEach
  void setUp() {
    // mock normal run conditions
    when(settingManager.getBoolSetting(SettingKey.ACCOUNT_EXPIRY_ALERT)).thenReturn(true);
    when(settingManager.getIntSetting(SettingKey.ACCOUNT_EXPIRES_IN_DAYS)).thenReturn(7);
  }

  @Test
  void testDisabledJobDoesNotExecute() {
    when(settingManager.getBoolSetting(SettingKey.ACCOUNT_EXPIRY_ALERT)).thenReturn(false);
    job.execute(new JobConfiguration(), NoopJobProgress.INSTANCE);
    verify(userService, never()).getExpiringUserAccounts(anyInt());
  }

  @Test
  void testEnabledJobSendsEmail() {
    when(userService.getExpiringUserAccounts(anyInt()))
        .thenReturn(
            singletonList(
                new UserAccountExpiryInfo(
                    "username", "email@example.com", Date.valueOf("2021-08-23"))));
    job.execute(new JobConfiguration(), NoopJobProgress.INSTANCE);
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
  void testValidate() {
    when(messageSender.isConfigured()).thenReturn(true);
    assertNull(job.validate());
  }

  @Test
  void testValidate_Error() {
    when(messageSender.isConfigured()).thenReturn(false);
    ErrorReport report = job.validate();
    assertEquals(ErrorCode.E7010, report.getErrorCode());
    assertEquals(
        "Failed to validate job runtime: `EMAIL gateway configuration does not exist`",
        report.getMessage());
  }
}
