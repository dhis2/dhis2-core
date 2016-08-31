package org.hisp.dhis.api.mobile.model.LWUITmodel;

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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.hisp.dhis.api.mobile.model.Model;

 /**
 * @author Nguyen Kim Lai
 *
 * @version LostEvent.java 4:11:08 PM Oct 8, 2013 $
 */
public class LostEvent extends Model
{
    private String dueDate;
    
    private int status;
    
    private boolean isRiskCase;
    
    private String comment;
    
    private String SMS;

    @Override
    public void serialize( DataOutputStream dout )
        throws IOException
    {
        super.serialize( dout );
        dout.writeUTF( dueDate );
        dout.writeInt( status );
        dout.writeBoolean( isRiskCase );
        dout.writeUTF( comment );
        dout.writeUTF( SMS );
    }

    @Override
    public void deSerialize( DataInputStream dint )
        throws IOException
    {
        super.deSerialize( dint );
        dueDate = dint.readUTF();
        status = dint.readInt();
        isRiskCase = dint.readBoolean();
        if ( dint.readBoolean() == true )
        {
            comment = dint.readUTF();
        }
        else
        {
            comment = null;
        }
        
        if ( dint.readBoolean() == true )
        {
            SMS = dint.readUTF();
        }
        else
        {
            SMS = null;
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

    public String getDueDate()
    {
        return dueDate;
    }

    public void setDueDate( String dueDate )
    {
        this.dueDate = dueDate;
    }

    public int getStatus()
    {
        return status;
    }

    public void setStatus( int status )
    {
        this.status = status;
    }

    public boolean isRiskCase()
    {
        return isRiskCase;
    }

    public void setRiskCase( boolean isRiskCase )
    {
        this.isRiskCase = isRiskCase;
    }

    public String getComment()
    {
        return comment;
    }

    public void setComment( String comment )
    {
        this.comment = comment;
    }

    public String getSMS()
    {
        return SMS;
    }

    public void setSMS( String sMS )
    {
        SMS = sMS;
    }
}
