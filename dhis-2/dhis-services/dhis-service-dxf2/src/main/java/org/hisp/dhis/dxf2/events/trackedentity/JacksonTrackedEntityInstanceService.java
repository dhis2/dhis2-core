package org.hisp.dhis.dxf2.events.trackedentity;

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
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dbms.DbmsManager;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.events.aggregates.TrackedEntityInstanceAggregate;
import org.hisp.dhis.dxf2.events.enrollment.EnrollmentService;
import org.hisp.dhis.dxf2.importsummary.ImportSummaries;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.hisp.dhis.dxf2.metadata.feedback.ImportReportMode;
import org.hisp.dhis.fileresource.FileResourceService;
import org.hisp.dhis.program.ProgramInstanceService;
import org.hisp.dhis.query.QueryService;
import org.hisp.dhis.relationship.RelationshipService;
import org.hisp.dhis.reservedvalue.ReservedValueService;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.system.notification.Notifier;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeStore;
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

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Service( "org.hisp.dhis.dxf2.events.trackedentity.TrackedEntityInstanceService" )
@Scope( value = "prototype", proxyMode = ScopedProxyMode.INTERFACES )
@Transactional
public class JacksonTrackedEntityInstanceService extends AbstractTrackedEntityInstanceService
{
    public JacksonTrackedEntityInstanceService(
            org.hisp.dhis.trackedentity.TrackedEntityInstanceService teiService,
            TrackedEntityAttributeService trackedEntityAttributeService,
            RelationshipService _relationshipService,
            org.hisp.dhis.dxf2.events.relationship.RelationshipService relationshipService,
            TrackedEntityAttributeValueService trackedEntityAttributeValueService,
            IdentifiableObjectManager manager,
            UserService userService,
            DbmsManager dbmsManager,
            EnrollmentService enrollmentService,
            ProgramInstanceService programInstanceService,
            CurrentUserService currentUserService,
            SchemaService schemaService,
            QueryService queryService,
            ReservedValueService reservedValueService,
            TrackerAccessManager trackerAccessManager,
            FileResourceService fileResourceService,
            TrackerOwnershipManager trackerOwnershipAccessManager,
            TrackedEntityInstanceAggregate trackedEntityInstanceAggregate,
            TrackedEntityAttributeStore trackedEntityAttributeStore,
            Notifier notifier,
            ObjectMapper jsonMapper,
            @Qualifier( "xmlMapper" ) ObjectMapper xmlMapper )
    {
        checkNotNull( teiService );
        checkNotNull( trackedEntityAttributeService );
        checkNotNull( _relationshipService );
        checkNotNull( relationshipService );
        checkNotNull( trackedEntityAttributeValueService );
        checkNotNull( manager );
        checkNotNull( userService );
        checkNotNull( dbmsManager );
        checkNotNull( enrollmentService );
        checkNotNull( programInstanceService );
        checkNotNull( currentUserService );
        checkNotNull( schemaService );
        checkNotNull( queryService );
        checkNotNull( reservedValueService );
        checkNotNull( trackerAccessManager );
        checkNotNull( fileResourceService );
        checkNotNull( trackerOwnershipAccessManager );
        checkNotNull( trackedEntityInstanceAggregate );
        checkNotNull( trackedEntityAttributeStore );
        checkNotNull( notifier );
        checkNotNull( jsonMapper );
        checkNotNull( xmlMapper );

        this.teiService = teiService;
        this.trackedEntityAttributeService = trackedEntityAttributeService;
        this._relationshipService = _relationshipService;
        this.relationshipService = relationshipService;
        this.trackedEntityAttributeValueService = trackedEntityAttributeValueService;
        this.manager = manager;
        this.userService = userService;
        this.dbmsManager = dbmsManager;
        this.enrollmentService = enrollmentService;
        this.programInstanceService = programInstanceService;
        this.currentUserService = currentUserService;
        this.schemaService = schemaService;
        this.queryService = queryService;
        this.reservedValueService = reservedValueService;
        this.trackerAccessManager = trackerAccessManager;
        this.fileResourceService = fileResourceService;
        this.trackerOwnershipAccessManager = trackerOwnershipAccessManager;
        this.trackedEntityInstanceAggregate = trackedEntityInstanceAggregate;
        this.trackedEntityAttributeStore = trackedEntityAttributeStore;
        this.notifier = notifier;
        this.jsonMapper = jsonMapper;
        this.xmlMapper = xmlMapper;
    }

