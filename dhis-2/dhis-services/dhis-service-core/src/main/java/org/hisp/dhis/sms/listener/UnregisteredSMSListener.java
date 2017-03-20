package org.hisp.dhis.sms.listener;

/*
 * Copyright (c) 2004-2017, University of Oslo
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

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.message.MessageSender;
import org.hisp.dhis.message.MessageService;
import org.hisp.dhis.message.MessageType;
import org.hisp.dhis.sms.command.SMSCommand;
import org.hisp.dhis.sms.command.SMSCommandService;
import org.hisp.dhis.sms.incoming.IncomingSms;
import org.hisp.dhis.sms.incoming.IncomingSmsListener;
import org.hisp.dhis.sms.incoming.IncomingSmsService;
import org.hisp.dhis.sms.incoming.SmsMessageStatus;
import org.hisp.dhis.sms.parse.ParserType;
import org.hisp.dhis.sms.parse.SMSParserException;
import org.hisp.dhis.system.util.SmsUtils;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserCredentials;
import org.hisp.dhis.user.UserGroup;
import org.hisp.dhis.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class UnregisteredSMSListener
    implements IncomingSmsListener
{

    public static final String USER_NAME = "anonymous";

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    @Autowired
    private SMSCommandService smsCommandService;

    @Autowired
    private UserService userService;

    @Autowired
    private MessageService messageService;
    
    @Autowired
    @Resource( name = "smsMessageSender" )
    private MessageSender smsSender;
    
    @Autowired
    private IncomingSmsService incomingSmsService;

    // -------------------------------------------------------------------------
    // IncomingSmsListener implementation
    // -------------------------------------------------------------------------

    @Transactional
    @Override
    public boolean accept( IncomingSms sms )
    {
        return smsCommandService.getSMSCommand( SmsUtils.getCommandString( sms ),
            ParserType.UNREGISTERED_PARSER ) != null;
    }

    @Transactional
    @Override
    public void receive( IncomingSms sms )
    {
        String message = sms.getText();
        SMSCommand smsCommand = smsCommandService.getSMSCommand( SmsUtils.getCommandString( sms ),
            ParserType.UNREGISTERED_PARSER );

        UserGroup userGroup = smsCommand.getUserGroup();

        String senderPhoneNumber = StringUtils.replace( sms.getOriginator(), "+", "" );

        if ( userGroup != null )
        {
            Collection<User> users = userService.getUsersByPhoneNumber( senderPhoneNumber );

            if ( users != null && users.size() >= 1 )
            {
                String messageError = "This number is already registered for user: ";
                for ( Iterator<User> iterator = users.iterator(); iterator.hasNext(); )
                {
                    User user = iterator.next();
                    messageError += user.getName();

                    if ( iterator.hasNext() )
                    {
                        messageError += ", ";
                    }
                }

                throw new SMSParserException( messageError );
            }
            else
            {
                Set<User> receivers = new HashSet<>( userGroup.getMembers() );

                UserCredentials anonymousUser = userService.getUserCredentialsByUsername( "anonymous" );

                if ( anonymousUser == null )
                {
                    User user = new User();
                    UserCredentials usercredential = new UserCredentials();
                    usercredential.setUsername( USER_NAME );
                    usercredential.setPassword( USER_NAME );
                    usercredential.setUserInfo( user );
                    user.setSurname( USER_NAME );
                    user.setFirstName( USER_NAME );
                    user.setUserCredentials( usercredential );

                    userService.addUserCredentials( usercredential );
                    userService.addUser( user );
                    anonymousUser = userService.getUserCredentialsByUsername( "anonymous" );
                }

                // forward to user group by SMS, E-mail, DHIS conversation
                messageService.sendMessage( smsCommand.getName(), message, null, receivers, anonymousUser.getUserInfo(),
                    MessageType.SYSTEM, false );

                // confirm SMS was received and forwarded completely
                Set<User> feedbackList = new HashSet<>();
                User sender = new User();
                sender.setPhoneNumber( senderPhoneNumber );
                feedbackList.add( sender );
                
                smsSender.sendMessage( smsCommand.getName(), smsCommand.getReceivedMessage(), null, null, feedbackList, true );
                
                sms.setStatus( SmsMessageStatus.PROCESSED );
                sms.setParsed( true );
                incomingSmsService.update( sms );
            }
        }
    }
}
