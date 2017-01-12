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

import org.hisp.dhis.api.mobile.model.Model;

 /**
 * @author Nguyen Kim Lai
 */
public class Relationship extends Model
{
    private String clientVersion;
    
    private String personAName;
    
    private String personBName;
    
    private int personAId;
    
    private int personBId;
    
    private String chosenRelationship;
    
    private String aIsToB;

    private String bIsToA;
    
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
    
    public String getPersonAName()
    {
        return personAName;
    }

    public void setPersonAName( String personAName )
    {
        this.personAName = personAName;
    }

    public String getPersonBName()
    {
        return personBName;
    }

    public void setPersonBName( String personBName )
    {
        this.personBName = personBName;
    }
    
    public int getPersonAId()
    {
        return personAId;
    }

    public void setPersonAId( int personAId )
    {
        this.personAId = personAId;
    }

    public int getPersonBId()
    {
        return personBId;
    }

    public void setPersonBId( int personBId )
    {
        this.personBId = personBId;
    }

    public String getChosenRelationship()
    {
        return chosenRelationship;
    }

    public void setChosenRelationship( String chosenRelationship )
    {
        this.chosenRelationship = chosenRelationship;
    }

    public String getaIsToB()
    {
        return aIsToB;
    }

    public void setaIsToB( String aIsToB )
    {
        this.aIsToB = aIsToB;
    }

    public String getbIsToA()
    {
        return bIsToA;
    }

    public void setbIsToA( String bIsToA )
    {
        this.bIsToA = bIsToA;
    }

    @Override
    public void serialize( DataOutputStream dout )
        throws IOException
    {
        dout.writeUTF( this.getName() );
         
        if( this.getPersonAName() != null )
        {
            dout.writeBoolean( true );
            dout.writeUTF( this.getPersonAName() );
        }
        else
        {
            dout.writeBoolean( false );
        }
        
        if( this.getPersonBName() != null )
        {
            dout.writeBoolean( true );
            dout.writeUTF( this.getPersonBName() );
        }
        else
        {
            dout.writeBoolean( false );
        }
        
        // for enrollment relationship, attributes below belong to relationship type
        if( this.getId() != 0 )
        {
            dout.writeBoolean( true );
            dout.writeInt( this.getId() );
        }
        else
        {
            dout.writeBoolean( false );
        }
        
        // relationship between A and B
        if( this.getaIsToB() != null )
        {
            dout.writeBoolean( true );
            dout.writeUTF( this.getaIsToB() );
        }
        else
        {
            dout.writeBoolean( false );
        }
        
        if( this.getbIsToA() != null )
        {
            dout.writeBoolean( true );
            dout.writeUTF( this.getbIsToA() );
        }
        else
        {
            dout.writeBoolean( false );
        }
        
        // A and B id
        if( this.getPersonAId() != 0 )
        {
            dout.writeBoolean( true );
            dout.writeInt( this.getPersonAId() );
        }
        else
        {
            dout.writeBoolean( false );
        }
        
        if( this.getPersonBId() != 0 )
        {
            dout.writeBoolean( true );
            dout.writeInt( this.getPersonBId() );
        }
        else
        {
            dout.writeBoolean( false );
        }
        
        if( this.getChosenRelationship() != null )
        {
            dout.writeBoolean( true );
            dout.writeUTF( this.getChosenRelationship() );
        }
        else
        {
            dout.writeBoolean( false );
        }
    }
    
    @Override
    public void deSerialize( DataInputStream dint )
        throws IOException
    {
        this.setName( dint.readUTF() );
        if ( dint.readBoolean() == true )
        {
            this.setPersonAName( dint.readUTF() );
        }
        else
        {
            this.setPersonAName( null );
        }
        
        if ( dint.readBoolean() == true )
        {
            this.setPersonBName( dint.readUTF() );
        }
        else
        {
            this.setPersonBName( null );
        }
        
        if ( dint.readBoolean() == true )
        {
            this.setId( dint.readInt() );
        }
        else
        {
            this.setId( 0 );
        }
        
        if ( dint.readBoolean() == true )
        {
            this.setaIsToB( dint.readUTF() );
        }
        else
        {
            this.setaIsToB( null );
        }
        
        if ( dint.readBoolean() == true )
        {
            this.setbIsToA( dint.readUTF() );
        }
        else
        {
            this.setbIsToA( null );
        }
        
        if ( dint.readBoolean() == true )
        {
            this.setPersonAId( dint.readInt() );
        }
        else
        {
            this.setPersonAId( 0 );
        }
        
        if ( dint.readBoolean() == true )
        {
            this.setPersonBId( dint.readInt() );
        }
        else
        {
            this.setPersonBId( 0 );
        }
        
        if ( dint.readBoolean() == true )
        {
            this.setChosenRelationship( dint.readUTF() );
        }
        else
        {
            this.setChosenRelationship( null );
        }
    }
}
