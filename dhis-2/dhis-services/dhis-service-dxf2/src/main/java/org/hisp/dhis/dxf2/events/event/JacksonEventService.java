package org.hisp.dhis.dxf2.events.event;

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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.importsummary.ImportSummaries;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.render.EmptyStringToNullStdDeserializer;
import org.hisp.dhis.render.ParseDateStdDeserializer;
import org.hisp.dhis.render.WriteDateStdSerializer;
import org.hisp.dhis.scheduling.TaskId;
import org.hisp.dhis.system.notification.NotificationLevel;
import org.hisp.dhis.system.util.Clock;
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
 * Implementation of EventService that uses Jackson for serialization and 
 * deserialization. This class has the prototype scope and can hence have
 * class scoped variables such as caches.
 *
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Transactional
public class JacksonEventService extends AbstractEventService
{
    private static final Log log = LogFactory.getLog( JacksonEventService.class );

    // -------------------------------------------------------------------------
    // EventService Impl
    // -------------------------------------------------------------------------

    private final static ObjectMapper XML_MAPPER = new XmlMapper();

    private final static ObjectMapper JSON_MAPPER = new ObjectMapper();

    @SuppressWarnings( "unchecked" )
    private static <T> T fromXml( String input, Class<?> clazz ) throws IOException
    {
        return (T) XML_MAPPER.readValue( input, clazz );
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

    @Override
    public List<Event> getEventsXml( InputStream inputStream ) throws IOException
    {
        String input = StreamUtils.copyToString( inputStream, Charset.forName( "UTF-8" ) );

        return parseXmlEvents( input );
    }

    @Override
    public List<Event> getEventsJson( InputStream inputStream ) throws IOException
    {
        String input = StreamUtils.copyToString( inputStream, Charset.forName( "UTF-8" ) );

        return parseJsonEvents( input );
    }

    @Override
    public ImportSummaries addEventsXml( InputStream inputStream, ImportOptions importOptions ) throws IOException
    {
        return addEventsXml( inputStream, null, importOptions );
    }

    @Override
    public ImportSummaries addEventsXml( InputStream inputStream, TaskId taskId, ImportOptions importOptions ) throws IOException
    {
        String input = StreamUtils.copyToString( inputStream, Charset.forName( "UTF-8" ) );
        List<Event> events = parseXmlEvents( input );

        return addEvents( events, taskId, importOptions );
    }

    @Override
    public ImportSummaries addEventsJson( InputStream inputStream, ImportOptions importOptions ) throws IOException
    {
        return addEventsJson( inputStream, null, importOptions );
    }

    @Override
    public ImportSummaries addEventsJson( InputStream inputStream, TaskId taskId, ImportOptions importOptions ) throws IOException
    {
        String input = StreamUtils.copyToString( inputStream, Charset.forName( "UTF-8" ) );

        List<Event> events = parseJsonEvents( input );

        return addEvents( events, taskId, importOptions );
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    private List<Event> parseXmlEvents( String input ) throws IOException
    {
        List<Event> events = new ArrayList<>();

        try
        {
            Events multiple = fromXml( input, Events.class );
            events.addAll( multiple.getEvents() );
        }
        catch ( JsonMappingException ex )
        {
            Event single = fromXml( input, Event.class );
            events.add( single );
        }

        return events;
    }

    private List<Event> parseJsonEvents( String input ) throws IOException
    {
        List<Event> events = new ArrayList<>();

        try
        {
            Events multiple = fromJson( input, Events.class );
            events.addAll( multiple.getEvents() );
        }
        catch ( JsonMappingException ex )
        {
            Event single = fromJson( input, Event.class );
            events.add( single );
        }

        return events;
    }

    private ImportSummaries addEvents( List<Event> events, TaskId taskId, ImportOptions importOptions )
    {
        ImportSummaries importSummaries = new ImportSummaries();

        notifier.clear( taskId ).notify( taskId, "Importing events" );
        Clock clock = new Clock( log ).startClock();

        List<Event> create = new ArrayList<>();
        List<Event> update = new ArrayList<>();
        List<String> delete = new ArrayList<>();

        if ( importOptions.getImportStrategy().isCreate() )
        {
            create.addAll( events );
        }
        else if ( importOptions.getImportStrategy().isCreateAndUpdate() )
        {
            for ( Event event : events )
            {
                if ( StringUtils.isEmpty( event.getEvent() ) )
                {
                    create.add( event );
                }
                else
                {
                    ProgramStageInstance programStageInstance = manager.getObject( ProgramStageInstance.class, importOptions.getIdSchemes().getProgramStageInstanceIdScheme(), event.getEvent() );

                    if ( programStageInstance == null )
                    {
                        create.add( event );
                    }
                    else
                    {
                        update.add( event );
                    }
                }
            }
        }
        else if ( importOptions.getImportStrategy().isUpdate() )
        {
            update.addAll( events );
        }
        else if ( importOptions.getImportStrategy().isDelete() )
        {
            delete.addAll( events.stream().map( Event::getEvent ).collect( Collectors.toList() ) );
        }

        importSummaries.addImportSummaries( addEvents( create, importOptions ) );
        importSummaries.addImportSummaries( updateEvents( update, false ) );
        importSummaries.addImportSummaries( deleteEvents( delete ) );

        if ( taskId != null )
        {
            notifier.notify( taskId, NotificationLevel.INFO, "Import done. Completed in " + clock.time() + ".", true ).
                addTaskSummary( taskId, importSummaries );
        }
        else
        {
            clock.logTime( "Import done" );
        }

        return importSummaries;
    }
}
