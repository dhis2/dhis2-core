/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.message;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.SessionFactory;
import org.hisp.dhis.test.integration.SingleSetupIntegrationTestBase;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Stian Sandvold
 */
class MessageConversationStoreTest extends SingleSetupIntegrationTestBase
{

    @Autowired
    private MessageConversationStore messageConversationStore;

    @Autowired
    private MessageService messageService;

    @Autowired
    private UserService _userService;

    @Autowired
    private SessionFactory sessionFactory;

    private User userB;

    private User userC;

    private long conversationA;

    private Collection<String> conversationIds;

    // -------------------------------------------------------------------------
    // Fixture
    // -------------------------------------------------------------------------
    @Override
    public void setUpTest()
    {
        userService = _userService;
        // 'A' used as currentUser
        setupUser( "A" );
        userB = setupUser( "B" );
        userC = setupUser( "C" );
        Set<User> usersA = new HashSet<>();
        usersA.add( userC );
        Set<User> usersB = new HashSet<>();
        usersB.add( userC );
        usersB.add( userB );
        conversationIds = new HashSet<>();
        conversationA = messageService.sendPrivateMessage( usersA, "Subject1", "Text", "Meta", null );
        MessageConversation mc = messageService.getMessageConversation( conversationA );
        mc.markRead( userC );
        messageService.updateMessageConversation( mc );
        conversationIds.add( mc.getUid() );
        messageService.sendReply( mc, "Message 1", "Meta", false, null );
        messageService.sendReply( mc, "Message 2", "Meta", false, null );
        messageService.sendReply( mc, "Message 3", "Meta", false, null );
        long conversationB = messageService.sendPrivateMessage( usersA, "Subject2", "Text", "Meta", null );
        mc = messageService.getMessageConversation( conversationB );
        mc.setFollowUp( true );
        messageService.updateMessageConversation( mc );
        conversationIds.add( mc.getUid() );
        long conversationC = messageService.sendPrivateMessage( usersB, "Subject3", "Text", "Meta", null );
        mc = messageService.getMessageConversation( conversationC );
        messageService.updateMessageConversation( mc );
        conversationIds.add( mc.getUid() );
    }

    private User setupUser( String id )
    {
        User user = makeUser( id );
        userService.addUser( user );
        return user;
    }

    @Test
    void testGetMessageConversationsReturnsCorrectAmountOfConversations()
    {
        List<MessageConversation> msgsC = messageConversationStore.getMessageConversations( userC, null, false, false,
            null, null );
        List<MessageConversation> msgsB = messageConversationStore.getMessageConversations( userB, null, false, false,
            null, null );
        assertEquals( 3, msgsC.size() );
        assertEquals( 1, msgsB.size() );
    }

    @Test
    void testGetMessageConversationsReturnCorrectNumberOfMessages()
    {
        MessageConversation conversation = messageConversationStore.get( conversationA );
        sessionFactory.getCurrentSession().flush();
        assertTrue( (conversation.getMessageCount() == 4) );
    }

    @Test
    void testGetMessageConversations()
    {
        List<MessageConversation> conversations = messageConversationStore.getMessageConversations( conversationIds );
        assertEquals( 3, conversations.size() );
    }
}
