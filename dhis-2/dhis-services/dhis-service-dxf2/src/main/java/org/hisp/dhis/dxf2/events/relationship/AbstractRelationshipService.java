package org.hisp.dhis.dxf2.events.relationship;

/*
 * Copyright (c) 2004-2018, University of Oslo
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

import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.commons.collection.ListUtils;
import org.hisp.dhis.dbms.DbmsManager;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.events.TrackerAccessManager;
import org.hisp.dhis.dxf2.events.trackedentity.Relationship;
import org.hisp.dhis.dxf2.importsummary.ImportConflict;
import org.hisp.dhis.dxf2.importsummary.ImportStatus;
import org.hisp.dhis.dxf2.importsummary.ImportSummaries;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.query.Query;
import org.hisp.dhis.query.QueryService;
import org.hisp.dhis.query.Restriction;
import org.hisp.dhis.query.Restrictions;
import org.hisp.dhis.relationship.RelationshipEntity;
import org.hisp.dhis.relationship.RelationshipItem;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static org.hisp.dhis.relationship.RelationshipEntity.PROGRAM_INSTANCE;
import static org.hisp.dhis.relationship.RelationshipEntity.PROGRAM_STAGE_INSTANCE;
import static org.hisp.dhis.relationship.RelationshipEntity.TRACKED_ENTITY_INSTANCE;

public abstract class AbstractRelationshipService
    implements RelationshipService
{
    private static Logger log = Logger.getLogger( AbstractRelationshipService.class.getSimpleName() );

    @Autowired
    protected DbmsManager dbmsManager;

    @Autowired
    private CurrentUserService currentUserService;

    @Autowired
    private SchemaService schemaService;

    @Autowired
    private QueryService queryService;

    @Autowired
    private TrackerAccessManager trackerAccessManager;

    @Autowired
    private org.hisp.dhis.relationship.RelationshipService relationshipService;

    private HashMap<String, RelationshipType> relationshipTypeCache = new HashMap<>();

    private HashMap<String, TrackedEntityInstance> trackedEntityInstanceCache = new HashMap<>();

    private HashMap<String, ProgramInstance> programInstanceCache = new HashMap<>();

    private HashMap<String, ProgramStageInstance> programStageInstanceCache = new HashMap<>();

    @Override
    public ImportSummaries addRelationships( List<Relationship> relationships, ImportOptions importOptions )
    {
        List<List<Relationship>> partitions = Lists.partition( relationships, FLUSH_FREQUENCY );
        importOptions = updateImportOptions( importOptions );

        ImportSummaries importSummaries = new ImportSummaries();

        for ( List<Relationship> _relationships : partitions )
        {
            prepareCaches( _relationships, importOptions.getUser() );

            for ( Relationship relationship : _relationships )
            {
                importSummaries.addImportSummary( addRelationship( relationship, importOptions ) );
            }

            clearSession();
        }

        return importSummaries;
    }

    @Override
    public ImportSummary addRelationship( Relationship relationship, ImportOptions importOptions )
    {
        importOptions = updateImportOptions( importOptions );

        if ( relationshipService.relationshipExists( relationship.getRelationship() ) )
        {
            String message = "Relationship " + relationship.getRelationship() +
                " already exists";
            return new ImportSummary( ImportStatus.ERROR, message )
                .setReference( relationship.getRelationship() ).incrementIgnored();
        }

        ImportSummary importSummary = new ImportSummary( relationship.getRelationship() );

        Set<ImportConflict> importConflicts = new HashSet<>();
        // Check conflicts!
        // importConflicts.addAll( checkTrackedEntityType( dtoEntityInstance, importOptions ) );
        // importConflicts.addAll( checkAttributes( dtoEntityInstance, importOptions ) );

        if ( !importConflicts.isEmpty() )
        {
            importSummary.setConflicts( importConflicts );
            importSummary.setStatus( ImportStatus.ERROR );
            importSummary.getImportCount().incrementIgnored();
            return importSummary;
        }

        org.hisp.dhis.relationship.Relationship daoRelationship = createDAORelationship(
            relationship, importOptions, importSummary );

        if ( daoRelationship == null )
        {
            return importSummary;
        }

        // Fix access manager
        // List<String> errors = trackerAccessManager.canWrite( importOptions.getUser(), daoRelationship.getFrom(). );

        /*
        if ( !errors.isEmpty() )
        {
            return new ImportSummary( ImportStatus.ERROR, errors.toString() )
                .incrementIgnored();
        }
        */

        relationshipService.addRelationship( daoRelationship );

        importSummary.setReference( daoRelationship.getUid() );
        importSummary.getImportCount().incrementImported();

        return importSummary;
    }

    protected org.hisp.dhis.relationship.Relationship createDAORelationship( Relationship relationship,
        ImportOptions importOptions, ImportSummary importSummary )
    {
        RelationshipType relationshipType = relationshipTypeCache.get( relationship.getRelationshipType() );
        org.hisp.dhis.relationship.Relationship daoRelationship = new org.hisp.dhis.relationship.Relationship();
        RelationshipItem fromItem = null;
        RelationshipItem toItem = null;

        if ( StringUtils.isEmpty( relationship.getRelationshipType() ) )
        {
            importSummary.getConflicts()
                .add( new ImportConflict( relationship.getRelationship(), "Missing property 'relationshipType'" ) );
            return null;
        }

        if ( StringUtils.isEmpty( relationship.getFrom() ) )
        {
            importSummary.getConflicts()
                .add( new ImportConflict( relationship.getRelationship(), "Missing property 'from'" ) );
            return null;
        }

        if ( StringUtils.isEmpty( relationship.getTo() ) )
        {
            importSummary.getConflicts()
                .add( new ImportConflict( relationship.getRelationship(), "Missing property 'to'" ) );
            return null;
        }

        daoRelationship.setRelationshipType( relationshipType );

        // FROM
        if ( relationshipType.getFromConstraint().getRelationshipEntity().equals( TRACKED_ENTITY_INSTANCE ) )
        {
            fromItem = new RelationshipItem();
            fromItem.setTrackedEntityInstance( trackedEntityInstanceCache.get( relationship.getFrom() ) );
        }
        else if ( relationshipType.getFromConstraint().getRelationshipEntity().equals( PROGRAM_INSTANCE ) )
        {
            fromItem = new RelationshipItem();
            fromItem.setProgramInstance( programInstanceCache.get( relationship.getFrom() ) );
        }
        else if ( relationshipType.getFromConstraint().getRelationshipEntity().equals( PROGRAM_STAGE_INSTANCE ) )
        {
            fromItem = new RelationshipItem();
            fromItem.setProgramStageInstance( programStageInstanceCache.get( relationship.getFrom() ) );
        }

        // TO
        if ( relationshipType.getToConstraint().getRelationshipEntity().equals( TRACKED_ENTITY_INSTANCE ) )
        {
            toItem = new RelationshipItem();
            toItem.setTrackedEntityInstance( trackedEntityInstanceCache.get( relationship.getTo() ) );
        }
        else if ( relationshipType.getToConstraint().getRelationshipEntity().equals( PROGRAM_INSTANCE ) )
        {
            toItem = new RelationshipItem();
            toItem.setProgramInstance( programInstanceCache.get( relationship.getTo() ) );
        }
        else if ( relationshipType.getToConstraint().getRelationshipEntity().equals( PROGRAM_STAGE_INSTANCE ) )
        {
            toItem = new RelationshipItem();
            toItem.setProgramStageInstance( programStageInstanceCache.get( relationship.getTo() ) );
        }

        if ( fromItem == null )
        {
            importSummary.getConflicts().add( new ImportConflict( relationship.getRelationship(),
                "Could not find " + relationshipType.getFromConstraint().getRelationshipEntity().getName() +
                    " with id '" + relationship.getFrom() + "'." ) );
            return null;
        }

        if ( toItem == null )
        {
            importSummary.getConflicts().add( new ImportConflict( relationship.getRelationship(),
                "Could not find " + relationshipType.getToConstraint().getRelationshipEntity().getName() +
                    " with id '" + relationship.getTo() + "'." ) );
            return null;
        }

        daoRelationship.setFrom( fromItem );
        daoRelationship.setTo( toItem );

        System.out.println( "Created: " + daoRelationship );

        return daoRelationship;
    }

    private void prepareCaches( List<Relationship> relationships, User user )
    {
        Map<RelationshipEntity, List<String>> relationshipEntities = new HashMap<>();
        Map<String, List<Relationship>> relationshipTypeMap = relationships.stream()
            .collect( Collectors.groupingBy( Relationship::getRelationshipType ) );

        // Find all the RelationshipTypes first, so we know what the uids refer to
        Query query = Query.from( schemaService.getDynamicSchema( RelationshipType.class ) );
        query.setUser( user );
        query.add( Restrictions.in( "id", relationshipTypeMap.keySet() ) );
        queryService.query( query ).forEach( rt -> relationshipTypeCache.put( rt.getUid(), (RelationshipType) rt ) );

        // Group all uids into their respective RelationshipEntities
        relationshipTypeCache.values().stream().forEach( relationshipType -> {
            List<String> fromUids = relationshipTypeMap.get( relationshipType.getUid() ).stream()
                .map( Relationship::getFrom ).collect( Collectors.toList() );

            List<String> toUids = relationshipTypeMap.get( relationshipType.getUid() ).stream()
                .map( Relationship::getTo ).collect( Collectors.toList() );

            // Merge existing results with newly found ones.

            relationshipEntities.merge( relationshipType.getFromConstraint().getRelationshipEntity(), fromUids,
                ( old, _new ) -> ListUtils.union( old, _new ) );

            relationshipEntities.merge( relationshipType.getToConstraint().getRelationshipEntity(), toUids,
                ( old, _new ) -> ListUtils.union( old, _new ) );
        } );

        // Find and put all Relationship members in their respective cache

        if ( relationshipEntities.get( TRACKED_ENTITY_INSTANCE ) != null )
        {
            Query teiQuery = Query.from( schemaService.getDynamicSchema( TrackedEntityInstance.class ) );
            teiQuery.setUser( user );
            teiQuery.add( Restrictions.in( "id", relationshipEntities.get( TRACKED_ENTITY_INSTANCE ) ) );
            queryService.query( teiQuery )
                .forEach( tei -> trackedEntityInstanceCache.put( tei.getUid(), (TrackedEntityInstance) tei ) );
        }

        if ( relationshipEntities.get( PROGRAM_INSTANCE ) != null )
        {
            Query piQuery = Query.from( schemaService.getDynamicSchema( ProgramInstance.class ) );
            piQuery.setUser( user );
            piQuery.add( Restrictions.in( "id", relationshipEntities.get( PROGRAM_INSTANCE ) ) );
            queryService.query( piQuery )
                .forEach( pi -> programInstanceCache.put( pi.getUid(), (ProgramInstance) pi ) );
        }
        if ( relationshipEntities.get( PROGRAM_STAGE_INSTANCE ) != null )
        {
            Query psiQuery = Query.from( schemaService.getDynamicSchema( ProgramStageInstance.class ) );
            psiQuery.setUser( user );
            psiQuery.add( Restrictions.in( "id", relationshipEntities.get( PROGRAM_STAGE_INSTANCE ) ) );
            queryService.query( psiQuery )
                .forEach( psi -> trackedEntityInstanceCache.put( psi.getUid(), (TrackedEntityInstance) psi ) );
        }
    }

    private void clearSession()
    {
        relationshipTypeCache.clear();
        trackedEntityInstanceCache.clear();
        programInstanceCache.clear();
        programStageInstanceCache.clear();

        dbmsManager.clearSession();
    }

    protected ImportOptions updateImportOptions( ImportOptions importOptions )
    {
        if ( importOptions == null )
        {
            importOptions = new ImportOptions();
        }

        if ( importOptions.getUser() == null )
        {
            importOptions.setUser( currentUserService.getCurrentUser() );
        }

        return importOptions;
    }
}
