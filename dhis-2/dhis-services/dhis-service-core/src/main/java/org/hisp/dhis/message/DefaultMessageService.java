package org.hisp.dhis.message;

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

import com.google.api.client.util.Sets;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.commons.util.DebugUtils;
import org.hisp.dhis.configuration.ConfigurationService;
import org.hisp.dhis.dataset.CompleteDataSetRegistration;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.fileresource.FileResource;
import org.hisp.dhis.i18n.I18nManager;
import org.hisp.dhis.i18n.locale.LocaleManager;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.system.velocity.VelocityManager;
import org.hisp.dhis.user.*;
import org.hisp.dhis.util.ObjectUtils;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

import static org.hisp.dhis.commons.util.TextUtils.LN;

/**
 * @author Lars Helge Overland
 */
@Transactional
public class DefaultMessageService
    implements MessageService
{
    private static final Log log = LogFactory.getLog( DefaultMessageService.class );

    private static final String COMPLETE_SUBJECT = "Form registered as complete";
    private static final String COMPLETE_TEMPLATE = "completeness_message";
    private static final String MESSAGE_EMAIL_FOOTER_TEMPLATE = "message_email_footer";
    private static final String MESSAGE_PATH = "/dhis-web-messaging/readMessage.action";

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private MessageConversationStore messageConversationStore;

    public void setMessageConversationStore( MessageConversationStore messageConversationStore )
    {
        this.messageConversationStore = messageConversationStore;
    }

    private CurrentUserService currentUserService;

    public void setCurrentUserService( CurrentUserService currentUserService )
    {
        this.currentUserService = currentUserService;
    }

    private ConfigurationService configurationService;

    public void setConfigurationService( ConfigurationService configurationService )
    {
        this.configurationService = configurationService;
    }

    private UserSettingService userSettingService;

    public void setUserSettingService( UserSettingService userSettingService )
    {
        this.userSettingService = userSettingService;
    }

    private I18nManager i18nManager;

    public void setI18nManager( I18nManager i18nManager )
    {
        this.i18nManager = i18nManager;
    }

    private SystemSettingManager systemSettingManager;

    public void setSystemSettingManager( SystemSettingManager systemSettingManager )
    {
        this.systemSettingManager = systemSettingManager;
    }

    private List<MessageSender> messageSenders;

    @Autowired
    public void setMessageSenders( List<MessageSender> messageSenders )
    {
        this.messageSenders = messageSenders;

        log.info( "Found the following message senders: " + messageSenders );
    }

    // -------------------------------------------------------------------------
    // MessageService implementation
    // -------------------------------------------------------------------------

    @Override
    public int sendTicketMessage( String subject, String text, String metaData )
    {
        User currentUser = currentUserService.getCurrentUser();

        MessageConversationParams params = new MessageConversationParams.Builder()
            .withRecipients( getFeedbackRecipients() )
            .withSender( currentUser )
            .withSubject( subject )
            .withText( text )
            .withMessageType( MessageType.TICKET )
            .withMetaData( metaData )
            .withStatus( MessageConversationStatus.OPEN ).build();
        
        return sendMessage( params );        
    }

    @Override
    public int sendPrivateMessage( Set<User> recipients, String subject, String text, String metaData, Set<FileResource> attachments )
    {
        User currentUser = currentUserService.getCurrentUser();

        MessageConversationParams params = new MessageConversationParams.Builder()
            .withRecipients( recipients )
            .withSender( currentUser )
            .withSubject( subject )
            .withText( text )
            .withMessageType( MessageType.PRIVATE )
            .withMetaData( metaData )
            .withAttachments( attachments ).build();
        
        return sendMessage( params );
    }
    
    @Override
    public int sendSystemMessage( Set<User> recipients, String subject, String text )
    {
        MessageConversationParams params = new MessageConversationParams.Builder()
            .withRecipients( recipients )
            .withSubject( subject )
            .withText( text )
            .withMessageType( MessageType.SYSTEM ).build();
        
        return sendMessage( params );
    }

    @Override
    public int sendValidationMessage( Set<User> recipients, String subject, String text, MessageConversationPriority priority )
    {
        MessageConversationParams params = new MessageConversationParams.Builder()
            .withRecipients( recipients )
            .withSubject( subject )
            .withText( text )
            .withMessageType( MessageType.VALIDATION_RESULT )
            .withStatus( MessageConversationStatus.OPEN )
            .withPriority( priority ).build();

        return sendMessage( params );
    }
    
    @Override
    public int sendMessage( MessageConversationParams params )
    {
        MessageConversation conversation = params.createMessageConversation();
        int id = saveMessageConversation( conversation );

        Message message = new Message( params.getText(), params.getMetadata(), params.getSender() );

        message.setAttachments( params.getAttachments() );
        conversation.addMessage( message );

        params.getRecipients().stream().filter( r -> !r.equals( params.getSender() ) )
            .forEach( ( recipient ) -> conversation.addUserMessage( new UserMessage( recipient, false ) ) );

        if ( params.getSender() != null )
        {
            conversation.addUserMessage( new UserMessage( params.getSender(), true ) );
        }

        String footer = getMessageFooter( conversation );

        invokeMessageSenders( params.getSubject(), params.getText(), footer, params.getSender(),
            params.getRecipients(), params.isForceNotification() );

        return id;
    }

    @Override
    public int sendSystemErrorNotification( String subject, Throwable t )
    {
        String title = (String) systemSettingManager.getSystemSetting( SettingKey.APPLICATION_TITLE );
        String baseUrl = (String) systemSettingManager.getSystemSetting( SettingKey.INSTANCE_BASE_URL );

        String text = new StringBuilder()
            .append( subject + LN + LN )
            .append( "System title: " + title + LN )
            .append( "Base URL: " + baseUrl + LN )
            .append( "Time: " + new DateTime().toString() + LN )
            .append( "Message: " + t.getMessage() + LN + LN )
            .append( "Cause: " + DebugUtils.getStackTrace( t.getCause() ) ).toString();

        MessageConversationParams params = new MessageConversationParams.Builder()
            .withRecipients( getFeedbackRecipients() )
            .withSubject( subject )
            .withText( text )
            .withMessageType( MessageType.SYSTEM ).build();
        
        return sendMessage( params );
    }

    @Override
    public void sendReply( MessageConversation conversation, String text, String metaData, boolean internal, Set<FileResource> attachments )
    {
        User sender = currentUserService.getCurrentUser();

        Message message = new Message( text, metaData, sender, internal );

        message.setAttachments( attachments );

        conversation.markReplied( sender, message );

        updateMessageConversation( conversation );

        Set<User> users = conversation.getUsers();

        if ( conversation.getMessageType().equals( MessageType.TICKET ) && internal )
        {
            users = users.stream().filter( this::hasAccessToManageFeedbackMessages )
                .collect( Collectors.toSet() );
        }

        invokeMessageSenders( conversation.getSubject(), text, null, sender, new HashSet<>( users ), false );
    }

    @Override
    public int sendCompletenessMessage( CompleteDataSetRegistration registration )
    {
        DataSet dataSet = registration.getDataSet();

        if ( dataSet == null )
        {
            return 0;
        }

        UserGroup userGroup = dataSet.getNotificationRecipients();

        User sender = currentUserService.getCurrentUser();

        // data set completed through sms
        if ( sender == null )
        {
            return 0;
        }

        Set<User> recipients = new HashSet<>();

        if ( userGroup != null )
        {
            recipients.addAll( new HashSet<>( userGroup.getMembers() ) );
        }

        if ( dataSet.isNotifyCompletingUser() )
        {
            recipients.add( sender );
        }

        if ( recipients.isEmpty() )
        {
            return 0;
        }

        String text = new VelocityManager().render( registration, COMPLETE_TEMPLATE );

        MessageConversation conversation = new MessageConversation( COMPLETE_SUBJECT, sender, MessageType.SYSTEM );

        conversation.addMessage( new Message( text, null, sender ) );

        for ( User user : recipients )
        {
            conversation.addUserMessage( new UserMessage( user ) );
        }

        if ( !conversation.getUserMessages().isEmpty() )
        {
            int id = saveMessageConversation( conversation );

            invokeMessageSenders( COMPLETE_SUBJECT, text, null, sender,
                new HashSet<>( conversation.getUsers() ), false );

            return id;
        }

        return 0;
    }

    @Override
    public int saveMessageConversation( MessageConversation conversation )
    {
        messageConversationStore.save( conversation );
        return conversation.getId();
    }

    @Override
    public void updateMessageConversation( MessageConversation conversation )
    {
        messageConversationStore.update( conversation );
    }

    @Override
    public MessageConversation getMessageConversation( int id )
    {
        return messageConversationStore.get( id );
    }

    @Override
    public MessageConversation getMessageConversation( String uid )
    {
        MessageConversation mc = messageConversationStore.getByUid( uid );

        if ( mc == null )
        {
            return null;
        }

        User user = currentUserService.getCurrentUser();

        mc.setFollowUp( mc.isFollowUp( user ) );
        mc.setRead( mc.isRead( user ) );

        return messageConversationStore.getByUid( uid );
    }

    @Override
    public long getUnreadMessageConversationCount()
    {
        return messageConversationStore.getUnreadUserMessageConversationCount( currentUserService.getCurrentUser() );
    }

    @Override
    public long getUnreadMessageConversationCount( User user )
    {
        return messageConversationStore.getUnreadUserMessageConversationCount( user );
    }

    @Override
    public List<MessageConversation> getMessageConversations()
    {
        return messageConversationStore
            .getMessageConversations( currentUserService.getCurrentUser(), null, false, false,
                null, null );
    }

    @Override
    public List<MessageConversation> getMessageConversations( int first, int max )
    {
        return messageConversationStore
            .getMessageConversations( currentUserService.getCurrentUser(), null, false, false,
                first, max );
    }

    @Override
    public List<MessageConversation> getMessageConversations( User user, Collection<String> uid )
    {
        List<MessageConversation> conversations = messageConversationStore
            .getMessageConversations( uid );

        // Set transient properties

        for ( MessageConversation mc : conversations )
        {
            mc.setFollowUp( mc.isFollowUp( user ) );
            mc.setRead( mc.isRead( user ) );
        }

        return conversations;
    }

    @Override
    public void deleteMessages( User user )
    {
        messageConversationStore.deleteMessages( user );
        messageConversationStore.deleteUserMessages( user );
        messageConversationStore.removeUserFromMessageConversations( user );
    }

    @Override
    public List<UserMessage> getLastRecipients( int first, int max )
    {
        return messageConversationStore.getLastRecipients( currentUserService.getCurrentUser(), first, max );
    }

    @Override
    public boolean hasAccessToManageFeedbackMessages( User user )
    {
        user = (user == null ? currentUserService.getCurrentUser() : user);
        
        return configurationService.isUserInFeedbackRecipientUserGroup( user ) || user.isAuthorized( "ALL" );
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    private Set<User> getFeedbackRecipients()
    {
        UserGroup feedbackRecipients = configurationService.getConfiguration().getFeedbackRecipients();

        if ( feedbackRecipients != null )
        {
            return feedbackRecipients.getMembers();
        }

        return Sets.newHashSet();
    }

    private void invokeMessageSenders( String subject, String text, String footer, User sender, Set<User> users,
        boolean forceSend )
    {
        for ( MessageSender messageSender : messageSenders )
        {
            log.debug( "Invoking message sender: " + messageSender.getClass().getSimpleName() );

            messageSender.sendMessageAsync( subject, text, footer, sender, new HashSet<>( users ), forceSend );
        }
    }

    private String getMessageFooter( MessageConversation conversation )
    {
        HashMap<String, Object> values = new HashMap<>( 2 );

        String baseUrl = systemSettingManager.getInstanceBaseUrl();

        if ( baseUrl == null )
        {
            return StringUtils.EMPTY;
        }

        Locale locale = (Locale) userSettingService.getUserSetting( UserSettingKey.UI_LOCALE, conversation.getUser() );

        locale = ObjectUtils.firstNonNull( locale, LocaleManager.DEFAULT_LOCALE );

        values.put( "responseUrl", baseUrl + MESSAGE_PATH + "?id=" + conversation.getUid() );
        values.put( "i18n", i18nManager.getI18n( locale ) );

        return new VelocityManager().render( values, MESSAGE_EMAIL_FOOTER_TEMPLATE );
    }
}
