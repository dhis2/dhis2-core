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

import org.hisp.dhis.api.mobile.model.Model;
import org.hisp.dhis.api.mobile.model.PatientAttribute;

/**
 * @author Nguyen Kim Lai
 */
public class Program
    extends Model
{
    public static final String WITH_REGISTRATION = "with_registration";

    public static final String WITHOUT_REGISTRATION = "without_registration";

    // Work as Program and ProgramInstance
    private String clientVersion;

    private int version;

    // multiple event with registration: 1
    // single event with registration: 2
    // single event without registration: 3

    private Integer type;

    private String dateOfEnrollmentDescription = "Enrollment Date";

    private String dateOfIncidentDescription = "Incident Date";

    private String trackedEntityName = "Tracked Entity";

    private List<ProgramStage> programStages = new ArrayList<>();

    private List<PatientAttribute> programAttributes = new ArrayList<>();

    private String relationshipText;

    private int relatedProgramId;

    private int relationshipType;

    public List<ProgramStage> getProgramStages()
    {
        return programStages;
    }

    public void setProgramStages( List<ProgramStage> programStages )
    {
        this.programStages = programStages;
    }

    public int getVersion()
    {
        return version;
    }

    public void setVersion( int version )
    {
        this.version = version;
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

    public Integer getType()
    {
        return type;
    }

    public void setType( Integer type )
    {
        this.type = type;
    }

    public void setType( String type )
    {
        if ( type.equalsIgnoreCase( WITH_REGISTRATION ) )
        {
            this.setType( 1 );
        }
        else
        {
            this.setType( 3 );
        }
    }

    public String getDateOfEnrollmentDescription()
    {
        return dateOfEnrollmentDescription;
    }

    public void setDateOfEnrollmentDescription( String dateOfEnrollmentDescription )
    {
        if ( dateOfEnrollmentDescription != null )
        {
            this.dateOfEnrollmentDescription = dateOfEnrollmentDescription;
        }
    }

    public String getDateOfIncidentDescription()
    {
        return dateOfIncidentDescription;
    }

    public void setDateOfIncidentDescription( String dateOfIncidentDescription )
    {
        if ( dateOfIncidentDescription != null )
        {
            this.dateOfIncidentDescription = dateOfIncidentDescription;
        }
    }

    public String getTrackedEntityName()
    {
        return trackedEntityName;
    }

    public void setTrackedEntityName( String trackedEntityName )
    {
        this.trackedEntityName = trackedEntityName;
    }

    public List<PatientAttribute> getProgramAttributes()
    {
        return programAttributes;
    }

    public void setProgramAttributes( List<PatientAttribute> programAttributes )
    {
        this.programAttributes = programAttributes;
    }

    public String getRelationshipText()
    {
        return relationshipText;
    }

    public void setRelationshipText( String relationshipText )
    {
        this.relationshipText = relationshipText;
    }

    public int getRelatedProgramId()
    {
        return relatedProgramId;
    }

    public void setRelatedProgramId( int relatedProgramId )
    {
        this.relatedProgramId = relatedProgramId;
    }

    public int getRelationshipType()
    {
        return relationshipType;
    }

    public void setRelationshipType( int relationshipType )
    {
        this.relationshipType = relationshipType;
    }

    @Override
    public void serialize( DataOutputStream dout )
        throws IOException
    {
        super.serialize( dout );
        dout.writeInt( getVersion() );
        dout.writeInt( this.getType() );
        dout.writeUTF( getDateOfEnrollmentDescription() );
        dout.writeUTF( getDateOfIncidentDescription() );
        dout.writeUTF( getTrackedEntityName() );

        // Write program stage
        dout.writeInt( programStages.size() );
        for ( ProgramStage ps : programStages )
        {
            ps.serialize( dout );
        }

        // Write program attribute
        dout.writeInt( programAttributes.size() );
        for ( PatientAttribute pa : programAttributes )
        {
            pa.serialize( dout );
        }

        String relationshipText = getRelationshipText();
        if ( relationshipText == null )
        {
            dout.writeUTF( "" );
        }
        else
        {
            dout.writeUTF( getRelationshipText() );
        }
        dout.writeInt( getRelatedProgramId() );
        dout.writeInt( relationshipType );

    }

    @Override
    public void deSerialize( DataInputStream dataInputStream )
        throws IOException
    {
        super.deSerialize( dataInputStream );
        this.setVersion( dataInputStream.readInt() );
        this.setType( dataInputStream.readInt() );
        this.setDateOfEnrollmentDescription( dataInputStream.readUTF() );
        this.setDateOfIncidentDescription( dataInputStream.readUTF() );
        this.setTrackedEntityName( dataInputStream.readUTF() );

        // Read program stage
        int programStageNumber = dataInputStream.readInt();
        if ( programStageNumber > 0 )
        {
            for ( int i = 0; i < programStageNumber; i++ )
            {
                ProgramStage programStage = new ProgramStage();
                programStage.deSerialize( dataInputStream );
                programStages.add( programStage );
            }
        }

        // Read program attribute
        int programAttSize = dataInputStream.readInt();
        if ( programAttSize > 0 )
        {
            for ( int i = 0; i < programAttSize; i++ )
            {
                PatientAttribute pa = new PatientAttribute();
                pa.deSerialize( dataInputStream );
                programAttributes.add( pa );
            }
        }

        this.setRelationshipText( dataInputStream.readUTF() );
        this.setRelatedProgramId( dataInputStream.readInt() );
        this.setRelationshipType( dataInputStream.readInt() );
    }
}
