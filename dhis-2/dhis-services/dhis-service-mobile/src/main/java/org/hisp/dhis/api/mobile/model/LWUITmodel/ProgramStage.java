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
 */
public class ProgramStage
    extends Model
{
    private String clientVersion;

    private String reportDate;

    private String dueDate;

    private String reportDateDescription;

    private boolean isRepeatable;

    private boolean isCompleted;

    private boolean isSingleEvent;

    private Integer standardInterval;

    private List<Section> sections;

    private List<ProgramStageDataElement> dataElements = new ArrayList<>();

    public List<Section> getSections()
    {
        return sections;
    }

    public void setSections( List<Section> sections )
    {
        this.sections = sections;
    }

    public List<ProgramStageDataElement> getDataElements()
    {
        return dataElements;
    }

    public void setDataElements( List<ProgramStageDataElement> dataElements )
    {
        this.dataElements = dataElements;
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

    public boolean isRepeatable()
    {
        return isRepeatable;
    }

    public void setRepeatable( boolean isRepeatable )
    {
        this.isRepeatable = isRepeatable;
    }

    public boolean isCompleted()
    {
        return isCompleted;
    }

    public void setCompleted( boolean isCompleted )
    {
        this.isCompleted = isCompleted;
    }

    public boolean isSingleEvent()
    {
        return isSingleEvent;
    }

    public void setSingleEvent( boolean isSingleEvent )
    {
        this.isSingleEvent = isSingleEvent;
    }

    public String getReportDate()
    {
        return reportDate;
    }

    public void setReportDate( String reportDate )
    {
        this.reportDate = reportDate;
    }

    public String getReportDateDescription()
    {
        return reportDateDescription;
    }

    public void setReportDateDescription( String reportDateDescription )
    {
        this.reportDateDescription = reportDateDescription;
    }

    public Integer getStandardInterval()
    {
        return standardInterval;
    }

    public void setStandardInterval( Integer standardInterval )
    {
        this.standardInterval = standardInterval;
    }

    public String getDueDate()
    {
        return dueDate;
    }

    public void setDueDate( String dueDate )
    {
        this.dueDate = dueDate;
    }

    @Override
    public void serialize( DataOutputStream dout )
        throws IOException
    {
        super.serialize( dout );
        if ( reportDate == null )
        {
            reportDate = "";
        }

        if ( dueDate == null )
        {
            dueDate = "";
        }

        dout.writeUTF( reportDate );
        dout.writeUTF( reportDateDescription );
        dout.writeUTF( dueDate );
        dout.writeBoolean( isRepeatable );
        dout.writeInt( standardInterval );
        dout.writeBoolean( isCompleted() );
        dout.writeBoolean( isSingleEvent );

        dout.writeInt( dataElements.size() );
        for ( ProgramStageDataElement dataElement : dataElements )
        {
            dataElement.serialize( dout );
        }

        dout.writeInt( sections.size() );
        for ( Section section : sections )
        {
            section.serialize( dout );
        }
    }

    @Override
    public void deSerialize( DataInputStream dint )
        throws IOException
    {
        super.deSerialize( dint );
        setReportDate( dint.readUTF() );
        setReportDateDescription( dint.readUTF() );
        setDueDate( dint.readUTF() );
        setRepeatable( dint.readBoolean() );
        setStandardInterval( dint.readInt() );
        setCompleted( dint.readBoolean() );
        setSingleEvent( dint.readBoolean() );
        int dataElementSize = dint.readInt();
        if ( dataElementSize > 0 )
        {
            for ( int i = 0; i < dataElementSize; i++ )
            {
                ProgramStageDataElement de = new ProgramStageDataElement();
                de.deSerialize( dint );
                dataElements.add( de );
            }
        }
        else
        {
        }

        int sectionSize = dint.readInt();
        if ( sectionSize > 0 )
        {
            for ( int i = 0; i < sectionSize; i++ )
            {
                sections = new ArrayList<>();
                Section se = new Section();
                se.deSerialize( dint );
                sections.add( se );
            }
        }
        else
        {
        }
    }
}
