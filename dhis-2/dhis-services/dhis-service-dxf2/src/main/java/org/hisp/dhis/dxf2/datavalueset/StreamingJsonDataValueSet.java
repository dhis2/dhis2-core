package org.hisp.dhis.dxf2.datavalueset;

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

import com.fasterxml.jackson.core.JsonGenerator;

import org.apache.commons.io.IOUtils;
import org.hisp.dhis.dxf2.datavalue.DataValue;
import org.hisp.dhis.dxf2.common.JacksonUtils;

import java.io.IOException;
import java.io.OutputStream;

import org.hisp.dhis.dxf2.datavalue.StreamingJsonDataValue;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class StreamingJsonDataValueSet extends DataValueSet
{
    private JsonGenerator generator;

    private boolean startedArray;

    public StreamingJsonDataValueSet( OutputStream out )
    {
        try
        {
            generator = JacksonUtils.getJsonMapper().getFactory().createGenerator( out );
            generator.writeStartObject();
        }
        catch ( IOException ignored )
        {
        }
    }

    @Override
    public void setDataElementIdScheme( String dataElementIdScheme )
    {
        writeObjectField( FIELD_DATAELEMENTIDSCHEME, dataElementIdScheme );
    }

    @Override
    public void setOrgUnitIdScheme( String orgUnitIdScheme )
    {
        writeObjectField( FIELD_ORGUNITIDSCHEME, orgUnitIdScheme );
    }

    @Override
    public void setDataSet( String dataSet )
    {
        writeObjectField( FIELD_DATASET, dataSet );
    }

    @Override
    public void setCompleteDate( String completeDate )
    {
        writeObjectField( FIELD_COMPLETEDATE, completeDate );
    }

    @Override
    public void setPeriod( String period )
    {
        writeObjectField( FIELD_PERIOD, period );
    }

    @Override
    public void setOrgUnit( String orgUnit )
    {
        writeObjectField( FIELD_ORGUNIT, orgUnit );
    }

    @Override
    public void setAttributeOptionCombo( String attributeOptionCombo )
    {
        writeObjectField( FIELD_ATTRIBUTE_OPTION_COMBO, attributeOptionCombo );
    }

    @Override
    public DataValue getDataValueInstance()
    {
        if ( !startedArray )
        {
            try
            {
                generator.writeArrayFieldStart( "dataValues" );
                startedArray = true;
            }
            catch ( IOException ignored )
            {
            }
        }

        return new StreamingJsonDataValue( generator );
    }

    @Override
    public void close()
    {
        if ( generator == null )
        {
            return;
        }

        try
        {
            if ( startedArray )
            {
                generator.writeEndArray();
            }

            generator.writeEndObject();            
        }
        catch ( IOException ignored )
        {
        }
        finally
        {
            IOUtils.closeQuietly( generator );
        }
    }

    private void writeObjectField( String fieldName, Object value )
    {
        if ( value == null )
        {
            return;
        }

        try
        {
            generator.writeObjectField( fieldName, value );
        }
        catch ( IOException ignored )
        {
        }
    }
}
