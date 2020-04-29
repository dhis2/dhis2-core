package org.hisp.dhis.dxf2.events.event;

/*
 * Copyright (c) 2004-2020, University of Oslo
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

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.events.event.context.WorkContextLoader;
import org.hisp.dhis.dxf2.events.event.persistence.EventPersistenceService;
import org.hisp.dhis.dxf2.events.event.preprocess.PreProcessorFactory;
import org.hisp.dhis.dxf2.events.event.preprocess.update.PreUpdateProcessorFactory;
import org.hisp.dhis.dxf2.events.event.validation.ValidationFactory;
import org.hisp.dhis.dxf2.events.event.validation.update.UpdateValidationFactory;
import org.hisp.dhis.dxf2.importsummary.ImportSummaries;
import org.hisp.dhis.importexport.ImportStrategy;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.system.notification.Notifier;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Implementation of EventService that uses Jackson for serialization and
 * deserialization. This class has the prototype scope and can hence have class
 * scoped variables such as caches.
 *
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Service( "org.hisp.dhis.dxf2.events.event.EventService" )
@Scope( value = "prototype", proxyMode = ScopedProxyMode.INTERFACES )
public class JacksonEventService2
    extends
    AbstractEventService2
{
    // -------------------------------------------------------------------------
    // EventService Impl
    // -------------------------------------------------------------------------

    private final ObjectMapper jsonMapper;

    private final ObjectMapper xmlMapper;

    public JacksonEventService2( Notifier notifier, WorkContextLoader workContextLoader,
        ValidationFactory validationFactory, UpdateValidationFactory updateValidationFactory,
        PreProcessorFactory preProcessorFactory, PreUpdateProcessorFactory preUpdateProcessorFactory,
        EventPersistenceService eventPersistenceService, ObjectMapper jsonMapper,
        @Qualifier( "xmlMapper" ) ObjectMapper xmlMapper )
    {

        checkNotNull( notifier );
        checkNotNull( workContextLoader );
        checkNotNull( validationFactory );
        checkNotNull( updateValidationFactory );
        checkNotNull( preProcessorFactory );
        checkNotNull( preUpdateProcessorFactory );
        checkNotNull( eventPersistenceService );
        checkNotNull( jsonMapper );
        checkNotNull( xmlMapper );

        this.notifier = notifier;
        this.workContextLoader = workContextLoader;
        this.validationFactory = validationFactory;
        this.updateValidationFactory = updateValidationFactory;
        this.preProcessorFactory = preProcessorFactory;
        this.preUpdateProcessorFactory = preUpdateProcessorFactory;
        this.eventPersistenceService = eventPersistenceService;
        this.jsonMapper = jsonMapper;
        this.xmlMapper = xmlMapper;
    }

    @SuppressWarnings( "unchecked" )
    private <T> T fromXml( String input, Class<?> clazz )
        throws IOException
    {
        return (T) xmlMapper.readValue( input, clazz );
    }

    @SuppressWarnings( "unchecked" )
    private <T> T fromJson( String input, Class<?> clazz )
        throws IOException
    {
        return (T) jsonMapper.readValue( input, clazz );
    }

    @Override
    public List<Event> getEventsXml( InputStream inputStream )
        throws IOException
    {
        String input = StreamUtils.copyToString( inputStream, StandardCharsets.UTF_8 );

        return parseXmlEvents( input );
    }

    @Override
    public List<Event> getEventsJson( InputStream inputStream )
        throws IOException
    {
        String input = StreamUtils.copyToString( inputStream, StandardCharsets.UTF_8 );

        return parseJsonEvents( input );
    }

    @Override
    public ImportSummaries addEventsXml( InputStream inputStream, ImportOptions importOptions )
        throws IOException
    {
        return addEventsXml( inputStream, null, updateImportOptions( importOptions ) );
    }

    @Override
    public ImportSummaries addEventsXml( InputStream inputStream, JobConfiguration jobId, ImportOptions importOptions )
        throws IOException
    {
        String input = StreamUtils.copyToString( inputStream, StandardCharsets.UTF_8 );
        List<Event> events = parseXmlEvents( input );

        return processEventImport( events, updateImportOptions( importOptions ), jobId );
    }

    @Override
    public ImportSummaries addEventsJson( InputStream inputStream, ImportOptions importOptions )
        throws IOException
    {
        return addEventsJson( inputStream, null, updateImportOptions( importOptions ) );
    }

    @Override
    public ImportSummaries addEventsJson( InputStream inputStream, JobConfiguration jobId, ImportOptions importOptions )
        throws IOException
    {
        String input = StreamUtils.copyToString( inputStream, StandardCharsets.UTF_8 );
        List<Event> events = parseJsonEvents( input );

        if ( importOptions != null && importOptions.getImportStrategy() != null )
        {
            final ImportStrategy importStrategy = importOptions.getImportStrategy();

            if ( importStrategy.isCreate() )
            {
                return processEventImport( events, updateImportOptions( importOptions ), jobId );
            }
            else if ( importStrategy.isUpdate() )
            {
                return processEventImportUpdate( events, updateImportOptions( importOptions ), jobId );
            }
            // TODO: Add specific import in case of:
            // importStrategy.isCreateAndUpdate()
            // importStrategy.isDelete()
            // importStrategy.isSync()
        }
        // Default is import - create, if no import strategy is defined
        return processEventImport( events, updateImportOptions( importOptions ), jobId );
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    private List<Event> parseXmlEvents( String input )
        throws IOException
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

    private List<Event> parseJsonEvents( String input )
        throws IOException
    {
        List<Event> events = new ArrayList<>();

        JsonNode root = jsonMapper.readTree( input );

        if ( root.get( "events" ) != null )
        {
            Events multiple = fromJson( input, Events.class );
            events.addAll( multiple.getEvents() );
        }
        else
        {
            Event single = fromJson( input, Event.class );
            events.add( single );
        }

        return events;
    }
}
