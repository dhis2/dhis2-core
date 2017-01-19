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

import org.hisp.dhis.api.mobile.model.DataStreamSerializable;

public class ProgramInstance
    implements DataStreamSerializable
{
    private Integer id;

    private Integer patientId;

    private Integer programId;
    
    private String name;

    // status active = 0
    // status complete = 1
    // status canceled = 2
    private Integer status;

    private String dateOfEnrollment;

    private String dateOfIncident;

    private List<ProgramStage> programStageInstances = new ArrayList<>();

    public Integer getId()
    {
        return id;
    }

    public void setId( Integer id )
    {
        this.id = id;
    }

    public Integer getPatientId()
    {
        return patientId;
    }

    public void setPatientId( Integer patientId )
    {
        this.patientId = patientId;
    }

    public Integer getProgramId()
    {
        return programId;
    }

    public void setProgramId( Integer programId )
    {
        this.programId = programId;
    }

    public Integer getStatus()
    {
        return status;
    }

    public void setStatus( Integer status )
    {
        this.status = status;
    }

    public String getDateOfEnrollment()
    {
        return dateOfEnrollment;
    }

    public void setDateOfEnrollment( String dateOfEnrollment )
    {
        this.dateOfEnrollment = dateOfEnrollment;
    }

    public String getDateOfIncident()
    {
        return dateOfIncident;
    }

    public void setDateOfIncident( String dateOfIncident )
    {
        this.dateOfIncident = dateOfIncident;
    }

    public List<ProgramStage> getProgramStageInstances()
    {
        return programStageInstances;
    }

    public void setProgramStageInstances( List<ProgramStage> programStageInstances )
    {
        this.programStageInstances = programStageInstances;
    }
    
    public String getName()
    {
        return name;
    }

    public void setName( String name )
    {
        this.name = name;
    }

    @Override
    public void serialize( DataOutputStream dataOutputStream )
        throws IOException
    {
        dataOutputStream.writeInt( this.getId() );
        dataOutputStream.writeInt( this.getPatientId() );
        dataOutputStream.writeInt( this.getProgramId() );
        dataOutputStream.writeUTF( this.getName() );
        dataOutputStream.writeInt( this.getStatus() );
        dataOutputStream.writeUTF( this.getDateOfEnrollment() );
        dataOutputStream.writeUTF( this.getDateOfIncident() );

        dataOutputStream.writeInt( programStageInstances.size() );
        for ( ProgramStage programStageInstance : programStageInstances )
        {
            programStageInstance.serialize( dataOutputStream );
        }

    }

    @Override
    public void deSerialize( DataInputStream dataInputStream )
        throws IOException
    {
        this.setId( dataInputStream.readInt() );
        this.setPatientId( dataInputStream.readInt() );
        this.setProgramId( dataInputStream.readInt() );
        this.setName( dataInputStream.readUTF() );
        this.setStatus( dataInputStream.readInt() );
        this.setDateOfEnrollment( dataInputStream.readUTF() );
        this.setDateOfIncident( dataInputStream.readUTF() );

        // Read programstage instance
        int programStageInstanceSize = dataInputStream.readInt();
        for ( int i = 0; i < programStageInstanceSize; i++ )
        {
            ProgramStage programStageInstance = new ProgramStage();
            programStageInstance.deSerialize( dataInputStream );
            programStageInstances.add( programStageInstance );
        }

    }

    @Override
    public void serializeVersion2_8( DataOutputStream dataOutputStream )
        throws IOException
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void serializeVersion2_9( DataOutputStream dataOutputStream )
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
