package org.hisp.dhis.dxf2.dataset.streaming;

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

import com.fasterxml.jackson.core.JsonGenerator;
import org.hisp.dhis.dxf2.dataset.CompleteDataSetRegistration;

import java.io.IOException;

/**
 * @author Halvdan Hoem Grelland
 */
public class StreamingJsonCompleteDataSetRegistration
    extends CompleteDataSetRegistration
{
    private JsonGenerator generator;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public StreamingJsonCompleteDataSetRegistration( JsonGenerator generator )
    {
        this.generator = generator;
    }

    // -------------------------------------------------------------------------
    // Logic
    // -------------------------------------------------------------------------

    @Override
    protected void open()
    {
        try
        {
            generator.writeStartObject();
        }
        catch ( IOException e )
        {
            // Intentionally ignored
        }
    }

    @Override
    protected void close()
    {
        if ( generator == null )
        {
            return;
        }

        try
        {
            generator.writeEndObject();
        }
        catch ( IOException e )
        {
            // Intentionally ignored
        }
    }

    // -------------------------------------------------------------------------
    // Setters
    // -------------------------------------------------------------------------

    @Override
    public void setDataSet( String dataSet )
    {
        writeField( FIELD_DATASET, dataSet );
    }

    @Override
    public void setPeriod( String period )
    {
        writeField( FIELD_PERIOD, period );
    }

    @Override
    public void setOrganisationUnit( String organisationUnit )
    {
        writeField( FIELD_ORGUNIT, organisationUnit );
    }

    @Override
    public void setAttributeOptionCombo( String attributeOptionCombo )
    {
        writeField( FIELD_ATTR_OPTION_COMBO, attributeOptionCombo );
    }

    @Override
    public void setDate( String date )
    {
        writeField( FIELD_DATE, date );
    }

    @Override
    public void setStoredBy( String storedBy )
    {
        writeField( FIELD_STORED_BY, storedBy );
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    @Override
    protected void writeField( String fieldName, String value )
    {
        if ( value == null )
        {
            return;
        }

        try
        {
            generator.writeObjectField( fieldName, value );
        }
        catch ( IOException e )
        {
            // Intentionally ignored
        }
    }
}
