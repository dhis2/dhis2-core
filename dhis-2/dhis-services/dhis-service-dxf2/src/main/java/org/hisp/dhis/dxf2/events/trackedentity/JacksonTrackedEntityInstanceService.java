/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.dxf2.events.trackedentity;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dbms.DbmsManager;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.events.aggregates.TrackedEntityInstanceAggregate;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.hisp.dhis.fileresource.FileResourceService;
import org.hisp.dhis.program.EnrollmentService;
import org.hisp.dhis.query.QueryService;
import org.hisp.dhis.relationship.RelationshipService;
import org.hisp.dhis.relationship.RelationshipTypeService;
import org.hisp.dhis.reservedvalue.ReservedValueService;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.system.notification.Notifier;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeStore;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceAuditService;
import org.hisp.dhis.trackedentity.TrackedEntityTypeService;
import org.hisp.dhis.trackedentity.TrackerAccessManager;
import org.hisp.dhis.trackedentity.TrackerOwnershipManager;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValueService;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.UserService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StreamUtils;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Service( "org.hisp.dhis.dxf2.events.trackedentity.TrackedEntityInstanceService" )
@Scope( value = "prototype", proxyMode = ScopedProxyMode.INTERFACES )
@Transactional
public class JacksonTrackedEntityInstanceService extends AbstractTrackedEntityInstanceService
{
    private final RelationshipTypeService relationshipTypeService;

    public JacksonTrackedEntityInstanceService(
        org.hisp.dhis.trackedentity.TrackedEntityInstanceService teiService,
        TrackedEntityAttributeService trackedEntityAttributeService,
        RelationshipService _relationshipService,
        org.hisp.dhis.dxf2.events.relationship.RelationshipService relationshipService,
        RelationshipTypeService relationshipTypeService,
        TrackedEntityAttributeValueService trackedEntityAttributeValueService,
        IdentifiableObjectManager manager,
        UserService userService,
        DbmsManager dbmsManager,
        org.hisp.dhis.dxf2.events.enrollment.EnrollmentService enrollmentService,
        EnrollmentService enrollmentService,
        CurrentUserService currentUserService,
        SchemaService schemaService,
        QueryService queryService,
        ReservedValueService reservedValueService,
        TrackerAccessManager trackerAccessManager,
        FileResourceService fileResourceService,
        TrackerOwnershipManager trackerOwnershipAccessManager,
        TrackedEntityInstanceAggregate trackedEntityInstanceAggregate,
        TrackedEntityAttributeStore trackedEntityAttributeStore,
        TrackedEntityInstanceAuditService trackedEntityInstanceAuditService,
        TrackedEntityTypeService trackedEntityTypeService,
        Notifier notifier,
        ObjectMapper jsonMapper,
        @Qualifier( "xmlMapper" ) ObjectMapper xmlMapper )
    {
        checkNotNull( teiService );
        checkNotNull( trackedEntityAttributeService );
        checkNotNull( _relationshipService );
        checkNotNull( relationshipService );
        checkNotNull( relationshipTypeService );
        checkNotNull( trackedEntityAttributeValueService );
        checkNotNull( manager );
        checkNotNull( userService );
        checkNotNull( dbmsManager );
        checkNotNull( enrollmentService );
        checkNotNull( enrollmentService );
        checkNotNull( currentUserService );
        checkNotNull( schemaService );
        checkNotNull( queryService );
        checkNotNull( reservedValueService );
        checkNotNull( trackerAccessManager );
        checkNotNull( fileResourceService );
        checkNotNull( trackerOwnershipAccessManager );
        checkNotNull( trackedEntityInstanceAggregate );
        checkNotNull( trackedEntityAttributeStore );
        checkNotNull( trackedEntityTypeService );
        checkNotNull( notifier );
        checkNotNull( jsonMapper );
        checkNotNull( xmlMapper );

        this.teiService = teiService;
        this.trackedEntityAttributeService = trackedEntityAttributeService;
        this._relationshipService = _relationshipService;
        this.relationshipService = relationshipService;
        this.relationshipTypeService = relationshipTypeService;
        this.trackedEntityAttributeValueService = trackedEntityAttributeValueService;
        this.manager = manager;
        this.userService = userService;
        this.dbmsManager = dbmsManager;
        this.enrollmentService = enrollmentService;
        this.enrollmentService = enrollmentService;
        this.currentUserService = currentUserService;
        this.schemaService = schemaService;
        this.queryService = queryService;
        this.reservedValueService = reservedValueService;
        this.trackerAccessManager = trackerAccessManager;
        this.fileResourceService = fileResourceService;
        this.trackerOwnershipAccessManager = trackerOwnershipAccessManager;
        this.trackedEntityInstanceAggregate = trackedEntityInstanceAggregate;
        this.trackedEntityAttributeStore = trackedEntityAttributeStore;
        this.trackedEntityInstanceAuditService = trackedEntityInstanceAuditService;
        this.trackedEntityTypeService = trackedEntityTypeService;
        this.notifier = notifier;
        this.jsonMapper = jsonMapper;
        this.xmlMapper = xmlMapper;
    }

