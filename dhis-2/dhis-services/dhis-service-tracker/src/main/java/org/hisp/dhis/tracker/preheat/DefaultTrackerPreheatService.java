package org.hisp.dhis.tracker.preheat;

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

import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.attribute.Attribute;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.commons.timer.SystemTimer;
import org.hisp.dhis.commons.timer.Timer;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.fieldfilter.Defaults;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.PeriodStore;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramInstanceStore;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.program.ProgramStageInstanceStore;
import org.hisp.dhis.program.ProgramType;
import org.hisp.dhis.query.Query;
import org.hisp.dhis.query.QueryService;
import org.hisp.dhis.query.Restriction;
import org.hisp.dhis.query.Restrictions;
import org.hisp.dhis.relationship.RelationshipStore;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceStore;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.tracker.TrackerIdScheme;
import org.hisp.dhis.tracker.TrackerIdentifier;
import org.hisp.dhis.tracker.TrackerIdentifierCollector;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.domain.Relationship;
import org.hisp.dhis.tracker.domain.TrackedEntity;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.api.client.util.Preconditions.checkNotNull;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Slf4j
@Service
public class DefaultTrackerPreheatService
    implements TrackerPreheatService
{

    public static final int SPLIT_LIST_PARTITION_SIZE = 20_000;

    private final SchemaService schemaService;

    private final QueryService queryService;

    private final IdentifiableObjectManager manager;

    private final CurrentUserService currentUserService;

    private final PeriodStore periodStore;

    private final TrackedEntityInstanceStore trackedEntityInstanceStore;

    private final ProgramInstanceStore programInstanceStore;

    private final ProgramStageInstanceStore programStageInstanceStore;

    private final RelationshipStore relationshipStore;

    private List<TrackerPreheatHook> preheatHooks = new ArrayList<>();

    @Autowired( required = false )
    public void setPreheatHooks( List<TrackerPreheatHook> preheatHooks )
    {
        this.preheatHooks = preheatHooks;
    }

    public DefaultTrackerPreheatService(
        SchemaService schemaService,
        QueryService queryService,
        IdentifiableObjectManager manager,
        CurrentUserService currentUserService,
        PeriodStore periodStore,
        TrackedEntityInstanceStore trackedEntityInstanceStore,
        ProgramInstanceStore programInstanceStore,
        ProgramStageInstanceStore programStageInstanceStore,
        RelationshipStore relationshipStore )
    {
        this.schemaService = schemaService;
        this.queryService = queryService;
        this.manager = manager;
        this.currentUserService = currentUserService;
        this.periodStore = periodStore;
        this.trackedEntityInstanceStore = trackedEntityInstanceStore;
        this.programInstanceStore = programInstanceStore;
        this.programStageInstanceStore = programStageInstanceStore;
        this.relationshipStore = relationshipStore;
    }

    @Override
    @Transactional
    public TrackerPreheat preheat( TrackerPreheatParams params )
    {
        Timer timer = new SystemTimer().start();

        TrackerPreheat preheat = new TrackerPreheat();
        preheat.setIdentifiers( params.getIdentifiers() );
        preheat.setUser( params.getUser() );
        preheat.setDefaults( manager.getDefaults() );
        User importingUser = getImportingUser( preheat.getUser() );
        preheat.setUser( importingUser );

        checkNotNull( preheat.getUser(), "Preheater is missing the user object." );

        Map<Class<?>, Set<String>> identifierMap = TrackerIdentifierCollector.collect( params );

        for ( Class<?> klass : identifierMap.keySet() )
        {
            Set<String> identifiers = identifierMap.get( klass );

            List<List<String>> splitList = Lists.partition( new ArrayList<>( identifiers ), SPLIT_LIST_PARTITION_SIZE );

            if ( klass.isAssignableFrom( TrackedEntity.class ) )
            {
                for ( List<String> ids : splitList )
                {
                    List<TrackedEntityInstance> trackedEntityInstances =
                        trackedEntityInstanceStore.getByUid( ids, preheat.getUser() );
                    preheat.putTrackedEntities( TrackerIdScheme.UID, trackedEntityInstances );
                }
            }
            else if ( klass.isAssignableFrom( Enrollment.class ) )
            {
                for ( List<String> ids : splitList )
                {
                    List<ProgramInstance> programInstances = programInstanceStore.getByUid( ids, preheat.getUser() );
                    preheat.putEnrollments( TrackerIdScheme.UID, programInstances );
                }
            }
            else if ( klass.isAssignableFrom( Event.class ) )
            {
                for ( List<String> ids : splitList )
                {
                    List<ProgramStageInstance> programStageInstances = programStageInstanceStore
                        .getByUid( ids, preheat.getUser() );
                    preheat.putEvents( TrackerIdScheme.UID, programStageInstances );
                }
            }
            else if ( klass.isAssignableFrom( OrganisationUnit.class ) )
            {
                TrackerIdentifier identifier = params.getIdentifiers().getOrgUnitIdScheme();
                Schema schema = schemaService.getDynamicSchema( OrganisationUnit.class );

                queryForIdentifiableObjects( preheat, schema, identifier, splitList );
            }
            else if ( klass.isAssignableFrom( Program.class ) )
            {
                Schema schema = schemaService.getDynamicSchema( Program.class );
                TrackerIdentifier identifier = params.getIdentifiers().getProgramIdScheme();

                queryForIdentifiableObjects( preheat, schema, identifier, splitList );
            }
            else if ( klass.isAssignableFrom( ProgramStage.class ) )
            {
                Schema schema = schemaService.getDynamicSchema( ProgramStage.class );
                TrackerIdentifier identifier = params.getIdentifiers().getProgramStageIdScheme();

                queryForIdentifiableObjects( preheat, schema, identifier, splitList );
            }
            else if ( klass.isAssignableFrom( DataElement.class ) )
            {
                Schema schema = schemaService.getDynamicSchema( DataElement.class );
                TrackerIdentifier identifier = params.getIdentifiers().getDataElementIdScheme();

                queryForIdentifiableObjects( preheat, schema, identifier, splitList );
            }
            else if ( klass.isAssignableFrom( CategoryOptionCombo.class ) )
            {
                Schema schema = schemaService.getDynamicSchema( CategoryOptionCombo.class );
                TrackerIdentifier identifier = params.getIdentifiers().getCategoryOptionComboIdScheme();

                queryForIdentifiableObjects( preheat, schema, identifier, splitList );
            }
            else if ( klass.isAssignableFrom( CategoryOption.class ) )
            {
                Schema schema = schemaService.getDynamicSchema( CategoryOption.class );
                TrackerIdentifier identifier = params.getIdentifiers().getCategoryOption();

                queryForIdentifiableObjects( preheat, schema, identifier, splitList );
            }
            else if ( klass.isAssignableFrom( Relationship.class ) )
            {
                for ( List<String> ids : splitList )
                {
                    List<org.hisp.dhis.relationship.Relationship> relationships = relationshipStore
                        .getByUid( ids, preheat.getUser() );
                    preheat.putRelationships( TrackerIdScheme.UID, relationships );
                }
            }
            else
            {
                Schema schema = schemaService.getDynamicSchema( klass );

                queryForIdentifiableObjects( preheat, schema, TrackerIdentifier.UID, splitList );
            }
        }

        // since TrackedEntityTypes are not really required by incoming payload, and they are small in size/count, we preload them all here
        preheat.put( TrackerIdentifier.UID, manager.getAll( TrackedEntityType.class ) );

        // since RelationshipTypes are not really required by incoming payload, and they are small in size/count, we preload them all here
        preheat.put( TrackerIdentifier.UID, manager.getAll( RelationshipType.class ) );

        periodStore.getAll().forEach( period -> preheat.getPeriodMap().put( period.getName(), period ) );
        periodStore.getAllPeriodTypes()
            .forEach( periodType -> preheat.getPeriodTypeMap().put( periodType.getName(), periodType ) );

        List<ProgramInstance> programInstances = programInstanceStore.getByType( ProgramType.WITHOUT_REGISTRATION );
        programInstances.forEach( pi -> preheat.putEnrollment( TrackerIdScheme.UID, pi.getProgram().getUid(), pi ) );

        preheatHooks.forEach( hook -> hook.preheat( params, preheat ) );

        log.info( "(" + preheat.getUsername() + ") Import:TrackerPreheat took " + timer.toString() );

        return preheat;
    }

    @Override
    public void validate( TrackerPreheatParams params )
    {
        //TODO: Implement validation
    }

    private Restriction generateRestrictionFromIdentifiers( TrackerIdScheme idScheme, List<String> ids )
    {
        if ( TrackerIdScheme.CODE.equals( idScheme ) )
        {
            return Restrictions.in( "code", ids );
        }
        else
        {
            return Restrictions.in( "id", ids );
        }
    }

    @SuppressWarnings( "unchecked" )
    private void queryForIdentifiableObjects( TrackerPreheat preheat, Schema schema, TrackerIdentifier identifier,
        List<List<String>> splitList )
    {
        TrackerIdScheme idScheme = identifier.getIdScheme();
        for ( List<String> ids : splitList )
        {
            List<? extends IdentifiableObject> objects;

            if ( TrackerIdScheme.ATTRIBUTE.equals( idScheme ) )
            {
                Attribute attribute = new Attribute();
                attribute.setUid( identifier.getValue() );
                objects = manager.getAllByAttributeAndValues(
                    (Class<? extends IdentifiableObject>) schema.getKlass(), attribute, ids );
            }
            else
            {
                Query query = Query.from( schema );
                query.setUser( preheat.getUser() );
                query.add( generateRestrictionFromIdentifiers( idScheme, ids ) );
                query.setDefaults( Defaults.INCLUDE );
                objects = queryService.query( query );
            }

            preheat.put( identifier, objects );
        }
    }

    private User getImportingUser( User user )
    {
        // ıf user already set, reload the user to make sure its loaded in the current tx
        if ( user != null )
        {
            return manager.get( User.class, user.getUid() );
        }

        return currentUserService.getCurrentUser();
    }
}
