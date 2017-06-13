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

public class Interpretation
    implements DataStreamSerializable

{
    private int id;

    private String text;

    private Collection<InterpretationComment> inComments;

    private List<InterpretationComment> interCommentList = new ArrayList<>();

    public int getId()
    {
        return id;
    }

    public void setId( int id )
    {
        this.id = id;
    }

    public String getText()
    {
        return text;
    }

    public void setText( String text )
    {
        this.text = text;
    }

    public Collection<InterpretationComment> getInComments()
    {
        return inComments;
    }

    public void setInComments( Collection<InterpretationComment> inComments )
    {
        this.inComments = inComments;
    }

    public List<InterpretationComment> getInterCommentList()
    {
        return interCommentList;
    }

    public void setInterCommentList( List<InterpretationComment> interCommentList )
    {
        this.interCommentList = interCommentList;
    }

    @Override
    public void serialize( DataOutputStream dout )
        throws IOException
    {
        dout.writeInt( this.getId() );
        dout.writeUTF( this.getText() );

        if ( inComments == null )
        {
            dout.writeInt( 0 );
        }
        else
        {
            dout.writeInt( inComments.size() );
            for ( InterpretationComment interpretation : inComments )
            {
                interpretation.serialize( dout );
            }
        }

    }

    @Override
    public void deSerialize( DataInputStream dataInputStream )
        throws IOException
    {
        id = dataInputStream.readInt();
        text = dataInputStream.readUTF();

        int interCommentSize = dataInputStream.readInt();

        for ( int i = 0; i < interCommentSize; i++ )
        {
            InterpretationComment interComment = new InterpretationComment();
            interComment.deSerialize( dataInputStream );
            interCommentList.add( interComment );
        }

    }

    @Override
    public void serializeVersion2_8( DataOutputStream dataOutputStream )
        throws IOException
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void serializeVersion2_9( DataOutputStream dataOutputStream )
        throws IOException
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void serializeVersion2_10( DataOutputStream dataOutputStream )
        throws IOException
    {
        // TODO Auto-generated method stub

    }

}