    // -------------------------------------------------------------------------
    // Implementation
    // -------------------------------------------------------------------------

    @SuppressWarnings( "unchecked" )
    private <T> T fromXml( InputStream inputStream, Class<?> clazz )
        throws IOException
    {
        return (T) xmlMapper.readValue( inputStream, clazz );
    }

    @SuppressWarnings( "unchecked" )
    private <T> T fromXml( String input, Class<?> clazz )
        throws IOException
    {
        return (T) xmlMapper.readValue( input, clazz );
    }

    @SuppressWarnings( "unchecked" )
    private <T> T fromJson( InputStream inputStream, Class<?> clazz )
        throws IOException
    {
        return (T) jsonMapper.readValue( inputStream, clazz );
    }

    @SuppressWarnings( "unchecked" )
    private <T> T fromJson( String input, Class<?> clazz )
        throws IOException
    {
        return (T) jsonMapper.readValue( input, clazz );
    }

    // -------------------------------------------------------------------------
    // CREATE
    // -------------------------------------------------------------------------

    @Override
    public List<TrackedEntityInstance> getTrackedEntityInstancesJson( InputStream inputStream )
        throws IOException
    {
        String input = StreamUtils.copyToString( inputStream, Charset.forName( "UTF-8" ) );

        return parseJsonTrackedEntityInstances( input );
    }

    @Override
    public List<TrackedEntityInstance> getTrackedEntityInstancesXml( InputStream inputStream )
        throws IOException
    {
        String input = StreamUtils.copyToString( inputStream, Charset.forName( "UTF-8" ) );

        return parseXmlTrackedEntityInstances( input );
    }

    private List<TrackedEntityInstance> parseJsonTrackedEntityInstances( String input )
        throws IOException
    {
        List<TrackedEntityInstance> trackedEntityInstances = new ArrayList<>();

        JsonNode root = jsonMapper.readTree( input );

        if ( root.get( "trackedEntityInstances" ) != null )
        {
            TrackedEntityInstances fromJson = fromJson( input, TrackedEntityInstances.class );
            trackedEntityInstances.addAll( fromJson.getTrackedEntityInstances() );
        }
        else
        {
            TrackedEntityInstance fromJson = fromJson( input, TrackedEntityInstance.class );
            trackedEntityInstances.add( fromJson );
        }

        return trackedEntityInstances;
    }

    private List<TrackedEntityInstance> parseXmlTrackedEntityInstances( String input )
        throws IOException
    {
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

        return trackedEntityInstances;
    }

    // -------------------------------------------------------------------------
    // UPDATE
    // -------------------------------------------------------------------------

    @Override
    public ImportSummary updateTrackedEntityInstanceXml( String id, String programId, InputStream inputStream,
        ImportOptions importOptions )
        throws IOException
    {
        TrackedEntityInstance trackedEntityInstance = fromXml( inputStream, TrackedEntityInstance.class );
        return updateTrackedEntityInstance( id, programId, importOptions, trackedEntityInstance );
    }

    @Override
    public ImportSummary updateTrackedEntityInstanceJson( String id, String programId, InputStream inputStream,
        ImportOptions importOptions )
        throws IOException
    {
        TrackedEntityInstance trackedEntityInstance = fromJson( inputStream, TrackedEntityInstance.class );
        return updateTrackedEntityInstance( id, programId, importOptions, trackedEntityInstance );
    }

    private ImportSummary updateTrackedEntityInstance( String id, String programId, ImportOptions importOptions,
        TrackedEntityInstance trackedEntityInstance )
    {
        trackedEntityInstance.setTrackedEntityInstance( id );
        setTeiRelationshipsBidirectionalFlag( trackedEntityInstance );
        return updateTrackedEntityInstance( trackedEntityInstance, programId, updateImportOptions( importOptions ),
            true );
    }

    private void setTeiRelationshipsBidirectionalFlag( TrackedEntityInstance trackedEntityInstance )
    {
        Optional.ofNullable( trackedEntityInstance )
            .map( TrackedEntityInstance::getRelationships )
            .ifPresent( relationships -> relationships.forEach( this::setTeiRelationshipBidirectionalFlag ) );
    }

    private void setTeiRelationshipBidirectionalFlag( Relationship relationship )
    {
        Optional.of( relationship )
            .map( Relationship::getRelationshipType )
            .map( relationshipTypeService::getRelationshipType )
            .ifPresent( relationshipType -> relationship.setBidirectional( relationshipType.isBidirectional() ) );
    }
}
