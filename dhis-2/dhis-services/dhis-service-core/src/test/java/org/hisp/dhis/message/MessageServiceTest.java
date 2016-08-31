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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Lars Helge Overland
 */
public class MessageServiceTest
    extends DhisSpringTest
{
    @Autowired
    private MessageService messageService;

    @Autowired
    private UserService _userService;
    
    private User sender;
    private User userA;
    private User userB;
   

    private Set<User> users;
    
    // -------------------------------------------------------------------------
    // Fixture
    // -------------------------------------------------------------------------

    @Override
    @Before
    public void setUpTest()
    {
        userService = _userService;
        
        sender = createUser( 'S' );
        userA = createUser( 'A' );
        userB = createUser( 'B' );
       

        userService.addUser( sender );
        userService.addUser( userA );
        userService.addUser( userB );
       
        
        users = new HashSet<>();
        users.add( userA );
        users.add( userB );        
    }

    @Test
    public void testSaveMessageConversationA()
    {
        MessageConversation conversationA = new MessageConversation( "SubjectA", sender );
        MessageConversation conversationB = new MessageConversation( "SubjectB", sender );
        
        int idA = messageService.saveMessageConversation( conversationA );
        int idB = messageService.saveMessageConversation( conversationB );
        
        conversationA = messageService.getMessageConversation( idA );
        conversationB = messageService.getMessageConversation( idB );
        
        assertNotNull( conversationA );
        assertEquals( "SubjectA", conversationA.getSubject() );

        assertNotNull( conversationB );
        assertEquals( "SubjectB", conversationB.getSubject() );
    }

    @Test
    public void testSaveMessageB()
    {
        MessageConversation conversation = new MessageConversation( "Subject", sender );
        
        UserMessage userMessageA = new UserMessage( userA );
        UserMessage userMessageB = new UserMessage( userB );
        
        conversation.addUserMessage( userMessageA );
        conversation.addUserMessage( userMessageB );
        
        Message contentA = new Message( "TextA", "MetaA", sender );
        Message contentB = new Message( "TextB", "MetaB", sender);
        
        conversation.addMessage( contentA );
        conversation.addMessage( contentB );
        
        int id = messageService.saveMessageConversation( conversation );
        
        conversation = messageService.getMessageConversation( id );
        
        assertNotNull( conversation );
        assertEquals( "Subject", conversation.getSubject() );
        assertEquals( 2, conversation.getUserMessages().size() );
        assertTrue( conversation.getUserMessages().contains( userMessageA ) );
        assertTrue( conversation.getUserMessages().contains( userMessageB ) );
        assertEquals( 2, conversation.getMessages().size() );
        assertTrue( conversation.getMessages().contains( contentA ) );
        assertTrue( conversation.getMessages().contains( contentB ) );
    }

    @Test
    public void testDeleteMessage()
    {
        MessageConversation conversation = new MessageConversation( "Subject", sender);
        
        UserMessage userMessageA = new UserMessage( userA );
        UserMessage userMessageB = new UserMessage( userB );
        
        conversation.addUserMessage( userMessageA );
        conversation.addUserMessage( userMessageB );
        
        Message contentA = new Message( "TextA", "MetaA", sender );
        Message contentB = new Message( "TextB", "MetaB", sender);
        
        conversation.addMessage( contentA );
        conversation.addMessage( contentB );
        
        int id = messageService.saveMessageConversation( conversation );
        
        conversation = messageService.getMessageConversation( id );
        
        assertNotNull( conversation );
        
        messageService.deleteMessages( userA );
        messageService.deleteMessages( userB );
        messageService.deleteMessages( sender );
    }

    @Test
    public void testSendMessage()
    {
        int id = messageService.sendMessage( "Subject", "Text", "Meta", users );
        
        MessageConversation conversation = messageService.getMessageConversation( id );
        
        assertNotNull( conversation );
        assertEquals( "Subject", conversation.getSubject() );
        assertEquals( 2, conversation.getUserMessages().size() );
        assertEquals( 1, conversation.getMessages().size() );
        assertTrue( conversation.getMessages().iterator().next().getText().equals( "Text" ) );
    }
    
    @Test
    public void testSendFeedback()
    {
        int id = messageService.sendFeedback( "Subject", "Text", "Meta" );
        
        MessageConversation conversation = messageService.getMessageConversation( id );
        
        assertNotNull( conversation );
        assertEquals( "Subject", conversation.getSubject() );
        assertEquals( 1, conversation.getMessages().size() );
        assertTrue( conversation.getMessages().iterator().next().getText().equals( "Text" ) );
    }
    
    @Test
    public void testSendReply()
    {
        MessageConversation message = new MessageConversation( "Subject", sender );
        message.addMessage( new Message( "TextA", "MetaA", sender) );
        int id = messageService.saveMessageConversation( message );
        
        messageService.sendReply( message, "TextB", "MetaB" );
        
        message = messageService.getMessageConversation( id );
        
        assertNotNull( message );
        assertEquals( "Subject", message.getSubject() );
        assertEquals( 2, message.getMessages().size() );       
    }

    @Test
    public void testGetMessageConversations()
    {
        MessageConversation conversationA = new MessageConversation( "SubjectA", sender );
        MessageConversation conversationB = new MessageConversation( "SubjectB", sender );
        MessageConversation conversationC = new MessageConversation( "SubjectC", userA );

        messageService.saveMessageConversation( conversationA );
        messageService.saveMessageConversation( conversationB );
        messageService.saveMessageConversation( conversationC );

        String uidA = conversationA.getUid();
        String uidB = conversationB.getUid();

        messageService.saveMessageConversation( conversationA );
        messageService.saveMessageConversation( conversationB );
        messageService.saveMessageConversation( conversationC );

        String[] uids = { uidA, uidB };

        List<MessageConversation> conversations = messageService.getMessageConversations( sender, uids );

        assertTrue( conversations.contains( conversationA ) );
        assertTrue( conversations.contains( conversationB ) );
        assertFalse( conversations.contains( conversationC ) );
    }
}
