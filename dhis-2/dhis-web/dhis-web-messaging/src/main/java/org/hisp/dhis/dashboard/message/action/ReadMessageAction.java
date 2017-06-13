package org.hisp.dhis.dashboard.message.action;

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

import com.opensymphony.xwork2.Action;
import org.hisp.dhis.configuration.ConfigurationService;
import org.hisp.dhis.message.Message;
import org.hisp.dhis.message.MessageConversation;
import org.hisp.dhis.message.MessageConversationStatus;
import org.hisp.dhis.message.MessageService;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Lars Helge Overland
 */
public class ReadMessageAction
    implements Action
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private MessageService messageService;

    public void setMessageService( MessageService messageService )
    {
        this.messageService = messageService;
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

    // -------------------------------------------------------------------------
    // Input
    // -------------------------------------------------------------------------

    private String id;

    public void setId( String id )
    {
        this.id = id;
    }

    // -------------------------------------------------------------------------
    // Output
    // -------------------------------------------------------------------------

    private MessageConversation conversation;

    public MessageConversation getConversation()
    {
        return conversation;
    }

    private List<Message> messages;

    public List<Message> getMessages()
    {
        return messages;
    }

    private boolean showTicketTools;

    public boolean getShowTicketTools()
    {
        return showTicketTools;
    }

    private Set<User> feedbackRecipientGroupUsers;

    public Set<User> getFeedbackRecipientGroupUsers()
    {
        return feedbackRecipientGroupUsers;
    }

    // -------------------------------------------------------------------------
    // Action implementation
    // -------------------------------------------------------------------------

    @Override
    public String execute()
        throws Exception
    {
        if ( id == null )
        {
            return ERROR;
        }

        User user = currentUserService.getCurrentUser();
        conversation = messageService.getMessageConversation( id );
        showTicketTools = messageService.hasAccessToManageFeedbackMessages( user ) && conversation.getStatus() !=
            MessageConversationStatus.NONE;

        if ( showTicketTools )
        {
            messages = conversation.getMessages();
            feedbackRecipientGroupUsers = configurationService.getConfiguration().getFeedbackRecipients().getMembers();

        }
        else
        {
            messages = conversation.getMessages().stream().filter( message -> !message.isInternal() )
                .collect( Collectors.toList() );
        }
        if ( conversation == null )
        {
            return ERROR;
        }

        if ( conversation.markRead( user ) )
        {
            messageService.updateMessageConversation( conversation );
        }

        conversation.setFollowUp( conversation.isFollowUp( user ) );

        return SUCCESS;
    }
}
