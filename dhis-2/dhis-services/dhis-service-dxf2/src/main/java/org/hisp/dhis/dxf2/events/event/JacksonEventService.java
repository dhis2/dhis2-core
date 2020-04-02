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

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hibernate.SessionFactory;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.dbms.DbmsManager;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.events.event.validation.ValidationContext;
import org.hisp.dhis.dxf2.events.eventdatavalue.EventDataValueService;
import org.hisp.dhis.dxf2.events.relationship.RelationshipService;
import org.hisp.dhis.dxf2.importsummary.ImportSummaries;
import org.hisp.dhis.fileresource.FileResourceService;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.EventSyncService;
import org.hisp.dhis.program.ProgramInstanceService;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramStageInstanceService;
import org.hisp.dhis.program.ProgramStageService;
import org.hisp.dhis.programrule.ProgramRuleVariableService;
import org.hisp.dhis.query.QueryService;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.system.notification.Notifier;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceService;
import org.hisp.dhis.trackedentity.TrackerAccessManager;
import org.hisp.dhis.trackedentity.TrackerOwnershipManager;
import org.hisp.dhis.trackedentitycomment.TrackedEntityCommentService;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.UserService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Implementation of EventService that uses Jackson for serialization and
 * deserialization. This class has the prototype scope and can hence have
 * class scoped variables such as caches.
 *
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
//@Service( "org.hisp.dhis.dxf2.events.event.EventService" )
@Scope( value = "prototype", proxyMode = ScopedProxyMode.INTERFACES )
public class JacksonEventService extends AbstractEventService
{
    // -------------------------------------------------------------------------
    // EventService Impl
    // -------------------------------------------------------------------------

    public JacksonEventService(
        ProgramService programService,
        ProgramStageService programStageService,
        ProgramInstanceService programInstanceService,
        ProgramStageInstanceService programStageInstanceService,
        OrganisationUnitService organisationUnitService,
        DataElementService dataElementService,
        CurrentUserService currentUserService,
        EventDataValueService eventDataValueService,
        TrackedEntityInstanceService entityInstanceService,
        TrackedEntityCommentService commentService,
        EventStore eventStore,
        Notifier notifier,
        SessionFactory sessionFactory,
        DbmsManager dbmsManager,
        IdentifiableObjectManager manager,
        CategoryService categoryService,
        FileResourceService fileResourceService,
        SchemaService schemaService,
        QueryService queryService,
        TrackerAccessManager trackerAccessManager,
        TrackerOwnershipManager trackerOwnershipAccessManager,
        AclService aclService,
        ApplicationEventPublisher eventPublisher,
        RelationshipService relationshipService,
        UserService userService,
        EventSyncService eventSyncService,
        ProgramRuleVariableService ruleVariableService,
        ObjectMapper jsonMapper,
        @Qualifier( "xmlMapper" ) ObjectMapper xmlMapper )
    {
        checkNotNull( programService );
        checkNotNull( programStageService );
        checkNotNull( programInstanceService );
        checkNotNull( programStageInstanceService );
        checkNotNull( organisationUnitService );
        checkNotNull( dataElementService );
        checkNotNull( currentUserService );
        checkNotNull( eventDataValueService );
        checkNotNull( entityInstanceService );
        checkNotNull( commentService );
        checkNotNull( eventStore );
        checkNotNull( notifier );
        checkNotNull( sessionFactory );
        checkNotNull( dbmsManager );
        checkNotNull( manager );
        checkNotNull( categoryService );
        checkNotNull( fileResourceService );
        checkNotNull( schemaService );
        checkNotNull( queryService );
        checkNotNull( trackerAccessManager );
        checkNotNull( trackerOwnershipAccessManager );
        checkNotNull( aclService );
        checkNotNull( eventPublisher );
        checkNotNull( userService );
        checkNotNull( eventSyncService );
        checkNotNull( ruleVariableService );
        checkNotNull( jsonMapper );
        checkNotNull( xmlMapper );

        this.programService = programService;
        this.programStageService = programStageService;
        this.programInstanceService = programInstanceService;
        this.programStageInstanceService = programStageInstanceService;
        this.organisationUnitService = organisationUnitService;
        this.dataElementService = dataElementService;
        this.currentUserService = currentUserService;
        this.eventDataValueService = eventDataValueService;
        this.entityInstanceService = entityInstanceService;
        this.commentService = commentService;
        this.eventStore = eventStore;
        this.notifier = notifier;
        this.sessionFactory = sessionFactory;
        this.dbmsManager = dbmsManager;
        this.manager = manager;
        this.categoryService = categoryService;
        this.fileResourceService = fileResourceService;
        this.schemaService = schemaService;
        this.queryService = queryService;
        this.trackerAccessManager = trackerAccessManager;
        this.trackerOwnershipAccessManager = trackerOwnershipAccessManager;
        this.aclService = aclService;
        this.eventPublisher = eventPublisher;
        this.relationshipService = relationshipService;
        this.userService = userService;
        this.eventSyncService = eventSyncService;
        this.ruleVariableService = ruleVariableService;
        this.jsonMapper = jsonMapper;
        this.xmlMapper = xmlMapper;
    }

    @SuppressWarnings( "unchecked" )
    private <T> T fromXml( String input, Class<?> clazz ) throws IOException
    {
        return (T) xmlMapper.readValue( input, clazz );
    }

    @SuppressWarnings( "unchecked" )
    private <T> T fromJson( String input, Class<?> clazz ) throws IOException
    {
        return (T) jsonMapper.readValue( input, clazz );
    }

    @Override
    public List<Event> getEventsXml( InputStream inputStream ) throws IOException
    {
        String input = StreamUtils.copyToString( inputStream, StandardCharsets.UTF_8 );

        return parseXmlEvents( input );
    }

    @Override
    public List<Event> getEventsJson( InputStream inputStream ) throws IOException
    {
        String input = StreamUtils.copyToString( inputStream, StandardCharsets.UTF_8 );

        return parseJsonEvents( input );
    }

    @Override
    public ImportSummaries addEvents(List<Event> events, ImportOptions importOptions, ValidationContext validationContext) {
        return null;
    }

    @Override
    public ImportSummaries addEventsXml( InputStream inputStream, ImportOptions importOptions ) throws IOException
    {
        return addEventsXml( inputStream, null, updateImportOptions( importOptions ) );
    }

    @Override
    public ImportSummaries addEventsXml( InputStream inputStream, JobConfiguration jobId, ImportOptions importOptions ) throws IOException
    {
        String input = StreamUtils.copyToString( inputStream, StandardCharsets.UTF_8 );
        List<Event> events = parseXmlEvents( input );

        return processEventImport( events, updateImportOptions( importOptions ), jobId );
    }

    @Override
    public ImportSummaries addEventsJson( InputStream inputStream, ImportOptions importOptions ) throws IOException
    {
        return addEventsJson( inputStream, null, updateImportOptions( importOptions ) );
    }

    @Override
    public ImportSummaries addEventsJson( InputStream inputStream, JobConfiguration jobId, ImportOptions importOptions ) throws IOException
    {
        String input = StreamUtils.copyToString( inputStream, StandardCharsets.UTF_8 );
        List<Event> events = parseJsonEvents( input );

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
