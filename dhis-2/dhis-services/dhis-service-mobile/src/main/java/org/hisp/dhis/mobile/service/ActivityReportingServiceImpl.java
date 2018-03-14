package org.hisp.dhis.mobile.service;

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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hisp.dhis.api.mobile.ActivityReportingService;
import org.hisp.dhis.api.mobile.NotAllowedException;
import org.hisp.dhis.api.mobile.model.Interpretation;
import org.hisp.dhis.api.mobile.model.InterpretationComment;
import org.hisp.dhis.chart.Chart;
import org.hisp.dhis.chart.ChartService;
import org.hisp.dhis.interpretation.InterpretationService;
import org.hisp.dhis.message.Message;
import org.hisp.dhis.message.MessageConversation;
import org.hisp.dhis.message.MessageService;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserQueryParams;
import org.hisp.dhis.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;

public class ActivityReportingServiceImpl
    implements ActivityReportingService
{
    private static final String FEEDBACK_SENT = "feedback_sent";

    private static final String MESSAGE_SENT = "message_sent";

    private static final String INTERPRETATION_SENT = "interpretation_sent";

    private static final String COMMENT_SENT = "comment_sent";

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    @Autowired
    private CurrentUserService currentUserService;

    @Autowired
    private MessageService messageService;

    @Autowired
    private UserService userService;

    @Autowired
    private InterpretationService interpretationService;

    @Autowired
    private ChartService chartService;

    @Override
    public String sendFeedback( org.hisp.dhis.api.mobile.model.Message message )
        throws NotAllowedException
    {

        String subject = message.getSubject();
        String text = message.getText();
        String metaData = MessageService.META_USER_AGENT;

        messageService.sendMessage( messageService.createTicketMessage( subject, text, metaData ).build() );

        return FEEDBACK_SENT;
    }

    @Override
    public Collection<org.hisp.dhis.api.mobile.model.User> findUser( String keyword )
        throws NotAllowedException
    {
        Collection<User> users = new HashSet<>();

        Collection<org.hisp.dhis.api.mobile.model.User> userList = new HashSet<>();

        if ( keyword != null )
        {
            int index = keyword.indexOf( ' ' );

            if ( index != -1 && index == keyword.lastIndexOf( ' ' ) )
            {
                String[] keys = keyword.split( " " );
                keyword = keys[0] + "  " + keys[1];
            }
        }

        UserQueryParams params = new UserQueryParams();
        params.setQuery( keyword );
        users = userService.getUsers( params );

        for ( User userCore : users )
        {

            org.hisp.dhis.api.mobile.model.User user = new org.hisp.dhis.api.mobile.model.User();
            user.setId( userCore.getId() );
            user.setSurname( userCore.getSurname() );
            user.setFirstName( userCore.getFirstName() );
            userList.add( user );

        }

        return userList;
    }

    public Date getDate( int operation, String adjustment )
    {
        Calendar calendar = Calendar.getInstance();

        if ( adjustment.equals( "1 day" ) )
        {
            calendar.add( Calendar.DATE, operation );
        }
        else if ( adjustment.equals( "3 days" ) )
        {
            calendar.add( Calendar.DATE, operation * 3 );
        }
        else if ( adjustment.equals( "1 week" ) )
        {
            calendar.add( Calendar.DATE, operation * 7 );
        }
        else if ( adjustment.equals( "1 month" ) )
        {
            calendar.add( Calendar.DATE, operation * 30 );
        }
        return calendar.getTime();
    }

    @Override
    public String sendMessage( org.hisp.dhis.api.mobile.model.Message message )
        throws NotAllowedException
    {
        String subject = message.getSubject();
        String text = message.getText();
        String metaData = MessageService.META_USER_AGENT;

        Set<User> users = new HashSet<>();

        for ( org.hisp.dhis.api.mobile.model.User user : message.getRecipient().getUserList() )
        {
            User userWeb = userService.getUser( user.getId() );
            users.add( userWeb );

        }

        messageService.sendMessage( messageService.createPrivateMessage( users, subject, text, metaData ).build() );

        return MESSAGE_SENT;
    }

    @Override
    public Collection<org.hisp.dhis.api.mobile.model.MessageConversation> downloadMessageConversation()
        throws NotAllowedException
    {
        Collection<MessageConversation> conversations = new HashSet<>();

        Collection<org.hisp.dhis.api.mobile.model.MessageConversation> mobileConversationList = new HashSet<>();

        conversations = new ArrayList<>( messageService.getMessageConversations( 0, 10 ) );

        for ( MessageConversation conversation : conversations )
        {
            if ( conversation.getLastSenderFirstname() != null )
            {
                org.hisp.dhis.api.mobile.model.MessageConversation messConversation = new org.hisp.dhis.api.mobile.model.MessageConversation();
                messConversation.setId( conversation.getId() );
                messConversation.setSubject( conversation.getSubject() );
                mobileConversationList.add( messConversation );
            }

        }

        return mobileConversationList;
    }

    @Override
    public Collection<org.hisp.dhis.api.mobile.model.Message> getMessage( String conversationId )
        throws NotAllowedException
    {

        MessageConversation conversation = messageService.getMessageConversation( Integer.parseInt( conversationId ) );
        List<Message> messageList = new ArrayList<>( conversation.getMessages() );

        Collection<org.hisp.dhis.api.mobile.model.Message> messages = new HashSet<>();

        for ( Message message : messageList )
        {

            if ( message.getSender().getFirstName() != null )
            {

                org.hisp.dhis.api.mobile.model.Message messageMobile = new org.hisp.dhis.api.mobile.model.Message();
                messageMobile.setSubject( conversation.getSubject() );
                messageMobile.setText( message.getText() );
                messageMobile.setLastSenderName( message.getSender().getName() );
                messages.add( messageMobile );
            }
        }

        return messages;
    }

    @Override
    public String replyMessage( org.hisp.dhis.api.mobile.model.Message message )
        throws NotAllowedException
    {
        String metaData = MessageService.META_USER_AGENT;

        MessageConversation conversation = messageService
            .getMessageConversation( Integer.parseInt( message.getSubject() ) );

        messageService.sendReply( conversation, message.getText(), metaData, false );

        return MESSAGE_SENT;
    }

    @Override
    public Interpretation getInterpretation( String uId )
        throws NotAllowedException
    {
        Chart chart = chartService.getChart( uId );
        org.hisp.dhis.interpretation.Interpretation interpretationCore = interpretationService
            .getInterpretationByChart( chart.getId() );

        Collection<InterpretationComment> interComments = new HashSet<>();

        for ( org.hisp.dhis.interpretation.InterpretationComment interCommentsCore : interpretationCore.getComments() )
        {

            InterpretationComment interComment = new InterpretationComment();
            interComment.setText( interCommentsCore.getText() );
            interComments.add( interComment );
        }

        Interpretation interpretation = new Interpretation();
        interpretation.setId( interpretationCore.getId() );
        interpretation.setText( interpretationCore.getText() );
        interpretation.setInComments( interComments );

        return interpretation;
    }

    private org.hisp.dhis.interpretation.Interpretation interpretation;

    public void setInterpretation( org.hisp.dhis.interpretation.Interpretation interpretation )
    {
        this.interpretation = interpretation;
    }

    public org.hisp.dhis.interpretation.Interpretation getInterpretation()
    {
        return interpretation;
    }

    @Override
    public String postInterpretation( String data )
        throws NotAllowedException
    {

        String uId = data.substring( 0, 11 );

        String interpretation = data.substring( 11, data.length() - 0 );

        Chart c = chartService.getChart( uId );

        org.hisp.dhis.interpretation.Interpretation i = new org.hisp.dhis.interpretation.Interpretation( c, null,
            interpretation );

        i.setUser( currentUserService.getCurrentUser() );

        interpretationService.saveInterpretation( i );

        return INTERPRETATION_SENT;
    }

    @Override
    public String postInterpretationComment( String data )
        throws NotAllowedException
    {
        int interpretationId = Integer.parseInt( data.substring( 0, 7 ) );
        String comment = data.substring( 7, data.length() - 0 );

        setInterpretation( interpretationService.getInterpretation( interpretationId ) );
        interpretationService.addInterpretationComment( interpretation.getUid(), comment );

        return COMMENT_SENT;
    }
}
