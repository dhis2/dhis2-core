package org.hisp.dhis.api.mobile.model;

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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class Conversation
    implements DataStreamSerializable
{
    private String clientVersion;

    private Collection<MessageConversation> conversations;

    private List<MessageConversation> conversationList = new ArrayList<>();

    private Collection<Message> messages;

    private List<Message> messageList = new ArrayList<>();

    public Conversation( Collection<Message> messages )
    {
        this.messages = messages;
    }

    public Conversation( String clientVersion, Collection<MessageConversation> conversations )
    {
        this.clientVersion = clientVersion;
        this.conversations = conversations;
    }

    public Conversation()
    {

    }

    public String getClientVersion()
    {
        return clientVersion;
    }

    public void setClientVersion( String clientVersion )
    {
        this.clientVersion = clientVersion;
    }

    public Collection<MessageConversation> getConversations()
    {
        return conversations;
    }

    public void setConversations( Collection<MessageConversation> conversations )
    {
        this.conversations = conversations;
    }

    public List<MessageConversation> getConversationList()
    {
        return conversationList;
    }

    public void setConversationList( List<MessageConversation> conversationList )
    {
        this.conversationList = conversationList;
    }

    public Collection<Message> getMessages()
    {
        return messages;
    }

    public void setMessages( Collection<Message> messages )
    {
        this.messages = messages;
    }

    public List<Message> getMessageList()
    {
        return messageList;
    }

    public void setMessageList( List<Message> messageList )
    {
        this.messageList = messageList;
    }

    @Override
    public void serialize( DataOutputStream dout )
        throws IOException
    {
        if ( conversations == null )
        {
            dout.writeInt( 0 );
        }
        else
        {
            dout.writeInt( conversations.size() );
            for ( MessageConversation conversation : conversations )
            {
                conversation.setClientVersion( DataStreamSerializable.TWO_POINT_TEN );
                conversation.serialize( dout );
            }
        }

        if ( messages == null )
        {
            dout.writeInt( 0 );
        }
        else
        {
            dout.writeInt( messages.size() );
            for ( Message message : messages )
            {
                message.setClientVersion( DataStreamSerializable.TWO_POINT_TEN );
                message.serialize( dout );
            }
        }

    }

    @Override
    public void deSerialize( DataInputStream din )
        throws IOException
    {
        int conversationSize = din.readInt();

        for ( int i = 0; i < conversationSize; i++ )
        {
            MessageConversation conversation = new MessageConversation();
            conversation.deSerialize( din );
            conversationList.add( conversation );
        }

        int messageSize = din.readInt();

        for ( int i = 0; i < messageSize; i++ )
        {
            Message message = new Message();
            message.deSerialize( din );
            messageList.add( message );
        }

    }

    @Override
    public void serializeVersion2_8( DataOutputStream dataOutputStream )
        throws IOException
    {

    }

    @Override
    public void serializeVersion2_9( DataOutputStream dataOutputStream )
        throws IOException
    {

    }

    @Override
    public void serializeVersion2_10( DataOutputStream dout )
        throws IOException
    {

    }

}
