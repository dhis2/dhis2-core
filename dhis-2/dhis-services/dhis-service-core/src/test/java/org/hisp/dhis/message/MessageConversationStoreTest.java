package org.hisp.dhis.message;

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

import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Stian Sandvold
 */
public class MessageConversationStoreTest
    extends DhisSpringTest
{
    @Autowired
    private MessageConversationStore messageConversationStore;

    @Autowired
    private MessageService messageService;

    @Autowired
    private UserService _userService;

    private User userB;

    private User userC;

    private int conversationA;

    private Collection<String> conversationIds;

    // -------------------------------------------------------------------------
    // Fixture
    // -------------------------------------------------------------------------

    @Override
    public void setUpTest()
    {
        userService = _userService;

        // 'A' used as currentUser
        setupUser( 'A' );
        userB = setupUser( 'B' );
        userC = setupUser( 'C' );

        Set<User> usersA = new HashSet<>();
        usersA.add( userC );

        Set<User> usersB = new HashSet<>();
        usersB.add( userC );
        usersB.add( userB );

        conversationIds = new HashSet<>();

        conversationA = messageService.sendPrivateMessage( "Subject1", "Text", "Meta", usersA );
        MessageConversation mc = messageService.getMessageConversation( conversationA );
        mc.markRead( userC );
        messageService.updateMessageConversation( mc );
        conversationIds.add( mc.getUid() );

        messageService.sendReply( mc, "Message 1", "Meta", false );
        messageService.sendReply( mc, "Message 2", "Meta", false );
        messageService.sendReply( mc, "Message 3", "Meta", false );

        int conversationB = messageService.sendPrivateMessage( "Subject2", "Text", "Meta", usersA );
        mc = messageService.getMessageConversation( conversationB );
        mc.setFollowUp( true );
        messageService.updateMessageConversation( mc );
        conversationIds.add( mc.getUid() );

        int conversationC = messageService.sendPrivateMessage( "Subject3", "Text", "Meta", usersB );
        mc = messageService.getMessageConversation( conversationC );
        messageService.updateMessageConversation( mc );
        conversationIds.add( mc.getUid() );

    }

    private User setupUser( char id )
    {
        User user = createUser( id );
        userService.addUser( user );
        userService.addUserCredentials( user.getUserCredentials() );
        return user;
    }

    @Test
    public void testGetMessageConversationsReturnsCorrectAmountOfConversations()
    {
        List<MessageConversation> msgsC = messageConversationStore
            .getMessageConversations( userC, null, false, false, null, null );
        List<MessageConversation> msgsB = messageConversationStore
            .getMessageConversations( userB, null, false, false, null, null );

        assertEquals( 3, msgsC.size() );
        assertEquals( 1, msgsB.size() );
    }

    @Test
    public void testGetMessageConversationsReturnCorrectNumberOfMessages()
    {
        MessageConversation conversation = messageConversationStore.get( conversationA );

        assertTrue( (conversation.getMessageCount() == 4) );
    }

    @Test
    public void testGetMessageConversations()
    {
        List<MessageConversation> conversations = messageConversationStore.getMessageConversations( conversationIds );

        assertEquals( 3, conversations.size() );
    }

}
