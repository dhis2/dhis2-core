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
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class DataSetValue
    extends Model
{
    private String clientVersion;

    private String periodName;

    private boolean completed;

    private List<DataValue> dataValues = new ArrayList<>();

    @XmlAttribute
    public boolean isCompleted()
    {
        return completed;
    }

    public void setCompleted( boolean completed )
    {
        this.completed = completed;
    }

    @XmlAttribute
    public String getPeriodName()
    {
        return periodName;
    }

    public void setPeriodName( String periodName )
    {
        this.periodName = periodName;
    }

    public void setDataValues( List<DataValue> dataValues )
    {
        this.dataValues = dataValues;
    }

    @XmlElement( name = "dataValue" )
    public List<DataValue> getDataValues()
    {
        return dataValues;
    }

    @Override
    public String getClientVersion()
    {
        return clientVersion;
    }

    @Override
    public void setClientVersion( String clientVersion )
    {
        this.clientVersion = clientVersion;
    }

    @Override
    public void serialize( DataOutputStream dataOutputStream )
        throws IOException
    {
        dataOutputStream.writeInt( this.getId() );
        dataOutputStream.writeUTF( this.getName() );
        dataOutputStream.writeUTF( this.getPeriodName() );
        dataOutputStream.writeBoolean( this.isCompleted() );

        dataOutputStream.writeInt( dataValues.size() );

        for ( DataValue datavalue : dataValues )
        {
            dataOutputStream.writeInt( datavalue.getId() );
            dataOutputStream.writeInt( datavalue.getCategoryOptComboID() );
            dataOutputStream.writeUTF( datavalue.getValue() );
        }

    }

    @Override
    public void deSerialize( DataInputStream din )
        throws IOException
    {
        this.setId( din.readInt() );
        this.setName( din.readUTF() );
        this.setPeriodName( din.readUTF() );
        this.setCompleted( din.readBoolean() );
        int size = din.readInt();

        for ( int i = 0; i < size; i++ )
        {
            DataValue dv = new DataValue();
            dv.setId( din.readInt() );
            dv.setCategoryOptComboID( din.readInt() );
            dv.setValue( din.readUTF() );
            this.dataValues.add( dv );
        }

    }

}
