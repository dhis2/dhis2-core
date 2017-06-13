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
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;

public class DataSet
    extends Model
    implements Comparable<DataSet>
{
    private String clientVersion;

    private String periodType;

    private List<Section> sections;

    private int version;

    public DataSet()
    {
    }

    public DataSet( DataSet dataSet )
    {
        this.setId( dataSet.getId() );
        this.setName( dataSet.getName() );
        this.periodType = dataSet.getPeriodType();
        this.sections = dataSet.getSections();
        this.version = dataSet.getVersion();
    }

    public String getPeriodType()
    {
        return periodType;
    }

    public void setPeriodType( String periodType )
    {
        this.periodType = periodType;
    }

    public int getVersion()
    {
        return version;
    }

    public void setVersion( int version )
    {
        this.version = version;
    }

    @XmlElementWrapper( name = "sections" )
    @XmlElement( name = "section" )
    public List<Section> getSections()
    {
        return sections;
    }

    public void setSections( List<Section> sections )
    {
        this.sections = sections;
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
    public void serializeVersion2_8( DataOutputStream dout )
        throws IOException
    {
        dout.writeInt( this.getId() );
        dout.writeUTF( this.getName() );
        dout.writeInt( this.getVersion() );
        dout.writeUTF( this.getPeriodType() );

        if ( this.sections == null )
        {
            dout.writeInt( 0 );
        }
        else
        {
            dout.writeInt( this.sections.size() );
            for ( Section section : this.sections )
            {
                section.setClientVersion( TWO_POINT_EIGHT );
                section.serialize( dout );
            }
        }
    }

    @Override
    public void serializeVersion2_9( DataOutputStream dout )
        throws IOException
    {
        dout.writeInt( this.getId() );
        dout.writeUTF( this.getName() );
        dout.writeInt( this.getVersion() );
        dout.writeUTF( this.getPeriodType() );

        if ( this.sections == null )
        {
            dout.writeInt( 0 );
        }
        else
        {
            dout.writeInt( this.sections.size() );
            for ( Section section : this.sections )
            {
                section.setClientVersion( TWO_POINT_NINE );
                section.serialize( dout );
            }
        }
    }

    @Override
    public void deSerialize( DataInputStream dataInputStream )
        throws IOException
    {
        this.setId( dataInputStream.readInt() );
        this.setName( dataInputStream.readUTF() );
        this.setVersion( dataInputStream.readInt() );
        this.setPeriodType( dataInputStream.readUTF() );

        int sectionSize = dataInputStream.readInt();

        for ( int i = 0; i < sectionSize; i++ )
        {
            Section section = new Section();
            section.deSerialize( dataInputStream );
            sections.add( section );
        }
    }

    @Override
    public void serializeVersion2_10( DataOutputStream dout )
        throws IOException
    {
        dout.writeInt( this.getId() );
        dout.writeUTF( this.getName() );
        dout.writeInt( this.getVersion() );
        dout.writeUTF( this.getPeriodType() );

        if ( this.sections == null )
        {
            dout.writeInt( 0 );
        }
        else
        {
            dout.writeInt( this.sections.size() );
            for ( Section section : this.sections )
            {
                section.setClientVersion( TWO_POINT_TEN );
                section.serialize( dout );
            }
        }
    }

    @Override
    public int hashCode()
    {
        return this.getId();
    }

    @Override
    public boolean equals( Object otherObject )
    {

        if ( this == otherObject )
        {
            return true;
        }

        if ( otherObject == null )
        {
            return false;
        }

        if ( ((DataSet) otherObject).getId() == this.getId() )
            return true;
        return false;
    }

    @Override
    public int compareTo( DataSet ds )
    {
        if ( this.getId() > ds.getId() )
        {
            return 1;
        }
        else if ( this.getId() < ds.getId() )
        {
            return -1;
        }
        else
        {
            return 0;
        }
    }

}
