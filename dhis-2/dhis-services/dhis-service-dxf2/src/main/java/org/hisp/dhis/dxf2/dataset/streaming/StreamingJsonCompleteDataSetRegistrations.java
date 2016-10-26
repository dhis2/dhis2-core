package org.hisp.dhis.dxf2.dataset.streaming;

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

import com.fasterxml.jackson.core.JsonGenerator;
import org.hisp.dhis.dxf2.dataset.CompleteDataSetRegistration;
import org.hisp.dhis.dxf2.dataset.CompleteDataSetRegistrations;
import org.hisp.dhis.render.DefaultRenderService;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Halvdan Hoem Grelland
 */
public class StreamingJsonCompleteDataSetRegistrations
    extends CompleteDataSetRegistrations
{
    private JsonGenerator jsonGenerator;

    private AtomicBoolean startedArray = new AtomicBoolean( false );

    public StreamingJsonCompleteDataSetRegistrations( OutputStream out )
    {
        try
        {
            jsonGenerator = DefaultRenderService.getJsonMapper().getFactory().createGenerator( out );
            open();
        }
        catch ( IOException e )
        {
            // Intentionally ignored
        }
    }

    @Override
    public CompleteDataSetRegistration getCompleteDataSetRegistrationInstance()
    {
        if ( !startedArray.getAndSet( true ) )
        {
            try
            {
                jsonGenerator.writeArrayFieldStart( "completeDataSetRegistrations" );
            }
            catch ( IOException ignored )
            {
                startedArray.set( false );
            }
        }

        return new StreamingJsonCompleteDataSetRegistration( jsonGenerator );
    }

    @Override
    protected void open()
    {
        try
        {
            jsonGenerator.writeStartObject();
        }
        catch ( IOException ignored )
        {
        }
    }

    @Override
    protected void close()
    {
    }

    @Override
    protected void writeField( String fieldName, String value )
    {
        if ( value == null )
        {
            return;
        }

        try
        {
            jsonGenerator.writeObjectField( fieldName, value );
        }
        catch ( IOException ignored )
        {
        }
    }



    @Override
    public void setDataSetIdScheme( String dataSetIdScheme )
    {
        writeField( FIELD_DATA_SET_ID_SCHEME, dataSetIdScheme );
    }

    @Override
    public void setOrgUnitIdScheme( String orgUnitIdScheme )
    {
        writeField( FIELD_ORG_UNIT_ID_SCHEME, orgUnitIdScheme );
    }

    @Override
    public void setAttributeOptionComboIdScheme( String attributeOptionComboIdScheme )
    {
        writeField( FIELD_ATTR_OPT_COMBO_ID_SCHEME, attributeOptionComboIdScheme );
    }

    @Override
    public void setPeriod( String period )
    {
        writeField( FIELD_PERIOD, period );
    }
}
