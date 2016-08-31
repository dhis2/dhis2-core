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

import org.hisp.dhis.common.GenericIdentifiableObjectStore;
import org.hisp.dhis.user.User;

/**
 * @author Lars Helge Overland
 */
public interface MessageConversationStore
    extends GenericIdentifiableObjectStore<MessageConversation>
{
    /**
     * Returns a list of MessageConversations.
     * 
     * @param user the User for which the MessageConversations are sent to, or
     *        all if null.
     * @param first the first record number to return, or all if null.
     * @param max the max number of records to return, or all if null.
     * @return a list of MessageConversations.
     */
    List<MessageConversation> getMessageConversations( User user, boolean followUpOnly, boolean unreadOnly, Integer first, Integer max );

    /**
     * Returns the MessageConversations given by the supplied UIDs.
     *
     * @param messageConversationUids the UIDs of the MessageConversations to get.
     * @return a collection of MessageConversations.
     */
    List<MessageConversation> getMessageConversations( String[] messageConversationUids );
    
    int getMessageConversationCount( User user, boolean followUpOnly, boolean unreadOnly );
    
    long getUnreadUserMessageConversationCount( User user );
    
    int deleteMessages( User sender );
    
    int deleteUserMessages( User user );
    
    int removeUserFromMessageConversations( User lastSender );
    
    List<UserMessage> getLastRecipients( User user, Integer first, Integer max );
}
