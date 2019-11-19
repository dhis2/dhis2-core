package org.hisp.dhis.hibernate.jsonb.type;

/*
 * Copyright (c) 2004-2019, University of Oslo
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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonDeserializer;
import org.hisp.dhis.render.DeviceRenderTypeMap;

import java.io.IOException;
import java.util.Properties;

public class JsonDeviceRenderTypeMap extends JsonBinaryType
{
    private Class<? extends JsonDeserializer> deserializer;

    @Override
    protected Object convertJsonToObject( String content )
    {
        try
        {
            JsonDeserializer<DeviceRenderTypeMap> jsonDeserializer = deserializer.newInstance();
            JsonParser jsonParser = reader.getFactory().createParser( content );
            return jsonDeserializer.deserialize( jsonParser, MAPPER.getDeserializationContext() );
        }
        catch ( IOException | IllegalAccessException | InstantiationException e )
        {
            throw new RuntimeException( e );
        }
    }

    @Override
    public void setParameterValues( Properties parameters )
    {
        super.setParameterValues( parameters );

        final String clazz = (String) parameters.get( "deserializer" );

        if ( clazz == null )
        {
            throw new IllegalArgumentException(
                String.format( "Required parameter '%s' is not configured", "deserializer" ) );
        }

        try
        {
            deserializer = (Class<? extends JsonDeserializer>) classForName( clazz );
        }
        catch ( ClassNotFoundException e )
        {
            throw new IllegalArgumentException( "Class: " + clazz + " is not a known class type." );
        }
    }

}