    // -------------------------------------------------------------------------
    // Implementation
    // -------------------------------------------------------------------------

    @SuppressWarnings( "unchecked" )
    private <T> T fromXml( InputStream inputStream, Class<?> clazz ) throws IOException
    {
        return (T) xmlMapper.readValue( inputStream, clazz );
    }

    @SuppressWarnings( "unchecked" )
    private <T> T fromXml( String input, Class<?> clazz ) throws IOException
    {
        return (T) xmlMapper.readValue( input, clazz );
    }

    @SuppressWarnings( "unchecked" )
    private <T> T fromJson( InputStream inputStream, Class<?> clazz ) throws IOException
    {
        return (T) jsonMapper.readValue( inputStream, clazz );
    }

    @SuppressWarnings( "unchecked" )
    private <T> T fromJson( String input, Class<?> clazz ) throws IOException
    {
        return (T) jsonMapper.readValue( input, clazz );
    }

    // -------------------------------------------------------------------------
    // CREATE
    // -------------------------------------------------------------------------

    @Override
    public List<TrackedEntityInstance> getTrackedEntityInstancesJson( InputStream inputStream ) throws IOException
    {
        String input = StreamUtils.copyToString( inputStream, Charset.forName( "UTF-8" ) );

        return parseJsonTrackedEntityInstances( input );
    }

    @Override
    public List<TrackedEntityInstance> getTrackedEntityInstancesXml( InputStream inputStream ) throws IOException
    {
        String input = StreamUtils.copyToString( inputStream, Charset.forName( "UTF-8" ) );

        return parseXmlTrackedEntityInstances( input );
    }

    @Override
    public ImportSummaries addTrackedEntityInstanceXml( InputStream inputStream, ImportOptions importOptions ) throws IOException
    {
        String input = StreamUtils.copyToString( inputStream, Charset.forName( "UTF-8" ) );
        List<TrackedEntityInstance> trackedEntityInstances = parseXmlTrackedEntityInstances( input );

        return addTrackedEntityInstanceList( trackedEntityInstances, updateImportOptions( importOptions ) );
    }

    @Override
    public ImportSummaries addTrackedEntityInstanceJson( InputStream inputStream, ImportOptions importOptions ) throws IOException
    {
        String input = StreamUtils.copyToString( inputStream, Charset.forName( "UTF-8" ) );
        List<TrackedEntityInstance> trackedEntityInstances = parseJsonTrackedEntityInstances( input );

        return addTrackedEntityInstanceList( trackedEntityInstances, updateImportOptions( importOptions ) );
    }

    private List<TrackedEntityInstance> parseJsonTrackedEntityInstances( String input ) throws IOException
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

    private List<TrackedEntityInstance> parseXmlTrackedEntityInstances( String input ) throws IOException
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

