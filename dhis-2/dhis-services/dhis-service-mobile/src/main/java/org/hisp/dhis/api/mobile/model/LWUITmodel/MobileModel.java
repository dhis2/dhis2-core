package org.hisp.dhis.api.mobile.model.LWUITmodel;

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
import java.util.Date;
import java.util.List;

import org.hisp.dhis.api.mobile.model.DataStreamSerializable;

/**
 * @author Nguyen Kim Lai
 * 
 * @version MobileModel.java 2:57:06 PM Jul 1, 2013 $
 */
public class MobileModel
    implements DataStreamSerializable
{
    private String clientVersion;

    private List<Program> programs;
    
    private List<RelationshipType> relationshipTypes;

    private Date serverCurrentDate;

    @Override
    public void serialize( DataOutputStream dout )
        throws IOException
    {
        if ( programs == null )
        {
            dout.writeInt( 0 );
        }
        else
        {
            dout.writeInt( programs.size() );
            for ( Program prog : programs )
            {
                prog.setClientVersion( DataStreamSerializable.TWO_POINT_TEN );
                prog.serialize( dout );
            }
        }
        // Write current server's date
        dout.writeLong( serverCurrentDate.getTime() );
        
        if(relationshipTypes == null)
        {
            dout.writeInt( 0 );
        }
        else
        {
            dout.writeInt( relationshipTypes.size() );
            for (RelationshipType relType:relationshipTypes)
            {
                relType.serialize( dout );
            }
        }
    }

    public List<Program> getPrograms()
    {
        return programs;
    }

    public void setPrograms( List<Program> programs )
    {
        this.programs = programs;
    }

    public Date getServerCurrentDate()
    {
        return serverCurrentDate;
    }

    public void setServerCurrentDate( Date serverCurrentDate )
    {
        this.serverCurrentDate = serverCurrentDate;
    }

    public String getClientVersion()
    {
        return clientVersion;
    }

    public void setClientVersion( String clientVersion )
    {
        this.clientVersion = clientVersion;
    }
    
    public List<RelationshipType> getRelationshipTypes()
    {
        return relationshipTypes;
    }
    
    public void setRelationshipTypes(List<RelationshipType> relationshipTypes)
    {
        this.relationshipTypes = relationshipTypes;
    }

    @Override
    public void deSerialize( DataInputStream dataInputStream )
        throws IOException
    {
        // TODO Auto-generated method stub
        
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
