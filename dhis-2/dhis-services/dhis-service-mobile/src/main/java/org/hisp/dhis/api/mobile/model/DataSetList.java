package org.hisp.dhis.api.mobile.model;

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

public class DataSetList
    extends Model
{
    private String clientVersion;

    private List<DataSet> addedDataSets = new ArrayList<>();

    private List<DataSet> deletedDataSets = new ArrayList<>();

    private List<DataSet> modifiedDataSets = new ArrayList<>();

    private List<DataSet> currentDataSets = new ArrayList<>();

    public DataSetList()
    {
    }

    public List<DataSet> getAddedDataSets()
    {
        return addedDataSets;
    }

    public void setAddedDataSets( List<DataSet> addedDataSets )
    {
        this.addedDataSets = addedDataSets;
    }

    public List<DataSet> getDeletedDataSets()
    {
        return deletedDataSets;
    }

    public void setDeletedDataSets( List<DataSet> deletedDataSets )
    {
        this.deletedDataSets = deletedDataSets;
    }

    public List<DataSet> getModifiedDataSets()
    {
        return modifiedDataSets;
    }

    public void setModifiedDataSets( List<DataSet> modifiedDataSets )
    {
        this.modifiedDataSets = modifiedDataSets;
    }

    public List<DataSet> getCurrentDataSets()
    {
        return currentDataSets;
    }

    public void setCurrentDataSets( List<DataSet> currentDataSets )
    {
        this.currentDataSets = currentDataSets;
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
            serializeVersion2_8( dout );
        }
        else if ( this.getClientVersion().equals( DataStreamSerializable.TWO_POINT_NINE ) )
        {
            serializeVersion2_9( dout );
        }
        else if ( this.getClientVersion().equals( DataStreamSerializable.TWO_POINT_TEN ) )
        {
            serializeVersion2_10( dout );
        }
    }

    @Override
    public void serializeVersion2_8( DataOutputStream dout )
        throws IOException
    {
        if ( addedDataSets != null )
        {
            dout.writeInt( addedDataSets.size() );
            for ( DataSet dataSet : addedDataSets )
            {
                dataSet.serializeVersion2_8( dout );
            }
        }
        else
        {
            dout.writeInt( 0 );
        }
        if ( deletedDataSets != null )
        {
            dout.writeInt( deletedDataSets.size() );
            for ( DataSet dataSet : deletedDataSets )
            {
                dataSet.serializeVersion2_8( dout );
            }
        }
        else
        {
            dout.writeInt( 0 );
        }
        if ( modifiedDataSets != null )
        {
            dout.writeInt( modifiedDataSets.size() );
            for ( DataSet dataSet : modifiedDataSets )
            {
                dataSet.serializeVersion2_8( dout );
            }
        }
        else
        {
            dout.writeInt( 0 );
        }
        if ( currentDataSets != null )
        {
            dout.writeInt( currentDataSets.size() );
            for ( DataSet dataSet : currentDataSets )
            {
                dataSet.serializeVersion2_8( dout );
            }
        }
        else
        {
            dout.writeInt( 0 );
        }
    }

    @Override
    public void serializeVersion2_9( DataOutputStream dout )
        throws IOException
    {
        if ( addedDataSets != null )
        {
            dout.writeInt( addedDataSets.size() );
            for ( DataSet dataSet : addedDataSets )
            {
                dataSet.serializeVersion2_9( dout );
            }
        }
        else
        {
            dout.writeInt( 0 );
        }
        if ( deletedDataSets != null )
        {
            dout.writeInt( deletedDataSets.size() );
            for ( DataSet dataSet : deletedDataSets )
            {
                dataSet.serializeVersion2_9( dout );
            }
        }
        else
        {
            dout.writeInt( 0 );
        }
        if ( modifiedDataSets != null )
        {
            dout.writeInt( modifiedDataSets.size() );
            for ( DataSet dataSet : modifiedDataSets )
            {
                dataSet.serializeVersion2_9( dout );
            }
        }
        else
        {
            dout.writeInt( 0 );
        }
        if ( currentDataSets != null )
        {
            dout.writeInt( currentDataSets.size() );
            for ( DataSet dataSet : currentDataSets )
            {
                dataSet.serializeVersion2_9( dout );
            }
        }
        else
        {
            dout.writeInt( 0 );
        }
    }

    @Override
    public void serializeVersion2_10( DataOutputStream dout )
        throws IOException
    {

        if ( addedDataSets != null )
        {
            dout.writeInt( addedDataSets.size() );
            for ( DataSet dataSet : addedDataSets )
            {
                dataSet.serializeVersion2_10( dout );
            }
        }
        else
        {
            dout.writeInt( 0 );
        }
        if ( deletedDataSets != null )
        {
            dout.writeInt( deletedDataSets.size() );
            for ( DataSet dataSet : deletedDataSets )
            {
                dataSet.serializeVersion2_10( dout );
            }
        }
        else
        {
            dout.writeInt( 0 );
        }
        if ( modifiedDataSets != null )
        {
            dout.writeInt( modifiedDataSets.size() );
            for ( DataSet dataSet : modifiedDataSets )
            {
                dataSet.serializeVersion2_10( dout );
            }
        }
        else
        {
            dout.writeInt( 0 );
        }
        if ( currentDataSets != null )
        {
            dout.writeInt( currentDataSets.size() );
            for ( DataSet dataSet : currentDataSets )
            {
                dataSet.serializeVersion2_10( dout );
            }
        }
        else
        {
            dout.writeInt( 0 );
        }
    }

    @Override
    public void deSerialize( DataInputStream dataInputStream )
        throws IOException
    {
        int temp = 0;
        temp = dataInputStream.readInt();
        if ( temp > 0 )
        {
            addedDataSets = new ArrayList<>();
            for ( int i = 0; i < temp; i++ )
            {
                DataSet dataSet = new DataSet();
                dataSet.deSerialize( dataInputStream );
                addedDataSets.add( dataSet );
            }
        }
        temp = dataInputStream.readInt();
        if ( temp > 0 )
        {
            deletedDataSets = new ArrayList<>();
            for ( int i = 0; i < temp; i++ )
            {
                DataSet dataSet = new DataSet();
                dataSet.deSerialize( dataInputStream );
                deletedDataSets.add( dataSet );
            }
        }
        temp = dataInputStream.readInt();
        if ( temp > 0 )
        {
            modifiedDataSets = new ArrayList<>();
            for ( int i = 0; i < temp; i++ )
            {
                DataSet dataSet = new DataSet();
                dataSet.deSerialize( dataInputStream );
                modifiedDataSets.add( dataSet );
            }
        }
        temp = dataInputStream.readInt();
        if ( temp > 0 )
        {
            currentDataSets = new ArrayList<>();
            for ( int i = 0; i < temp; i++ )
            {
                DataSet dataSet = new DataSet();
                dataSet.deSerialize( dataInputStream );
                currentDataSets.add( dataSet );
            }
        }
    }

}
