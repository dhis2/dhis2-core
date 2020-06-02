package org.hisp.dhis.mobile.service;

/*
 * Copyright (c) 2004-2020, University of Oslo
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

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.hisp.dhis.api.mobile.ActivityReportingService;
import org.hisp.dhis.api.mobile.model.Interpretation;
import org.hisp.dhis.api.mobile.model.InterpretationComment;
import org.hisp.dhis.interpretation.InterpretationService;
import org.hisp.dhis.message.Message;
import org.hisp.dhis.message.MessageConversation;
import org.hisp.dhis.message.MessageService;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserQueryParams;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.visualization.Visualization;
import org.hisp.dhis.visualization.VisualizationService;
import org.springframework.stereotype.Service;

@Service( "org.hisp.dhis.mobile.api.ActivityReportingService" )
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

    private final CurrentUserService currentUserService;

    private final MessageService messageService;

    private final UserService userService;

    private final InterpretationService interpretationService;

    private final VisualizationService visualizationService;

    public ActivityReportingServiceImpl( CurrentUserService currentUserService, MessageService messageService,
        UserService userService, InterpretationService interpretationService, VisualizationService visualizationService )
    {
        checkNotNull( currentUserService );
        checkNotNull( messageService );
        checkNotNull( userService );
        checkNotNull( interpretationService );
        checkNotNull( visualizationService );

        this.currentUserService = currentUserService;
        this.messageService = messageService;
        this.userService = userService;
        this.interpretationService = interpretationService;
        this.visualizationService = visualizationService;
    }

    @Override
    public String sendFeedback( org.hisp.dhis.api.mobile.model.Message message ) {

        String subject = message.getSubject();
        String text = message.getText();
        String metaData = MessageService.META_USER_AGENT;

        messageService.sendTicketMessage( subject, text, metaData );

        return FEEDBACK_SENT;
    }

    @Override
    public Collection<org.hisp.dhis.api.mobile.model.User> findUser( String keyword ) {
        Collection<User> users;

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

        switch (adjustment) {
            case "1 day":
                calendar.add(Calendar.DATE, operation);
                break;
            case "3 days":
                calendar.add(Calendar.DATE, operation * 3);
                break;
            case "1 week":
                calendar.add(Calendar.DATE, operation * 7);
                break;
            case "1 month":
                calendar.add(Calendar.DATE, operation * 30);
                break;
        }
        return calendar.getTime();
    }

    @Override
    public String sendMessage( org.hisp.dhis.api.mobile.model.Message message ) {
        String subject = message.getSubject();
        String text = message.getText();
        String metaData = MessageService.META_USER_AGENT;

        Set<User> users = new HashSet<>();

        for ( org.hisp.dhis.api.mobile.model.User user : message.getRecipient().getUserList() )
        {
            User userWeb = userService.getUser( user.getId() );
            users.add( userWeb );

        }

        messageService.sendPrivateMessage( users, subject, text, metaData, null );

        return MESSAGE_SENT;
    }

    @Override
    public Collection<org.hisp.dhis.api.mobile.model.MessageConversation> downloadMessageConversation() {
        Collection<MessageConversation> conversations;

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
    public Collection<org.hisp.dhis.api.mobile.model.Message> getMessage( String conversationId ) {

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
    public String replyMessage( org.hisp.dhis.api.mobile.model.Message message ) {
        String metaData = MessageService.META_USER_AGENT;

        MessageConversation conversation = messageService
            .getMessageConversation( Integer.parseInt( message.getSubject() ) );

        messageService.sendReply( conversation, message.getText(), metaData, false, null );

        return MESSAGE_SENT;
    }

    @Override
    public Interpretation getInterpretation( String uid ) {
        Visualization visualization = visualizationService.loadVisualization( uid );
        org.hisp.dhis.interpretation.Interpretation interpretationCore = interpretationService
            .getInterpretationByVisualization( visualization.getId() );

        Collection<InterpretationComment> interComments = interpretationCore.getComments().stream().map( i -> {
            InterpretationComment interComment = new InterpretationComment();
            interComment.setText( i.getText() );
            return interComment;
        } ).collect( Collectors.toList() );

        Interpretation interpretation = new Interpretation();
        interpretation.setId( interpretationCore.getId() );
        interpretation.setText( interpretationCore.getText() );
        interpretation.setInComments( interComments );

        return interpretation;
    }

    private org.hisp.dhis.interpretation.Interpretation interpretation;

    private void setInterpretation(org.hisp.dhis.interpretation.Interpretation interpretation)
    {
        this.interpretation = interpretation;
    }

    @Override
    public String postInterpretation( String data ) {

        String uid = data.substring( 0, 11 );

        String interpretation = data.substring( 11 );

        Visualization visualization = visualizationService.loadVisualization( uid );

        org.hisp.dhis.interpretation.Interpretation i = new org.hisp.dhis.interpretation.Interpretation( visualization, null,
            interpretation );

        i.setUser( currentUserService.getCurrentUser() );

        interpretationService.saveInterpretation( i );

        return INTERPRETATION_SENT;
    }

    @Override
    public String postInterpretationComment( String data ) {
        int interpretationId = Integer.parseInt( data.substring( 0, 7 ) );
        String comment = data.substring( 7 );

        setInterpretation( interpretationService.getInterpretation( interpretationId ) );
        interpretationService.addInterpretationComment( interpretation.getUid(), comment );

        return COMMENT_SENT;
    }
}
