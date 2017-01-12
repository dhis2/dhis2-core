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
import java.util.Date;

import javax.xml.bind.annotation.XmlAttribute;

public class Activity
    implements DataStreamSerializable
{
    private String clientVersion;

    private Beneficiary beneficiary;

    private boolean late = false;

    private Task task;

    private Date dueDate;

    private Date expireDate;

    public Beneficiary getBeneficiary()
    {
        return beneficiary;
    }

    public void setBeneficiary( Beneficiary beneficiary )
    {
        this.beneficiary = beneficiary;
    }

    public Task getTask()
    {
        return task;
    }

    public void setTask( Task task )
    {
        this.task = task;
    }

    public Date getDueDate()
    {
        return dueDate;
    }

    public void setDueDate( Date dueDate )
    {
        this.dueDate = dueDate;
    }

    @XmlAttribute
    public boolean isLate()
    {
        return late;
    }

    public void setLate( boolean late )
    {
        this.late = late;
    }

    public Date getExpireDate()
    {
        return expireDate;
    }

    public void setExpireDate( Date expireDate )
    {
        this.expireDate = expireDate;
    }

    public String getClientVersion()
    {
        return clientVersion;
    }

    public void setClientVersion( String clientVersion )
    {
        this.clientVersion = clientVersion;
    }

    @Override
    public void serialize( DataOutputStream dout )
        throws IOException
    {
        if ( this.getClientVersion().equals( DataStreamSerializable.TWO_POINT_EIGHT ) )
        {
            this.serializeVersion2_8( dout );
        }
        else if ( this.getClientVersion().equals( DataStreamSerializable.TWO_POINT_NINE ) )
        {
            this.serializeVersion2_9( dout );
        }
        else if ( this.getClientVersion().equals( DataStreamSerializable.TWO_POINT_TEN ) )
        {
            this.serializeVersion2_10( dout );
        }
    }

    @Override
    public void deSerialize( DataInputStream dataInputStream )
        throws IOException
    {
        this.task = new Task();
        task.deSerialize( dataInputStream );

        this.beneficiary = new Beneficiary();
        beneficiary.deSerialize( dataInputStream );

        this.late = dataInputStream.readBoolean();
        this.dueDate = new Date( dataInputStream.readLong() );
        this.expireDate = new Date( dataInputStream.readLong() );
    }

    @Override
    public void serializeVersion2_8( DataOutputStream dout )
        throws IOException
    {
        this.task.setClientVersion( TWO_POINT_EIGHT );
        this.getTask().serialize( dout );
        this.getBeneficiary().serializeVersion2_8( dout );
        dout.writeBoolean( late );
        dout.writeLong( this.getDueDate().getTime() );
        dout.writeLong( this.getExpireDate().getTime() );
    }

    @Override
    public void serializeVersion2_9( DataOutputStream dout )
        throws IOException
    {
        this.task.setClientVersion( TWO_POINT_NINE );
        this.getTask().serialize( dout );
        this.getBeneficiary().serializeVersion2_9( dout );
        dout.writeBoolean( late );
        dout.writeLong( this.getDueDate().getTime() );
        dout.writeLong( this.getExpireDate().getTime() );

    }

    @Override
    public void serializeVersion2_10( DataOutputStream dout )
        throws IOException
    {
        this.task.setClientVersion( TWO_POINT_TEN );
        this.getTask().serialize( dout );
        this.getBeneficiary().serializeVersion2_9( dout );
        dout.writeBoolean( late );
        dout.writeLong( this.getDueDate().getTime() );
        dout.writeLong( this.getExpireDate().getTime() );
    }

}
