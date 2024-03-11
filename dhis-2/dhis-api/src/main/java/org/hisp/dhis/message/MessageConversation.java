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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.MetadataObject;
import org.hisp.dhis.schema.annotation.PropertyRange;
import org.hisp.dhis.user.User;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * @author Lars Helge Overland
 */
@JacksonXmlRootElement( localName = "messageConversation", namespace = DxfNamespaces.DXF_2_0 )
public class MessageConversation
    extends BaseIdentifiableObject implements MetadataObject
{
    private static final int RECIPIENTS_MAX_DISPLAY = 25;

    // -------------------------------------------------------------------------
    // Persistent fields
    // -------------------------------------------------------------------------

    private String subject;

    private User lastSender;

    private Date lastMessage;

    private Set<UserMessage> userMessages = new HashSet<>();

    private List<Message> messages = new ArrayList<>();

    private int messageCount;

    private MessageType messageType;

    private MessageConversationPriority priority;

    private MessageConversationStatus status;

    private User assignee;

    // -------------------------------------------------------------------------
    // Transient fields
    // -------------------------------------------------------------------------

    private transient boolean read;

    private transient boolean followUp;

    private transient String userSurname;

    private transient String userFirstname;

    private transient String lastSenderSurname;

    private transient String lastSenderFirstname;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public MessageConversation()
    {
        this.priority = MessageConversationPriority.NONE;
        this.status = MessageConversationStatus.NONE;
    }

    public MessageConversation( String subject, User lastSender, MessageType messageType )
    {
        this.subject = subject;
        this.lastSender = lastSender;
        this.lastMessage = new Date();
        this.messageType = messageType;
        this.priority = MessageConversationPriority.NONE;
        this.status = MessageConversationStatus.NONE;
    }

    // -------------------------------------------------------------------------
    // Logic
    // -------------------------------------------------------------------------

    @Override
    public String toString()
    {
        return "MessageConversation{" +
            "subject='" + subject + '\'' +
            ", lastSender=" + lastSender +
            ", lastMessage=" + lastMessage +
            ", userMessages=" + userMessages +
            ", messages=" + messages +
            ", read=" + read +
            ", followUp=" + followUp +
            ", lastSenderSurname='" + lastSenderSurname + '\'' +
            ", lastSenderFirstname='" + lastSenderFirstname + '\'' +
            ", messageCount=" + messageCount +
            "} " + super.toString();
    }

    public void addUserMessage( UserMessage userMessage )
    {
        this.userMessages.add( userMessage );
    }

    public void addMessage( Message message )
    {
        if ( message != null )
        {
            message.setAutoFields();
        }

        this.messages.add( message );
        if ( !message.isInternal() )
        {
            setMessageCount( getMessageCount() + 1 );
        }
    }

    public boolean toggleFollowUp( User user )
    {
        for ( UserMessage userMessage : userMessages )
        {
            if ( userMessage.getUser() != null && userMessage.getUser().equals( user ) )
            {
                userMessage.setFollowUp( !userMessage.isFollowUp() );

                return userMessage.isFollowUp();
            }
        }

        return false;
    }

    public boolean isFollowUp( User user )
    {
        for ( UserMessage userMessage : userMessages )
        {
            if ( userMessage.getUser() != null && userMessage.getUser().equals( user ) )
            {
                return userMessage.isFollowUp();
            }
        }

        return false;
    }

    public boolean isRead( User user )
    {
        for ( UserMessage userMessage : userMessages )
        {
            if ( userMessage.getUser() != null && userMessage.getUser().equals( user ) )
            {
                return userMessage.isRead();
            }
        }

        return false;
    }

    public boolean markRead( User user )
    {
        for ( UserMessage userMessage : userMessages )
        {
            if ( userMessage.getUser() != null && userMessage.getUser().equals( user ) )
            {
                boolean read = userMessage.isRead();

                userMessage.setRead( true );

                return !read;
            }
        }

        return false;
    }

    public boolean markUnread( User user )
    {
        for ( UserMessage userMessage : userMessages )
        {
            if ( userMessage.getUser() != null && userMessage.getUser().equals( user ) )
            {
                boolean read = userMessage.isRead();

                userMessage.setRead( false );

                return read;
            }
        }

        return false;
    }

    public void markReplied( User sender, Message message )
    {
        for ( UserMessage userMessage : userMessages )
        {
            User user = userMessage.getUser();
            if ( user != null && !user.equals( sender ) && (!message.isInternal() ||
                (message.isInternal() && (user.isAuthorized( "F_MANAGE_TICKETS" ) || user.isAuthorized( "ALL" )))) )
            {
                userMessage.setRead( false );
            }
        }

        addMessage( message );

        this.lastSender = sender;
        this.setLastMessage( new Date() );
    }

    public boolean remove( User user )
    {
        Iterator<UserMessage> iterator = userMessages.iterator();

        while ( iterator.hasNext() )
        {
            UserMessage userMessage = iterator.next();

            if ( userMessage.getUser() != null && userMessage.getUser().equals( user ) )
            {
                iterator.remove();

                return true;
            }
        }
        return false;
    }

    public Set<User> getUsers()
    {
        Set<User> users = new HashSet<>();

        for ( UserMessage userMessage : userMessages )
        {
            users.add( userMessage.getUser() );
        }

        return users;
    }

    public void removeAllMessages()
    {
        messages.clear();
        setMessageCount( 0 );
    }

    public void removeAllUserMessages()
    {
        userMessages.clear();
    }

    public String getSenderDisplayName()
    {
        String userDisplayName = getFullNameNullSafe( userFirstname, userSurname );
        String lastSenderName = getFullNameNullSafe( lastSenderFirstname, lastSenderSurname );

        if ( !userDisplayName.isEmpty() && !lastSenderName.isEmpty() && !userDisplayName.equals( lastSenderName ) )
        {
            userDisplayName += ", " + lastSenderName;
        }
        else if ( !lastSenderName.isEmpty() )
        {
            userDisplayName = lastSenderName;
        }

        return StringUtils.trimToNull( StringUtils.substring( userDisplayName, 0, 28 ) );
    }

    public Set<User> getTopRecipients()
    {
        Set<User> recipients = new HashSet<>();

        for ( UserMessage userMessage : userMessages )
        {
            recipients.add( userMessage.getUser() );

            if ( recipients.size() > RECIPIENTS_MAX_DISPLAY )
            {
                break;
            }
        }

        return recipients;
    }

    public int getBottomRecipients()
    {
        return userMessages.size() - RECIPIENTS_MAX_DISPLAY;
    }

    // -------------------------------------------------------------------------
    // Persistent fields
    // -------------------------------------------------------------------------

    @Override
    public String getName()
    {
        return subject;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    @PropertyRange( min = 2 )
    public String getSubject()
    {
        return subject;
    }

    public void setSubject( String subject )
    {
        this.subject = subject;
    }

    @JsonProperty
    @JsonSerialize( as = BaseIdentifiableObject.class )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public User getLastSender()
    {
        return lastSender;
    }

    public void setLastSender( User lastSender )
    {
        this.lastSender = lastSender;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Date getLastMessage()
    {
        return lastMessage;
    }

    public void setLastMessage( Date lastMessage )
    {
        this.lastMessage = lastMessage;
    }

    @JsonProperty
    @JacksonXmlElementWrapper( localName = "userMessages", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "userMessage", namespace = DxfNamespaces.DXF_2_0 )
    public Set<UserMessage> getUserMessages()
    {
        return userMessages;
    }

    public void setUserMessages( Set<UserMessage> userMessages )
    {
        this.userMessages = userMessages;
    }

    @JsonProperty
    @JacksonXmlElementWrapper( localName = "messages", namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "message", namespace = DxfNamespaces.DXF_2_0 )
    public List<Message> getMessages()
    {
        return messages;
    }

    public void setMessages( List<Message> messages )
    {
        this.messages = messages;
    }

    // -------------------------------------------------------------------------
    // Transient fields
    // -------------------------------------------------------------------------

    @JsonProperty
    @JacksonXmlProperty( isAttribute = true )
    public boolean isRead()
    {
        return read;
    }

    public void setRead( boolean read )
    {
        this.read = read;
    }

    @JsonProperty
    @JacksonXmlProperty( isAttribute = true )
    public boolean isFollowUp()
    {
        return followUp;
    }

    public void setFollowUp( boolean followUp )
    {
        this.followUp = followUp;
    }

    @JsonProperty
    @JacksonXmlProperty( isAttribute = true )
    public String getUserSurname()
    {
        return userSurname;
    }

    public void setUserSurname( String userSurname )
    {
        this.userSurname = userSurname;
    }

    @JsonProperty
    @JacksonXmlProperty( isAttribute = true )
    public String getUserFirstname()
    {
        return userFirstname;
    }

    public void setUserFirstname( String userFirstname )
    {
        this.userFirstname = userFirstname;
    }

    @JsonProperty
    @JacksonXmlProperty( isAttribute = true )
    public String getLastSenderSurname()
    {
        return lastSenderSurname;
    }

    public void setLastSenderSurname( String lastSenderSurname )
    {
        this.lastSenderSurname = lastSenderSurname;
    }

    @JsonProperty
    @JacksonXmlProperty( isAttribute = true )
    public String getLastSenderFirstname()
    {
        return lastSenderFirstname;
    }

    public void setLastSenderFirstname( String lastSenderFirstname )
    {
        this.lastSenderFirstname = lastSenderFirstname;
    }

    @JsonProperty
    @JacksonXmlProperty( isAttribute = true )
    public int getMessageCount()
    {
        return messageCount;
    }

    public void setMessageCount( int messageCount )
    {
        this.messageCount = messageCount;
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    private String getFullNameNullSafe( String firstName, String surname )
    {
        return StringUtils.defaultString( firstName ) +
            (StringUtils.isBlank( firstName ) ? StringUtils.EMPTY : " ") + StringUtils.defaultString( surname );
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public MessageConversationPriority getPriority()
    {
        return priority;
    }

    public void setPriority( MessageConversationPriority messagePriority )
    {
        this.priority = messagePriority;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public MessageConversationStatus getStatus()
    {
        return status;
    }

    public void setStatus( MessageConversationStatus messageConversationStatus )
    {
        this.status = messageConversationStatus;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public User getAssignee()
    {
        return assignee;
    }

    public void setAssignee( User assignee )
    {
        this.assignee = assignee;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public MessageType getMessageType()
    {
        return messageType;
    }

    public void setMessageType( MessageType messageType )
    {
        this.messageType = messageType;
    }
}
