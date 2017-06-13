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

public class Recipient
    implements DataStreamSerializable
{
    private String clientVersion;

    private Collection<User> users;

    private List<User> userList = new ArrayList<>();

    public String getClientVersion()
    {
        return clientVersion;
    }

    public void setClientVersion( String clientVersion )
    {
        this.clientVersion = clientVersion;
    }

    public Collection<User> getUsers()
    {
        return users;
    }

    public void setUsers( Collection<User> users )
    {
        this.users = users;
    }

    public List<User> getUserList()
    {
        return userList;
    }

    public void setUserList( List<User> userList )
    {
        this.userList = userList;
    }

    @Override
    public void serialize( DataOutputStream dout )
        throws IOException
    {
        if ( users == null )
        {
            dout.writeInt( 0 );
        }
        else
        {
            dout.writeInt( users.size() );
            for ( User user : users )
            {
                user.setClientVersion( DataStreamSerializable.TWO_POINT_TEN );
                user.serialize( dout );
            }
        }

    }

    @Override
    public void deSerialize( DataInputStream dataInputStream )
        throws IOException
    {
        int userSize = dataInputStream.readInt();

        for ( int i = 0; i < userSize; i++ )
        {
            User user = new User();
            user.deSerialize( dataInputStream );
            userList.add( user );
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

        if ( users == null )
        {
            dout.writeInt( 0 );
        }
        else
        {
            dout.writeInt( users.size() );
            for ( User user : users )
            {
                user.setClientVersion( DataStreamSerializable.TWO_POINT_TEN );
                user.serialize( dout );
            }
        }

    }

}
