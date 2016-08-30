package org.hisp.dhis.program;

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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.DxfNamespaces;

/**
 * @author Chau Thu Tran
 */
@JacksonXmlRootElement( localName = "programValidationResult", namespace = DxfNamespaces.DXF_2_0 )
public class ProgramValidationResult
{
    private ProgramStageInstance programStageInstance;

    private ProgramValidation programValidation;

    private String leftsideValue;

    private String rightsideValue;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public ProgramValidationResult()
    {
    }

    public ProgramValidationResult( ProgramStageInstance programStageInstance, ProgramValidation programValidation,
        String leftsideValue, String rightsideValue )
    {
        super();
        this.programStageInstance = programStageInstance;
        this.programValidation = programValidation;
        this.leftsideValue = leftsideValue;
        this.rightsideValue = rightsideValue;
    }

    // -------------------------------------------------------------------------
    // Equals, hashCode and toString
    // -------------------------------------------------------------------------

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((programStageInstance == null) ? 0 : programStageInstance.hashCode());
        result = prime * result + ((programValidation == null) ? 0 : programValidation.hashCode());
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

        final ProgramValidationResult other = (ProgramValidationResult) object;

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

        if ( programValidation == null )
        {
            if ( other.programValidation != null )
            {
                return false;
            }
        }
        else if ( !programValidation.equals( other.programValidation ) )
        {
            return false;
        }

        return true;
    }

    // -------------------------------------------------------------------------
    // Setters && Getters
    // -------------------------------------------------------------------------

    @JsonProperty
    @JsonSerialize( as = BaseIdentifiableObject.class )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public ProgramStageInstance getProgramStageInstance()
    {
        return programStageInstance;
    }

    public void setProgramStageInstance( ProgramStageInstance programStageInstance )
    {
        this.programStageInstance = programStageInstance;
    }

    @JsonProperty
    @JsonSerialize( as = BaseIdentifiableObject.class )
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public ProgramValidation getProgramValidation()
    {
        return programValidation;
    }

    public void setProgramValidation( ProgramValidation programValidation )
    {
        this.programValidation = programValidation;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getLeftsideValue()
    {
        return leftsideValue;
    }

    public void setLeftsideValue( String leftsideValue )
    {
        this.leftsideValue = leftsideValue;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getRightsideValue()
    {
        return rightsideValue;
    }

    public void setRightsideValue( String rightsideValue )
    {
        this.rightsideValue = rightsideValue;
    }
}
