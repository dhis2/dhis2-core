package org.hisp.dhis.message;

/*
 * Copyright (c) 2004-2015, University of Oslo
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

import java.util.List;
import java.util.Set;

import org.hisp.dhis.dataset.CompleteDataSetRegistration;
import org.hisp.dhis.user.User;

/**
 * @author Lars Helge Overland
 */
public interface MessageService
{
    String ID = MessageService.class.getName();

    String META_USER_AGENT = "User-agent: ";

    /**
     * Sends a message to the in-box of the given recipients.
     * 
     * @param subject the message subject.
     * @param text the message text.
     * @param metaData message meta-data.
     * @param users the recipients of the message.
     * 
     * @return the identifier of the created message conversation.
     */
    int sendMessage( String subject, String text, String metaData, Set<User> users );

    /**
     * Sends a message to the in-box of the given recipients.
     * 
     * @param subject the message subject.
     * @param text the message text.
     * @param metaData message meta-data.
     * @param users the recipients of the message.
     * @param includeFeedbackRecipients include the feedback recipients user group
     *        in the message recipients.
     * @param forceNotifications send notifications to message senders ignoring
     *        whether users have enabled it.
     *        
     * @return the identifier of the created message conversation.
     */
    int sendMessage( String subject, String text, String metaData, Set<User> users, User sender, boolean includeFeedbackRecipients, boolean forceNotifications );

    int sendFeedback( String subject, String text, String metaData );
    
    int sendSystemNotification( String subject, String text );

    void sendReply( MessageConversation conversation, String text, String metaData );

    int saveMessageConversation( MessageConversation conversation );

    void updateMessageConversation( MessageConversation conversation );

    int sendCompletenessMessage( CompleteDataSetRegistration registration );

    MessageConversation getMessageConversation( int id );

    MessageConversation getMessageConversation( String uid );

    long getUnreadMessageConversationCount();

    long getUnreadMessageConversationCount( User user );

    /**
     * Get all MessageConversations for the current user.
     * @return a list of all message conversations for the current user.
     */
    List<MessageConversation> getMessageConversations();

    List<MessageConversation> getMessageConversations( int first, int max );

    List<MessageConversation> getMessageConversations( boolean followUpOnly, boolean unreadOnly, int first, int max );

    List<MessageConversation> getMessageConversations( User user, String[] messageConversationUids );

    int getMessageConversationCount();

    int getMessageConversationCount( boolean followUpOnly, boolean unreadOnly );

    void deleteMessages( User sender );

    List<UserMessage> getLastRecipients( int first, int max );
}