    private ImportSummaries addTrackedEntityInstanceList( List<TrackedEntityInstance> trackedEntityInstances, ImportOptions importOptions )
    {
        ImportSummaries importSummaries = new ImportSummaries();
        importOptions = updateImportOptions( importOptions );

        List<TrackedEntityInstance> create = new ArrayList<>();
        List<TrackedEntityInstance> update = new ArrayList<>();
        List<TrackedEntityInstance> delete = new ArrayList<>();

        //TODO: Check whether relationships are modified during create/update/delete TEI logic. Decide whether logic below can be removed
        List<Relationship> relationships = new ArrayList<>();
        trackedEntityInstances.stream()
            .filter( tei -> !tei.getRelationships().isEmpty() )
            .forEach( tei ->
            {
                RelationshipItem item = new RelationshipItem();
                item.setTrackedEntityInstance( tei );

                tei.getRelationships().forEach( rel ->
                {
                    // Update from if it is empty. Current tei is then "from"
                    if ( rel.getFrom() == null )
                    {
                        rel.setFrom( item );
                    }
                    relationships.add( rel );
                } );
            } );

        if ( importOptions.getImportStrategy().isCreate() )
        {
            create.addAll( trackedEntityInstances );
        }
        else if ( importOptions.getImportStrategy().isCreateAndUpdate() )
        {
            sortCreatesAndUpdates( trackedEntityInstances, create, update );
        }
        else if ( importOptions.getImportStrategy().isUpdate() )
        {
            update.addAll( trackedEntityInstances );
        }
        else if ( importOptions.getImportStrategy().isDelete() )
        {
            delete.addAll( trackedEntityInstances );
        }
        else if ( importOptions.getImportStrategy().isSync() )
        {
            for ( TrackedEntityInstance trackedEntityInstance : trackedEntityInstances )
            {
                if ( trackedEntityInstance.isDeleted() )
                {
                    delete.add( trackedEntityInstance );
                }
                else
                {
                    sortCreatesAndUpdates( trackedEntityInstance, create, update );
                }
            }
        }

        importSummaries.addImportSummaries( addTrackedEntityInstances( create, importOptions ) );
        importSummaries.addImportSummaries( updateTrackedEntityInstances( update, importOptions ) );
        importSummaries.addImportSummaries( deleteTrackedEntityInstances( delete, importOptions ) );

        //TODO: Created importSummaries don't contain correct href (TEI endpoint instead of relationships is used)
        importSummaries.addImportSummaries( relationshipService.processRelationshipList( relationships, importOptions ) );

        if ( ImportReportMode.ERRORS == importOptions.getReportMode() )
        {
            importSummaries.getImportSummaries().removeIf( is -> is.getConflicts().isEmpty() );
        }

        return importSummaries;
    }

    private void sortCreatesAndUpdates( List<TrackedEntityInstance> trackedEntityInstances, List<TrackedEntityInstance> create, List<TrackedEntityInstance> update )
    {
        List<String> ids = trackedEntityInstances.stream().map( TrackedEntityInstance::getTrackedEntityInstance ).collect( Collectors.toList() );
        List<String> existingUids = teiService.getTrackedEntityInstancesUidsIncludingDeleted( ids );

        for ( TrackedEntityInstance trackedEntityInstance : trackedEntityInstances )
        {
            if ( StringUtils.isEmpty( trackedEntityInstance.getTrackedEntityInstance() ) || !existingUids.contains( trackedEntityInstance.getTrackedEntityInstance() ) )
            {
                create.add( trackedEntityInstance );
            }
            else
            {
                update.add( trackedEntityInstance );
            }
        }
    }

    private void sortCreatesAndUpdates( TrackedEntityInstance trackedEntityInstance, List<TrackedEntityInstance> create, List<TrackedEntityInstance> update )
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

    // -------------------------------------------------------------------------
    // UPDATE
    // -------------------------------------------------------------------------

    @Override
    public ImportSummary updateTrackedEntityInstanceXml( String id, String programId, InputStream inputStream, ImportOptions importOptions ) throws IOException
    {
        TrackedEntityInstance trackedEntityInstance = fromXml( inputStream, TrackedEntityInstance.class );
        trackedEntityInstance.setTrackedEntityInstance( id );

        return updateTrackedEntityInstance( trackedEntityInstance, programId, updateImportOptions( importOptions ), true );
    }

    @Override
    public ImportSummary updateTrackedEntityInstanceJson( String id, String programId, InputStream inputStream, ImportOptions importOptions ) throws IOException
    {
        TrackedEntityInstance trackedEntityInstance = fromJson( inputStream, TrackedEntityInstance.class );
        trackedEntityInstance.setTrackedEntityInstance( id );

        return updateTrackedEntityInstance( trackedEntityInstance, programId, updateImportOptions( importOptions ), true );
    }
}
