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
import org.hisp.dhis.common.IdSchemes;
import org.hisp.dhis.commons.collection.ListUtils;
import org.hisp.dhis.dbms.DbmsManager;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.events.TrackerAccessManager;
import org.hisp.dhis.dxf2.events.trackedentity.Relationship;
import org.hisp.dhis.dxf2.importsummary.ImportConflict;
import org.hisp.dhis.dxf2.importsummary.ImportStatus;
import org.hisp.dhis.dxf2.importsummary.ImportSummaries;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramInstanceService;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.program.ProgramStageInstanceService;
import org.hisp.dhis.query.Query;
import org.hisp.dhis.query.QueryService;
import org.hisp.dhis.query.Restriction;
import org.hisp.dhis.query.Restrictions;
import org.hisp.dhis.relationship.RelationshipConstraint;
import org.hisp.dhis.relationship.RelationshipEntity;
import org.hisp.dhis.relationship.RelationshipItem;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.relationship.RelationshipTypeService;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceService;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.springframework.beans.factory.annotation.Autowired;

import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.Collection;
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

@Transactional
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
    private RelationshipTypeService relationshipTypeService;

    @Autowired
    private TrackedEntityInstanceService trackedEntityInstanceService;

    @Autowired
    private ProgramInstanceService programInstanceService;

    @Autowired
    private ProgramStageInstanceService programStageInstanceService;

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
        ImportSummary importSummary = new ImportSummary( relationship.getRelationship() );
        Set<ImportConflict> importConflicts = new HashSet<>();

        importOptions = updateImportOptions( importOptions );

        // Set up cache if not set already
        if ( !cacheExists() )
        {
            prepareCaches( Lists.newArrayList( relationship ), importOptions.getUser() );
        }

        if ( relationshipService.relationshipExists( relationship.getRelationship() ) )
        {
            String message = "Relationship " + relationship.getRelationship() +
                " already exists";
            return new ImportSummary( ImportStatus.ERROR, message )
                .setReference( relationship.getRelationship() )
                .incrementIgnored();
        }

        importConflicts.addAll( checkRelationship( relationship, importOptions ) );

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

        // Check access for both sides
        List<String> errors = new ArrayList<>();
        errors.addAll( trackerAccessManager
            .canWrite( importOptions.getUser(), daoRelationship.getFrom().getTrackedEntityInstance() ) );
        errors.addAll(
            trackerAccessManager.canWrite( importOptions.getUser(), daoRelationship.getFrom().getProgramInstance() ) );
        errors.addAll( trackerAccessManager
            .canWrite( importOptions.getUser(), daoRelationship.getFrom().getProgramStageInstance() ) );

        errors.addAll( trackerAccessManager
            .canWrite( importOptions.getUser(), daoRelationship.getTo().getTrackedEntityInstance() ) );
        errors.addAll(
            trackerAccessManager.canWrite( importOptions.getUser(), daoRelationship.getTo().getProgramInstance() ) );
        errors.addAll( trackerAccessManager
            .canWrite( importOptions.getUser(), daoRelationship.getTo().getProgramStageInstance() ) );

        if ( !errors.isEmpty() )
        {
            return new ImportSummary( ImportStatus.ERROR, errors.toString() )
                .incrementIgnored();
        }

        relationshipService.addRelationship( daoRelationship );

        importSummary.setReference( daoRelationship.getUid() );
        importSummary.getImportCount().incrementImported();

        return importSummary;
    }

    @Override
    public ImportSummaries updateRelationships( List<Relationship> relationships, ImportOptions importOptions )
    {
        importOptions = updateImportOptions( importOptions );
        List<List<Relationship>> partitions = Lists.partition( relationships, FLUSH_FREQUENCY );

        ImportSummaries importSummaries = new ImportSummaries();

        for ( List<Relationship> _relationships : partitions )
        {
            prepareCaches( _relationships, importOptions.getUser() );

            for ( Relationship relationship : _relationships )
            {
                importSummaries.addImportSummary( updateRelationship( relationship, importOptions ) );
            }

            clearSession();
        }

        return importSummaries;
    }

    @Override
    public ImportSummary updateRelationship( Relationship relationship, ImportOptions importOptions )
    {
        ImportSummary importSummary = new ImportSummary( relationship.getRelationship() );
        importOptions = updateImportOptions( importOptions );

        Set<ImportConflict> importConflicts = new HashSet<>();
        importConflicts.addAll( checkRelationship( relationship, importOptions ) );

        org.hisp.dhis.relationship.Relationship daoRelationship = relationshipService
            .getRelationship( relationship.getRelationship() );

        if ( daoRelationship == null )
        {
            String message =
                "Relationship '" + relationship.getRelationship() + "' does not exist";
            importConflicts.add( new ImportConflict( "Relationship", message ) );
            return importSummary;
        }

        // Check access for both sides
        List<String> errors = new ArrayList<>();
        errors.addAll( trackerAccessManager
            .canWrite( importOptions.getUser(), daoRelationship.getFrom().getTrackedEntityInstance() ) );
        errors.addAll(
            trackerAccessManager.canWrite( importOptions.getUser(), daoRelationship.getFrom().getProgramInstance() ) );
        errors.addAll( trackerAccessManager
            .canWrite( importOptions.getUser(), daoRelationship.getFrom().getProgramStageInstance() ) );

        errors.addAll( trackerAccessManager
            .canWrite( importOptions.getUser(), daoRelationship.getTo().getTrackedEntityInstance() ) );
        errors.addAll(
            trackerAccessManager.canWrite( importOptions.getUser(), daoRelationship.getTo().getProgramInstance() ) );
        errors.addAll( trackerAccessManager
            .canWrite( importOptions.getUser(), daoRelationship.getTo().getProgramStageInstance() ) );


        if ( !errors.isEmpty() || !importConflicts.isEmpty() )
        {
            importSummary.setStatus( ImportStatus.ERROR );
            importSummary.getImportCount().incrementIgnored();

            if ( !errors.isEmpty() )
            {
                importSummary.setDescription( errors.toString() );
            }

            importSummary.setConflicts( importConflicts );
            return importSummary;
        }

        org.hisp.dhis.relationship.Relationship _relationship = createDAORelationship( relationship, importOptions, importSummary );


        daoRelationship.setRelationshipType( _relationship.getRelationshipType() );
        daoRelationship.setTo( _relationship.getTo() );
        daoRelationship.setFrom( _relationship.getFrom() );

        relationshipService.updateRelationship( daoRelationship );

        importSummary.setReference( daoRelationship.getUid() );
        importSummary.getImportCount().incrementUpdated();

        return importSummary;
    }

    /**
     * Checks the relationship for any conflicts, like missing or invalid references.
     *
     * @param relationship
     * @param importOptions
     * @return
     */
    private List<ImportConflict> checkRelationship( Relationship relationship, ImportOptions importOptions )
    {
        List<ImportConflict> conflicts = new ArrayList<>();

        RelationshipType relationshipType = null;

        if ( StringUtils.isEmpty( relationship.getRelationshipType() ) )
        {
            conflicts
                .add( new ImportConflict( relationship.getRelationship(), "Missing property 'relationshipType'" ) );
        }
        else
        {
            relationshipType = relationshipTypeCache.get( relationship.getRelationshipType() );
        }

        if ( StringUtils.isEmpty( relationship.getFrom() ) )
        {
            conflicts.add( new ImportConflict( relationship.getRelationship(), "Missing property 'from'" ) );
        }

        if ( StringUtils.isEmpty( relationship.getTo() ) )
        {
            conflicts.add( new ImportConflict( relationship.getRelationship(), "Missing property 'to'" ) );
        }

        if ( !conflicts.isEmpty() )
        {
            return conflicts;
        }

        if ( relationshipType == null )
        {
            conflicts.add( new ImportConflict( relationship.getRelationship(),
                "relationshipType '" + relationship.getRelationshipType() + "' not found." ) );
            return conflicts;
        }

        conflicts.addAll(
            getRelationshipConstraintConflicts( relationshipType.getFromConstraint(), relationship.getFrom(),
                relationship.getRelationship() ) );
        conflicts.addAll( getRelationshipConstraintConflicts( relationshipType.getToConstraint(), relationship.getTo(),
            relationship.getRelationship() ) );

        return conflicts;
    }

    /**
     * Finds and returns any conflicts between relationship and relationship type
     *
     * @param constraint      the constraint to check
     * @param constraintUid   the uid of the entity to check
     * @param relationshipUid the uid of the relationship
     * @return a list of conflicts
     */
    private List<ImportConflict> getRelationshipConstraintConflicts( RelationshipConstraint constraint,
        String constraintUid, String relationshipUid )
    {
        List<ImportConflict> conflicts = new ArrayList<>();
        RelationshipEntity entity = constraint.getRelationshipEntity();

        if ( TRACKED_ENTITY_INSTANCE.equals( entity ) )
        {
            TrackedEntityInstance tei = trackedEntityInstanceCache.get( constraintUid );

            if ( tei == null )
            {
                conflicts.add( new ImportConflict( relationshipUid,
                    "TrackedEntityInstance '" + constraintUid + "' not found." ) );
            }
            else if ( !tei.getTrackedEntityType().equals( constraint.getTrackedEntityType() ) )
            {
                conflicts.add( new ImportConflict( relationshipUid,
                    "TrackedEntityInstance '" + constraintUid + "' has invalid TrackedEntityType." ) );
            }
        }
        else if ( PROGRAM_INSTANCE.equals( entity ) )
        {
            ProgramInstance pi = programInstanceCache.get( constraintUid );

            if ( pi == null )
            {
                conflicts.add( new ImportConflict( relationshipUid,
                    "ProgramInstance '" + constraintUid + "' not found." ) );
            }
            else if ( !pi.getProgram().equals( constraint.getProgram() ) )
            {
                conflicts.add( new ImportConflict( relationshipUid,
                    "ProgramInstance '" + constraintUid + "' has invalid Program." ) );
            }
        }
        else if ( PROGRAM_STAGE_INSTANCE.equals( entity ) )
        {
            ProgramStageInstance psi = programStageInstanceCache.get( constraintUid );

            if ( psi == null )
            {
                conflicts.add( new ImportConflict( relationshipUid,
                    "ProgramStageInstance '" + constraintUid + "' not found." ) );
            }
            else
            {
                if ( constraint.getProgram() != null &&
                    !psi.getProgramStage().getProgram().equals( constraint.getProgram() ) )
                {
                    conflicts.add( new ImportConflict( relationshipUid,
                        "ProgramStageInstance '" + constraintUid + "' has invalid Program." ) );
                }
                else if ( constraint.getProgramStage() != null &&
                    !psi.getProgramStage().equals( constraint.getProgramStage() ) )
                {
                    conflicts.add( new ImportConflict( relationshipUid,
                        "ProgramStageInstance '" + constraintUid + "' has invalid ProgramStage." ) );
                }
            }
        }

        return conflicts;
    }

    protected org.hisp.dhis.relationship.Relationship createDAORelationship( Relationship relationship,
        ImportOptions importOptions, ImportSummary importSummary )
    {
        RelationshipType relationshipType = relationshipTypeCache.get( relationship.getRelationshipType() );
        org.hisp.dhis.relationship.Relationship daoRelationship = new org.hisp.dhis.relationship.Relationship();
        RelationshipItem fromItem = null;
        RelationshipItem toItem = null;

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

        daoRelationship.setFrom( fromItem );
        daoRelationship.setTo( toItem );

        return daoRelationship;
    }

    private boolean cacheExists()
    {
        return !relationshipTypeCache.isEmpty();
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
                .forEach( psi -> programStageInstanceCache.put( psi.getUid(), (ProgramStageInstance) psi ) );
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
