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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.jsontree.JsonMixed;
import org.hisp.dhis.message.FakeMessageSender;
import org.hisp.dhis.message.MessageSender;
import org.hisp.dhis.outboundmessage.OutboundMessage;
import org.hisp.dhis.security.PasswordManager;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.user.User;
import org.hisp.dhis.web.HttpStatus;
import org.hisp.dhis.webapi.DhisControllerIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Slf4j
class UserAccountControllerTest extends DhisControllerIntegrationTest {

  @Autowired private MessageSender messageSender;
  @Autowired private SystemSettingManager systemSettingManager;
  @Autowired private PasswordManager passwordEncoder;

  @Test
  void testResetPasswordOk() {
    systemSettingManager.saveSystemSetting(SettingKey.ACCOUNT_RECOVERY, true);

    User test = switchToNewUser("test");

    clearSecurityContext();
    String token = sendForgotPasswordRequest(test);

    log.error("token: {}", token);

    String newPassword = "Abxf123###...";

    HttpResponse response =
        POST(
            "/auth/passwordReset",
            "{'newPassword':'%s', 'resetToken':'%s'}".formatted(newPassword, token));

    JsonMixed jsonValues = response.contentUnchecked();

    log.error("jsonValues: {}", jsonValues);

    response.content(HttpStatus.OK);

    User updatedUser = userService.getUserByUsername(test.getUsername());

    boolean passwordMatch = passwordEncoder.matches(newPassword, updatedUser.getPassword());

    assertTrue(passwordMatch);
  }

  private String sendForgotPasswordRequest(User test) {
    POST("/auth/forgotPassword", "{'username':'%s'}".formatted(test.getUsername()))
        .content(HttpStatus.OK);

    OutboundMessage message = assertMessageSendTo(test.getEmail());

    Pattern pattern = Pattern.compile("\\?token=(.*?)\\n");
    Matcher matcher = pattern.matcher(message.getText());
    String token = "";
    if (matcher.find()) {
      token = matcher.group(1);
    }

    assertFalse(token.isEmpty());
    return token;
  }

  private OutboundMessage assertMessageSendTo(String email) {
    List<OutboundMessage> messagesByEmail =
        ((FakeMessageSender) messageSender).getMessagesByEmail(email);
    assertFalse(messagesByEmail.isEmpty());
    return messagesByEmail.get(0);
  }
}
