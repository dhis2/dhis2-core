package org.hisp.dhis.dxf2.events.trackedentity;

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
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.importsummary.ImportSummaries;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.hisp.dhis.render.EmptyStringToNullStdDeserializer;
import org.hisp.dhis.render.ParseDateStdDeserializer;
import org.hisp.dhis.render.WriteDateStdSerializer;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Transactional
public class JacksonTrackedEntityInstanceService extends AbstractTrackedEntityInstanceService
{
    // -------------------------------------------------------------------------
    // Implementation
    // -------------------------------------------------------------------------

    private static final ObjectMapper XML_MAPPER = new XmlMapper();

    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

    @SuppressWarnings( "unchecked" )
    private static <T> T fromXml( InputStream inputStream, Class<?> clazz ) throws IOException
    {
        return (T) XML_MAPPER.readValue( inputStream, clazz );
    }

    @SuppressWarnings( "unchecked" )
    private static <T> T fromXml( String input, Class<?> clazz ) throws IOException
    {
        return (T) XML_MAPPER.readValue( input, clazz );
    }

    @SuppressWarnings( "unchecked" )
    private static <T> T fromJson( InputStream inputStream, Class<?> clazz ) throws IOException
    {
        return (T) JSON_MAPPER.readValue( inputStream, clazz );
    }

    @SuppressWarnings( "unchecked" )
    private static <T> T fromJson( String input, Class<?> clazz ) throws IOException
    {
        return (T) JSON_MAPPER.readValue( input, clazz );
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

    // -------------------------------------------------------------------------
    // CREATE
    // -------------------------------------------------------------------------

    @Override
    public ImportSummaries addTrackedEntityInstanceXml( InputStream inputStream, ImportOptions importOptions ) throws IOException
    {
        String input = StreamUtils.copyToString( inputStream, Charset.forName( "UTF-8" ) );
        List<TrackedEntityInstance> trackedEntityInstances = new ArrayList<>();

        try
        {
            TrackedEntityInstances fromXml = fromXml( input, TrackedEntityInstances.class );
            trackedEntityInstances.addAll( fromXml.getTrackedEntityInstances() );
        }
        catch ( JsonMappingException ex )
        {
            TrackedEntityInstance fromXml = fromXml( input, TrackedEntityInstance.class );
            trackedEntityInstances.add( fromXml );
        }

        return addTrackedEntityInstanceList( trackedEntityInstances, importOptions );
    }

    @Override
    public ImportSummaries addTrackedEntityInstanceJson( InputStream inputStream, ImportOptions importOptions ) throws IOException
    {
        String input = StreamUtils.copyToString( inputStream, Charset.forName( "UTF-8" ) );
        List<TrackedEntityInstance> trackedEntityInstances = new ArrayList<>();

        try
        {
            TrackedEntityInstances fromJson = fromJson( input, TrackedEntityInstances.class );
            trackedEntityInstances.addAll( fromJson.getTrackedEntityInstances() );
        }
        catch ( JsonMappingException ex )
        {
            TrackedEntityInstance fromJson = fromJson( input, TrackedEntityInstance.class );
            trackedEntityInstances.add( fromJson );
        }

        return addTrackedEntityInstanceList( trackedEntityInstances, importOptions );
    }

    private ImportSummaries addTrackedEntityInstanceList( List<TrackedEntityInstance> trackedEntityInstances, ImportOptions importOptions )
    {
        ImportSummaries importSummaries = new ImportSummaries();

        List<TrackedEntityInstance> create = new ArrayList<>();
        List<TrackedEntityInstance> update = new ArrayList<>();
        List<String> delete = new ArrayList<>();

        if ( importOptions.getImportStrategy().isCreate() )
        {
            create.addAll( trackedEntityInstances );
        }
        else if ( importOptions.getImportStrategy().isCreateAndUpdate() )
        {
            for ( TrackedEntityInstance trackedEntityInstance : trackedEntityInstances )
            {
                if ( StringUtils.isEmpty( trackedEntityInstance.getTrackedEntityInstance() ) )
                {
                    create.add( trackedEntityInstance );
                }
                else
                {
                    if ( !teiService.trackedEntityInstanceExists( trackedEntityInstance.getTrackedEntityInstance() ) )
                    {
                        create.add( trackedEntityInstance );
                    }
                    else
                    {
                        update.add( trackedEntityInstance );
                    }
                }
            }
        }
        else if ( importOptions.getImportStrategy().isUpdate() )
        {
            update.addAll( trackedEntityInstances );
        }
        else if ( importOptions.getImportStrategy().isDelete() )
        {
            delete.addAll( trackedEntityInstances.stream().map( TrackedEntityInstance::getTrackedEntityInstance ).collect( Collectors.toList() ) );
        }

        importSummaries.addImportSummaries( addTrackedEntityInstances( create, importOptions ) );
        importSummaries.addImportSummaries( updateTrackedEntityInstances( update, importOptions ) );
        importSummaries.addImportSummaries( deleteTrackedEntityInstances( delete ) );

        return importSummaries;
    }

    // -------------------------------------------------------------------------
    // UPDATE
    // -------------------------------------------------------------------------

    @Override
    public ImportSummary updateTrackedEntityInstanceXml( String id, InputStream inputStream, ImportOptions importOptions ) throws IOException
    {
        TrackedEntityInstance trackedEntityInstance = fromXml( inputStream, TrackedEntityInstance.class );
        trackedEntityInstance.setTrackedEntityInstance( id );

        return updateTrackedEntityInstance( trackedEntityInstance, importOptions );
    }

    @Override
    public ImportSummary updateTrackedEntityInstanceJson( String id, InputStream inputStream, ImportOptions importOptions ) throws IOException
    {
        TrackedEntityInstance trackedEntityInstance = fromJson( inputStream, TrackedEntityInstance.class );
        trackedEntityInstance.setTrackedEntityInstance( id );

        return updateTrackedEntityInstance( trackedEntityInstance, importOptions );
    }
}
