package org.hisp.dhis.dxf2.render;

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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.util.JSONPObject;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator;

import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Default implementation that uses Jackson to serialize/deserialize
 *
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class DefaultRenderService
    implements RenderService
{
    private final ObjectMapper jsonMapper = new ObjectMapper();

    private final XmlMapper xmlMapper = new XmlMapper();

    public DefaultRenderService()
    {
        configureObjectMappers();
    }

    //--------------------------------------------------------------------------
    // RenderService
    //--------------------------------------------------------------------------

    @Override
    public void toJson( OutputStream output, Object value )
        throws IOException
    {
        jsonMapper.writeValue( output, value );
    }

    @Override
    public void toJson( OutputStream output, Object value, Class<?> klass )
        throws IOException
    {
        jsonMapper.writerWithView( klass ).writeValue( output, value );
    }

    @Override
    public void toJsonP( OutputStream output, Object value, String callback )
        throws IOException
    {
        if ( StringUtils.isEmpty( callback ) )
        {
            callback = "callback";
        }

        jsonMapper.writeValue( output, new JSONPObject( callback, value ) );
    }

    @Override
    public void toJsonP( OutputStream output, Object value, Class<?> klass, String callback )
        throws IOException
    {
        if ( StringUtils.isEmpty( callback ) )
        {
            callback = "callback";
        }

        jsonMapper.writerWithView( klass ).writeValue( output, new JSONPObject( callback, value ) );
    }

    @Override
    public <T> T fromJson( InputStream input, Class<T> klass )
        throws IOException
    {
        return jsonMapper.readValue( input, klass );
    }

    @Override
    public <T> T fromJson( String input, Class<T> klass )
        throws IOException
    {
        return jsonMapper.readValue( input, klass );
    }

    @Override
    public <T> void toXml( OutputStream output, T value )
        throws IOException
    {
        xmlMapper.writeValue( output, value );
    }

    @Override
    public <T> void toXml( OutputStream output, T value, Class<?> klass )
        throws IOException
    {
        xmlMapper.writerWithView( klass ).writeValue( output, value );
    }

    @Override
    public <T> T fromXml( InputStream input, Class<T> klass )
        throws IOException
    {
        return xmlMapper.readValue( input, klass );
    }

    @Override
    public <T> T fromXml( String input, Class<T> klass )
        throws IOException
    {
        return xmlMapper.readValue( input, klass );
    }

    @Override
    public boolean isValidJson( String json )
        throws IOException
    {
        try
        {
            jsonMapper.readValue( json, Object.class );
        }
        catch ( JsonParseException | JsonMappingException e )
        {
            return false;
        }

        return true;
    }
    
    //--------------------------------------------------------------------------
    // Helpers
    //--------------------------------------------------------------------------

    public ObjectMapper getJsonMapper()
    {
        return jsonMapper;
    }

    public XmlMapper getXmlMapper()
    {
        return xmlMapper;
    }

    private void configureObjectMappers()
    {
        ObjectMapper[] objectMappers = new ObjectMapper[] { jsonMapper, xmlMapper };

        for ( ObjectMapper objectMapper : objectMappers )
        {
            // objectMapper.setDateFormat( format );
            objectMapper.setSerializationInclusion( JsonInclude.Include.NON_NULL );
            objectMapper.configure( SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false );
            objectMapper.configure( SerializationFeature.WRITE_EMPTY_JSON_ARRAYS, false );
            objectMapper.configure( SerializationFeature.FAIL_ON_EMPTY_BEANS, false );
            objectMapper.configure( SerializationFeature.WRAP_EXCEPTIONS, true );

            objectMapper.configure( DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false );
            objectMapper.configure( DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, true );
            objectMapper.configure( DeserializationFeature.WRAP_EXCEPTIONS, true );

            objectMapper.disable( MapperFeature.AUTO_DETECT_FIELDS );
            objectMapper.disable( MapperFeature.AUTO_DETECT_CREATORS );
            objectMapper.disable( MapperFeature.AUTO_DETECT_GETTERS );
            objectMapper.disable( MapperFeature.AUTO_DETECT_SETTERS );
            objectMapper.disable( MapperFeature.AUTO_DETECT_IS_GETTERS );
        }

        jsonMapper.getFactory().enable( JsonGenerator.Feature.QUOTE_FIELD_NAMES );
        xmlMapper.configure( ToXmlGenerator.Feature.WRITE_XML_DECLARATION, true );
    }
}
