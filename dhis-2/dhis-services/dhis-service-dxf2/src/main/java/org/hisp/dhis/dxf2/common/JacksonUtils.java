package org.hisp.dhis.dxf2.common;

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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import org.hisp.dhis.common.view.ExportView;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class JacksonUtils
{
    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

    private static final XmlMapper XML_MAPPER = new XmlMapper();

    private static final Map<String, Class<?>> VIEW_CLASSES = new HashMap<>();

    static
    {
        ObjectMapper[] objectMappers = new ObjectMapper[]{ JSON_MAPPER, XML_MAPPER };
        // DateFormat format = new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ssZ" );

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

        JSON_MAPPER.getFactory().enable( JsonGenerator.Feature.QUOTE_FIELD_NAMES );
        XML_MAPPER.configure( ToXmlGenerator.Feature.WRITE_XML_DECLARATION, true );

        // Register view classes

        VIEW_CLASSES.put( "default", ExportView.class );
        VIEW_CLASSES.put( "export", ExportView.class );
    }

    /**
     * TODO remove
     */
    public static Class<?> getViewClass( Object viewName )
    {
        if ( viewName == null || !(viewName instanceof String && ((String) viewName).length() != 0) )
        {
            return VIEW_CLASSES.get( "default" );
        }

        return VIEW_CLASSES.get( viewName );
    }

    //--------------------------------------------------------------------------
    // Global pre-configured instances of ObjectMapper and XmlMapper
    //--------------------------------------------------------------------------

    public static ObjectMapper getJsonMapper()
    {
        return JSON_MAPPER;
    }

    public static XmlMapper getXmlMapper()
    {
        return XML_MAPPER;
    }

    //--------------------------------------------------------------------------
    // JSON
    //--------------------------------------------------------------------------

    public static void toJson( OutputStream output, Object value ) throws IOException
    {
        JSON_MAPPER.writeValue( output, value );
    }

    public static String toJsonAsString( Object value ) throws IOException
    {
        return JSON_MAPPER.writeValueAsString( value );
    }

    public static String toJsonAsStringSilent( Object value )
    {
        try
        {
            return JSON_MAPPER.writeValueAsString( value );
        }
        catch ( IOException ex )
        {
            return null;
        }
    }
    
    /**
     * TODO remove
     */
    public static void toJsonWithView( OutputStream output, Object value, Class<?> viewClass ) throws IOException
    {
        JSON_MAPPER.writerWithView( viewClass ).writeValue( output, value );
    }

    /**
     * TODO remove
     */
    public static String toJsonWithViewAsString( Object value, Class<?> viewClass ) throws IOException
    {
        return JSON_MAPPER.writerWithView( viewClass ).writeValueAsString( value );
    }

    @SuppressWarnings( "unchecked" )
    public static <T> T fromJson( InputStream input, Class<?> clazz ) throws IOException
    {
        return (T) JSON_MAPPER.readValue( input, clazz );
    }

    @SuppressWarnings( "unchecked" )
    public static <T> T fromJson( String input, Class<?> clazz ) throws IOException
    {
        return (T) JSON_MAPPER.readValue( input, clazz );
    }

    //--------------------------------------------------------------------------
    // XML
    //--------------------------------------------------------------------------

    public static void toXml( OutputStream output, Object value ) throws IOException
    {
        XML_MAPPER.writeValue( output, value );
    }

    public static String toXmlAsString( Object value ) throws IOException
    {
        return XML_MAPPER.writeValueAsString( value );
    }

    /**
     * TODO remove
     */
    public static void toXmlWithView( OutputStream output, Object value, Class<?> viewClass ) throws IOException
    {
        XML_MAPPER.writerWithView( viewClass ).writeValue( output, value );
    }

    /**
     * TODO remove
     */
    public static String toXmlWithViewAsString( Object value, Class<?> viewClass ) throws IOException
    {
        return XML_MAPPER.writerWithView( viewClass ).writeValueAsString( value );
    }

    @SuppressWarnings( "unchecked" )
    public static <T> T fromXml( InputStream input, Class<?> clazz ) throws IOException
    {
        return (T) XML_MAPPER.readValue( input, clazz );
    }

    @SuppressWarnings( "unchecked" )
    public static <T> T fromXml( String input, Class<?> clazz ) throws IOException
    {
        return (T) XML_MAPPER.readValue( input, clazz );
    }

    @SuppressWarnings( "unchecked" )
    public static <T> T fromJson( InputStream inputStream, TypeReference<?> typeReference ) throws IOException
    {
        return (T) JSON_MAPPER.readValue( inputStream, typeReference );
    }
}
