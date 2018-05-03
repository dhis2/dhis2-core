package org.hisp.dhis.sms.listener;

/*
 * Copyright (c) 2004-2018, University of Oslo
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

import org.hisp.dhis.message.MessageConversationParams;
import org.hisp.dhis.message.MessageSender;
import org.hisp.dhis.message.MessageService;
import org.hisp.dhis.message.MessageType;
import org.hisp.dhis.sms.command.SMSCommand;
import org.hisp.dhis.sms.command.SMSCommandService;
import org.hisp.dhis.sms.incoming.IncomingSms;
import org.hisp.dhis.sms.incoming.IncomingSmsService;
import org.hisp.dhis.sms.incoming.SmsMessageStatus;
import org.hisp.dhis.sms.parse.ParserType;
import org.hisp.dhis.sms.parse.SMSParserException;
import org.hisp.dhis.system.util.SmsUtils;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserGroup;
import org.jfree.util.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;

@Transactional
public class DhisMessageAlertListener
    extends BaseSMSListener
{
    @Autowired
    private SMSCommandService smsCommandService;

    @Autowired
    private MessageService messageService;

    @Autowired
    private IncomingSmsService incomingSmsService;

    @Autowired
    @Resource( name = "smsMessageSender" )
    private MessageSender smsSender;

    @Transactional
    @Override
    public boolean accept( IncomingSms sms )
    {
        return smsCommandService.getSMSCommand( SmsUtils.getCommandString( sms ), ParserType.ALERT_PARSER ) != null;
    }

    @Transactional
    @Override
    public void receive( IncomingSms sms )
    {
        String message = sms.getText();
        SMSCommand smsCommand = smsCommandService.getSMSCommand( SmsUtils.getCommandString( sms ),
            ParserType.ALERT_PARSER );
        UserGroup userGroup = smsCommand.getUserGroup();

        if ( userGroup != null )
        {
            Collection<User> users = Collections.singleton( sms.getUser() );

            if ( users != null && users.size() > 1 )
            {
                String messageMoreThanOneUser = smsCommand.getMoreThanOneOrgUnitMessage();
                
                if ( messageMoreThanOneUser.trim().isEmpty() )
                {
                    messageMoreThanOneUser = SMSCommand.MORE_THAN_ONE_ORGUNIT_MESSAGE;
                }
                
                for ( Iterator<User> i = users.iterator(); i.hasNext(); )
                {
                    User user = i.next();
                    messageMoreThanOneUser += " " + user.getName();
                    if ( i.hasNext() )
                    {
                        messageMoreThanOneUser += ",";
                    }
                }
                
                throw new SMSParserException( messageMoreThanOneUser );
            }
            else if ( users != null && users.size() == 1 )
            {
                User sender = users.iterator().next();

                Set<User> receivers = new HashSet<>( userGroup.getMembers() );
                messageService.sendMessage( new MessageConversationParams.Builder( receivers, sender, smsCommand.getName(), message, MessageType.SYSTEM ).build() );

                Set<User> feedbackList = new HashSet<>();
                feedbackList.add( sender );

                String confirmMessage = smsCommand.getReceivedMessage();

                if ( confirmMessage == null )
                {
                    confirmMessage = SMSCommand.ALERT_FEEDBACK;
                }

                if ( smsSender.isConfigured() )
                {
                    smsSender.sendMessage( smsCommand.getName(), confirmMessage, null, null, feedbackList, false );
                }
                else
                {
                    Log.info( "No sms configuration found." );
                }

                sms.setStatus( SmsMessageStatus.PROCESSED );
                sms.setParsed( true );

                incomingSmsService.update( sms );
            }
            else if ( users == null || users.size() == 0 )
            {
                throw new SMSParserException(
                    "No user associated with this phone number. Please contact your supervisor." );
            }
        }
    }
}