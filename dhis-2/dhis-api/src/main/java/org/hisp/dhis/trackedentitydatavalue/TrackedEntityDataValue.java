package org.hisp.dhis.trackedentitydatavalue;

/*
 * Copyright (c) 2004-2015, University of Oslo
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

import java.io.Serializable;
import java.util.Date;

import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.program.ProgramStageInstance;

/**
 * @author Abyot Asalefew Gizaw
 */
public class TrackedEntityDataValue
    implements Serializable
{
    /**
     * Determines if a de-serialized file is compatible with this class.
     */
    private static final long serialVersionUID = 8538519573273769587L;

    private DataElement dataElement;

    private ProgramStageInstance programStageInstance;

    private Date timestamp;

    private String value;

    private Boolean providedElsewhere;

    private String storedBy;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public TrackedEntityDataValue()
    {
    }

    public TrackedEntityDataValue( ProgramStageInstance programStageInstance, DataElement dataElement, String value )
    {
        this.dataElement = dataElement;
        this.programStageInstance = programStageInstance;
        this.timestamp = new Date();
        this.value = value;
    }

    public TrackedEntityDataValue( ProgramStageInstance programStageInstance, DataElement dataElement, Date timeStamp, String value )
    {
        this.programStageInstance = programStageInstance;
        this.dataElement = dataElement;
        this.timestamp = timeStamp;
        this.value = value;
    }

    // -------------------------------------------------------------------------
    // hashCode, equals and toString
    // -------------------------------------------------------------------------

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((dataElement == null) ? 0 : dataElement.hashCode());
        result = prime * result + ((programStageInstance == null) ? 0 : programStageInstance.hashCode());
        return result;
    }

    @Override
    public boolean equals( Object object )
    {
        if ( this == object )
        {
            return true;
        }

        if ( object == null )
        {
            return false;
        }

        if ( getClass() != object.getClass() )
        {
            return false;
        }

        final TrackedEntityDataValue other = (TrackedEntityDataValue) object;

        if ( dataElement == null )
        {
            if ( other.dataElement != null )
            {
                return false;
            }
        }
        else if ( !dataElement.equals( other.dataElement ) )
        {
            return false;
        }

        if ( programStageInstance == null )
        {
            if ( other.programStageInstance != null )
            {
                return false;
            }
        }
        else if ( !programStageInstance.equals( other.programStageInstance ) )
        {
            return false;
        }

        return true;
    }

    @Override
    public String toString()
    {
        return "TrackedEntityDataValue{" + "dataElement=" + dataElement + ", programStageInstance=" + programStageInstance
            + ", timestamp=" + timestamp + ", value='" + value + '\'' + ", providedElsewhere=" + providedElsewhere
            + ", storedBy='" + storedBy + '\'' + '}';
    }

    // -------------------------------------------------------------------------
    // Getters and setters
    // -------------------------------------------------------------------------

    public void setProgramStageInstance( ProgramStageInstance programStageInstance )
    {
        this.programStageInstance = programStageInstance;
    }

    public Boolean getProvidedElsewhere()
    {
        return providedElsewhere;
    }

    public void setProvidedElsewhere( Boolean providedElsewhere )
    {
        this.providedElsewhere = providedElsewhere;
    }

    public ProgramStageInstance getProgramStageInstance()
    {
        return programStageInstance;
    }

    public void setDataElement( DataElement dataElement )
    {
        this.dataElement = dataElement;
    }

    public DataElement getDataElement()
    {
        return dataElement;
    }

    public Date getTimestamp()
    {
        return timestamp;
    }

    public void setTimestamp( Date timestamp )
    {
        this.timestamp = timestamp;
    }

    public void setValue( String value )
    {
        this.value = value;
    }

    public String getValue()
    {
        return value;
    }

    public String getStoredBy()
    {
        return storedBy;
    }

    public void setStoredBy( String storedBy )
    {
        this.storedBy = storedBy;
    }
}
