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
import java.io.EOFException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.hisp.dhis.api.mobile.model.DataStreamSerializable;
import org.hisp.dhis.api.mobile.model.PatientAttribute;

/**
 * @author Nguyen Kim Lai
 */
public class Patient
    implements DataStreamSerializable
{
    private String clientVersion;

    private int id;
    
    private String organisationUnitName;
    
    private String trackedEntityName;

    private List<PatientAttribute> attributes = new ArrayList<>();

    private List<ProgramInstance> enrollmentPrograms = new ArrayList<>();

    private List<ProgramInstance> completedPrograms = new ArrayList<>();

    private List<Relationship> relationships = new ArrayList<>();

    private int idToAddRelative;
    
    private int relTypeIdToAdd;
    
    private Relationship enrollmentRelationship;


    // -------------------------------------------------------------------------
    // Getters and setters
    // -------------------------------------------------------------------------

    public List<Relationship> getRelationships()
    {
        return relationships;
    }

    public void setRelationships( List<Relationship> relationships )
    {
        this.relationships = relationships;
    }

    public List<PatientAttribute> getAttributes()
    {
        return attributes;
    }

    public void setAttributes( List<PatientAttribute> attributes )
    {
        this.attributes = attributes;
    }

    public int getId()
    {
        return id;
    }

    public void setId( int id )
    {
        this.id = id;
    }

    public String getClientVersion()
    {
        return clientVersion;
    }

    public void setClientVersion( String clientVersion )
    {
        this.clientVersion = clientVersion;
    }

    public String getOrganisationUnitName()
    {
        return organisationUnitName;
    }

    public void setOrganisationUnitName( String organisationUnitName )
    {
        this.organisationUnitName = organisationUnitName;
    }
    
    public String getTrackedEntityName()
    {
        return trackedEntityName;
    }
    
    public void setTrackedEntityName( String trackedEntityName )
    {
        this.trackedEntityName = trackedEntityName;
    }
    
    public List<ProgramInstance> getEnrollmentPrograms()
    {
        return enrollmentPrograms;
    }

    public void setEnrollmentPrograms( List<ProgramInstance> enrollmentPrograms )
    {
        this.enrollmentPrograms = enrollmentPrograms;
    }

    public List<ProgramInstance> getCompletedPrograms()
    {
        return completedPrograms;
    }

    public void setCompletedPrograms( List<ProgramInstance> completedPrograms )
    {
        this.completedPrograms = completedPrograms;
    }

    public int getIdToAddRelative()
    {
        return idToAddRelative;
    }

    public void setIdToAddRelative( int idToAddRelative )
    {
        this.idToAddRelative = idToAddRelative;
    }
    
    public int getRelTypeIdToAdd()
    {
        return relTypeIdToAdd;
    }
    
    public void setRelTypeIdToAdd( int relTypeIdToAdd )
    {
        this.relTypeIdToAdd = relTypeIdToAdd;
    }
    
    public Relationship getEnrollmentRelationship()
    {
        return enrollmentRelationship;
    }
    
    public void setEnrollmentRelationship( Relationship enrollmentRelationship )
    {
        this.enrollmentRelationship = enrollmentRelationship;
    }

    // -------------------------------------------------------------------------
    // Override Methods
    // -------------------------------------------------------------------------

    @Override
    public void serialize( DataOutputStream dout )
        throws IOException
    {
//        ByteArrayOutputStream bout = new ByteArrayOutputStream();
//        DataOutputStream dout = new DataOutputStream( bout );

        dout.writeInt( this.getId() );
        dout.writeUTF( this.getOrganisationUnitName() );
        dout.writeUTF( this.getTrackedEntityName() );

        // Write Patient Attribute
        dout.writeInt( attributes.size() );
        for ( PatientAttribute patientAtt : attributes )
        {
            patientAtt.serialize( dout );
        }

        // Write Enrollment Programs
        dout.writeInt( enrollmentPrograms.size() );
        for ( ProgramInstance program : enrollmentPrograms )
        {
            program.serialize( dout );
        }

        // Write completed Programs
        dout.writeInt( completedPrograms.size() );
        for ( ProgramInstance each : completedPrograms )
        {
            each.serialize( dout );
        }

        // Write Relationships
        dout.writeInt( relationships.size() );
        for ( Relationship each : relationships )
        {
            each.serialize( dout );
        }
        dout.writeInt( this.getIdToAddRelative() );
        dout.writeInt( this.getRelTypeIdToAdd() );
        if(enrollmentRelationship!=null)
        {
            dout.writeInt( 1 );
            enrollmentRelationship.serialize( dout );
        }
        else
        {
            dout.writeInt( 0 );
        }

//        bout.flush();
//        bout.writeTo( out );
    }

    @Override
    public void deSerialize( DataInputStream din )
        throws IOException, EOFException
    {
        this.setId( din.readInt() );
        this.setOrganisationUnitName( din.readUTF() );
        this.setTrackedEntityName( din.readUTF() );

        // Read Attribute
        int attsNumb = din.readInt();
        if ( attsNumb > 0 )
        {
            attributes = new ArrayList<>();
            for ( int j = 0; j < attsNumb; j++ )
            {
                PatientAttribute pa = new PatientAttribute();
                pa.deSerialize( din );
                attributes.add( pa );
            }
        }

        // Read enrollment programs
        int numbEnrollmentPrograms = din.readInt();
        if ( numbEnrollmentPrograms > 0 )
        {
            this.enrollmentPrograms = new ArrayList<>();
            for ( int i = 0; i < numbEnrollmentPrograms; i++ )
            {
                ProgramInstance program = new ProgramInstance();
                program.deSerialize( din );
                this.enrollmentPrograms.add( program );
            }
        }

        // Read completed programs
        int numbCompletedPrograms = din.readInt();
        if ( numbCompletedPrograms > 0 )
        {
            this.completedPrograms = new ArrayList<>();
            for ( int i = 0; i < numbCompletedPrograms; i++ )
            {
                ProgramInstance program = new ProgramInstance();
                program.deSerialize( din );
                this.completedPrograms.add( program );
            }
        }

        // Read relationships
        int numbRelationships = din.readInt();
        if ( numbRelationships > 0 )
        {
            this.relationships = new ArrayList<>();
            for ( int i = 0; i < numbRelationships; i++ )
            {
                Relationship relationship = new Relationship();
                relationship.deSerialize( din );
                this.relationships.add( relationship );
            }
        }
        this.setIdToAddRelative( din.readInt() );
        this.setRelTypeIdToAdd( din.readInt() );
        
        int numEnrollmentRelationships = din.readInt();
        if(numEnrollmentRelationships > 0)
        {
            Relationship enrollmentRelationship = new Relationship();
            enrollmentRelationship.deSerialize( din );
            this.setEnrollmentRelationship( enrollmentRelationship );
        }
    }

    @Override
    public void serializeVersion2_8( DataOutputStream out )
        throws IOException
    {
        // TODO Auto-generated method stub
    }

    @Override
    public void serializeVersion2_9( DataOutputStream dout )
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
