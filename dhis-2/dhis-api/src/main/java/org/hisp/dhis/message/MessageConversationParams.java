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

import com.google.common.collect.ImmutableSet;
import org.hisp.dhis.user.User;

import java.util.Collection;

/**
 * @author Stian Sandvold
 */
public class MessageConversationParams
{

    /*
        Required properties
     */

    private ImmutableSet<User> recipients;

    private User sender;

    private String subject;

    private String text;

    private MessageType messageType;

    /*
        Optional properties
     */

    private String metadata;

    private User assignee;

    private MessageConversationPriority priority;

    private MessageConversationStatus status;

    private boolean forceNotification;

    private MessageConversationParams( Collection<User> recipients, User sender, String subject, String text,
        MessageType messageType )
    {
        this.recipients = ImmutableSet.copyOf( recipients );
        this.sender = sender;
        this.subject = subject;
        this.text = text;
        this.messageType = messageType;

        this.priority = MessageConversationPriority.NONE;
        this.status = MessageConversationStatus.NONE;

        this.forceNotification = false;
    }

    public ImmutableSet<User> getRecipients()
    {
        return recipients;
    }

    public User getSender()
    {
        return sender;
    }

    public String getSubject()
    {
        return subject;
    }

    public String getText()
    {
        return text;
    }

    public MessageType getMessageType()
    {
        return messageType;
    }

    public String getMetadata()
    {
        return metadata;
    }

    public User getAssignee()
    {
        return assignee;
    }

    public MessageConversationPriority getPriority()
    {
        return priority;
    }

    public MessageConversationStatus getStatus()
    {
        return status;
    }

    public boolean isForceNotification()
    {
        return forceNotification;
    }

    public MessageConversation createMessageConversation()
    {
        MessageConversation conversation = new MessageConversation( subject, sender, messageType );

        // Set all in case present in params
        conversation.setAssignee( assignee );
        conversation.setStatus( status );
        conversation.setPriority( priority );

        return conversation;
    }

    public static class Builder
    {

        private MessageConversationParams params;

        public Builder( Collection<User> recipients, User sender, String subject, String text, MessageType messageType )
        {
            this.params = new MessageConversationParams( recipients, sender, subject, text, messageType );
        }

        public Builder withMetaData( String metaData )
        {
            this.params.metadata = metaData;
            return this;
        }

        public Builder withAssignee( User assignee )
        {
            this.params.assignee = assignee;
            return this;
        }

        public Builder withPriority( MessageConversationPriority priority )
        {
            this.params.priority = priority;
            return this;
        }

        public Builder withStatus( MessageConversationStatus status )
        {
            this.params.status = status;
            return this;
        }

        public Builder withForceNotification( boolean forceNotification )
        {
            this.params.forceNotification = forceNotification;
            return this;
        }

        public MessageConversationParams build()
        {
            return this.params;
        }

    }
}
