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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.SessionFactory;
import org.hisp.dhis.cache.Cache;
import org.hisp.dhis.cache.SimpleCacheBuilder;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.AssignedUserSelectionMode;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.GridHeader;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.common.IdSchemes;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.IdentifiableProperty;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.common.Pager;
import org.hisp.dhis.common.QueryFilter;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.common.QueryOperator;
import org.hisp.dhis.commons.collection.CachingMap;
import org.hisp.dhis.commons.util.DebugUtils;
import org.hisp.dhis.commons.util.TextUtils;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.dbms.DbmsManager;
import org.hisp.dhis.dxf2.Constants;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.events.RelationshipParams;
import org.hisp.dhis.dxf2.events.enrollment.EnrollmentStatus;
import org.hisp.dhis.dxf2.events.eventdatavalue.EventDataValueService;
import org.hisp.dhis.dxf2.events.relationship.RelationshipService;
import org.hisp.dhis.dxf2.events.report.EventRow;
import org.hisp.dhis.dxf2.events.report.EventRows;
import org.hisp.dhis.dxf2.importsummary.ImportConflict;
import org.hisp.dhis.dxf2.importsummary.ImportStatus;
import org.hisp.dhis.dxf2.importsummary.ImportSummaries;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.hisp.dhis.dxf2.metadata.feedback.ImportReportMode;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.eventdatavalue.EventDataValue;
import org.hisp.dhis.fileresource.FileResourceService;
import org.hisp.dhis.organisationunit.FeatureType;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.program.EventSyncService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramInstanceService;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageDataElement;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.program.ProgramStageInstanceService;
import org.hisp.dhis.program.ProgramStageService;
import org.hisp.dhis.program.ProgramStatus;
import org.hisp.dhis.program.ProgramType;
import org.hisp.dhis.program.notification.event.ProgramStageCompletionNotificationEvent;
import org.hisp.dhis.programrule.ProgramRuleVariableService;
import org.hisp.dhis.programrule.engine.DataValueUpdatedEvent;
import org.hisp.dhis.programrule.engine.StageCompletionEvaluationEvent;
import org.hisp.dhis.programrule.engine.StageScheduledEvaluationEvent;
import org.hisp.dhis.query.Order;
import org.hisp.dhis.query.Query;
import org.hisp.dhis.query.QueryService;
import org.hisp.dhis.query.Restrictions;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.security.Authorities;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.system.grid.ListGrid;
import org.hisp.dhis.system.notification.NotificationLevel;
import org.hisp.dhis.system.notification.Notifier;
import org.hisp.dhis.system.util.Clock;
import org.hisp.dhis.system.util.GeoUtils;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceService;
import org.hisp.dhis.trackedentity.TrackerAccessManager;
import org.hisp.dhis.trackedentity.TrackerOwnershipManager;
import org.hisp.dhis.trackedentitycomment.TrackedEntityComment;
import org.hisp.dhis.trackedentitycomment.TrackedEntityCommentService;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserCredentials;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.util.DateUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.hisp.dhis.dxf2.events.event.EventSearchParams.*;
import static org.hisp.dhis.system.notification.NotificationLevel.ERROR;
import static org.hisp.dhis.util.DateUtils.getMediumDateString;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Slf4j
public abstract class AbstractEventService
    implements EventService
{
    public static final List<String> STATIC_EVENT_COLUMNS = Arrays.asList( EVENT_ID, EVENT_ENROLLMENT_ID, EVENT_CREATED_ID,
        EVENT_LAST_UPDATED_ID, EVENT_STORED_BY_ID, EVENT_COMPLETED_BY_ID, EVENT_COMPLETED_DATE_ID,
        EVENT_EXECUTION_DATE_ID, EVENT_DUE_DATE_ID, EVENT_ORG_UNIT_ID, EVENT_ORG_UNIT_NAME, EVENT_STATUS_ID,
        EVENT_PROGRAM_STAGE_ID, EVENT_PROGRAM_ID,
        EVENT_ATTRIBUTE_OPTION_COMBO_ID, EVENT_DELETED, EVENT_GEOMETRY );

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    protected ProgramService programService;

    protected ProgramStageService programStageService;

    protected ProgramInstanceService programInstanceService;

    protected ProgramStageInstanceService programStageInstanceService;

    protected OrganisationUnitService organisationUnitService;

    protected DataElementService dataElementService;

    protected CurrentUserService currentUserService;

    protected EventDataValueService eventDataValueService;

    protected TrackedEntityInstanceService entityInstanceService;

    protected TrackedEntityCommentService commentService;

    protected EventStore eventStore;

    protected Notifier notifier;

    protected SessionFactory sessionFactory;

    protected DbmsManager dbmsManager;

    protected IdentifiableObjectManager manager;

    protected CategoryService categoryService;

    protected FileResourceService fileResourceService;

    protected SchemaService schemaService;

    protected QueryService queryService;

    protected TrackerAccessManager trackerAccessManager;

    protected TrackerOwnershipManager trackerOwnershipAccessManager;

    protected AclService aclService;

    protected ApplicationEventPublisher eventPublisher;

    protected RelationshipService relationshipService;

    protected UserService userService;

    protected EventSyncService eventSyncService;

    protected ProgramRuleVariableService ruleVariableService;

    protected ObjectMapper jsonMapper;

    protected ObjectMapper xmlMapper;

    private static final int FLUSH_FREQUENCY = 100;

    // -------------------------------------------------------------------------
    // Caches
    // -------------------------------------------------------------------------

    private CachingMap<String, OrganisationUnit> organisationUnitCache = new CachingMap<>();

    private CachingMap<String, Program> programCache = new CachingMap<>();

    private CachingMap<String, ProgramStage> programStageCache = new CachingMap<>();

    private CachingMap<String, CategoryOption> categoryOptionCache = new CachingMap<>();

    private CachingMap<String, CategoryOptionCombo> categoryOptionComboCache = new CachingMap<>();

    private CachingMap<String, CategoryOptionCombo> attributeOptionComboCache = new CachingMap<>();

    private CachingMap<String, List<ProgramInstance>> activeProgramInstanceCache = new CachingMap<>();

    private CachingMap<String, ProgramInstance> programInstanceCache = new CachingMap<>();

    private CachingMap<String, ProgramStageInstance> programStageInstanceCache = new CachingMap<>();

    private CachingMap<String, TrackedEntityInstance> trackedEntityInstanceCache = new CachingMap<>();

    private CachingMap<Class<? extends IdentifiableObject>, IdentifiableObject> defaultObjectsCache = new CachingMap<>();

    private Set<TrackedEntityInstance> trackedEntityInstancesToUpdate = new HashSet<>();

    private CachingMap<String, User> userCache = new CachingMap<>();

    private static Cache<DataElement> DATA_ELEM_CACHE = new SimpleCacheBuilder<DataElement>()
        .forRegion( "dataElementCache" )
        .expireAfterAccess( 60, TimeUnit.MINUTES )
        .withInitialCapacity( 1000 )
        .withMaximumSize( 50000 )
        .build();

    private static Cache<Boolean> PROGRAM_HAS_ORG_UNIT_CACHE = new SimpleCacheBuilder<Boolean>()
        .forRegion( "programHasOrgUnitCache" )
        .expireAfterAccess( 60, TimeUnit.MINUTES )
        .withInitialCapacity( 1000 )
        .withMaximumSize( 50000 )
        .build();

    // -------------------------------------------------------------------------
    // CREATE
    // -------------------------------------------------------------------------

    @Transactional
    @Override
    public ImportSummaries processEventImport( List<Event> events, ImportOptions importOptions, JobConfiguration jobId )
    {
        ImportSummaries importSummaries = new ImportSummaries();
        importOptions = updateImportOptions( importOptions );

        notifier.clear( jobId ).notify( jobId, "Importing events" );
        Clock clock = new Clock( log ).startClock();

        List<List<Event>> partitions = Lists.partition( events, FLUSH_FREQUENCY );

        for ( List<Event> _events : partitions )
        {
            reloadUser( importOptions );
            prepareCaches( importOptions.getUser(), _events );

            List<Event> create = new ArrayList<>();
            List<Event> update = new ArrayList<>();
            List<String> delete = new ArrayList<>();

            if ( importOptions.getImportStrategy().isCreate() )
            {
                create.addAll( _events );
            }
            else if ( importOptions.getImportStrategy().isCreateAndUpdate() )
            {
                for ( Event event : _events )
                {
                    sortCreatesAndUpdates( event, create, update );
                }
            }
            else if ( importOptions.getImportStrategy().isUpdate() )
            {
                update.addAll( _events );
            }
            else if ( importOptions.getImportStrategy().isDelete() )
            {
                delete.addAll( _events.stream().map( Event::getEvent ).collect( Collectors.toList() ) );
            }
            else if ( importOptions.getImportStrategy().isSync() )
            {
                for ( Event event : _events )
                {
                    if ( event.isDeleted() )
                    {
                        delete.add( event.getEvent() );
                    }
                    else
                    {
                        sortCreatesAndUpdates( event, create, update );
                    }
                }
            }

            importSummaries.addImportSummaries( addEvents( create, importOptions, true ) );
            importSummaries.addImportSummaries( updateEvents( update, importOptions, false, true ) );
            importSummaries.addImportSummaries( deleteEvents( delete, true ) );

            if ( events.size() >= FLUSH_FREQUENCY )
            {
                clearSession( importOptions.getUser() );
            }
        }

        if ( jobId != null )
        {
            notifier.notify( jobId, NotificationLevel.INFO, "Import done. Completed in " + clock.time() + ".", true ).
                addJobSummary( jobId, importSummaries, ImportSummaries.class );
        }
        else
        {
            clock.logTime( "Import done" );
        }

        if ( ImportReportMode.ERRORS == importOptions.getReportMode() )
        {
            importSummaries.getImportSummaries().removeIf( is -> is.getConflicts().isEmpty() );
        }

        return importSummaries;
    }

    @Transactional
    @Override
    public ImportSummaries addEvents( List<Event> events, ImportOptions importOptions, boolean clearSession )
    {
        ImportSummaries importSummaries = new ImportSummaries();
        // TODO: luciano question -> can this be removed and replaced with assertions ?
        importOptions = updateImportOptions( importOptions );

        List<Event> validEvents = resolveImportableEvents( events, importSummaries );

        List<List<Event>> partitions = Lists.partition( validEvents, FLUSH_FREQUENCY );

        for ( List<Event> _events : partitions )
        {
            reloadUser( importOptions ); // TODO luciano why this in a loop?
            prepareCaches( importOptions.getUser(), _events );  // TODO luciano why this in a loop?

            for ( Event event : _events )
            {
                importSummaries.addImportSummary( addEvent( event, importOptions, true ) );
            }

            if ( clearSession && events.size() >= FLUSH_FREQUENCY )
            {
                clearSession( importOptions.getUser() );
            }
        }

        updateEntities( importOptions.getUser() );

        return importSummaries;
    }

    /**
     * Filters out Events which are already present in the database (regardless of the 'deleted' state)
     *
     * @param events          Events to import
     * @param importSummaries ImportSummaries used for import
     * @return Events that is possible to import (pass validation)
     */
    private List<Event> resolveImportableEvents( List<Event> events, ImportSummaries importSummaries )
    {

        List<String> conflictingEventUids = checkForExistingEventsIncludingDeleted( events, importSummaries );

        return events.stream()
            .filter( e -> !conflictingEventUids.contains( e.getEvent() ) )
            .collect( Collectors.toList() );
    }

    private List<String> checkForExistingEventsIncludingDeleted( List<Event> events, ImportSummaries importSummaries )
    {

        // TODO luciano replace with "select from programstageinstance where uid in (....)

        List<String> foundEvents = programStageInstanceService.getProgramStageInstanceUidsIncludingDeleted(
            events.stream()
                .map( Event::getEvent )
                .collect( Collectors.toList() )
        );

        for ( String foundEventUid : foundEvents )
        {
            ImportSummary is = new ImportSummary( ImportStatus.ERROR,
                "Event " + foundEventUid + " already exists or was deleted earlier" )
                .setReference( foundEventUid )
                .incrementIgnored();

            importSummaries.addImportSummary( is );
        }

        return foundEvents;
    }

    @Transactional
    @Override
    public ImportSummaries addEvents( List<Event> events, ImportOptions importOptions, JobConfiguration jobId )
    {
        notifier.clear( jobId ).notify( jobId, "Importing events" );
        importOptions = updateImportOptions( importOptions );

        try
        {
            ImportSummaries importSummaries = addEvents( events, importOptions, true );

            if ( jobId != null )
            {
                notifier.notify( jobId, NotificationLevel.INFO, "Import done", true ).addJobSummary( jobId, importSummaries, ImportSummaries.class );
            }

            return importSummaries;
        }
        catch ( RuntimeException ex )
        {
            log.error( DebugUtils.getStackTrace( ex ) );
            notifier.notify( jobId, ERROR, "Process failed: " + ex.getMessage(), true );
            return new ImportSummaries().addImportSummary( new ImportSummary( ImportStatus.ERROR, "The import process failed: " + ex.getMessage() ) );
        }
    }

    @Transactional
    @Override
    public ImportSummary addEvent( Event event, ImportOptions importOptions, boolean bulkImport )
    {
        // TODO: luciano question -> isn't this un-necessary, since we did it earlier?
        if ( !bulkImport && programStageInstanceService.programStageInstanceExistsIncludingDeleted( event.getEvent() ) )
        {
            return new ImportSummary( ImportStatus.ERROR,
                "Event " + event.getEvent() + " already exists or was deleted earlier" )
                .setReference( event.getEvent() )
                .incrementIgnored();
        }
        // TODO: luciano question ->  isn't this also un-necessary at this point?
        importOptions = updateImportOptions( importOptions );

        // TODO: luciano question -> since this is an 'add' operation, isn't this always going to be null?
        ProgramStageInstance programStageInstance = getProgramStageInstance( event.getEvent() );

        // FIXME: luciano to-rule -> EventDateCheck
//        if ( EventStatus.ACTIVE == event.getStatus() && event.getEventDate() == null )
//        {
//            return new ImportSummary( ImportStatus.ERROR, "Event date is required. " ).setReference( event.getEvent() ).incrementIgnored();
//        }

        // TODO: luciano question ->  why do we check this stuff here? This should only be checked in an update operation -> discussed with Morten, can be removed
//        if ( programStageInstance == null && !StringUtils.isEmpty( event.getEvent() ) && !CodeGenerator.isValidUid( event.getEvent() ) )
//        {
//            return new ImportSummary( ImportStatus.ERROR, "Event.event did not point to a valid event: " + event.getEvent() ).setReference( event.getEvent() ).incrementIgnored();
//        }
        // }


        Program program = getProgram( importOptions.getIdSchemes().getProgramIdScheme(), event.getProgram() );
        ProgramStage programStage = getProgramStage( importOptions.getIdSchemes().getProgramStageIdScheme(), event.getProgramStage() );
        OrganisationUnit organisationUnit = getOrganisationUnit( importOptions.getIdSchemes(), event.getOrgUnit() );
        TrackedEntityInstance entityInstance = getTrackedEntityInstance( event.getTrackedEntityInstance() );
        ProgramInstance programInstance = getProgramInstance( event.getEnrollment() );
        User assignedUser = getUser( event.getAssignedUser() );

//        FIXME: luciano to-rule ->  OrgUnitCheck
//        if ( organisationUnit == null )
//        {
//            return new ImportSummary( ImportStatus.ERROR, "Event.orgUnit does not point to a valid organisation unit: " + event.getOrgUnit() )
//                .setReference( event.getEvent() ).incrementIgnored();
//        }

//        FIXME: luciano to-rule ->  ProgramCheck
//        if ( program == null )
//        {
//            return new ImportSummary( ImportStatus.ERROR, "Event.program does not point to a valid program: " + event.getProgram() )
//                .setReference( event.getEvent() ).incrementIgnored();
//        }

//        FIXME: luciano to-rule ->  ProgramStageCheck
//        programStage = programStage == null && program.isWithoutRegistration() ? program.getProgramStageByStage( 1 ) : programStage;
//
//        if ( programStage == null )
//        {
//            return new ImportSummary( ImportStatus.ERROR, "Event.programStage does not point to a valid programStage: " + event.getProgramStage() );
//        }

        if ( program.isRegistration() )
        {

//        FIXME: luciano to-rule ->  TrackedEntityInstanceCheck

//            if ( entityInstance == null )
//            {
//                return new ImportSummary( ImportStatus.ERROR, "Event.trackedEntityInstance does not point to a valid tracked entity instance: "
//                    + event.getTrackedEntityInstance() ).setReference( event.getEvent() ).incrementIgnored();
//            }

//        FIXME: luciano to-rule ->  ProgramInstanceCheck

//            if ( programInstance == null )
//            {
//                List<ProgramInstance> programInstances = new ArrayList<>( programInstanceService.getProgramInstances( entityInstance, program, ProgramStatus.ACTIVE ) );
//
//                if ( programInstances.isEmpty() )
//                {
//                    return new ImportSummary( ImportStatus.ERROR, "Tracked entity instance: " + entityInstance.getUid() + " is not enrolled in program: " + program.getUid() ).setReference( event.getEvent() ).incrementIgnored();
//                }
//                else if ( programInstances.size() > 1 )
//                {
//                    return new ImportSummary( ImportStatus.ERROR, "Tracked entity instance: " + entityInstance.getUid() + " has multiple active enrollments in program: " + program.getUid() ).setReference( event.getEvent() ).incrementIgnored();
//                }
//
            // TODO: luciano ->  handle this situation where we store the PI to use
//                programInstance = programInstances.get( 0 );
//            }

            // TODO : luciano -> this validation can be done by moving it to the very end of the chain, when we are sure we got a valid programInstance
            if ( !programStage.getRepeatable() && programInstance.hasProgramStageInstance( programStage ) )
            {
                return new ImportSummary( ImportStatus.ERROR, "Program stage is not repeatable and an event already exists" )
                    .setReference( event.getEvent() ).incrementIgnored();
            }
        }
        else
        {
            // TODO: luciano -> this "side" effect should be handled elsewhere...
            String cacheKey = program.getUid() + "-" + ProgramStatus.ACTIVE;
            List<ProgramInstance> programInstances = getActiveProgramInstances( cacheKey, program );

            if ( programInstances.isEmpty() )
            {
                // Create PI if it doesn't exist (should only be one)

                String storedBy = getValidUsername( event.getStoredBy(), null,
                    User.username( importOptions.getUser(), Constants.UNKNOWN ) );

                ProgramInstance pi = new ProgramInstance();
                pi.setEnrollmentDate( new Date() );
                pi.setIncidentDate( new Date() );
                pi.setProgram( program );
                pi.setStatus( ProgramStatus.ACTIVE );
                pi.setStoredBy( storedBy );

                programInstanceService.addProgramInstance( pi, importOptions.getUser() );

                programInstances.add( pi );
            }
            else if ( programInstances.size() > 1 )
            {
                return new ImportSummary( ImportStatus.ERROR, "Multiple active program instances exists for program: " + program.getUid() )
                    .setReference( event.getEvent() ).incrementIgnored();
            }
            // TODO: luciano ->  handle this situation where we store the PI to use
            programInstance = programInstances.get( 0 );
        }

        program = programInstance.getProgram();

        if ( programStageInstance != null )
        {
            programStage = programStageInstance.getProgramStage();
        }

        final Program instanceProgram = programInstance.getProgram();
        final String cacheKey = instanceProgram.getUid() + organisationUnit.getUid();

//        FIXME: luciano to-rule ->  ProgramOrgUnitCheck
//        boolean programHasOrgUnit = PROGRAM_HAS_ORG_UNIT_CACHE.get( cacheKey,
//            key -> instanceProgram.hasOrganisationUnit( organisationUnit ) ).get();
//
//        if ( !programHasOrgUnit )
//        {
//            return new ImportSummary( ImportStatus.ERROR, "Program is not assigned to this organisation unit: " + event.getOrgUnit() )
//                .setReference( event.getEvent() ).incrementIgnored();
//        }

        // TODO: luciano question -> this is probably a mistake, because during add a  programStageInstance is always null?
        validateExpiryDays( importOptions, event, program, programStageInstance );


        if ( event.getGeometry() != null )
        {
//      FIXME: luciano to-rule ->  EventGeometryCheck
//            if ( programStage.getFeatureType().equals( FeatureType.NONE ) || !programStage.getFeatureType().value().equals( event.getGeometry().getGeometryType() ) )
//            {
//                return new ImportSummary( ImportStatus.ERROR,
//                    "Geometry (" + event.getGeometry().getGeometryType() + ") does not conform to the feature type (" + programStage.getFeatureType().value() + ") specified for the program stage: " + programStage.getUid() )
//                    .setReference( event.getEvent() ).incrementIgnored();
//            }

            // TODO: luciano -> this is a side effect and should take place after validation
            event.getGeometry().setSRID( GeoUtils.SRID );
        }

        else if ( event.getCoordinate() != null && event.getCoordinate().hasLatitudeLongitude() )
            {
//            Coordinate coordinate = event.getCoordinate();
//      FIXME: luciano to-rule ->  EventGeometryCheck
//            try
//            {
//                // TODO: luciano -> this is a side effect and should take place after validation
//                event.setGeometry( GeoUtils.getGeoJsonPoint( coordinate.getLongitude(), coordinate.getLatitude() ) );
//            }
//            catch ( IOException e )
//            {
//                return new ImportSummary( ImportStatus.ERROR, "Invalid longitude or latitude for property 'coordinates'." ).setReference( event.getEvent() ).incrementIgnored();
//            }
        }

        // FIXME: luciano to-rule -> EventAclCheck
//        List<String> errors = trackerAccessManager.canCreate( importOptions.getUser(),
//            new ProgramStageInstance( programInstance, programStage ).setOrganisationUnit( organisationUnit ).setStatus( event.getStatus() ), false );
//
//        if ( !errors.isEmpty() )
//        {
//            ImportSummary importSummary = new ImportSummary( ImportStatus.ERROR, errors.toString() );
//            importSummary.incrementIgnored();
//
//            return importSummary;
//        }

        return saveEvent( program, programInstance, programStage, programStageInstance, organisationUnit, event, assignedUser, importOptions, bulkImport );
    }

    // -------------------------------------------------------------------------
    // READ
    // -------------------------------------------------------------------------

    @Transactional( readOnly = true )
    @Override
    public Events getEvents( EventSearchParams params )
    {
        validate( params );

        List<OrganisationUnit> organisationUnits = getOrganisationUnits( params );

        User user = currentUserService.getCurrentUser();

        params.handleCurrentUserSelectionMode( user );

        if ( !params.isPaging() && !params.isSkipPaging() )
        {
            params.setDefaultPaging();
        }

        Events events = new Events();

        if ( params.isPaging() )
        {
            int count = 0;

            if ( params.isTotalPages() )
            {
                count = eventStore.getEventCount( params, organisationUnits );
            }

            Pager pager = new Pager( params.getPageWithDefault(), count, params.getPageSizeWithDefault() );
            events.setPager( pager );
        }

        List<Event> eventList = eventStore.getEvents( params, organisationUnits, Collections.emptyMap() );

        for ( Event event : eventList )
        {
            if ( trackerOwnershipAccessManager.hasAccess( user,
                entityInstanceService.getTrackedEntityInstance( event.getTrackedEntityInstance() ),
                programService.getProgram( event.getProgram() ) ) )
            {
                events.getEvents().add( event );
            }
        }

        return events;
    }

    @Transactional( readOnly = true )
    @Override
    public Grid getEventsGrid( EventSearchParams params )
    {
        User user = currentUserService.getCurrentUser();

        if ( params.getProgramStage() == null || params.getProgramStage().getProgram() == null )
        {
            throw new IllegalQueryException( "Program stage can not be null" );
        }

        if ( params.getProgramStage().getProgramStageDataElements() == null )
        {
            throw new IllegalQueryException( "Program stage should have at least one data element" );
        }

        List<OrganisationUnit> organisationUnits = getOrganisationUnits( params );

        params.handleCurrentUserSelectionMode( user );

        // ---------------------------------------------------------------------
        // If includeAllDataElements is set to true, return all data elements.
        // If no data element is specified, use those set as display in report.
        // ---------------------------------------------------------------------

        if ( params.isIncludeAllDataElements() )
        {
            for ( ProgramStageDataElement pde : params.getProgramStage().getProgramStageDataElements() )
            {
                QueryItem qi = new QueryItem( pde.getDataElement(), pde.getDataElement().getLegendSet(),
                    pde.getDataElement().getValueType(), pde.getDataElement().getAggregationType(),
                    pde.getDataElement().hasOptionSet() ? pde.getDataElement().getOptionSet() : null );
                params.getDataElements().add( qi );
            }
        }
        else
        {
            if ( params.getDataElements().isEmpty() )
            {
                for ( ProgramStageDataElement pde : params.getProgramStage().getProgramStageDataElements() )
                {
                    if ( pde.getDisplayInReports() )
                    {
                        QueryItem qi = new QueryItem( pde.getDataElement(), pde.getDataElement().getLegendSet(),
                            pde.getDataElement().getValueType(), pde.getDataElement().getAggregationType(),
                            pde.getDataElement().hasOptionSet() ? pde.getDataElement().getOptionSet() : null );
                        params.getDataElements().add( qi );
                    }
                }
            }
        }

        // ---------------------------------------------------------------------
        // Grid headers
        // ---------------------------------------------------------------------

        Grid grid = new ListGrid();

        for ( String col : STATIC_EVENT_COLUMNS )
        {
            grid.addHeader( new GridHeader( col, col ) );
        }

        for ( QueryItem item : params.getDataElements() )
        {
            grid.addHeader( new GridHeader( item.getItem().getUid(), item.getItem().getName() ) );
        }

        List<Map<String, String>> events = eventStore.getEventsGrid( params, organisationUnits );

        // ---------------------------------------------------------------------
        // Grid rows
        // ---------------------------------------------------------------------

        for ( Map<String, String> event : events )
        {
            grid.addRow();

            if ( params.getProgramStage().getProgram().isRegistration() && user != null || !user.isSuper() )
            {
                ProgramInstance enrollment = programInstanceService.getProgramInstance( event.get( EVENT_ENROLLMENT_ID ) );

                if ( enrollment != null && enrollment.getEntityInstance() != null )
                {
                    if ( !trackerOwnershipAccessManager.hasAccess( user, enrollment.getEntityInstance(), params.getProgramStage().getProgram() ) )
                    {
                        continue;
                    }
                }
            }

            for ( String col : STATIC_EVENT_COLUMNS )
            {
                grid.addValue( event.get( col ) );
            }

            for ( QueryItem item : params.getDataElements() )
            {
                grid.addValue( event.get( item.getItemId() ) );
            }
        }

        Map<String, Object> metaData = new HashMap<>();

        if ( params.isPaging() )
        {
            int count = 0;

            if ( params.isTotalPages() )
            {
                count = eventStore.getEventCount( params, organisationUnits );
            }

            Pager pager = new Pager( params.getPageWithDefault(), count, params.getPageSizeWithDefault() );
            metaData.put( PAGER_META_KEY, pager );
        }

        grid.setMetaData( metaData );

        return grid;
    }

    @Transactional( readOnly = true )
    @Override
    public int getAnonymousEventReadyForSynchronizationCount( Date skipChangedBefore )
    {
        EventSearchParams params = new EventSearchParams()
            .setProgramType( ProgramType.WITHOUT_REGISTRATION )
            .setIncludeDeleted( true )
            .setSynchronizationQuery( true )
            .setSkipChangedBefore( skipChangedBefore );

        return eventStore.getEventCount( params, null );
    }

    @Override
    public Events getAnonymousEventsForSync( int pageSize, Date skipChangedBefore, Map<String, Set<String>> psdesWithSkipSyncTrue )
    {
        // A page is not specified here as it would lead to SQLGrammarException after a successful sync of few pages
        // (total count will change and offset won't be valid)

        EventSearchParams params = new EventSearchParams()
            .setProgramType( ProgramType.WITHOUT_REGISTRATION )
            .setIncludeDeleted( true )
            .setSynchronizationQuery( true )
            .setPageSize( pageSize )
            .setSkipChangedBefore( skipChangedBefore );

        Events anonymousEvents = new Events();
        List<Event> events = eventStore.getEvents( params, null, psdesWithSkipSyncTrue );
        anonymousEvents.setEvents( events );
        return anonymousEvents;
    }

    @Transactional( readOnly = true )
    @Override
    public EventRows getEventRows( EventSearchParams params )
    {
        List<OrganisationUnit> organisationUnits = getOrganisationUnits( params );

        User user = currentUserService.getCurrentUser();

        EventRows eventRows = new EventRows();

        List<EventRow> eventRowList = eventStore.getEventRows( params, organisationUnits );

        for ( EventRow eventRow : eventRowList )
        {
            if ( trackerOwnershipAccessManager.hasAccess( user,
                entityInstanceService.getTrackedEntityInstance( eventRow.getTrackedEntityInstance() ),
                programService.getProgram( eventRow.getProgram() ) ) )
            {
                eventRows.getEventRows().add( eventRow );
            }
        }

        return eventRows;
    }

    @Transactional( readOnly = true )
    @Override
    public Event getEvent( ProgramStageInstance programStageInstance )
    {
        return getEvent( programStageInstance, false, false );
    }

    @Transactional( readOnly = true )
    @Override
    public Event getEvent( ProgramStageInstance programStageInstance, boolean isSynchronizationQuery, boolean skipOwnershipCheck )
    {
        if ( programStageInstance == null )
        {
            return null;
        }

        Event event = new Event();
        event.setEvent( programStageInstance.getUid() );

        if ( programStageInstance.getProgramInstance().getEntityInstance() != null )
        {
            event.setTrackedEntityInstance( programStageInstance.getProgramInstance().getEntityInstance().getUid() );
        }

        event.setFollowup( programStageInstance.getProgramInstance().getFollowup() );
        event.setEnrollmentStatus( EnrollmentStatus.fromProgramStatus( programStageInstance.getProgramInstance().getStatus() ) );
        event.setStatus( programStageInstance.getStatus() );
        event.setEventDate( DateUtils.getIso8601NoTz( programStageInstance.getExecutionDate() ) );
        event.setDueDate( DateUtils.getIso8601NoTz( programStageInstance.getDueDate() ) );
        event.setStoredBy( programStageInstance.getStoredBy() );
        event.setCompletedBy( programStageInstance.getCompletedBy() );
        event.setCompletedDate( DateUtils.getIso8601NoTz( programStageInstance.getCompletedDate() ) );
        event.setCreated( DateUtils.getIso8601NoTz( programStageInstance.getCreated() ) );
        event.setCreatedAtClient( DateUtils.getIso8601NoTz( programStageInstance.getCreatedAtClient() ) );
        event.setLastUpdated( DateUtils.getIso8601NoTz( programStageInstance.getLastUpdated() ) );
        event.setLastUpdatedAtClient( DateUtils.getIso8601NoTz( programStageInstance.getLastUpdatedAtClient() ) );
        event.setGeometry( programStageInstance.getGeometry() );
        event.setDeleted( programStageInstance.isDeleted() );

        // Lat and lnt deprecated in 2.30, remove by 2.33
        if ( event.getGeometry() != null && event.getGeometry().getGeometryType().equals( "Point" ) )
        {
            com.vividsolutions.jts.geom.Coordinate geometryCoordinate = event.getGeometry().getCoordinate();
            event.setCoordinate( new Coordinate( geometryCoordinate.x, geometryCoordinate.y ) );
        }

        if ( programStageInstance.getAssignedUser() != null )
        {
            event.setAssignedUser( programStageInstance.getAssignedUser().getUid() );
            event.setAssignedUserUsername( programStageInstance.getAssignedUser().getUsername() );
        }

        User user = currentUserService.getCurrentUser();
        OrganisationUnit ou = programStageInstance.getOrganisationUnit();

        List<String> errors = trackerAccessManager.canRead( user, programStageInstance, skipOwnershipCheck );

        if ( !errors.isEmpty() )
        {
            throw new IllegalQueryException( errors.toString() );
        }

        if ( ou != null )
        {
            event.setOrgUnit( ou.getUid() );
            event.setOrgUnitName( ou.getName() );
        }

        Program program = programStageInstance.getProgramInstance().getProgram();

        event.setProgram( program.getUid() );
        event.setEnrollment( programStageInstance.getProgramInstance().getUid() );
        event.setProgramStage( programStageInstance.getProgramStage().getUid() );
        event.setAttributeOptionCombo( programStageInstance.getAttributeOptionCombo().getUid() );
        event.setAttributeCategoryOptions( String.join( ";", programStageInstance.getAttributeOptionCombo()
            .getCategoryOptions().stream().map( CategoryOption::getUid ).collect( Collectors.toList() ) ) );

        if ( programStageInstance.getProgramInstance().getEntityInstance() != null )
        {
            event.setTrackedEntityInstance( programStageInstance.getProgramInstance().getEntityInstance().getUid() );
        }

        Collection<EventDataValue> dataValues;
        if ( !isSynchronizationQuery )
        {
            dataValues = programStageInstance.getEventDataValues();
        }
        else
        {
            Set<String> dataElementsToSync = programStageInstance.getProgramStage().getProgramStageDataElements().stream()
                .filter( psde -> !psde.getSkipSynchronization() )
                .map( psde -> psde.getDataElement().getUid() )
                .collect( Collectors.toSet() );

            dataValues = programStageInstance.getEventDataValues().stream()
                .filter( dv -> dataElementsToSync.contains( dv.getDataElement() ) )
                .collect( Collectors.toSet() );
        }

        for ( EventDataValue dataValue : dataValues )
        {

            DataElement dataElement = getDataElement( IdScheme.UID, dataValue.getDataElement() );

            if ( dataElement != null )
            {
                errors = trackerAccessManager.canRead( user, programStageInstance, dataElement, true );

                if ( !errors.isEmpty() )
                {
                    continue;
                }

                DataValue value = new DataValue();
                value.setCreated( DateUtils.getIso8601NoTz( dataValue.getCreated() ) );
                value.setLastUpdated( DateUtils.getIso8601NoTz( dataValue.getLastUpdated() ) );
                value.setDataElement( dataValue.getDataElement() );
                value.setValue( dataValue.getValue() );
                value.setProvidedElsewhere( dataValue.getProvidedElsewhere() );
                value.setStoredBy( dataValue.getStoredBy() );

                event.getDataValues().add( value );
            }
            else
            {
                log.info( "Can not find a Data Element having UID [" + dataValue.getDataElement() + "]" );
            }
        }

        List<TrackedEntityComment> comments = programStageInstance.getComments();

        for ( TrackedEntityComment comment : comments )
        {
            Note note = new Note();

            note.setNote( comment.getUid() );
            note.setValue( comment.getCommentText() );
            note.setStoredBy( comment.getCreator() );
            note.setStoredDate( DateUtils.getIso8601NoTz( comment.getCreated() ) );

            event.getNotes().add( note );
        }

        event.setRelationships( programStageInstance.getRelationshipItems().stream()
            .map( ( r ) -> relationshipService.getRelationship( r.getRelationship(), RelationshipParams.FALSE, user ) )
            .collect( Collectors.toSet() )
        );

        return event;
    }

    @Transactional( readOnly = true )
    @Override
    public EventSearchParams getFromUrl( String program, String programStage, ProgramStatus programStatus,
        Boolean followUp, String orgUnit, OrganisationUnitSelectionMode orgUnitSelectionMode,
        String trackedEntityInstance, Date startDate, Date endDate, Date dueDateStart, Date dueDateEnd,
        Date lastUpdatedStartDate, Date lastUpdatedEndDate, String lastUpdatedDuration, EventStatus status,
        CategoryOptionCombo attributeOptionCombo, IdSchemes idSchemes, Integer page, Integer pageSize,
        boolean totalPages, boolean skipPaging, List<Order> orders, List<String> gridOrders, boolean includeAttributes,
        Set<String> events, Boolean skipEventId, AssignedUserSelectionMode assignedUserSelectionMode, Set<String> assignedUsers,
        Set<String> filters, Set<String> dataElements, boolean includeAllDataElements, boolean includeDeleted )
    {
        User user = currentUserService.getCurrentUser();
        UserCredentials userCredentials = user.getUserCredentials();

        EventSearchParams params = new EventSearchParams();

        Program pr = programService.getProgram( program );

        if ( !StringUtils.isEmpty( program ) && pr == null )
        {
            throw new IllegalQueryException( "Program is specified but does not exist: " + program );
        }

        ProgramStage ps = programStageService.getProgramStage( programStage );

        if ( !StringUtils.isEmpty( programStage ) && ps == null )
        {
            throw new IllegalQueryException( "Program stage is specified but does not exist: " + programStage );
        }

        OrganisationUnit ou = organisationUnitService.getOrganisationUnit( orgUnit );

        if ( !StringUtils.isEmpty( orgUnit ) && ou == null )
        {
            throw new IllegalQueryException( "Org unit is specified but does not exist: " + orgUnit );
        }

        if ( ou != null && !organisationUnitService.isInUserHierarchy( ou ) )
        {
            if ( !userCredentials.isSuper()
                && !userCredentials.isAuthorized( "F_TRACKED_ENTITY_INSTANCE_SEARCH_IN_ALL_ORGUNITS" ) )
            {
                throw new IllegalQueryException( "User has no access to organisation unit: " + ou.getUid() );
            }
        }

        if ( pr != null && !userCredentials.isSuper() && !aclService.canDataRead( user, pr ) )
        {
            throw new IllegalQueryException( "User has no access to program: " + pr.getUid() );
        }

        if ( ps != null && !userCredentials.isSuper() && !aclService.canDataRead( user, ps ) )
        {
            throw new IllegalQueryException( "User has no access to program stage: " + ps.getUid() );
        }

        TrackedEntityInstance tei = entityInstanceService.getTrackedEntityInstance( trackedEntityInstance );

        if ( !StringUtils.isEmpty( trackedEntityInstance ) && tei == null )
        {
            throw new IllegalQueryException( "Tracked entity instance is specified but does not exist: " + trackedEntityInstance );
        }

        if ( attributeOptionCombo != null && !userCredentials.isSuper() && !aclService.canDataRead( user, attributeOptionCombo ) )
        {
            throw new IllegalQueryException( "User has no access to attribute category option combo: " + attributeOptionCombo.getUid() );
        }

        if ( events != null && filters != null )
        {
            throw new IllegalQueryException( "Event UIDs and filters can not be specified at the same time" );
        }

        if ( events == null )
        {
            events = new HashSet<>();
        }

        if ( filters != null )
        {
            if ( !StringUtils.isEmpty( programStage ) && ps == null )
            {
                throw new IllegalQueryException( "ProgramStage needs to be specified for event filtering to work" );
            }

            for ( String filter : filters )
            {
                QueryItem item = getQueryItem( filter );
                params.getFilters().add( item );
            }
        }

        if ( dataElements != null )
        {
            for ( String de : dataElements )
            {
                QueryItem dataElement = getQueryItem( de );

                params.getDataElements().add( dataElement );
            }
        }

        if ( assignedUserSelectionMode != null && assignedUsers != null && !assignedUsers.isEmpty()
            && !assignedUserSelectionMode.equals( AssignedUserSelectionMode.PROVIDED ) )
        {
            throw new IllegalQueryException( "Assigned User uid(s) cannot be specified if selectionMode is not PROVIDED" );
        }

        return params
            .setProgram( pr )
            .setProgramStage( ps )
            .setOrgUnit( ou )
            .setTrackedEntityInstance( tei )
            .setProgramStatus( programStatus )
            .setFollowUp( followUp )
            .setOrgUnitSelectionMode( orgUnitSelectionMode )
            .setAssignedUserSelectionMode( assignedUserSelectionMode )
            .setAssignedUsers( assignedUsers )
            .setStartDate( startDate )
            .setEndDate( endDate )
            .setDueDateStart( dueDateStart )
            .setDueDateEnd( dueDateEnd )
            .setLastUpdatedStartDate( lastUpdatedStartDate )
            .setLastUpdatedEndDate( lastUpdatedEndDate )
            .setLastUpdatedDuration( lastUpdatedDuration )
            .setEventStatus( status )
            .setCategoryOptionCombo( attributeOptionCombo )
            .setIdSchemes( idSchemes )
            .setPage( page )
            .setPageSize( pageSize )
            .setTotalPages( totalPages )
            .setSkipPaging( skipPaging )
            .setSkipEventId( skipEventId )
            .setIncludeAttributes( includeAttributes )
            .setIncludeAllDataElements( includeAllDataElements )
            .setOrders( orders )
            .setGridOrders( gridOrders )
            .setEvents( events )
            .setIncludeDeleted( includeDeleted );
    }

    // -------------------------------------------------------------------------
    // UPDATE
    // -------------------------------------------------------------------------

    @Transactional
    @Override
    public ImportSummaries updateEvents( List<Event> events, ImportOptions importOptions, boolean singleValue, boolean clearSession )
    {
        ImportSummaries importSummaries = new ImportSummaries();
        importOptions = updateImportOptions( importOptions );
        List<List<Event>> partitions = Lists.partition( events, FLUSH_FREQUENCY );

        for ( List<Event> _events : partitions )
        {
            reloadUser( importOptions );
            prepareCaches( importOptions.getUser(), _events );

            for ( Event event : _events )
            {
                importSummaries.addImportSummary( updateEvent( event, singleValue, importOptions, true ) );
            }

            if ( clearSession && events.size() >= FLUSH_FREQUENCY )
            {
                clearSession( importOptions.getUser() );
            }
        }

        updateEntities( importOptions.getUser() );

        return importSummaries;
    }

    @Transactional
    @Override
    public ImportSummary updateEvent( Event event, boolean singleValue, boolean bulkUpdate )
    {
        return updateEvent( event, singleValue, null, bulkUpdate );
    }

    @Transactional
    @Override
    public ImportSummary updateEvent( Event event, boolean singleValue, ImportOptions importOptions, boolean bulkUpdate )
    {
        importOptions = updateImportOptions( importOptions );

        if ( event == null || StringUtils.isEmpty( event.getEvent() ) )
        {
            return new ImportSummary( ImportStatus.ERROR, "No event or event ID was supplied" ).incrementIgnored();
        }

        ImportSummary importSummary = new ImportSummary( event.getEvent() );
        ProgramStageInstance programStageInstance = getProgramStageInstance( event.getEvent() );

        if ( programStageInstance == null )
        {
            importSummary.setStatus( ImportStatus.ERROR );
            importSummary.setDescription( "ID " + event.getEvent() + " doesn't point to valid event" );
            importSummary.getConflicts().add( new ImportConflict( "Invalid Event ID", event.getEvent() ) );

            return importSummary.incrementIgnored();
        }

        if ( programStageInstance != null && (programStageInstance.isDeleted() || importOptions.getImportStrategy().isCreate()) )
        {
            return new ImportSummary( ImportStatus.ERROR, "Event ID " + event.getEvent() + " was already used and/or deleted. This event can not be modified." )
                .setReference( event.getEvent() ).incrementIgnored();
        }

        List<String> errors = trackerAccessManager.canUpdate( importOptions.getUser(), programStageInstance, false );

        if ( !errors.isEmpty() )
        {
            return new ImportSummary( ImportStatus.ERROR, errors.toString() ).incrementIgnored();
        }

        OrganisationUnit organisationUnit = getOrganisationUnit( importOptions.getIdSchemes(), event.getOrgUnit() );

        if ( organisationUnit == null )
        {
            organisationUnit = programStageInstance.getOrganisationUnit();
        }

        Program program = getProgram( importOptions.getIdSchemes().getProgramIdScheme(), event.getProgram() );

        if ( program == null )
        {
            return new ImportSummary( ImportStatus.ERROR, "Program '" + event.getProgram() + "' for event '" + event.getEvent() + "' was not found." );
        }

        errors = validateEvent( event, programStageInstance.getProgramInstance(), importOptions );

        if ( !errors.isEmpty() )
        {
            importSummary.setStatus( ImportStatus.ERROR );
            importSummary.getConflicts().addAll( errors.stream().map( s -> new ImportConflict( "Event", s ) ).collect( Collectors.toList() ) );
            importSummary.incrementIgnored();

            return importSummary;
        }

        if ( event.getEventDate() != null )
        {
            Date executionDate = DateUtils.parseDate( event.getEventDate() );
            programStageInstance.setExecutionDate( executionDate );
        }

        Date dueDate = new Date();

        if ( event.getDueDate() != null )
        {
            dueDate = DateUtils.parseDate( event.getDueDate() );
        }

        String storedBy = getValidUsername( event.getStoredBy(), null, User.username( importOptions.getUser(), Constants.UNKNOWN ) );
        programStageInstance.setStoredBy( storedBy );

        String completedBy = getValidUsername( event.getCompletedBy(), null, User.username( importOptions.getUser(), Constants.UNKNOWN ) );

        if ( event.getStatus() != programStageInstance.getStatus()
            && programStageInstance.getStatus() == EventStatus.COMPLETED )
        {
            UserCredentials userCredentials = importOptions.getUser().getUserCredentials();

            if ( !userCredentials.isSuper() && !userCredentials.isAuthorized( "F_UNCOMPLETE_EVENT" ) )
            {
                importSummary.setStatus( ImportStatus.ERROR );
                importSummary.setDescription( "User is not authorized to uncomplete events" );

                return importSummary;
            }
        }

        if ( event.getStatus() == EventStatus.ACTIVE )
        {
            programStageInstance.setStatus( EventStatus.ACTIVE );
            programStageInstance.setCompletedBy( null );
            programStageInstance.setCompletedDate( null );
        }
        else if ( programStageInstance.getStatus() != event.getStatus() && event.getStatus() == EventStatus.COMPLETED )
        {
            programStageInstance.setCompletedBy( completedBy );

            Date completedDate = new Date();

            if ( event.getCompletedDate() != null )
            {
                completedDate = DateUtils.parseDate( event.getCompletedDate() );
            }
            programStageInstance.setCompletedDate( completedDate );
            programStageInstance.setStatus( EventStatus.COMPLETED );
        }
        else if ( event.getStatus() == EventStatus.SKIPPED )
        {
            programStageInstance.setStatus( EventStatus.SKIPPED );
        }

        else if ( event.getStatus() == EventStatus.SCHEDULE )
        {
            programStageInstance.setStatus( EventStatus.SCHEDULE );
        }

        programStageInstance.setDueDate( dueDate );
        programStageInstance.setOrganisationUnit( organisationUnit );

        validateExpiryDays( importOptions, event, program, programStageInstance );

        CategoryOptionCombo aoc = programStageInstance.getAttributeOptionCombo();

        if ( (event.getAttributeCategoryOptions() != null && program.getCategoryCombo() != null)
            || event.getAttributeOptionCombo() != null )
        {
            IdScheme idScheme = importOptions.getIdSchemes().getCategoryOptionIdScheme();

            try
            {
                aoc = getAttributeOptionCombo( program.getCategoryCombo(),
                    event.getAttributeCategoryOptions(), event.getAttributeOptionCombo(), idScheme );
            }
            catch ( IllegalQueryException ex )
            {
                importSummary.setStatus( ImportStatus.ERROR );
                importSummary.getConflicts().add( new ImportConflict( ex.getMessage(), event.getAttributeCategoryOptions() ) );
                return importSummary.incrementIgnored();
            }
        }

        if ( aoc != null && aoc.isDefault() && program.getCategoryCombo() != null && !program.getCategoryCombo().isDefault() )
        {
            importSummary.setStatus( ImportStatus.ERROR );
            importSummary.getConflicts().add( new ImportConflict( "attributeOptionCombo", "Default attribute option combo is not allowed since program has non-default category combo" ) );
            return importSummary.incrementIgnored();
        }

        if ( aoc != null )
        {
            programStageInstance.setAttributeOptionCombo( aoc );
        }

        Date eventDate = programStageInstance.getExecutionDate() != null ? programStageInstance.getExecutionDate() : programStageInstance.getDueDate();

        validateAttributeOptionComboDate( aoc, eventDate );

        if ( event.getGeometry() != null )
        {
            if ( programStageInstance.getProgramStage().getFeatureType().equals( FeatureType.NONE ) ||
                !programStageInstance.getProgramStage().getFeatureType().value().equals( event.getGeometry().getGeometryType() ) )
            {
                return new ImportSummary( ImportStatus.ERROR, String.format(
                    "Geometry '%s' does not conform to the feature type '%s' specified for the program stage: '%s'",
                    programStageInstance.getProgramStage().getUid(), event.getGeometry().getGeometryType(), programStageInstance.getProgramStage().getFeatureType().value() ) )
                    .setReference( event.getEvent() ).incrementIgnored();
            }

            event.getGeometry().setSRID( GeoUtils.SRID );
        }
        else if ( event.getCoordinate() != null && event.getCoordinate().hasLatitudeLongitude() )
        {
            Coordinate coordinate = event.getCoordinate();

            try
            {
                event.setGeometry( GeoUtils.getGeoJsonPoint( coordinate.getLongitude(), coordinate.getLatitude() ) );
            }
            catch ( IOException e )
            {
                return new ImportSummary( ImportStatus.ERROR,
                    "Invalid longitude or latitude for property 'coordinates'." );
            }
        }

        programStageInstance.setGeometry( event.getGeometry() );

        if ( programStageInstance.getProgramStage().isEnableUserAssignment() )
        {
            programStageInstance.setAssignedUser( getUser( event.getAssignedUser() ) );
        }

        saveTrackedEntityComment( programStageInstance, event, storedBy );
        preheatDataElementsCache( event, importOptions );

        eventDataValueService.processDataValues( programStageInstance, event, singleValue, importOptions, importSummary, DATA_ELEM_CACHE );

        programStageInstanceService.updateProgramStageInstance( programStageInstance );

        // Trigger rule engine:
        // 1. only once for whole event
        // 2. only if data value is associated with any ProgramRuleVariable

        boolean isLinkedWithRuleVariable = false;

        for ( DataValue dv : event.getDataValues() )
        {
            DataElement dataElement = DATA_ELEM_CACHE.get( dv.getDataElement() ).orElse( null );

            if ( dataElement != null )
            {
                isLinkedWithRuleVariable = ruleVariableService.isLinkedToProgramRuleVariable( program, dataElement );

                if ( isLinkedWithRuleVariable )
                {
                    break;
                }
            }
        }

        if ( !importOptions.isSkipNotifications() && isLinkedWithRuleVariable )
        {
            eventPublisher.publishEvent( new DataValueUpdatedEvent( this, programStageInstance.getId() ) );
        }

        sendProgramNotification( programStageInstance, importOptions );

        if ( !importOptions.isSkipLastUpdated() )
        {
            updateTrackedEntityInstance( programStageInstance, importOptions.getUser(), bulkUpdate );
        }

        if ( importSummary.getConflicts().isEmpty() )
        {
            importSummary.setStatus( ImportStatus.SUCCESS );
            importSummary.incrementUpdated();
        }
        else
        {
            importSummary.setStatus( ImportStatus.ERROR );
            importSummary.incrementIgnored();
        }

        return importSummary;
    }

    private void preheatDataElementsCache( Event event, ImportOptions importOptions )
    {
        IdScheme dataElementIdScheme = importOptions.getIdSchemes().getDataElementIdScheme();

        Set<String> dataElementIdentificators = event.getDataValues().stream()
            .map( DataValue::getDataElement )
            .collect( Collectors.toSet() );

        //Should happen in the most of the cases
        if ( dataElementIdScheme.isNull() || dataElementIdScheme.is( IdentifiableProperty.UID ) )
        {
            List<DataElement> dataElements = manager.getObjects( DataElement.class, IdentifiableProperty.UID,
                dataElementIdentificators );

            dataElements.forEach( de -> DATA_ELEM_CACHE.put( de.getUid(), de ) );
        }
        else
        {
            //Slower, but shouldn't happen so often
            dataElementIdentificators.forEach( deId -> getDataElement( dataElementIdScheme, deId ) );
        }
    }

    @Transactional
    @Override
    public void updateEventForNote( Event event )
    {
        ProgramStageInstance programStageInstance = programStageInstanceService
            .getProgramStageInstance( event.getEvent() );

        if ( programStageInstance == null )
        {
            return;
        }

        User currentUser = currentUserService.getCurrentUser();

        saveTrackedEntityComment( programStageInstance, event,
            getValidUsername( event.getStoredBy(), null, User.username( currentUser, Constants.UNKNOWN ) ) );

        updateTrackedEntityInstance( programStageInstance, currentUser, false );
    }

    @Transactional
    @Override
    public void updateEventForEventDate( Event event )
    {
        ProgramStageInstance programStageInstance = programStageInstanceService
            .getProgramStageInstance( event.getEvent() );

        if ( programStageInstance == null )
        {
            return;
        }

        List<String> errors = trackerAccessManager.canUpdate( currentUserService.getCurrentUser(), programStageInstance, false );

        if ( !errors.isEmpty() )
        {
            return;
        }

        Date executionDate = new Date();

        if ( event.getEventDate() != null )
        {
            executionDate = DateUtils.parseDate( event.getEventDate() );
        }

        Date eventDate = executionDate != null ? executionDate : programStageInstance.getDueDate();

        validateAttributeOptionComboDate( programStageInstance.getAttributeOptionCombo(), eventDate );

        if ( event.getStatus() == EventStatus.COMPLETED )
        {
            programStageInstance.setStatus( EventStatus.COMPLETED );
        }
        else
        {
            programStageInstance.setStatus( EventStatus.VISITED );
        }

        ImportOptions importOptions = new ImportOptions();

        OrganisationUnit organisationUnit = getOrganisationUnit( importOptions.getIdSchemes(), event.getOrgUnit() );

        if ( organisationUnit == null )
        {
            organisationUnit = programStageInstance.getOrganisationUnit();
        }

        programStageInstance.setOrganisationUnit( organisationUnit );
        programStageInstance.setExecutionDate( executionDate );
        programStageInstanceService.updateProgramStageInstance( programStageInstance );
    }

    @Transactional
    @Override
    public void updateEventsSyncTimestamp( List<String> eventsUIDs, Date lastSynchronized )
    {
        programStageInstanceService.updateProgramStageInstancesSyncTimestamp( eventsUIDs, lastSynchronized );
    }

    // -------------------------------------------------------------------------
    // DELETE
    // -------------------------------------------------------------------------

    @Transactional
    @Override
    public ImportSummary deleteEvent( String uid )
    {

        boolean existsEvent = programStageInstanceService.programStageInstanceExists( uid );

        if ( existsEvent )
        {
            ProgramStageInstance programStageInstance = programStageInstanceService.getProgramStageInstance( uid );

            List<String> errors = trackerAccessManager.canDelete( currentUserService.getCurrentUser(), programStageInstance, false );

            if ( !errors.isEmpty() )
            {
                return new ImportSummary( ImportStatus.ERROR, errors.toString() ).incrementIgnored();
            }

            programStageInstanceService.deleteProgramStageInstance( programStageInstance );

            if ( programStageInstance.getProgramStage().getProgram().isRegistration() )
            {
                entityInstanceService.updateTrackedEntityInstance( programStageInstance.getProgramInstance().getEntityInstance() );
            }

            ImportSummary importSummary = new ImportSummary( ImportStatus.SUCCESS, "Deletion of event " + uid + " was successful" ).incrementDeleted();
            importSummary.setReference( uid );
            return importSummary;
        }
        else
        {
            return new ImportSummary( ImportStatus.SUCCESS, "Event " + uid + " cannot be deleted as it is not present in the system" ).incrementIgnored();
        }
    }

    @Transactional
    @Override
    public ImportSummaries deleteEvents( List<String> uids, boolean clearSession )
    {
        User user = currentUserService.getCurrentUser();
        ImportSummaries importSummaries = new ImportSummaries();
        int counter = 0;

        for ( String uid : uids )
        {
            importSummaries.addImportSummary( deleteEvent( uid ) );

            if ( clearSession && counter % FLUSH_FREQUENCY == 0 )
            {
                clearSession( user );
            }

            counter++;
        }

        return importSummaries;
    }

    // -------------------------------------------------------------------------
    // HELPERS
    // -------------------------------------------------------------------------

    @SuppressWarnings( "unchecked" )
    private void prepareCaches( User user, List<Event> events )
    {
        // prepare caches
        Collection<String> orgUnits = events.stream().map( Event::getOrgUnit ).collect( Collectors.toSet() );
        Collection<String> programIds = events.stream().map( Event::getProgram ).collect( Collectors.toSet() );
        Collection<String> eventIds = events.stream().map( Event::getEvent ).collect( Collectors.toList() );
        Collection<String> userIds = events.stream().map( Event::getAssignedUser ).collect( Collectors.toSet() );

        if ( !orgUnits.isEmpty() )
        {
            Query query = Query.from( schemaService.getDynamicSchema( OrganisationUnit.class ) );
            query.setUser( user );
            query.add( Restrictions.in( "id", orgUnits ) );
            queryService.query( query ).forEach( ou -> organisationUnitCache.put( ou.getUid(), (OrganisationUnit) ou ) );
        }

        if ( !programIds.isEmpty() )
        {
            Query query = Query.from( schemaService.getDynamicSchema( Program.class ) );
            query.setUser( user );
            query.add( Restrictions.in( "id", programIds ) );

            List<Program> programs = (List<Program>) queryService.query( query );

            if ( !programs.isEmpty() )
            {
                for ( Program program : programs )
                {
                    programCache.put( program.getUid(), program );
                    programStageCache.putAll( program.getProgramStages().stream().collect( Collectors.toMap( ProgramStage::getUid, ps -> ps ) ) );

                    for ( ProgramStage programStage : program.getProgramStages() )
                    {
                        for ( DataElement dataElement : programStage.getDataElements() )
                        {
                            DATA_ELEM_CACHE.put( dataElement.getUid(), dataElement );
                        }
                    }
                }
            }
        }

        if ( !eventIds.isEmpty() )
        {
            eventSyncService.getEvents( (List<String>) eventIds ).forEach( psi -> programStageInstanceCache.put( psi.getUid(), psi ) );

            manager.getObjects( TrackedEntityInstance.class, IdentifiableProperty.UID,
                events.stream()
                    .filter( event -> event.getTrackedEntityInstance() != null )
                    .map( Event::getTrackedEntityInstance ).collect( Collectors.toSet() ) )
                .forEach( tei -> trackedEntityInstanceCache.put( tei.getUid(), tei ) );

            manager.getObjects( ProgramInstance.class, IdentifiableProperty.UID,
                events.stream()
                    .filter( event -> event.getEnrollment() != null )
                    .map( Event::getEnrollment ).collect( Collectors.toSet() ) )
                .forEach( tei -> programInstanceCache.put( tei.getUid(), tei ) );
        }

        if ( !userIds.isEmpty() )
        {
            Query query = Query.from( schemaService.getDynamicSchema( User.class ) );
            query.setUser( user );
            query.add( Restrictions.in( "id", userIds ) );
            queryService.query( query ).forEach( assignedUser -> userCache.put( assignedUser.getUid(), (User) assignedUser ) );
        }
    }

    private List<OrganisationUnit> getOrganisationUnits( EventSearchParams params )
    {
        List<OrganisationUnit> organisationUnits = new ArrayList<>();

        OrganisationUnit orgUnit = params.getOrgUnit();
        OrganisationUnitSelectionMode orgUnitSelectionMode = params.getOrgUnitSelectionMode();

        if ( params.getOrgUnit() != null )
        {
            if ( OrganisationUnitSelectionMode.DESCENDANTS.equals( orgUnitSelectionMode ) )
            {
                organisationUnits.addAll( organisationUnitService.getOrganisationUnitWithChildren( orgUnit.getUid() ) );
            }
            else if ( OrganisationUnitSelectionMode.CHILDREN.equals( orgUnitSelectionMode ) )
            {
                organisationUnits.add( orgUnit );
                organisationUnits.addAll( orgUnit.getChildren() );
            }
            else // SELECTED
            {
                organisationUnits.add( orgUnit );
            }
        }

        return organisationUnits;
    }

    private ImportSummary saveEvent( Program program, ProgramInstance programInstance, ProgramStage programStage,
        ProgramStageInstance programStageInstance, OrganisationUnit organisationUnit, Event event, User assignedUser,
        ImportOptions importOptions, boolean bulkSave )
    {
        Assert.notNull( program, "Program cannot be null" );
        Assert.notNull( programInstance, "Program instance cannot be null" );
        Assert.notNull( programStage, "Program stage cannot be null" );

        ImportSummary importSummary = new ImportSummary( event.getEvent() );
        importOptions = updateImportOptions( importOptions );

        boolean dryRun = importOptions.isDryRun();

//        FIXME: luciano to-rule -> EventBaseCheck
        List<String> errors = new ArrayList<>();
//        List<String> errors = validateEvent( event, programInstance, importOptions );

//        if ( !errors.isEmpty() )
//        {
//            importSummary.setStatus( ImportStatus.ERROR );
//            importSummary.getConflicts().addAll( errors.stream().map( s -> new ImportConflict( "Event", s ) ).collect( Collectors.toList() ) );
//            importSummary.incrementIgnored();
//
//            return importSummary;
//        }

        Date executionDate = null;

        if ( event.getEventDate() != null )
        {
            executionDate = DateUtils.parseDate( event.getEventDate() );
        }

        Date dueDate = new Date();

        if ( event.getDueDate() != null )
        {
            dueDate = DateUtils.parseDate( event.getDueDate() );
        }

        User user = importOptions.getUser();
        // TODO: luciano ->  move at the beginning of the process - no need to repeat it for each event

        String storedBy = getValidUsername( event.getStoredBy(), importSummary, User.username( user, Constants.UNKNOWN ) );
        String completedBy = getValidUsername( event.getCompletedBy(), importSummary, User.username( user, Constants.UNKNOWN ) );

        // FIXME: luciano -> this block has been moved to the context loading
//        CategoryOptionCombo aoc;
//
//
//        if ( (event.getAttributeCategoryOptions() != null && program.getCategoryCombo() != null)
//            || event.getAttributeOptionCombo() != null )
//        {
//            // TODO: luciano question -> do we still need to support this?
//            IdScheme idScheme = importOptions.getIdSchemes().getCategoryOptionIdScheme();
//
//            try
//            {
//                // TODO: luciano -> AttributeOptionComboCheck
//                aoc = getAttributeOptionCombo( program.getCategoryCombo(), event.getAttributeCategoryOptions(),
//                    event.getAttributeOptionCombo(), idScheme );
//            }
//            catch ( IllegalQueryException ex )
//            {
//                importSummary.getConflicts().add( new ImportConflict( ex.getMessage(), event.getAttributeCategoryOptions() ) );
//                importSummary.setStatus( ImportStatus.ERROR );
//                return importSummary.incrementIgnored();
//            }
//        }
//        else
//        {
//            aoc = (CategoryOptionCombo) getDefaultObject( CategoryOptionCombo.class );
//        }
//        FIXME: luciano to-rule -> AttributeOptionComboCheck
//        if ( aoc != null && aoc.isDefault() && program.getCategoryCombo() != null && !program.getCategoryCombo().isDefault() )
//        {
//            importSummary.getConflicts().add( new ImportConflict( "attributeOptionCombo", "Default attribute option combo is not allowed since program has non-default category combo" ) );
//            importSummary.setStatus( ImportStatus.ERROR );
//            return importSummary.incrementIgnored();
//        }

        CategoryOptionCombo aoc = null; // TODO just here for compilation...

        Date eventDate = executionDate != null ? executionDate : dueDate;

        // TODO: luciano question -> here we throw an exception, rather than using ImportSummary, why?
        validateAttributeOptionComboDate( aoc, eventDate );

        // FIXME: luciano to-rule -> AttributeOptionComboAclCheck
        errors = trackerAccessManager.canWrite( user, aoc );
        // TODO end access control block

//        if ( !errors.isEmpty() )
//        {
//            importSummary.setStatus( ImportStatus.ERROR );
//            importSummary.getConflicts().addAll( errors.stream().map( s -> new ImportConflict( "CategoryOptionCombo", s ) ).collect( Collectors.toList() ) );
//            importSummary.incrementIgnored();
//
//            return importSummary;
//        }

        if ( !dryRun )
        {
            if ( programStageInstance == null )
            {
                programStageInstance = createProgramStageInstance( event, programStage, programInstance,
                    organisationUnit, dueDate, executionDate, event.getStatus().getValue(),
                    completedBy, storedBy, event.getEvent(), aoc, assignedUser, importOptions, importSummary );

                if ( program.isRegistration() )
                {
                    programInstance.getProgramStageInstances().add( programStageInstance );
                }
            }
            else
            {
                updateProgramStageInstance( event, programStage, programInstance, organisationUnit, dueDate,
                    executionDate, event.getStatus().getValue(), completedBy, storedBy,
                    programStageInstance, aoc, assignedUser, importOptions, importSummary );
            }

            if ( !importOptions.isSkipLastUpdated() )
            {
                updateTrackedEntityInstance( programStageInstance, user, bulkSave );
            }

            importSummary.setReference( programStageInstance.getUid() );
        }

        if ( dryRun && programStageInstance == null )
        {

            log.error( "The request is a dry run and at the same time the programStageInstance is null. This will lead to NullPointerException. Stopping it now." );
            importSummary.setStatus( ImportStatus.ERROR );
            importSummary.setDescription( "The request is a dryRun. However, the provided event does not point to a valid event: " + event.getEvent() + ". Cannot continue." );

            return importSummary.setReference( event.getEvent() ).incrementIgnored();
        }

        programInstanceCache.put( programInstance.getUid(), programInstance );

        sendProgramNotification( programStageInstance, importOptions );

        if ( importSummary.getConflicts().size() > 0 )
        {
            importSummary.setStatus( ImportStatus.ERROR );
            importSummary.incrementIgnored();
        }
        else
        {
            importSummary.setStatus( ImportStatus.SUCCESS );
            importSummary.incrementImported();
        }

        return importSummary;
    }

    private void sendProgramNotification( ProgramStageInstance programStageInstance, ImportOptions importOptions )
    {
        if ( !importOptions.isSkipNotifications() )
        {
            if ( programStageInstance.isCompleted() )
            {
                eventPublisher.publishEvent( new ProgramStageCompletionNotificationEvent( this, programStageInstance.getId() ) );
                eventPublisher.publishEvent( new StageCompletionEvaluationEvent( this, programStageInstance.getId() ) );
            }

            if ( EventStatus.SCHEDULE.equals( programStageInstance.getStatus() ) )
            {
                eventPublisher.publishEvent( new StageScheduledEvaluationEvent( this, programStageInstance.getId() ) );
            }
        }
    }

    private ProgramStageInstance createProgramStageInstance( Event event, ProgramStage programStage,
        ProgramInstance programInstance, OrganisationUnit organisationUnit, Date dueDate, Date executionDate,
        int status, String completedBy, String storedBy, String programStageInstanceIdentifier,
        CategoryOptionCombo aoc, User assignedUser, ImportOptions importOptions, ImportSummary importSummary )
    {
        ProgramStageInstance programStageInstance = new ProgramStageInstance();

        if ( importOptions.getIdSchemes().getProgramStageInstanceIdScheme().equals( IdScheme.UID ) )
        {
            programStageInstance
                .setUid( CodeGenerator.isValidUid( programStageInstanceIdentifier ) ? programStageInstanceIdentifier
                    : CodeGenerator.generateUid() );
        }
        else if ( importOptions.getIdSchemes().getProgramStageInstanceIdScheme().equals( IdScheme.CODE ) )
        {
            programStageInstance.setUid( CodeGenerator.generateUid() );
            programStageInstance.setCode( programStageInstanceIdentifier );
        }

        programStageInstance.setStoredBy( storedBy );

        updateProgramStageInstance( event, programStage, programInstance, organisationUnit, dueDate, executionDate,
            status, completedBy, storedBy, programStageInstance, aoc, assignedUser, importOptions, importSummary );

        return programStageInstance;
    }

    private void updateProgramStageInstance( Event event, ProgramStage programStage, ProgramInstance programInstance,
        OrganisationUnit organisationUnit, Date dueDate, Date executionDate, int status,
        String completedBy, String storedBy, ProgramStageInstance programStageInstance, CategoryOptionCombo aoc, User assignedUser,
        ImportOptions importOptions, ImportSummary importSummary )
    {
        programStageInstance.setProgramInstance( programInstance );
        programStageInstance.setProgramStage( programStage );
        programStageInstance.setDueDate( dueDate );
        programStageInstance.setExecutionDate( executionDate );
        programStageInstance.setOrganisationUnit( organisationUnit );
        programStageInstance.setAttributeOptionCombo( aoc );
        programStageInstance.setGeometry( event.getGeometry() );

        if ( programStageInstance.getProgramStage().isEnableUserAssignment() )
        {
            programStageInstance.setAssignedUser( assignedUser );
        }

        updateDateFields( event, programStageInstance );

        programStageInstance.setStatus( EventStatus.fromInt( status ) );

        saveTrackedEntityComment( programStageInstance, event, storedBy );

        if ( programStageInstance.isCompleted() )
        {
            Date completedDate = new Date();
            if ( event.getCompletedDate() != null )
            {
                completedDate = DateUtils.parseDate( event.getCompletedDate() );
            }
            programStageInstance.setCompletedBy( completedBy );
            programStageInstance.setCompletedDate( completedDate );
        }

        preheatDataElementsCache( event, importOptions );

        if ( programStageInstance.getId() == 0 )
        {
            programStageInstance.setAutoFields();
            programStageInstanceService.addProgramStageInstance( programStageInstance, importOptions.getUser() );

            eventDataValueService.processDataValues( programStageInstance, event, false, importOptions, importSummary, DATA_ELEM_CACHE );
            programStageInstanceService.updateProgramStageInstance( programStageInstance, importOptions.getUser() );
        }
        else
        {
            eventDataValueService.processDataValues( programStageInstance, event, false, importOptions, importSummary, DATA_ELEM_CACHE );
            programStageInstanceService.updateProgramStageInstance( programStageInstance, importOptions.getUser() );
        }
        // TODO: luciano question -> what is this ???
        eventDataValueService.processDataValues( programStageInstance, event, false, importOptions, importSummary, DATA_ELEM_CACHE );
        // TODO: luciano question -> why are we calling update after an insert???
        programStageInstanceService.updateProgramStageInstance( programStageInstance );
    }

    private void saveTrackedEntityComment( ProgramStageInstance programStageInstance, Event event, String storedBy )
    {
        for ( Note note : event.getNotes() )
        {
            String noteUid = CodeGenerator.isValidUid( note.getNote() ) ? note.getNote() : CodeGenerator.generateUid();

            if ( !commentService.trackedEntityCommentExists( noteUid ) && !StringUtils.isEmpty( note.getValue() ) )
            {
                TrackedEntityComment comment = new TrackedEntityComment();
                comment.setUid( noteUid );
                comment.setCommentText( note.getValue() );
                comment.setCreator( getValidUsername( note.getStoredBy(), null, storedBy ) );

                Date created = DateUtils.parseDate( note.getStoredDate() );
                comment.setCreated( created );

                commentService.addTrackedEntityComment( comment );

                programStageInstance.getComments().add( comment );
            }
        }
    }

    public static String getValidUsername( String userName, ImportSummary importSummary, String fallbackUsername )
    {
        if ( StringUtils.isEmpty( userName ) )
        {
            return fallbackUsername;
        }
        else if ( userName.length() > UserCredentials.USERNAME_MAX_LENGTH )
        {
            if ( importSummary != null )
            {
                importSummary.getConflicts().add( new ImportConflict( "Username",
                    userName + " is more than " + UserCredentials.USERNAME_MAX_LENGTH + " characters, using current username instead" ) );
            }

            return fallbackUsername;
        }

        return userName;
    }

    private OrganisationUnit getOrganisationUnit( IdSchemes idSchemes, String id )
    {
        return organisationUnitCache.get( id,
            () -> manager.getObject( OrganisationUnit.class, idSchemes.getOrgUnitIdScheme(), id ) );
    }

    private ProgramStageInstance getProgramStageInstance( String uid )
    {
        if ( uid == null )
        {
            return null;
        }

        ProgramStageInstance programStageInstance = programStageInstanceCache.get( uid );

        if ( programStageInstance == null )
        {
            programStageInstance = eventSyncService.getEvent( uid );

            programStageInstanceCache.put( uid, programStageInstance );
        }

        return programStageInstance;
    }

    private ProgramInstance getProgramInstance( String uid )
    {
        if ( uid == null )
        {
            return null;
        }

        ProgramInstance programInstance = programInstanceCache.get( uid );

        if ( programInstance == null )
        {
            eventSyncService.getEnrollment( uid );
        }

        return programInstance;
    }

    private User getUser( String uid )
    {
        if ( uid == null )
        {
            return null;
        }

        User user = userCache.get( uid );

        if ( user == null )
        {
            user = userService.getUser( uid );
            userCache.put( uid, user );
        }

        return user;
    }

    private TrackedEntityInstance getTrackedEntityInstance( String uid )
    {
        if ( uid == null )
        {
            return null;
        }

        TrackedEntityInstance tei = trackedEntityInstanceCache.get( uid );

        if ( tei == null )
        {
            tei = entityInstanceService.getTrackedEntityInstance( uid );

            trackedEntityInstanceCache.put( uid, tei );
        }

        return tei;
    }

    private Program getProgram( IdScheme idScheme, String id )
    {
        if ( id == null )
        {
            return null;
        }

        Program program = programCache.get( id );

        if ( program == null )
        {
            program = manager.getObject( Program.class, idScheme, id );

            if ( program != null )
            {
                programCache.put( id, program );

                programStageCache.putAll( program.getProgramStages().stream().collect( Collectors.toMap( ProgramStage::getUid, ps -> ps ) ) );

                cacheDataElements( program.getProgramStages() );
            }
        }

        return program;
    }

    private void cacheDataElements( Set<ProgramStage> programStages )
    {
        for ( ProgramStage programStage : programStages )
        {
            for ( DataElement dataElement : programStage.getDataElements() )
            {
                DATA_ELEM_CACHE.put( dataElement.getUid(), dataElement );
            }
        }
    }

    private ProgramStage getProgramStage( IdScheme idScheme, String id )
    {
        if ( id == null )
        {
            return null;
        }

        ProgramStage programStage = programStageCache.get( id );

        if ( programStage == null )
        {
            programStage = manager.getObject( ProgramStage.class, idScheme, id );

            if ( programStage != null )
            {
                programStageCache.put( id, programStage );

                cacheDataElements( Sets.newHashSet( programStage ) );
            }
        }

        return programStage;
    }

    private DataElement getDataElement( IdScheme idScheme, String id )
    {
        return DATA_ELEM_CACHE.get( id, s -> manager.getObject( DataElement.class, idScheme, id ) ).orElse( null );
    }

    private CategoryOption getCategoryOption( IdScheme idScheme, String id )
    {
        return categoryOptionCache.get( id, () -> manager.getObject( CategoryOption.class, idScheme, id ) );
    }

    private CategoryOptionCombo getCategoryOptionCombo( IdScheme idScheme, String id )
    {
        return categoryOptionComboCache.get( id, () -> manager.getObject( CategoryOptionCombo.class, idScheme, id ) );
    }

    private CategoryOptionCombo getAttributeOptionCombo( String key, CategoryCombo categoryCombo,
        Set<CategoryOption> categoryOptions )
    {
        return attributeOptionComboCache.get( key,
            () -> categoryService.getCategoryOptionCombo( categoryCombo, categoryOptions ) );
    }

    private List<ProgramInstance> getActiveProgramInstances( String key, Program program )
    {
        return activeProgramInstanceCache.get( key,
            () -> programInstanceService.getProgramInstances( program, ProgramStatus.ACTIVE ) );
    }

    private IdentifiableObject getDefaultObject( Class<? extends IdentifiableObject> key )
    {
        return defaultObjectsCache.get( key, () -> manager.getByName( CategoryOptionCombo.class, "default" ) );
    }

    @Override
    public void validate( EventSearchParams params )
        throws IllegalQueryException
    {
        String violation = null;

        if ( params == null )
        {
            throw new IllegalQueryException( "Query parameters can not be empty" );
        }

        if ( params.getProgram() == null && params.getOrgUnit() == null && params.getTrackedEntityInstance() == null
            && params.getEvents().isEmpty() )
        {
            violation = "At least one of the following query parameters are required: orgUnit, program, trackedEntityInstance or event";
        }

        if ( params.hasLastUpdatedDuration() && (params.hasLastUpdatedStartDate() || params.hasLastUpdatedEndDate()) )
        {
            violation = "Last updated from and/or to and last updated duration cannot be specified simultaneously";
        }

        if ( params.hasLastUpdatedDuration() && DateUtils.getDuration( params.getLastUpdatedDuration() ) == null )
        {
            violation = "Duration is not valid: " + params.getLastUpdatedDuration();
        }

        if ( violation != null )
        {
            log.warn( "Validation failed: " + violation );

            throw new IllegalQueryException( violation );
        }
    }

    private void validateAttributeOptionComboDate( CategoryOptionCombo attributeOptionCombo, Date date )
    {
        if ( date == null )
        {
            throw new IllegalQueryException( "Event date can not be empty" );
        }

        for ( CategoryOption option : attributeOptionCombo.getCategoryOptions() )
        {
            if ( option.getStartDate() != null && date.compareTo( option.getStartDate() ) < 0 )
            {
                throw new IllegalQueryException( "Event date " + getMediumDateString( date )
                    + " is before start date " + getMediumDateString( option.getStartDate() )
                    + " for attributeOption '" + option.getName() + "'" );
            }

            if ( option.getEndDate() != null && date.compareTo( option.getEndDate() ) > 0 )
            {
                throw new IllegalQueryException( "Event date " + getMediumDateString( date )
                    + " is after end date " + getMediumDateString( option.getEndDate() )
                    + " for attributeOption '" + option.getName() + "'" );
            }
        }
    }

    private void validateExpiryDays( ImportOptions importOptions, Event event, Program program, ProgramStageInstance programStageInstance )
    {
        if ( importOptions == null || importOptions.getUser() == null || importOptions.getUser().isAuthorized( Authorities.F_EDIT_EXPIRED.getAuthority() ) )
        {
            return;
        }

        if ( program != null )
        {
            if ( program.getCompleteEventsExpiryDays() > 0 )
            {
                if ( event.getStatus() == EventStatus.COMPLETED
                    || programStageInstance != null && programStageInstance.getStatus() == EventStatus.COMPLETED )
                {
                    Date referenceDate = null;

                    if ( programStageInstance != null )
                    {
                        referenceDate = programStageInstance.getCompletedDate();
                    }

                    else
                    {
                        if ( event.getCompletedDate() != null )
                        {
                            referenceDate = DateUtils.parseDate( event.getCompletedDate() );
                        }
                    }

                    if ( referenceDate == null )
                    {
                        throw new IllegalQueryException( "Event needs to have completed date" );
                    }

                    if ( (new Date()).after(
                        DateUtils.getDateAfterAddition( referenceDate, program.getCompleteEventsExpiryDays() ) ) )
                    {
                        throw new IllegalQueryException(
                            "The event's completeness date has expired. Not possible to make changes to this event" );
                    }
                }
            }

            PeriodType periodType = program.getExpiryPeriodType();

            if ( periodType != null && program.getExpiryDays() > 0 )
            {
                if ( programStageInstance != null )
                {
                    Date today = new Date();

                    if ( programStageInstance.getExecutionDate() == null )
                    {
                        throw new IllegalQueryException( "Event needs to have event date" );
                    }

                    Period period = periodType.createPeriod( programStageInstance.getExecutionDate() );

                    if ( today.after( DateUtils.getDateAfterAddition( period.getEndDate(), program.getExpiryDays() ) ) )
                    {
                        throw new IllegalQueryException( "The program's expiry date has passed. It is not possible to make changes to this event" );
                    }
                }
                else
                {
                    String referenceDate = event.getEventDate() != null ? event.getEventDate()
                        : event.getDueDate() != null ? event.getDueDate() : null;

                    if ( referenceDate == null )
                    {
                        throw new IllegalQueryException( "Event needs to have at least one (event or schedule) date" );
                    }

                    Period period = periodType.createPeriod( new Date() );

                    if ( DateUtils.parseDate( referenceDate ).before( period.getStartDate() ) )
                    {
                        throw new IllegalQueryException( "The event's date belongs to an expired period. It is not possible to create such event" );
                    }
                }
            }
        }
    }

    private QueryItem getQueryItem( String item )
    {
        String[] split = item.split( DimensionalObject.DIMENSION_NAME_SEP );

        if ( split == null || split.length % 2 != 1 )
        {
            throw new IllegalQueryException( "Query item or filter is invalid: " + item );
        }

        QueryItem queryItem = getItem( split[0] );

        if ( split.length > 1 )
        {
            for ( int i = 1; i < split.length; i += 2 )
            {
                QueryOperator operator = QueryOperator.fromString( split[i] );
                queryItem.getFilters().add( new QueryFilter( operator, split[i + 1] ) );
            }
        }

        return queryItem;
    }

    private QueryItem getItem( String item )
    {
        DataElement de = dataElementService.getDataElement( item );

        if ( de == null )
        {
            throw new IllegalQueryException( "Dataelement does not exist: " + item );
        }

        return new QueryItem( de, null, de.getValueType(), de.getAggregationType(), de.getOptionSet() );
    }

    private void updateEntities( User user )
    {
        trackedEntityInstancesToUpdate.forEach( tei -> manager.update( tei, user ) );
        trackedEntityInstancesToUpdate.clear();
    }

    private void clearSession( User user )
    {
        organisationUnitCache.clear();
        programCache.clear();
        programStageCache.clear();
        programStageInstanceCache.clear();
        programInstanceCache.clear();
        activeProgramInstanceCache.clear();
        trackedEntityInstanceCache.clear();
        DATA_ELEM_CACHE.invalidateAll();
        categoryOptionCache.clear();
        categoryOptionComboCache.clear();
        attributeOptionComboCache.clear();
        defaultObjectsCache.clear();

        updateEntities( user );

        dbmsManager.flushSession();
    }

    private void updateDateFields( Event event, ProgramStageInstance programStageInstance )
    {
        programStageInstance.setAutoFields();

        Date createdAtClient = DateUtils.parseDate( event.getCreatedAtClient() );

        if ( createdAtClient != null )
        {
            programStageInstance.setCreatedAtClient( createdAtClient );
        }

        String lastUpdatedAtClient = event.getLastUpdatedAtClient();

        if ( lastUpdatedAtClient != null )
        {
            programStageInstance.setLastUpdatedAtClient( DateUtils.parseDate( lastUpdatedAtClient ) );
        }
    }

    private void updateTrackedEntityInstance( ProgramStageInstance programStageInstance, User user, boolean bulkUpdate )
    {
        updateTrackedEntityInstance( Lists.newArrayList( programStageInstance ), user, bulkUpdate );
    }

    private void updateTrackedEntityInstance( List<ProgramStageInstance> programStageInstances, User user, boolean bulkUpdate )
    {
        for ( ProgramStageInstance programStageInstance : programStageInstances )
        {
            if ( programStageInstance.getProgramInstance() != null )
            {
                if ( !bulkUpdate )
                {
                    if ( programStageInstance.getProgramInstance().getEntityInstance() != null )
                    {
                        manager.update( programStageInstance.getProgramInstance().getEntityInstance(), user );
                    }
                }
                else
                {
                    if ( programStageInstance.getProgramInstance().getEntityInstance() != null )
                    {
                        trackedEntityInstancesToUpdate.add( programStageInstance.getProgramInstance().getEntityInstance() );
                    }
                }
            }
        }
    }

    private CategoryOptionCombo getAttributeOptionCombo( CategoryCombo categoryCombo, String cp,
        String attributeOptionCombo, IdScheme idScheme )
    {
        Set<String> opts = TextUtils.splitToArray( cp, TextUtils.SEMICOLON );

        return getAttributeOptionCombo( categoryCombo, opts, attributeOptionCombo, idScheme );
    }

    private CategoryOptionCombo getAttributeOptionCombo( CategoryCombo categoryCombo, Set<String> opts,
        String attributeOptionCombo, IdScheme idScheme )
    {
        if ( categoryCombo == null )
        {
            throw new IllegalQueryException( "Illegal category combo" );
        }

        // ---------------------------------------------------------------------
        // Attribute category options validation
        // ---------------------------------------------------------------------

        CategoryOptionCombo attrOptCombo = null;

        if ( opts != null )
        {
            Set<CategoryOption> categoryOptions = new HashSet<>();

            for ( String uid : opts )
            {
                CategoryOption categoryOption = getCategoryOption( idScheme, uid );

                if ( categoryOption == null )
                {
                    throw new IllegalQueryException( "Illegal category option identifier: " + uid );
                }

                categoryOptions.add( categoryOption );
            }

            List<String> options = Lists.newArrayList( opts );
            Collections.sort( options );

            String cacheKey = categoryCombo.getUid() + "-" + Joiner.on( "-" ).join( options );
            attrOptCombo = getAttributeOptionCombo( cacheKey, categoryCombo, categoryOptions );

            if ( attrOptCombo == null )
            {
                throw new IllegalQueryException( "Attribute option combo does not exist for given category combo and category options" );
            }
        }
        else if ( attributeOptionCombo != null )
        {
            attrOptCombo = getCategoryOptionCombo( idScheme, attributeOptionCombo );
        }

        // ---------------------------------------------------------------------
        // Fall back to default category option combination
        // ---------------------------------------------------------------------

        if ( attrOptCombo == null )
        {
            attrOptCombo = (CategoryOptionCombo) getDefaultObject( CategoryOptionCombo.class );
        }

        if ( attrOptCombo == null )
        {
            throw new IllegalQueryException( "Default attribute option combo does not exist" );
        }

        return attrOptCombo;
    }

    private void sortCreatesAndUpdates( Event event, List<Event> create, List<Event> update )
    {
        if ( StringUtils.isEmpty( event.getEvent() ) )
        {
            create.add( event );
        }
        else
        {
            ProgramStageInstance programStageInstance = getProgramStageInstance( event.getEvent() );

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

    private void reloadUser( ImportOptions importOptions )
    {
        if ( importOptions == null || importOptions.getUser() == null )
        {
            return;
        }

        importOptions.setUser( userService.getUser( importOptions.getUser().getId() ) );
    }

    private List<String> validateEvent( Event event, ProgramInstance programInstance, ImportOptions importOptions )
    {
        List<String> errors = new ArrayList<>();

        if ( event.getDueDate() != null && !DateUtils.dateIsValid( event.getDueDate() ) )
        {
            errors.add( "Invalid event due date: " + event.getDueDate() );
        }

        if ( event.getEventDate() != null && !DateUtils.dateIsValid( event.getEventDate() ) )
        {
            errors.add( "Invalid event date: " + event.getEventDate() );
        }

        if ( event.getCreatedAtClient() != null && !DateUtils.dateIsValid( event.getCreatedAtClient() ) )
        {
            errors.add( "Invalid event created at client date: " + event.getCreatedAtClient() );
        }

        if ( event.getLastUpdatedAtClient() != null && !DateUtils.dateIsValid( event.getLastUpdatedAtClient() ) )
        {
            errors.add( "Invalid event last updated at client date: " + event.getLastUpdatedAtClient() );
        }

        if ( programInstance.getStatus().equals( ProgramStatus.COMPLETED ) )
        {
            if ( importOptions == null || importOptions.getUser() == null || importOptions.getUser().isAuthorized( Authorities.F_EDIT_EXPIRED.getAuthority() ) )
            {
                return errors;
            }

            Date referenceDate = DateUtils.parseDate( event.getCreated() );

            if ( referenceDate == null )
            {
                referenceDate = new Date();
            }

            referenceDate = DateUtils.removeTimeStamp( referenceDate );

            if ( referenceDate.after( DateUtils.removeTimeStamp( programInstance.getEndDate() ) ) )
            {
                errors.add( "Not possible to add event to a completed enrollment. Event created date ( " + referenceDate + " ) is after enrollment completed date ( " + DateUtils.removeTimeStamp( programInstance.getEndDate() ) + " )." );
            }
        }

        return errors;
    }
}
