package org.hisp.dhis.dashboard.message.action;

/*
 * Copyright (c) 2004-2016, University of Oslo
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

import org.hisp.dhis.message.MessageConversation;
import org.hisp.dhis.message.MessageConversationStatus;
import org.hisp.dhis.message.MessageService;
import org.hisp.dhis.paging.ActionPagingSupport;
import org.hisp.dhis.user.CurrentUserService;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Lars Helge Overland
 */
public class GetMessagesAction
    extends ActionPagingSupport<MessageConversation>
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

    // -------------------------------------------------------------------------
    // Input
    // -------------------------------------------------------------------------

    private boolean followUp;

    public void setFollowUp( boolean followUp )
    {
        this.followUp = followUp;
    }

    private boolean unread;

    public void setUnread( boolean unread )
    {
        this.unread = unread;
    }

    private MessageConversationStatus ticketStatus;

    public void setTicketStatus( MessageConversationStatus messageConversationStatus )
    {
        this.ticketStatus = messageConversationStatus;
    }

    private String showAssignedToMe;

    public void setShowAssignedToMe( String showAssignedToMe )
    {
        this.showAssignedToMe = showAssignedToMe;
    }

    // -------------------------------------------------------------------------
    // Output
    // -------------------------------------------------------------------------

    private List<MessageConversation> conversations;

    public List<MessageConversation> getConversations()
    {
        return conversations;
    }

    private boolean showTicketTools;

    public boolean getShowTicketTools()
    {
        return showTicketTools;
    }

    // -------------------------------------------------------------------------
    // Action implementation
    // -------------------------------------------------------------------------

    @Override
    public String execute()
    {
        showTicketTools = messageService.hasAccessToManageFeedbackMessages( currentUserService.getCurrentUser() );

        this.paging = createPaging( messageService.getMessageConversationCount( followUp, unread ) );

        conversations = messageService
            .getMessageConversations( ticketStatus, followUp, unread, paging.getStartPos(), paging.getPageSize() );

        if ( showAssignedToMe != null && showAssignedToMe.equals( "true" ) )
        {
            conversations = conversations.stream().filter(
                messageConversation -> messageConversation.getAssignee() == currentUserService.getCurrentUser() )
                .collect(
                    Collectors.toList() );
        }

        return SUCCESS;
    }
}
