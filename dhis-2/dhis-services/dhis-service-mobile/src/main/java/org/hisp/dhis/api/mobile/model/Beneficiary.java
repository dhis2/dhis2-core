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

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@XmlAccessorType( XmlAccessType.FIELD )
public class Beneficiary
    implements DataStreamSerializable
{
    private String clientVersion;

    @XmlAttribute
    private int id;

    @XmlAttribute
    private String name;

    @XmlAttribute
    private int age;

    @XmlElementWrapper( name = "attributes" )
    @XmlElement( name = "attribute" )
    private List<PatientAttribute> patientAttValues;

    private PatientAttribute groupAttribute;

    private String gender;

    private Date birthDate;

    private Date registrationDate;

    private Character dobType;

    public int getAge()
    {
        return age;
    }

    public String getGender()
    {
        return gender;
    }

    public void setGender( String gender )
    {
        this.gender = gender;
    }

    public Date getBirthDate()
    {
        return birthDate;
    }

    public void setBirthDate( Date birthDate )
    {
        this.birthDate = birthDate;
    }

    public Date getRegistrationDate()
    {
        return registrationDate;
    }

    public void setRegistrationDate( Date registrationDate )
    {
        this.registrationDate = registrationDate;
    }

    public Character getDobType()
    {
        return dobType;
    }

    public void setDobType( Character dobType )
    {
        this.dobType = dobType;
    }

    public void setAge( int age )
    {
        this.age = age;
    }

    public PatientAttribute getGroupAttribute()
    {
        return groupAttribute;
    }

    public void setGroupAttribute( PatientAttribute groupAttribute )
    {
        this.groupAttribute = groupAttribute;
    }

    public List<PatientAttribute> getPatientAttValues()
    {
        return patientAttValues;
    }

    public void setPatientAttValues( List<PatientAttribute> patientAttValues )
    {
        this.patientAttValues = patientAttValues;
    }

    public int getId()
    {
        return id;
    }

    public void setId( int id )
    {
        this.id = id;
    }

    public String getName()
    {
        return name;
    }

    public void setName( String name )
    {
        this.name = name;
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
    public void serialize( DataOutputStream out )
        throws IOException
    {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        DataOutputStream dout = new DataOutputStream( bout );

        dout.writeInt( this.getId() );
        dout.writeUTF( this.getName() );
        dout.writeInt( this.getAge() );

        if ( gender != null )
        {
            dout.writeBoolean( true );
            dout.writeUTF( gender );
        }
        else
        {
            dout.writeBoolean( false );
        }

        if ( dobType != null )
        {
            dout.writeBoolean( true );
            dout.writeChar( dobType );
        }
        else
        {
            dout.writeBoolean( false );
        }

        if ( birthDate != null )
        {
            dout.writeBoolean( true );
            dout.writeLong( birthDate.getTime() );
        }
        else
        {
            dout.writeBoolean( false );
        }
        // doesn't transfer blood group to client
        dout.writeBoolean( false );

        if ( registrationDate != null )
        {
            dout.writeBoolean( true );
            dout.writeLong( registrationDate.getTime() );
        }
        else
        {
            dout.writeBoolean( false );
        }

        /*
         * Write attribute which is used as group factor of beneficiary - false:
         * no group factor, true: with group factor
         */
        if ( this.getGroupAttribute() != null )
        {
            dout.writeBoolean( true );
            this.getGroupAttribute().serialize( dout );
        }
        else
        {
            dout.writeBoolean( false );
        }

        List<PatientAttribute> atts = this.getPatientAttValues();
        dout.writeInt( atts.size() );
        for ( PatientAttribute att : atts )
        {
            dout.writeUTF( att.getName() + ":" + att.getValue() );
        }

        bout.flush();
        bout.writeTo( out );
    }

    @Override
    public void deSerialize( DataInputStream dataInputStream )
        throws IOException
    {

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

        if ( getClass() != otherObject.getClass() )
        {
            return false;
        }

        final Beneficiary otherBeneficiary = (Beneficiary) otherObject;

        if ( birthDate == null )
        {
            if ( otherBeneficiary.birthDate != null )
            {
                return false;
            }
        }
        else if ( !birthDate.equals( otherBeneficiary.birthDate ) )
        {
            return false;
        }

        if ( name == null )
        {
            if ( otherBeneficiary.name != null )
            {
                return false;
            }
        }
        else if ( !name.equals( otherBeneficiary.name ) )
        {
            return false;
        }

        if ( gender == null )
        {
            if ( otherBeneficiary.gender != null )
                return false;
        }
        else if ( !gender.equals( otherBeneficiary.gender ) )
        {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;

        result = prime * result + ((birthDate == null) ? 0 : birthDate.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((gender == null) ? 0 : gender.hashCode());

        return result;
    }

    @Override
    public void serializeVersion2_8( DataOutputStream out )
        throws IOException
    {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        DataOutputStream dout = new DataOutputStream( bout );

        dout.writeInt( this.getId() );
        dout.writeUTF( this.getName() );
        dout.writeInt( this.getAge() );

        if ( gender != null )
        {
            dout.writeBoolean( true );
            dout.writeUTF( gender );
        }
        else
        {
            dout.writeBoolean( false );
        }

        if ( dobType != null )
        {
            dout.writeBoolean( true );
            dout.writeChar( dobType );
        }
        else
        {
            dout.writeBoolean( false );
        }

        if ( birthDate != null )
        {
            dout.writeBoolean( true );
            dout.writeLong( birthDate.getTime() );
        }
        else
        {
            dout.writeBoolean( false );
        }
        // doesn't transfer blood group to client
        dout.writeBoolean( false );

        if ( registrationDate != null )
        {
            dout.writeBoolean( true );
            dout.writeLong( registrationDate.getTime() );
        }
        else
        {
            dout.writeBoolean( false );
        }

        /*
         * Write attribute which is used as group factor of beneficiary - false:
         * no group factor, true: with group factor
         */
        if ( this.getGroupAttribute() != null )
        {
            dout.writeBoolean( true );
            this.getGroupAttribute().serialize( dout );
        }
        else
        {
            dout.writeBoolean( false );
        }

        List<PatientAttribute> atts = this.getPatientAttValues();
        dout.writeInt( atts.size() );
        for ( PatientAttribute att : atts )
        {
            dout.writeUTF( att.getName() + ":" + att.getValue() );
        }

        bout.flush();
        bout.writeTo( out );
    }

    @Override
    public void serializeVersion2_9( DataOutputStream dout )
        throws IOException
    {
        dout.writeInt( this.getId() );
        dout.writeUTF( this.getName() );
        dout.writeInt( this.getAge() );

        if ( gender != null )
        {
            dout.writeBoolean( true );
            dout.writeUTF( gender );
        }
        else
        {
            dout.writeBoolean( false );
        }

        if ( dobType != null )
        {
            dout.writeBoolean( true );
            dout.writeChar( dobType );
        }
        else
        {
            dout.writeBoolean( false );
        }

        if ( birthDate != null )
        {
            dout.writeBoolean( true );
            dout.writeLong( birthDate.getTime() );
        }
        else
        {
            dout.writeBoolean( false );
        }
        // doesn't transfer blood group to client
        dout.writeBoolean( false );

        if ( registrationDate != null )
        {
            dout.writeBoolean( true );
            dout.writeLong( registrationDate.getTime() );
        }
        else
        {
            dout.writeBoolean( false );
        }

        /*
         * Write attribute which is used as group factor of beneficiary - false:
         * no group factor, true: with group factor
         */
        if ( this.getGroupAttribute() != null )
        {
            dout.writeBoolean( true );
            this.getGroupAttribute().serialize( dout );
        }
        else
        {
            dout.writeBoolean( false );
        }

        List<PatientAttribute> atts = this.getPatientAttValues();
        dout.writeInt( atts.size() );
        for ( PatientAttribute att : atts )
        {
            dout.writeUTF( att.getName() + ":" + att.getValue() );
        }

    }

    @Override
    public void serializeVersion2_10( DataOutputStream dataOutputStream )
        throws IOException
    {
        // TODO Auto-generated method stub

    }
}
