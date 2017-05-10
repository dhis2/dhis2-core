package org.hisp.dhis.dxf2.webmessage.utils;

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

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.apache.commons.io.IOUtils;
import org.hisp.dhis.dxf2.webmessage.WebMessageParseException;
import org.hisp.dhis.render.EmptyStringToNullStdDeserializer;
import org.hisp.dhis.render.ParseDateStdDeserializer;
import org.hisp.dhis.render.WriteDateStdSerializer;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.Date;

/**
 * Created by vanyas on 5/4/17.
 */
public class WebMessageParseUtils
{
    private final static ObjectMapper JSON_MAPPER = new ObjectMapper();
    private final static ObjectMapper XML_MAPPER = new XmlMapper();


    public static <T> T fromWebMessageResponse( InputStream input, Class<T> klass ) throws WebMessageParseException
    {
        StringWriter writer = new StringWriter();
        try
        {
            IOUtils.copy( input, writer, "UTF-8" );
        }
        catch ( IOException e )
        {
            throw new WebMessageParseException( "Could not read the InputStream" + e.getMessage(), e );
        }
        return parseJson( writer.toString(), klass );
    }

    public static <T> T fromWebMessageResponse( String input, Class<T> klass ) throws WebMessageParseException
    {
        return parseJson( input, klass );
    }

    private static <T> T parseJson( String input, Class<T> klass ) throws WebMessageParseException
    {
        JsonNode objectNode = null;
        try
        {
            objectNode = JSON_MAPPER.readTree( input );
        }
        catch ( IOException e )
        {
            throw new WebMessageParseException( "Invalid JSON String. " + e.getMessage(), e );
        }

        JsonNode responseNode = null;

        if ( objectNode != null )
        {
            responseNode = objectNode.get( "response" );
        }
        else
        {
            throw new WebMessageParseException( "The object node is null. Could not parse the JSON." );
        }
        try
        {
            return JSON_MAPPER.readValue( responseNode.toString(), klass );
        }
        catch ( IOException e )
        {
            throw new WebMessageParseException( "Could not parse the JSON." + e.getMessage(), e );
        }
    }


    static
    {
        SimpleModule module = new SimpleModule();
        module.addDeserializer( String.class, new EmptyStringToNullStdDeserializer() );
        module.addDeserializer( Date.class, new ParseDateStdDeserializer() );
        module.addSerializer( Date.class, new WriteDateStdSerializer() );

        XML_MAPPER.configure( DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true );
        XML_MAPPER.configure( DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, true );
        XML_MAPPER.configure( DeserializationFeature.WRAP_EXCEPTIONS, true );
        JSON_MAPPER.configure( DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true );
        JSON_MAPPER.configure( DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, true );
        JSON_MAPPER.configure( DeserializationFeature.WRAP_EXCEPTIONS, true );

        XML_MAPPER.disable( MapperFeature.AUTO_DETECT_FIELDS );
        XML_MAPPER.disable( MapperFeature.AUTO_DETECT_CREATORS );
        XML_MAPPER.disable( MapperFeature.AUTO_DETECT_GETTERS );
        XML_MAPPER.disable( MapperFeature.AUTO_DETECT_SETTERS );
        XML_MAPPER.disable( MapperFeature.AUTO_DETECT_IS_GETTERS );

        JSON_MAPPER.disable( MapperFeature.AUTO_DETECT_FIELDS );
        JSON_MAPPER.disable( MapperFeature.AUTO_DETECT_CREATORS );
        JSON_MAPPER.disable( MapperFeature.AUTO_DETECT_GETTERS );
        JSON_MAPPER.disable( MapperFeature.AUTO_DETECT_SETTERS );
        JSON_MAPPER.disable( MapperFeature.AUTO_DETECT_IS_GETTERS );

        JSON_MAPPER.registerModule( module );
        XML_MAPPER.registerModule( module );
    }
}
