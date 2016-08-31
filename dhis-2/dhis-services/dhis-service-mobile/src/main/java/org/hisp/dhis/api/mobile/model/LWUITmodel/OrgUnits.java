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
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.hisp.dhis.api.mobile.model.DataStreamSerializable;

@XmlRootElement
public class OrgUnits
    implements DataStreamSerializable
{
    private String clientVersion;

    private List<MobileOrgUnitLinks> orgUnits = new ArrayList<>();

    public OrgUnits()
    {
    }

    public OrgUnits( List<MobileOrgUnitLinks> unitList )
    {
        this.orgUnits = unitList;
    }

    @XmlElement( name = "orgUnit" )
    public List<MobileOrgUnitLinks> getOrgUnits()
    {
        return orgUnits;
    }

    public void setOrgUnits( List<MobileOrgUnitLinks> orgUnits )
    {
        this.orgUnits = orgUnits;
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
    public void serialize( DataOutputStream dataOutputStream )
        throws IOException
    {
        if ( this.getClientVersion().equals( DataStreamSerializable.TWO_POINT_EIGHT ) )
        {
            this.serializeVersion2_8( dataOutputStream );
        }
        else if ( this.getClientVersion().equals( DataStreamSerializable.TWO_POINT_NINE ) )
        {
            this.serializeVersion2_9( dataOutputStream );
        }
        else
        {
            this.serializeVersion2_10( dataOutputStream );
        }
    }

    @Override
    public void deSerialize( DataInputStream dataInputStream )
        throws IOException
    {
        orgUnits = new ArrayList<>();
        dataInputStream.readDouble(); // TODO fix
        int size = dataInputStream.readInt();

        for ( int i = 0; i < size; i++ )
        {
            MobileOrgUnitLinks unit = new MobileOrgUnitLinks();
            unit.deSerialize( dataInputStream );
            orgUnits.add( unit );
        }
    }

    @Override
    public void serializeVersion2_8( DataOutputStream dataOutputStream )
        throws IOException
    {
        dataOutputStream.writeInt( orgUnits.size() );
        for ( MobileOrgUnitLinks unit : orgUnits )
        {
            unit.serializeVersion2_8( dataOutputStream );
        }
    }

    @Override
    public void serializeVersion2_9( DataOutputStream dataOutputStream )
        throws IOException
    {
        // send the current version to client for updating or not
        dataOutputStream.writeDouble( MobileOrgUnitLinks.currentVersion );
        dataOutputStream.writeInt( orgUnits.size() );
        for ( MobileOrgUnitLinks unit : orgUnits )
        {
            unit.serializeVersion2_9( dataOutputStream );
        }
    }

    @Override
    public void serializeVersion2_10( DataOutputStream dataOutputStream )
        throws IOException
    {
        // send the current version to client for updating or not
        dataOutputStream.writeDouble( MobileOrgUnitLinks.currentVersion );
        dataOutputStream.writeInt( orgUnits.size() );
        for ( MobileOrgUnitLinks unit : orgUnits )
        {
            unit.serializeVersion2_10( dataOutputStream );
        }
        
    }

}
