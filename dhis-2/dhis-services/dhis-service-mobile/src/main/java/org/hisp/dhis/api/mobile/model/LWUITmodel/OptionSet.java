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
import java.util.ArrayList;
import java.util.List;

import org.hisp.dhis.api.mobile.model.Model;

/**
 * @author Nguyen Kim Lai
 * 
 * @version OptionSet.java 3:42:54 PM Mar 12, 2013 $
 */
public class OptionSet
    extends Model
{
private String clientVersion;
    
    private List<String> options = new ArrayList<>();

    public List<String> getOptions()
    {
        return options;
    }

    public void setOptions( List<String> options )
    {
        this.options = options;
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

        dout.writeInt( this.getId() );
        dout.writeUTF( this.getName() );
        dout.writeInt( this.options.size() );

        for ( String option : this.options )
        {
            dout.writeUTF( option );
        }

    }
    
    @Override
    public void deSerialize( DataInputStream dint )
        throws IOException
    {
        int id = dint.readInt();
        if ( id != 0 )
        {
            this.setId( id );
            this.setName( dint.readUTF() );
            int optionSize = dint.readInt();
            if ( optionSize > 0 )
            {
                for ( int i = 0; i < optionSize; i++ )
                {
                    String option = dint.readUTF();
                    options.add( option );
                }
            }
        }
        else
        {
            this.setId( id );
        }
    }
}
