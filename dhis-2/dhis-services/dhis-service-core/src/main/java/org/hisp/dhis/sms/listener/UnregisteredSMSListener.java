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
package org.hisp.dhis.sms.listener;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Map;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.message.MessageConversationParams;
import org.hisp.dhis.message.MessageSender;
import org.hisp.dhis.message.MessageService;
import org.hisp.dhis.message.MessageType;
import org.hisp.dhis.program.ProgramInstanceService;
import org.hisp.dhis.program.ProgramStageInstanceService;
import org.hisp.dhis.sms.command.SMSCommand;
import org.hisp.dhis.sms.command.SMSCommandService;
import org.hisp.dhis.sms.incoming.IncomingSms;
import org.hisp.dhis.sms.incoming.IncomingSmsService;
import org.hisp.dhis.sms.incoming.SmsMessageStatus;
import org.hisp.dhis.sms.parse.ParserType;
import org.hisp.dhis.system.util.SmsUtils;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserGroup;
import org.hisp.dhis.user.UserService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component("org.hisp.dhis.sms.listener.UnregisteredSMSListener")
@Transactional
public class UnregisteredSMSListener extends CommandSMSListener {
  private final SMSCommandService smsCommandService;

  private final UserService userService;

  private final MessageService messageService;

  public UnregisteredSMSListener(
      ProgramInstanceService programInstanceService,
      CategoryService dataElementCategoryService,
      ProgramStageInstanceService programStageInstanceService,
      UserService userService,
      CurrentUserService currentUserService,
      IncomingSmsService incomingSmsService,
      @Qualifier("smsMessageSender") MessageSender smsSender,
      SMSCommandService smsCommandService,
      UserService userService1,
      MessageService messageService) {
    super(
        programInstanceService,
        dataElementCategoryService,
        programStageInstanceService,
        userService,
        currentUserService,
        incomingSmsService,
        smsSender);

    checkNotNull(smsCommandService);
    checkNotNull(userService);
    checkNotNull(messageService);

    this.smsCommandService = smsCommandService;
    this.userService = userService1;
    this.messageService = messageService;
  }

  // -------------------------------------------------------------------------
  // IncomingSmsListener implementation
  // -------------------------------------------------------------------------

  @Override
  protected SMSCommand getSMSCommand(IncomingSms sms) {
    return smsCommandService.getSMSCommand(
        SmsUtils.getCommandString(sms), ParserType.UNREGISTERED_PARSER);
  }

  @Override
  protected boolean hasCorrectFormat(IncomingSms sms, SMSCommand smsCommand) {
    return true;
  }

  @Override
  protected void postProcess(
      IncomingSms sms, SMSCommand smsCommand, Map<String, String> parsedMessage) {
    UserGroup userGroup = smsCommand.getUserGroup();

    String userName = sms.getOriginator();

    if (userGroup != null) {
      User anonymousUser = userService.getUserByUsername(userName);
      if (anonymousUser == null) {
        User user = new User();
        user.setSurname(userName);
        user.setFirstName("");
        user.setAutoFields();

        userService.addUser(user);

        anonymousUser = userService.getUserByUsername(userName);
      }

      messageService.sendMessage(
          new MessageConversationParams.Builder(
                  userGroup.getMembers(),
                  anonymousUser,
                  smsCommand.getName(),
                  sms.getText(),
                  MessageType.SYSTEM,
                  null)
              .build());

      sendFeedback(smsCommand.getReceivedMessage(), sms.getOriginator(), INFO);

      update(sms, SmsMessageStatus.PROCESSED, true);
    }
  }
}
