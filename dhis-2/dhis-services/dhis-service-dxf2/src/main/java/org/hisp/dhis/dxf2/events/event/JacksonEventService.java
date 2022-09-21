/*
<<<<<<< HEAD
 * Copyright (c) 2004-2020, University of Oslo
=======
 * Copyright (c) 2004-2021, University of Oslo
>>>>>>> refs/remotes/origin/2.35.8-EMBARGOED_za
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
package org.hisp.dhis.dxf2.events.event;

import static com.google.common.base.Preconditions.checkNotNull;

<<<<<<< HEAD
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hibernate.SessionFactory;
=======
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

>>>>>>> refs/remotes/origin/2.35.8-EMBARGOED_za
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dbms.DbmsManager;
import org.hisp.dhis.dxf2.common.ImportOptions;
<<<<<<< HEAD
import org.hisp.dhis.dxf2.events.eventdatavalue.EventDataValueService;
=======
import org.hisp.dhis.dxf2.events.importer.EventImporter;
import org.hisp.dhis.dxf2.events.importer.EventManager;
import org.hisp.dhis.dxf2.events.importer.EventServiceFacade;
import org.hisp.dhis.dxf2.events.importer.context.WorkContextLoader;
>>>>>>> refs/remotes/origin/2.35.8-EMBARGOED_za
import org.hisp.dhis.dxf2.events.relationship.RelationshipService;
import org.hisp.dhis.dxf2.importsummary.ImportSummaries;
import org.hisp.dhis.fileresource.FileResourceService;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.EventSyncService;
import org.hisp.dhis.program.ProgramInstanceService;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramStageInstanceService;
<<<<<<< HEAD
import org.hisp.dhis.program.ProgramStageService;
import org.hisp.dhis.programrule.ProgramRuleVariableService;
=======
>>>>>>> refs/remotes/origin/2.35.8-EMBARGOED_za
import org.hisp.dhis.query.QueryService;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.system.notification.Notifier;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceService;
import org.hisp.dhis.trackedentity.TrackerAccessManager;
import org.hisp.dhis.trackedentity.TrackerOwnershipManager;
import org.hisp.dhis.trackedentitycomment.TrackedEntityCommentService;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.UserService;
import org.springframework.beans.factory.annotation.Qualifier;
<<<<<<< HEAD
import org.springframework.context.ApplicationEventPublisher;
=======
>>>>>>> refs/remotes/origin/2.35.8-EMBARGOED_za
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

<<<<<<< HEAD
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
=======
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
>>>>>>> refs/remotes/origin/2.35.8-EMBARGOED_za

/**
 * Implementation of EventService that uses Jackson for serialization and
 * deserialization. This class has the prototype scope and can hence have class
 * scoped variables such as caches.
 *
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Service( "org.hisp.dhis.dxf2.events.event.EventService" )
@Scope( value = "prototype", proxyMode = ScopedProxyMode.INTERFACES )
public class JacksonEventService extends AbstractEventService
{
    private final EventServiceFacade jacksonEventServiceFacade;

    // -------------------------------------------------------------------------
    // EventService Impl
    // -------------------------------------------------------------------------

<<<<<<< HEAD
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
=======
    protected ObjectMapper jsonMapper;

    protected ObjectMapper xmlMapper;

    public JacksonEventService( EventImporter eventImporter, EventManager eventManager,
        WorkContextLoader workContextLoader, EventServiceFacade jacksonEventServiceFacade,
        ProgramService programService, ProgramInstanceService programInstanceService,
        ProgramStageInstanceService programStageInstanceService, OrganisationUnitService organisationUnitService,
        CurrentUserService currentUserService, TrackedEntityInstanceService entityInstanceService,
        TrackedEntityCommentService commentService, EventStore eventStore, Notifier notifier, DbmsManager dbmsManager,
        IdentifiableObjectManager manager, CategoryService categoryService, FileResourceService fileResourceService,
        SchemaService schemaService, QueryService queryService, TrackerAccessManager trackerAccessManager,
        TrackerOwnershipManager trackerOwnershipAccessManager, RelationshipService relationshipService,
        UserService userService, EventSyncService eventSyncService, ObjectMapper jsonMapper,
        @Qualifier( "xmlMapper" ) ObjectMapper xmlMapper, EventServiceContextBuilder eventServiceContextBuilder )
>>>>>>> refs/remotes/origin/2.35.8-EMBARGOED_za
    {
<<<<<<< HEAD
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
=======
        checkNotNull( eventImporter );
        checkNotNull( eventManager );
        checkNotNull( workContextLoader );
        checkNotNull( jacksonEventServiceFacade );
        checkNotNull( programService );
        checkNotNull( programInstanceService );
        checkNotNull( programStageInstanceService );
        checkNotNull( organisationUnitService );
        checkNotNull( currentUserService );
        checkNotNull( entityInstanceService );
        checkNotNull( commentService );
        checkNotNull( eventStore );
        checkNotNull( notifier );
        checkNotNull( dbmsManager );
        checkNotNull( manager );
        checkNotNull( categoryService );
        checkNotNull( fileResourceService );
        checkNotNull( schemaService );
        checkNotNull( queryService );
        checkNotNull( trackerAccessManager );
        checkNotNull( trackerOwnershipAccessManager );
        checkNotNull( userService );
        checkNotNull( eventSyncService );
        checkNotNull( jsonMapper );
        checkNotNull( xmlMapper );
        checkNotNull( eventServiceContextBuilder );

        this.eventImporter = eventImporter;
        this.eventManager = eventManager;
        this.workContextLoader = workContextLoader;
        this.jacksonEventServiceFacade = jacksonEventServiceFacade;
        this.programService = programService;
        this.programInstanceService = programInstanceService;
        this.programStageInstanceService = programStageInstanceService;
        this.organisationUnitService = organisationUnitService;
        this.currentUserService = currentUserService;
        this.entityInstanceService = entityInstanceService;
        this.commentService = commentService;
        this.eventStore = eventStore;
        this.notifier = notifier;
        this.dbmsManager = dbmsManager;
        this.manager = manager;
        this.categoryService = categoryService;
        this.fileResourceService = fileResourceService;
        this.schemaService = schemaService;
        this.queryService = queryService;
        this.trackerAccessManager = trackerAccessManager;
        this.trackerOwnershipAccessManager = trackerOwnershipAccessManager;
        this.relationshipService = relationshipService;
        this.userService = userService;
        this.eventSyncService = eventSyncService;
        this.jsonMapper = jsonMapper;
        this.xmlMapper = xmlMapper;
        this.eventServiceContextBuilder = eventServiceContextBuilder;
>>>>>>> refs/remotes/origin/2.35.8-EMBARGOED_za
    }

    @SuppressWarnings( "unchecked" )
<<<<<<< HEAD
    private <T> T fromXml( String input, Class<?> clazz ) throws IOException
=======
    private <T> T fromXml( String input, Class<?> clazz )
        throws IOException
>>>>>>> refs/remotes/origin/2.35.8-EMBARGOED_za
    {
        return (T) xmlMapper.readValue( input, clazz );
    }

    @SuppressWarnings( "unchecked" )
<<<<<<< HEAD
    private <T> T fromJson( String input, Class<?> clazz ) throws IOException
=======
    private <T> T fromJson( String input, Class<?> clazz )
        throws IOException
>>>>>>> refs/remotes/origin/2.35.8-EMBARGOED_za
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
        return jacksonEventServiceFacade.addEventsXml( inputStream, null, importOptions );
    }

    @Override
    public ImportSummaries addEventsJson( InputStream inputStream, ImportOptions importOptions )
        throws IOException
    {
        return jacksonEventServiceFacade.addEventsJson( inputStream, null, importOptions );
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
