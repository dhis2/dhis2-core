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

import java.util.*;
import lombok.extern.slf4j.Slf4j;
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
import org.hisp.dhis.sms.parse.SMSParserException;
import org.hisp.dhis.system.util.SmsUtils;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserGroup;
import org.hisp.dhis.user.UserService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component("org.hisp.dhis.sms.listener.DhisMessageAlertListener")
@Transactional
public class DhisMessageAlertListener extends CommandSMSListener {
  private final SMSCommandService smsCommandService;

  private final MessageService messageService;

  public DhisMessageAlertListener(
      ProgramInstanceService programInstanceService,
      CategoryService dataElementCategoryService,
      ProgramStageInstanceService programStageInstanceService,
      UserService userService,
      CurrentUserService currentUserService,
      IncomingSmsService incomingSmsService,
      @Qualifier("smsMessageSender") MessageSender smsSender,
      SMSCommandService smsCommandService,
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
    checkNotNull(messageService);

    this.smsCommandService = smsCommandService;
    this.messageService = messageService;
  }

  @Override
  protected SMSCommand getSMSCommand(IncomingSms sms) {
    return smsCommandService.getSMSCommand(SmsUtils.getCommandString(sms), ParserType.ALERT_PARSER);
  }

  @Override
  protected void postProcess(
      IncomingSms sms, SMSCommand smsCommand, Map<String, String> parsedMessage) {
    String message = sms.getText();

    UserGroup userGroup = smsCommand.getUserGroup();

    if (userGroup != null) {
      Collection<User> users = Collections.singleton(sms.getCreatedBy());

      if (users != null && users.size() > 1) {
        String messageMoreThanOneUser = smsCommand.getMoreThanOneOrgUnitMessage();

        if (messageMoreThanOneUser.trim().isEmpty()) {
          messageMoreThanOneUser = SMSCommand.MORE_THAN_ONE_ORGUNIT_MESSAGE;
        }

        for (Iterator<User> i = users.iterator(); i.hasNext(); ) {
          User user = i.next();
          messageMoreThanOneUser += " " + user.getName();
          if (i.hasNext()) {
            messageMoreThanOneUser += ",";
          }
        }

        throw new SMSParserException(messageMoreThanOneUser);
      } else if (users != null && users.size() == 1) {
        User sender = users.iterator().next();

        Set<User> receivers = new HashSet<>(userGroup.getMembers());
        messageService.sendMessage(
            new MessageConversationParams.Builder(
                    receivers, sender, smsCommand.getName(), message, MessageType.SYSTEM, null)
                .build());

        Set<User> feedbackList = new HashSet<>();
        feedbackList.add(sender);

        String confirmMessage = smsCommand.getReceivedMessage();

        if (confirmMessage == null) {
          confirmMessage = SMSCommand.ALERT_FEEDBACK;
        }

        if (smsSender.isConfigured()) {
          smsSender.sendMessage(
              smsCommand.getName(), confirmMessage, null, null, feedbackList, false);
        } else {
          log.info("No sms configuration found.");
        }

        update(sms, SmsMessageStatus.PROCESSED, true);
      } else if (users == null || users.size() == 0) {
        throw new SMSParserException(
            "No user associated with this phone number. Please contact your supervisor.");
      }
    }
  }
}
