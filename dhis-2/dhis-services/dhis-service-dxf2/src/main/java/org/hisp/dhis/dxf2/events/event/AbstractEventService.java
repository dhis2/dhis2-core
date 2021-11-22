package org.hisp.dhis.dxf2.events.event;

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

import static org.hisp.dhis.dxf2.events.event.EventSearchParams.EVENT_ATTRIBUTE_OPTION_COMBO_ID;
import static org.hisp.dhis.dxf2.events.event.EventSearchParams.EVENT_COMPLETED_BY_ID;
import static org.hisp.dhis.dxf2.events.event.EventSearchParams.EVENT_COMPLETED_DATE_ID;
import static org.hisp.dhis.dxf2.events.event.EventSearchParams.EVENT_CREATED_ID;
import static org.hisp.dhis.dxf2.events.event.EventSearchParams.EVENT_DELETED;
import static org.hisp.dhis.dxf2.events.event.EventSearchParams.EVENT_DUE_DATE_ID;
import static org.hisp.dhis.dxf2.events.event.EventSearchParams.EVENT_ENROLLMENT_ID;
import static org.hisp.dhis.dxf2.events.event.EventSearchParams.EVENT_EXECUTION_DATE_ID;
import static org.hisp.dhis.dxf2.events.event.EventSearchParams.EVENT_GEOMETRY;
import static org.hisp.dhis.dxf2.events.event.EventSearchParams.EVENT_ID;
import static org.hisp.dhis.dxf2.events.event.EventSearchParams.EVENT_LAST_UPDATED_ID;
import static org.hisp.dhis.dxf2.events.event.EventSearchParams.EVENT_ORG_UNIT_ID;
import static org.hisp.dhis.dxf2.events.event.EventSearchParams.EVENT_ORG_UNIT_NAME;
import static org.hisp.dhis.dxf2.events.event.EventSearchParams.EVENT_PROGRAM_ID;
import static org.hisp.dhis.dxf2.events.event.EventSearchParams.EVENT_PROGRAM_STAGE_ID;
import static org.hisp.dhis.dxf2.events.event.EventSearchParams.EVENT_STATUS_ID;
import static org.hisp.dhis.dxf2.events.event.EventSearchParams.EVENT_STORED_BY_ID;
import static org.hisp.dhis.dxf2.events.event.EventSearchParams.PAGER_META_KEY;
import static org.hisp.dhis.system.notification.NotificationLevel.ERROR;
import static org.hisp.dhis.system.util.DateUtils.getMediumDateString;

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
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.SessionFactory;
import org.hisp.dhis.cache.Cache;
import org.hisp.dhis.cache.SimpleCacheBuilder;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.GridHeader;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.common.IdSchemes;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.IdentifiableObjectUtils;
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
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.events.RelationshipParams;
import org.hisp.dhis.dxf2.events.TrackerAccessManager;
import org.hisp.dhis.dxf2.events.enrollment.EnrollmentStatus;
import org.hisp.dhis.dxf2.events.relationship.RelationshipService;
import org.hisp.dhis.dxf2.events.report.EventRow;
import org.hisp.dhis.dxf2.events.report.EventRows;
import org.hisp.dhis.dxf2.importsummary.ImportConflict;
import org.hisp.dhis.dxf2.importsummary.ImportStatus;
import org.hisp.dhis.dxf2.importsummary.ImportSummaries;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.hisp.dhis.dxf2.metadata.feedback.ImportReportMode;
import org.hisp.dhis.event.EventStatus;
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
import org.hisp.dhis.program.ValidationStrategy;
import org.hisp.dhis.program.notification.ProgramNotificationEventType;
import org.hisp.dhis.program.notification.ProgramNotificationPublisher;
import org.hisp.dhis.programrule.ProgramRuleVariableService;
import org.hisp.dhis.programrule.engine.DataValueUpdatedEvent;
import org.hisp.dhis.programrule.engine.ProgramStageInstanceScheduledEvent;
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
import org.hisp.dhis.system.util.DateUtils;
import org.hisp.dhis.system.util.GeoUtils;
import org.hisp.dhis.system.util.ValidationUtils;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceService;
import org.hisp.dhis.trackedentity.TrackerOwnershipManager;
import org.hisp.dhis.trackedentitycomment.TrackedEntityComment;
import org.hisp.dhis.trackedentitycomment.TrackedEntityCommentService;
import org.hisp.dhis.trackedentitydatavalue.TrackedEntityDataValue;
import org.hisp.dhis.trackedentitydatavalue.TrackedEntityDataValueService;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserCredentials;
import org.hisp.dhis.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Transactional
public abstract class AbstractEventService
    implements EventService
{
    private static final Log log = LogFactory.getLog( AbstractEventService.class );

    public static final List<String> STATIC_EVENT_COLUMNS = Arrays.asList( EVENT_ID, EVENT_ENROLLMENT_ID, EVENT_CREATED_ID,
        EVENT_LAST_UPDATED_ID, EVENT_STORED_BY_ID, EVENT_COMPLETED_BY_ID, EVENT_COMPLETED_DATE_ID,
        EVENT_EXECUTION_DATE_ID, EVENT_DUE_DATE_ID, EVENT_ORG_UNIT_ID, EVENT_ORG_UNIT_NAME, EVENT_STATUS_ID,
        EVENT_PROGRAM_STAGE_ID, EVENT_PROGRAM_ID,
        EVENT_ATTRIBUTE_OPTION_COMBO_ID, EVENT_DELETED, EVENT_GEOMETRY );

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    @Autowired
    protected ProgramService programService;

    @Autowired
    protected ProgramStageService programStageService;

    @Autowired
    protected ProgramInstanceService programInstanceService;

    @Autowired
    protected ProgramStageInstanceService programStageInstanceService;

    @Autowired
    protected OrganisationUnitService organisationUnitService;

    @Autowired
    protected DataElementService dataElementService;

    @Autowired
    protected CurrentUserService currentUserService;

    @Autowired
    protected TrackedEntityDataValueService dataValueService;

    @Autowired
    protected TrackedEntityInstanceService entityInstanceService;

    @Autowired
    protected TrackedEntityCommentService commentService;

    @Autowired
    protected EventStore eventStore;

    @Autowired
    protected Notifier notifier;

    @Autowired
    protected SessionFactory sessionFactory;

    @Autowired
    protected DbmsManager dbmsManager;

    @Autowired
    protected IdentifiableObjectManager manager;

    @Autowired
    protected CategoryService categoryService;

    @Autowired
    protected FileResourceService fileResourceService;

    @Autowired
    protected SchemaService schemaService;

    @Autowired
    protected QueryService queryService;

    @Autowired
    protected TrackerAccessManager trackerAccessManager;

    @Autowired
    protected TrackerOwnershipManager trackerOwnershipAccessManager;

    @Autowired
    protected AclService aclService;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    protected ProgramNotificationPublisher programNotificationPublisher;

    @Autowired
    protected RelationshipService relationshipService;

    @Autowired
    protected ProgramRuleVariableService ruleVariableService;

    @Autowired
    protected UserService userService;

    @Autowired
    protected EventSyncService eventSyncService;

    protected static final int FLUSH_FREQUENCY = 100;

    // -------------------------------------------------------------------------
    // Caches
    // -------------------------------------------------------------------------

    private CachingMap<String, OrganisationUnit> organisationUnitCache = new CachingMap<>();

    private CachingMap<String, Program> programCache = new CachingMap<>();

    private CachingMap<String, ProgramStage> programStageCache = new CachingMap<>();

    private CachingMap<String, DataElement> dataElementCache = new CachingMap<>();

    private CachingMap<String, CategoryOption> categoryOptionCache = new CachingMap<>();

    private CachingMap<String, CategoryOptionCombo> categoryOptionComboCache = new CachingMap<>();

    private CachingMap<String, CategoryOptionCombo> attributeOptionComboCache = new CachingMap<>();

    private CachingMap<String, List<ProgramInstance>> activeProgramInstanceCache = new CachingMap<>();

    private CachingMap<String, ProgramInstance> programInstanceCache = new CachingMap<>();

    private CachingMap<String, ProgramStageInstance> programStageInstanceCache = new CachingMap<>();

    private CachingMap<String, TrackedEntityInstance> trackedEntityInstanceCache = new CachingMap<>();

    private CachingMap<Class<? extends IdentifiableObject>, IdentifiableObject> defaultObjectsCache = new CachingMap<>();

    private static Cache<Boolean> PROGRAM_HAS_ORG_UNIT_CACHE = new SimpleCacheBuilder<Boolean>()
        .forRegion( "programHasOrgUnitCache" )
        .expireAfterAccess( 60, TimeUnit.MINUTES )
        .withInitialCapacity( 1000 )
        .withMaximumSize( 50000 )
        .build();

    private Set<TrackedEntityInstance> trackedEntityInstancesToUpdate = new HashSet<>();

    // -------------------------------------------------------------------------
    // CREATE
    // -------------------------------------------------------------------------

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

    @Override
    public ImportSummaries addEvents( List<Event> events, ImportOptions importOptions, boolean clearSession )
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

    @Override
    public ImportSummary addEvent( Event event, ImportOptions importOptions, boolean bulkImport )
    {
        importOptions = updateImportOptions( importOptions );

        ProgramStageInstance programStageInstance = getProgramStageInstance( event.getEvent() );

        if ( EventStatus.ACTIVE == event.getStatus() && event.getEventDate() == null )
        {
            return new ImportSummary( ImportStatus.ERROR, "Event date is required. " ).setReference( event.getEvent() ).incrementIgnored();
        }

        if (  programStageInstance != null && programStageInstance.isDeleted() )
        {
            return new ImportSummary( ImportStatus.ERROR, "Event ID " + event.getEvent() + " was already used and deleted. This event can not be modified." )
                .setReference( event.getEvent() ).incrementIgnored();
        }

        if ( programStageInstance == null && !StringUtils.isEmpty( event.getEvent() ) && !CodeGenerator.isValidUid( event.getEvent() ) )
        {
            return new ImportSummary( ImportStatus.ERROR, "Event.event did not point to a valid event: " + event.getEvent() ).setReference( event.getEvent() ).incrementIgnored();
        }

        Program program = getProgram( importOptions.getIdSchemes().getProgramIdScheme(), event.getProgram() );
        ProgramStage programStage = getProgramStage( importOptions.getIdSchemes().getProgramStageIdScheme(), event.getProgramStage() );
        OrganisationUnit organisationUnit = getOrganisationUnit( importOptions.getIdSchemes(), event.getOrgUnit() );
        TrackedEntityInstance entityInstance = getTrackedEntityInstance( event.getTrackedEntityInstance() );
        ProgramInstance programInstance = getProgramInstance( event.getEnrollment() );

        if ( organisationUnit == null )
        {
            return new ImportSummary( ImportStatus.ERROR, "Event.orgUnit does not point to a valid organisation unit: " + event.getOrgUnit() )
                .setReference( event.getEvent() ).incrementIgnored();
        }

        if ( program == null )
        {
            return new ImportSummary( ImportStatus.ERROR, "Event.program does not point to a valid program: " + event.getProgram() )
                .setReference( event.getEvent() ).incrementIgnored();
        }

        programStage = program.isWithoutRegistration() && programStage == null ? program.getProgramStageByStage( 1 ) : programStage;

        if ( programStage == null )
        {
            return new ImportSummary( ImportStatus.ERROR, "Event.programStage does not point to a valid programStage: " + event.getProgramStage() );
        }

        if ( program.isRegistration() )
        {
            if ( entityInstance == null )
            {
                return new ImportSummary( ImportStatus.ERROR, "Event.trackedEntityInstance does not point to a valid tracked entity instance: "
                    + event.getTrackedEntityInstance() ).setReference( event.getEvent() ).incrementIgnored();
            }

            if ( programInstance == null )
            {
                List<ProgramInstance> programInstances = new ArrayList<>( programInstanceService.getProgramInstances( entityInstance, program, ProgramStatus.ACTIVE ) );

                if ( programInstances.isEmpty() )
                {
                    return new ImportSummary( ImportStatus.ERROR, "Tracked entity instance: " + entityInstance.getUid() + " is not enrolled in program: " + program.getUid() ).setReference( event.getEvent() ).incrementIgnored();
                }
                else if ( programInstances.size() > 1 )
                {
                    return new ImportSummary( ImportStatus.ERROR, "Tracked entity instance: " + entityInstance.getUid() + " has multiple active enrollments in program: " + program.getUid() ).setReference( event.getEvent() ).incrementIgnored();
                }

                programInstance = programInstances.get( 0 );
            }

            if ( !programStage.getRepeatable() && programInstance.hasActiveProgramStageInstance( programStage ) )
            {
                return new ImportSummary( ImportStatus.ERROR, "Program stage is not repeatable and an event already exists" )
                    .setReference( event.getEvent() ).incrementIgnored();
            }
        }
        else
        {
            String cacheKey = program.getUid() + "-" + ProgramStatus.ACTIVE;
            List<ProgramInstance> programInstances = getActiveProgramInstances( cacheKey, program );

            if ( programInstances.isEmpty() )
            {
                // Create PI if it doesn't exist (should only be one)

                String storedBy = getValidUsername( event.getStoredBy(), null, importOptions.getUser() != null ? importOptions.getUser().getUsername() : "[Unknown]" );

                ProgramInstance pi = new ProgramInstance();
                pi.setEnrollmentDate( new Date() );
                pi.setIncidentDate( new Date() );
                pi.setProgram( program );
                pi.setStatus( ProgramStatus.ACTIVE );
                pi.setStoredBy( storedBy );

                programInstanceService.addProgramInstance( pi );

                programInstances.add( pi );
            }
            else if ( programInstances.size() > 1 )
            {
                return new ImportSummary( ImportStatus.ERROR, "Multiple active program instances exists for program: " + program.getUid() )
                    .setReference( event.getEvent() ).incrementIgnored();
            }

            programInstance = programInstances.get( 0 );
        }

        program = programInstance.getProgram();

        if ( programStageInstance != null )
        {
            programStage = programStageInstance.getProgramStage();
        }

        final Program instanceProgram = programInstance.getProgram();
        final String cacheKey = instanceProgram.getUid() + organisationUnit.getUid();
        boolean programHasOrgUnit = PROGRAM_HAS_ORG_UNIT_CACHE.get( cacheKey,
            key -> instanceProgram.hasOrganisationUnit( organisationUnit ) ).get();

        if ( !programHasOrgUnit )
        {
            return new ImportSummary( ImportStatus.ERROR, "Program is not assigned to this organisation unit: " + event.getOrgUnit() )
                .setReference( event.getEvent() ).incrementIgnored();
        }

        if ( importOptions.getUser() == null || !importOptions.getUser().isAuthorized( Authorities.F_EDIT_EXPIRED.getAuthority() ) )
        {
            validateExpiryDays( event, program, null );
        }

        if ( event.getGeometry() != null )
        {
            if ( programStage.getFeatureType().equals( FeatureType.NONE ) || !programStage.getFeatureType().value().equals( event.getGeometry().getGeometryType() ) )
            {
                return new ImportSummary( ImportStatus.ERROR, "Geometry (" + event.getGeometry().getGeometryType() + ") does not conform to the feature type (" + programStage.getFeatureType().value() + ") specified for the program stage: " + programStage.getUid() )
                    .setReference( event.getEvent() ).incrementIgnored();
            }
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
                return new ImportSummary( ImportStatus.ERROR, "Invalid longitude or latitude for property 'coordinates'." ).setReference( event.getEvent() ).incrementIgnored();
            }
        }

        List<String> errors = trackerAccessManager.canCreate( importOptions.getUser(),
            new ProgramStageInstance( programInstance, programStage ).setOrganisationUnit( organisationUnit ).setStatus( event.getStatus() ), false );

        if ( !errors.isEmpty() )
        {
            ImportSummary importSummary = new ImportSummary( ImportStatus.ERROR, errors.toString() );
            importSummary.incrementIgnored();

            return importSummary;
        }

        return saveEvent( program, programInstance, programStage, programStageInstance, organisationUnit, event, importOptions, bulkImport );
    }

    // -------------------------------------------------------------------------
    // READ
    // -------------------------------------------------------------------------

    @Override
    public Events getEvents( EventSearchParams params )
    {
        User user = currentUserService.getCurrentUser();

        validate( params, user );

        List<OrganisationUnit> organisationUnits = getOrganisationUnits( params, user );

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

        List<Event> eventList = eventStore.getEvents( params, organisationUnits );

        for ( Event event : eventList )
        {
            if ( trackerOwnershipAccessManager.hasAccess( user, event.getTrackedEntityInstance(), event.getProgram() ) )
            {
                events.getEvents().add( event );
            }
        }

        return events;
    }

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

        List<OrganisationUnit> organisationUnits = getOrganisationUnits( params, user );

        // ---------------------------------------------------------------------
        // If includeAllDataElements is set to true, return all data elements.
        // If no data element is specified, use those configured for display
        // in report
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

    @Override
    public int getAnonymousEventReadyForSynchronizationCount( Date skipChangedBefore )
    {
        EventSearchParams params = new EventSearchParams();
        params.setProgramType( ProgramType.WITHOUT_REGISTRATION );
        params.setIncludeDeleted( true );
        params.setSynchronizationQuery( true );
        params.setSkipChangedBefore( skipChangedBefore );

        return eventStore.getEventCount( params, null );
    }

    @Override
    public Events getAnonymousEventsForSync( int pageSize, Date skipChangedBefore )
    {
        // A page is not specified here. The reason is, that after a page is synchronized, the items that were in that page
        // get lastSynchronized column updated. Therefore, they are not present in the results in the next query anymore.
        // If I used paging, I would come to SQLGrammarException because I would try to fetch entries (with specific offset)
        // that don't exist anymore.

        EventSearchParams params = new EventSearchParams();
        params.setProgramType( ProgramType.WITHOUT_REGISTRATION );
        params.setIncludeDeleted( true );
        params.setSynchronizationQuery( true );
        params.setPageSize( pageSize );
        params.setSkipChangedBefore( skipChangedBefore );

        Events anonymousEvents = new Events();
        List<Event> events = eventStore.getEvents( params, null );
        anonymousEvents.setEvents( events );
        return anonymousEvents;
    }

    @Override
    public EventRows getEventRows( EventSearchParams params )
    {
        User user = currentUserService.getCurrentUser();

        List<OrganisationUnit> organisationUnits = getOrganisationUnits( params, user );

        EventRows eventRows = new EventRows();

        List<EventRow> eventRowList = eventStore.getEventRows( params, organisationUnits );

        for ( EventRow eventRow : eventRowList )
        {
            if ( trackerOwnershipAccessManager.hasAccess( user, eventRow.getTrackedEntityInstance(), eventRow.getProgram() ) )
            {
                eventRows.getEventRows().add( eventRow );
            }
        }

        return eventRows;
    }

    @Override
    public Event getEvent( ProgramStageInstance programStageInstance )
    {
        return getEvent( programStageInstance, false, false );
    }

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

        Collection<TrackedEntityDataValue> dataValues;
        if ( !isSynchronizationQuery )
        {
            dataValues = dataValueService.getTrackedEntityDataValues( programStageInstance );
        }
        else
        {
            dataValues = dataValueService.getTrackedEntityDataValuesForSynchronization( programStageInstance );
        }

        for ( TrackedEntityDataValue dataValue : dataValues )
        {
            errors = trackerAccessManager.canRead( user, dataValue, true );

            if ( !errors.isEmpty() )
            {
                continue;
            }

            DataValue value = new DataValue();
            value.setCreated( DateUtils.getIso8601NoTz( dataValue.getCreated() ) );
            value.setLastUpdated( DateUtils.getIso8601NoTz( dataValue.getLastUpdated() ) );
            value.setDataElement( dataValue.getDataElement().getUid() );
            value.setValue( dataValue.getValue() );
            value.setProvidedElsewhere( dataValue.getProvidedElsewhere() );
            value.setStoredBy( dataValue.getStoredBy() );

            event.getDataValues().add( value );
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

    @Override
    public EventSearchParams getFromUrl( String program, String programStage, ProgramStatus programStatus,
        Boolean followUp, String orgUnit, OrganisationUnitSelectionMode orgUnitSelectionMode,
        String trackedEntityInstance, Date startDate, Date endDate, Date dueDateStart, Date dueDateEnd,
        Date lastUpdatedStartDate, Date lastUpdatedEndDate, String lastUpdatedDuration, EventStatus status,
        CategoryOptionCombo attributeOptionCombo, IdSchemes idSchemes, Integer page, Integer pageSize,
        boolean totalPages, boolean skipPaging, List<Order> orders, List<String> gridOrders, boolean includeAttributes,
        Set<String> events, Boolean skipEventId, Set<String> filters, Set<String> dataElements, boolean includeAllDataElements,
        boolean includeDeleted )
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

        params.setProgram( pr );
        params.setProgramStage( ps );
        params.setOrgUnit( ou );
        params.setTrackedEntityInstance( tei );
        params.setProgramStatus( programStatus );
        params.setFollowUp( followUp );
        params.setOrgUnitSelectionMode( orgUnitSelectionMode );
        params.setStartDate( startDate );
        params.setEndDate( endDate );
        params.setDueDateStart( dueDateStart );
        params.setDueDateEnd( dueDateEnd );
        params.setLastUpdatedStartDate( lastUpdatedStartDate );
        params.setLastUpdatedEndDate( lastUpdatedEndDate );
        params.setLastUpdatedDuration( lastUpdatedDuration );
        params.setEventStatus( status );
        params.setCategoryOptionCombo( attributeOptionCombo );
        params.setIdSchemes( idSchemes );
        params.setPage( page );
        params.setPageSize( pageSize );
        params.setTotalPages( totalPages );
        params.setSkipPaging( skipPaging );
        params.setSkipEventId( skipEventId );
        params.setIncludeAttributes( includeAttributes );
        params.setIncludeAllDataElements( includeAllDataElements );
        params.setOrders( orders );
        params.setGridOrders( gridOrders );
        params.setEvents( events );
        params.setIncludeDeleted( includeDeleted );

        return params;
    }

    // -------------------------------------------------------------------------
    // UPDATE
    // -------------------------------------------------------------------------

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

    @Override
    public ImportSummary updateEvent( Event event, boolean singleValue, boolean bulkUpdate )
    {
        return updateEvent( event, singleValue, null, bulkUpdate );
    }

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

        Date executionDate;

        if ( event.getEventDate() != null )
        {
            executionDate = DateUtils.parseDate( event.getEventDate() );
            programStageInstance.setExecutionDate( executionDate );
        }

        Date dueDate = new Date();

        if ( event.getDueDate() != null )
        {
            dueDate = DateUtils.parseDate( event.getDueDate() );
        }

        String storedBy = getValidUsername( event.getStoredBy(), null, importOptions.getUser() != null ? importOptions.getUser().getUsername() : "[Unknown]" );
        programStageInstance.setStoredBy( storedBy );

        String completedBy = getValidUsername( event.getCompletedBy(), null, importOptions.getUser() != null ? importOptions.getUser().getUsername() : "[Unknown]" );

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

        if ( importOptions.getUser() == null || !importOptions.getUser().isAuthorized( Authorities.F_EDIT_EXPIRED.getAuthority() ) )
        {
            validateExpiryDays( event, program, programStageInstance );
        }

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
        }
        else if ( event.getCoordinate() != null && event.getCoordinate().hasLatitudeLongitude() )
        {
            Coordinate coordinate = event.getCoordinate();

            try
            {
                event
                    .setGeometry( GeoUtils.getGeoJsonPoint( coordinate.getLongitude(), coordinate.getLatitude() ) );
            }
            catch ( IOException e )
            {
                return new ImportSummary( ImportStatus.ERROR,
                    "Invalid longitude or latitude for property 'coordinates'." );
            }
        }
        programStageInstance.setGeometry( event.getGeometry() );

        saveTrackedEntityComment( programStageInstance, event, storedBy );
        programStageInstanceService.updateProgramStageInstance( programStageInstance );

        if ( !importOptions.isSkipLastUpdated() )
        {
            updateTrackedEntityInstance( programStageInstance, importOptions.getUser(), bulkUpdate );
        }

        Set<TrackedEntityDataValue> dataValues = new HashSet<>(
            dataValueService.getTrackedEntityDataValues( programStageInstance ) );
        Map<String, TrackedEntityDataValue> dataElementToValueMap = getDataElementDataValueMap( dataValues, importOptions );

        Map<String, DataElement> newDataElements = new HashMap<>();

        ImportSummary validationResult = validateDataValues( event, programStageInstance, dataElementToValueMap,
            dataValues, newDataElements, importSummary, importOptions, singleValue );

        if ( validationResult.getStatus() == ImportStatus.ERROR )
        {
            return validationResult;
        }

        for ( DataValue dataValue : event.getDataValues() )
        {
            DataElement dataElement;

            // The element was already saved so make an update
            if ( dataElementToValueMap.containsKey( dataValue.getDataElement() ) )
            {

                TrackedEntityDataValue teiDataValue = dataElementToValueMap.get( dataValue.getDataElement() );
                dataElement = teiDataValue.getDataElement();

                if ( StringUtils.isEmpty( dataValue.getValue() ) && dataElement.isFileType()
                    && !StringUtils.isEmpty( teiDataValue.getValue() ) )
                {
                    fileResourceService.deleteFileResource( teiDataValue.getValue() );
                }

                teiDataValue.setValue( dataValue.getValue() );
                teiDataValue.setProvidedElsewhere( dataValue.getProvidedElsewhere() );
                dataValueService.updateTrackedEntityDataValue( teiDataValue );

                // Marking that this data value was a part of an update so it should not be removed
                dataValues.remove( teiDataValue );
            }
            // Value is not present so consider it a new and save
            else
            {
                dataElement = newDataElements.get( dataValue.getDataElement() );

                saveDataValue( programStageInstance, event.getStoredBy(), dataElement, dataValue.getValue(),
                    dataValue.getProvidedElsewhere(), null, null );
            }

            if ( !importOptions.isSkipNotifications() && ruleVariableService.isLinkedToProgramRuleVariable( program, dataElement ) )
            {
                eventPublisher.publishEvent( new DataValueUpdatedEvent( this, programStageInstance ) );
            }
        }

        if ( !singleValue )
        {
            dataValues.forEach( dataValueService::deleteTrackedEntityDataValue );
        }

        importSummary.incrementUpdated();

        return importSummary;
    }

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
            getValidUsername( event.getStoredBy(), null, currentUser != null ? currentUser.getUsername() : "[Unknown]" ) );

        updateTrackedEntityInstance( programStageInstance, currentUser, false );
    }

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

    @Override
    public void updateEventsSyncTimestamp( List<String> eventsUIDs, Date lastSynchronized )
    {
        programStageInstanceService.updateProgramStageInstancesSyncTimestamp( eventsUIDs, lastSynchronized );
    }

    // -------------------------------------------------------------------------
    // DELETE
    // -------------------------------------------------------------------------

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
                        dataElementCache.putAll( programStage.getAllDataElements().stream().collect( Collectors.toMap( DataElement::getUid, de -> de ) ) );
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
    }

    private List<OrganisationUnit> getOrganisationUnits( EventSearchParams params, User user )
    {
        OrganisationUnitSelectionMode orgUnitSelectionMode = params.getOrgUnitSelectionMode();

        if ( orgUnitSelectionMode == null )
        {
            if ( params.getOrgUnit() != null )
            {
                return Arrays.asList( params.getOrgUnit() );
            }

            return getAccessibleOrgUnits( params, user );
        }

        List<OrganisationUnit> organisationUnits;

        switch ( orgUnitSelectionMode )
        {
        case ALL:
            organisationUnits = getAllOrgUnits( params, user );
            break;
        case CHILDREN:
            organisationUnits = getChildrenOrgUnits( params );
            break;
        case DESCENDANTS:
            organisationUnits = getDescendantOrgUnits( params );
            break;
        case CAPTURE:
            organisationUnits = getCaptureOrgUnits( params, user );
            break;
        case SELECTED:
            organisationUnits = getSelectedOrgUnits( params );
            break;
        default:
            organisationUnits = getAccessibleOrgUnits( params, user );
            break;
        }

        return organisationUnits;
    }

    private List<OrganisationUnit> getAllOrgUnits( EventSearchParams params, User user )
    {
        if ( params.getOrgUnit() != null )
        {
            return Arrays.asList( params.getOrgUnit() );
        }

        if ( !userCanSearchOuModeALL( user ) )
        {
            throw new IllegalQueryException( "User is not authorized to use ALL organisation units. " );
        }

        return organisationUnitService
            .getOrganisationUnitsWithChildren( organisationUnitService.getRootOrganisationUnits().stream()
                .map( BaseIdentifiableObject::getUid ).collect( Collectors.toList() ) );
    }

    private List<OrganisationUnit> getChildrenOrgUnits( EventSearchParams params )
    {
        if ( params.getOrgUnit() == null )
        {
            throw new IllegalQueryException( "Organisation unit is required to use CHILDREN scope." );
        }

        return organisationUnitService.getOrganisationUnitsWithChildren( params.getOrgUnit().getChildren().stream()
            .map( BaseIdentifiableObject::getUid ).collect( Collectors.toList() ) );
    }

    private List<OrganisationUnit> getSelectedOrgUnits( EventSearchParams params )
    {
        if ( params.getOrgUnit() == null )
        {
            throw new IllegalQueryException( "Organisation unit is required to use SELECTED scope. " );
        }

        return Collections.singletonList( params.getOrgUnit() );
    }

    private List<OrganisationUnit> getDescendantOrgUnits( EventSearchParams params )
    {
        if ( params.getOrgUnit() == null )
        {
            throw new IllegalQueryException( "Organisation unit is required to use DESCENDANTS scope. " );
        }

        return organisationUnitService.getOrganisationUnitWithChildren( params.getOrgUnit().getUid() );
    }

    private List<OrganisationUnit> getCaptureOrgUnits( EventSearchParams params, User user )
    {
        if ( params.getOrgUnit() != null )
        {
            return Arrays.asList( params.getOrgUnit() );
        }

        if ( user == null )
        {
            throw new IllegalQueryException( "User is required to use CAPTURE scope." );
        }

        return organisationUnitService.getOrganisationUnitsWithChildren( user.getOrganisationUnits().stream()
            .map( BaseIdentifiableObject::getUid ).collect( Collectors.toList() ) );
    }

    private List<OrganisationUnit> getAccessibleOrgUnits( EventSearchParams params, User user )
    {
        if ( params.getOrgUnit() != null )
        {
            return Arrays.asList( params.getOrgUnit() );
        }

        if ( user == null )
        {
            throw new IllegalQueryException( "User is required to use ACCESSIBLE scope." );
        }

        Set<OrganisationUnit> orgUnits = user.getOrganisationUnits();

        if ( params.getProgram() != null )
        {
            orgUnits = user.getTeiSearchOrganisationUnitsWithFallback();

            if ( params.getProgram().isClosed() )
            {
                orgUnits = user.getOrganisationUnits();
            }
        }

        return organisationUnitService.getOrganisationUnitsWithChildren( orgUnits.stream()
            .map( BaseIdentifiableObject::getUid ).collect( Collectors.toList() ) );
    }

    private boolean doValidationOfMandatoryAttributes( User user )
    {
        return user == null || !user.isAuthorized( Authorities.F_IGNORE_TRACKER_REQUIRED_VALUE_VALIDATION.getAuthority() );
    }

    private Map<String, String> validatePresenceOfMandatoryDataElements( Event event, ProgramStageInstance programStageInstance,
        ImportOptions importOptions, ImportSummary importSummary, boolean isSingleValueUpdate ) {

        ValidationStrategy validationStrategy = programStageInstance.getProgramStage().getValidationStrategy();
        IdScheme dataElementIdScheme = importOptions.getIdSchemes().getDataElementIdScheme();

        if ( validationStrategy == ValidationStrategy.ON_UPDATE_AND_INSERT ||
            (validationStrategy == ValidationStrategy.ON_COMPLETE && event.getStatus() == EventStatus.COMPLETED) )
        {
            if ( dataElementIdScheme.isAttribute() )
            {
                return validateMandatoryDataElementsForAttributeScheme( event, programStageInstance,
                    dataElementIdScheme, importSummary, isSingleValueUpdate );
            }
            else
            {
                return validateMandatoryDataElementsForOtherSchemes( event, programStageInstance,
                    dataElementIdScheme, importSummary, isSingleValueUpdate );
            }
        }

        return Collections.emptyMap();
    }

    private Map<String, String> validateMandatoryDataElementsForOtherSchemes( Event event, ProgramStageInstance programStageInstance,
        IdScheme dataElementIdScheme, ImportSummary importSummary, boolean isSingleValueUpdate )
    {
        //The map contains <DataElement UID, DataElement identificator in given dataElementIdScheme> entries
        Map<String, String> mandatoryDataElements = programStageInstance.getProgramStage().getProgramStageDataElements().stream()
            .filter( ProgramStageDataElement::isCompulsory )
            .map( ProgramStageDataElement::getDataElement )
            .filter( de -> IdentifiableObjectUtils.getIdentifierBasedOnIdScheme( de, dataElementIdScheme ) != null )
            .collect( Collectors.toMap(
                DataElement::getUid,
                de -> IdentifiableObjectUtils.getIdentifierBasedOnIdScheme( de, dataElementIdScheme )
            ) );

        Set<String> presentDataElements = event.getDataValues().stream()
            .map( DataValue::getDataElement )
            .collect( Collectors.toSet() );

        // When the request is update, then only changed data values can be in the payload and so I should take into
        // account also already stored data values in order to make correct decision. Basically, this situation happens when
        // only 1 dataValue is updated and /events/{uid}/{dataElementUid} endpoint is leveraged.
        if ( isSingleValueUpdate )
        {
            presentDataElements.addAll( programStageInstance.getDataValues().stream()
                .filter( dv -> !StringUtils.isEmpty( dv.getValue() ) )
                .map( dv -> IdentifiableObjectUtils
                    .getIdentifierBasedOnIdScheme( dv.getDataElement(), dataElementIdScheme ) )
                .filter( Objects::nonNull )
                .collect( Collectors.toSet() ) );
        }

        Set<String> notPresentMandatoryDataElements = Sets
            .difference( new HashSet<>( mandatoryDataElements.values() ), presentDataElements );

        if ( !notPresentMandatoryDataElements.isEmpty() )
        {
            notPresentMandatoryDataElements.forEach( deIdentificator -> importSummary.getConflicts()
                .add( new ImportConflict( deIdentificator, "value_required_but_not_provided" ) ) );
            importSummary.incrementIgnored();
            importSummary.setStatus( ImportStatus.ERROR );
        }

        return mandatoryDataElements;
    }

    private Map<String, String> validateMandatoryDataElementsForAttributeScheme( Event event, ProgramStageInstance programStageInstance,
        IdScheme dataElementIdScheme, ImportSummary importSummary, boolean isSingleValueUpdate )
    {
        Map<String, String> mandatoryDataElements = programStageInstance.getProgramStage().getProgramStageDataElements().stream()
            .filter( ProgramStageDataElement::isCompulsory )
            .collect( Collectors.toMap(
                psde -> psde.getDataElement().getUid(),
                psde -> psde.getDataElement().getName() ) );

        //Using Map<UID, Attribute:UID> to be able to make a Sets.difference below
        Map<String, String> presentDataElements = new HashMap<>();
        event.getDataValues().stream()
            .map( DataValue::getDataElement )
            .forEach( de ->
            {
                DataElement dataElement = getDataElement( dataElementIdScheme, de );

                if ( dataElement != null )
                {
                    presentDataElements.put( dataElement.getUid(), de );

                    //If I have a correct Attribute:UID, then replace the Name byt this Attribute:UID. It will come handy later
                    mandatoryDataElements.put( dataElement.getUid(), de );
                }
            } );

        // When the request is update, then only changed data values can be in the payload and so I should take into
        // account also already stored data values in order to make correct decision. Basically, this situation happens when
        // only 1 dataValue is updated and /events/{uid}/{dataElementUid} endpoint is leveraged.
        if ( isSingleValueUpdate )
        {
            //Using Map<UID, Attribute:UID> to be able to make a Sets.difference below. Arbitrary Attribute:UID is used here as it is not significant
            presentDataElements.putAll( programStageInstance.getDataValues().stream()
                .filter( dv -> !StringUtils.isEmpty( dv.getValue() ) )
                .map( dv -> dv.getDataElement().getUid() )
                .collect( Collectors.toMap( Function.identity(), v -> "" ) ) );
        }

        Set<String> notPresentMandatoryDataElements = Sets.difference( mandatoryDataElements.keySet(), presentDataElements.keySet() );

        if ( !notPresentMandatoryDataElements.isEmpty() )
        {
            //Using DataElement name as I am not able to obtain the Attribute:UID and name is more informative than DE UID
            notPresentMandatoryDataElements.forEach( deUid -> importSummary.getConflicts()
                .add( new ImportConflict( mandatoryDataElements.get( deUid ), "value_required_but_not_provided" ) ) );
            importSummary.incrementIgnored();
            importSummary.setStatus( ImportStatus.ERROR );
        }

        return mandatoryDataElements;
    }

    private ImportSummary validateDataValues( Event event, ProgramStageInstance programStageInstance,
        Map<String, TrackedEntityDataValue> dataElementToValueMap, Set<TrackedEntityDataValue> trackedEntityDataValues,
        Map<String, DataElement> newDataElements, ImportSummary importSummary, ImportOptions importOptions,
        boolean isSingleValueUpdate )
    {
        boolean validateMandatoryAttributes = doValidationOfMandatoryAttributes( importOptions.getUser() );

        Map<String, String> mandatoryDataElements = new HashMap<>();
        if (validateMandatoryAttributes)
        {
            mandatoryDataElements = validatePresenceOfMandatoryDataElements( event, programStageInstance, importOptions,
                importSummary, isSingleValueUpdate );
            if ( importSummary.getStatus() == ImportStatus.ERROR )
            {
                return importSummary;
            }
        }

        IdScheme dataElementIdScheme = importOptions.getIdSchemes().getDataElementIdScheme();

        // Loop through values, if only one validation problem occurs -> FAIL
        for ( DataValue dataValue : event.getDataValues() )
        {
            DataElement dataElement;
            if ( dataElementToValueMap.containsKey( dataValue.getDataElement() ) )
            {
                dataElement = dataElementToValueMap.get( dataValue.getDataElement() ).getDataElement();
            }
            else
            {
                boolean isNewDataElement = true;
                dataElement = getDataElement( dataElementIdScheme, dataValue.getDataElement() );

                // This can happen if a wrong data element identifier is provided
                if ( dataElement == null )
                {
                    String descMsg = "Data element " + dataValue.getDataElement() + " doesn't exist in the system. Please, provide correct data element";

                    importSummary.setStatus( ImportStatus.ERROR );
                    importSummary.setDescription( descMsg );

                    return importSummary;
                }

                //A special treatment for idScheme with IdentifiableProperty.ATTRIBUTE is needed
                if ( dataElementIdScheme.isAttribute() )
                {
                    Optional<TrackedEntityDataValue> matchingTrackedEntityDataValue = trackedEntityDataValues.stream()
                        .filter( tedv -> tedv.getDataElement().getUid().equals( dataElement.getUid() ) )
                        .findFirst();

                    if ( matchingTrackedEntityDataValue.isPresent() )
                    {
                        isNewDataElement = false;
                        dataElementToValueMap.put( dataValue.getDataElement(), matchingTrackedEntityDataValue.get() );
                    }
                }

                if ( isNewDataElement )
                {
                    newDataElements.put( dataValue.getDataElement(), dataElement );
                }
            }

            // Return error if one or more values fail validation
            if ( !validateDataValue( programStageInstance, importOptions, dataElement, dataValue.getValue(),
                event.getStatus(), mandatoryDataElements, validateMandatoryAttributes, importSummary ) )
            {
                return importSummary;
            }
        }

        return importSummary;
    }

    private boolean validateDataValue( ProgramStageInstance programStageInstance, ImportOptions importOptions,
        DataElement dataElement, String value, EventStatus eventStatus, Map<String, String> mandatoryDataElements,
        boolean validateMandatoryAttributes, ImportSummary importSummary )
    {
        String status = ValidationUtils.dataValueIsValid( value, dataElement );

        if ( status != null )
        {
            importSummary.getConflicts().add( new ImportConflict( dataElement.getUid(), status ) );
            importSummary.incrementIgnored();
            importSummary.setStatus( ImportStatus.ERROR );

            return false;
        }

        if ( validateMandatoryAttributes )
        {
            ValidationStrategy validationStrategy = programStageInstance.getProgramStage().getValidationStrategy();

            if ( ( validationStrategy == ValidationStrategy.ON_UPDATE_AND_INSERT ||
                ( validationStrategy == ValidationStrategy.ON_COMPLETE && eventStatus == EventStatus.COMPLETED )) &&
                ( mandatoryDataElements.containsKey( dataElement.getUid() ) && ( StringUtils.isEmpty( value ) || "null".equals( value ) ) ) )
            {
                importSummary.getConflicts().add(
                    new ImportConflict( mandatoryDataElements.get( dataElement.getUid() ), "value_required_but_not_provided" ) );
                importSummary.incrementIgnored();
                importSummary.setStatus( ImportStatus.ERROR );

                return false;
            }
        }

        List<String> errors = trackerAccessManager.canWrite( importOptions.getUser(),
            new TrackedEntityDataValue( programStageInstance, dataElement, value ), true );

        if ( !errors.isEmpty() )
        {
            String identifier;

            if ( mandatoryDataElements.containsKey( dataElement.getUid() ) )
            {
                identifier = mandatoryDataElements.get( dataElement.getUid() );
            }
            else
            {
                String tempId = IdentifiableObjectUtils
                    .getIdentifierBasedOnIdScheme( dataElement, importOptions.getIdSchemes().getDataElementIdScheme() );
                identifier = tempId != null ? tempId : dataElement.getName();
            }

            errors.forEach( error -> importSummary.getConflicts().add( new ImportConflict( identifier, error ) ) );
            importSummary.setStatus( ImportStatus.ERROR );
            importSummary.incrementIgnored();

            return false;
        }

        return true;
    }

    private ImportSummary saveEvent( Program program, ProgramInstance programInstance, ProgramStage programStage,
        ProgramStageInstance programStageInstance, OrganisationUnit organisationUnit, Event event,
        ImportOptions importOptions, boolean bulkSave )
    {
        Assert.notNull( program, "Program cannot be null" );
        Assert.notNull( programInstance, "Program instance cannot be null" );
        Assert.notNull( programStage, "Program stage cannot be null" );

        ImportSummary importSummary = new ImportSummary( event.getEvent() );
        importOptions = updateImportOptions( importOptions );

        boolean existingEvent = programStageInstance != null;
        boolean dryRun = importOptions.isDryRun();

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

        String storedBy = getValidUsername( event.getStoredBy(), importSummary, importOptions.getUser() != null ? importOptions.getUser().getUsername() : "[Unknown]" );
        String completedBy = getValidUsername( event.getCompletedBy(), importSummary, importOptions.getUser() != null ? importOptions.getUser().getUsername() : "[Unknown]" );

        CategoryOptionCombo aoc = null;

        if ( (event.getAttributeCategoryOptions() != null && program.getCategoryCombo() != null)
            || event.getAttributeOptionCombo() != null )
        {
            IdScheme idScheme = importOptions.getIdSchemes().getCategoryOptionIdScheme();

            try
            {
                aoc = getAttributeOptionCombo( program.getCategoryCombo(), event.getAttributeCategoryOptions(),
                    event.getAttributeOptionCombo(), idScheme );
            }
            catch ( IllegalQueryException ex )
            {
                importSummary.getConflicts().add( new ImportConflict( ex.getMessage(), event.getAttributeCategoryOptions() ) );
                importSummary.setStatus( ImportStatus.ERROR );
                return importSummary.incrementIgnored();
            }
        }
        else
        {
            aoc = (CategoryOptionCombo) getDefaultObject( CategoryOptionCombo.class );
        }

        if ( aoc != null && aoc.isDefault() && program.getCategoryCombo() != null && !program.getCategoryCombo().isDefault() )
        {
            importSummary.getConflicts().add( new ImportConflict( "attributeOptionCombo", "Default attribute option combo is not allowed since program has non-default category combo" ) );
            importSummary.setStatus( ImportStatus.ERROR );
            return importSummary.incrementIgnored();
        }

        Date eventDate = executionDate != null ? executionDate : dueDate;

        validateAttributeOptionComboDate( aoc, eventDate );

        List<String> errors = trackerAccessManager.canWrite( importOptions.getUser(), aoc );

        if ( !errors.isEmpty() )
        {
            importSummary.setStatus( ImportStatus.ERROR );
            importSummary.getConflicts().addAll( errors.stream().map( s -> new ImportConflict( "CategoryOptionCombo", s ) ).collect( Collectors.toList() ) );
            importSummary.incrementIgnored();

            return importSummary;
        }

        if ( !dryRun )
        {
            if ( programStageInstance == null )
            {
                programStageInstance = createProgramStageInstance( event, programStage, programInstance,
                    organisationUnit, dueDate, executionDate, event.getStatus().getValue(),
                    completedBy, storedBy, event.getEvent(), aoc, importOptions );
            }
            else
            {
                updateProgramStageInstance( event, programStage, programInstance, organisationUnit, dueDate,
                    executionDate, event.getStatus().getValue(), completedBy, storedBy, programStageInstance, aoc );
            }

            if ( !importOptions.isSkipLastUpdated() )
            {
                updateTrackedEntityInstance( programStageInstance, importOptions.getUser(), bulkSave );
            }

            importSummary.setReference( programStageInstance.getUid() );
        }

        Map<String, TrackedEntityDataValue> dataElementValueMap = Maps.newHashMap();

        if ( existingEvent )
        {
            dataElementValueMap = getDataElementDataValueMap(
                dataValueService.getTrackedEntityDataValues( programStageInstance ), importOptions );
        }

        boolean validateMandatoryAttributes = doValidationOfMandatoryAttributes( importOptions.getUser() );
        Map<String, String> mandatoryDataElements = new HashMap<>();
        if (validateMandatoryAttributes)
        {
            mandatoryDataElements = validatePresenceOfMandatoryDataElements( event, programStageInstance, importOptions,
                importSummary, false );
            if ( importSummary.getStatus() == ImportStatus.ERROR )
            {
                return importSummary;
            }
        }

        for ( DataValue dataValue : event.getDataValues() )
        {
            DataElement dataElement;

            if ( dataElementValueMap.containsKey( dataValue.getDataElement() ) )
            {
                dataElement = dataElementValueMap.get( dataValue.getDataElement() ).getDataElement();
            }
            else
            {
                dataElement = getDataElement( importOptions.getIdSchemes().getDataElementIdScheme(),
                    dataValue.getDataElement() );
            }

            if ( dataElement != null )
            {
                if ( validateDataValue( programStageInstance, importOptions, dataElement, dataValue.getValue(), event.getStatus(), mandatoryDataElements, validateMandatoryAttributes, importSummary ) )
                {
                    String dataValueStoredBy = dataValue.getStoredBy() != null ? dataValue.getStoredBy() : storedBy;

                    if ( !dryRun )
                    {
                        TrackedEntityDataValue existingDataValue = dataElementValueMap.get( dataValue.getDataElement() );

                        saveDataValue( programStageInstance, dataValueStoredBy, dataElement, dataValue.getValue(),
                            dataValue.getProvidedElsewhere(), existingDataValue, importSummary );
                    }
                }
            }
            else
            {
                importSummary.getConflicts().add( new ImportConflict( "dataElement", dataValue.getDataElement() + " is not a valid data element" ) );
                importSummary.getImportCount().incrementIgnored();
            }
        }

        sendProgramNotification( programStageInstance, importOptions );


        if ( importSummary.getConflicts().size() > 0 ) {
            importSummary.setStatus( ImportStatus.ERROR );
        }
        else {
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
                programNotificationPublisher.publishEvent( programStageInstance, ProgramNotificationEventType.PROGRAM_STAGE_COMPLETION );
            }

            if ( EventStatus.SCHEDULE.equals( programStageInstance.getStatus() ) )
            {
                eventPublisher.publishEvent( new ProgramStageInstanceScheduledEvent( this, programStageInstance ) );
            }
        }
    }

    private void saveDataValue( ProgramStageInstance programStageInstance, String storedBy, DataElement dataElement,
        String value, Boolean providedElsewhere, TrackedEntityDataValue dataValue, ImportSummary importSummary )
    {
        if ( value != null && value.trim().length() == 0 )
        {
            value = null;
        }

        if ( value != null )
        {
            if ( dataValue == null )
            {
                dataValue = new TrackedEntityDataValue( programStageInstance, dataElement, value );
                dataValue.setStoredBy( storedBy );
                dataValue.setProvidedElsewhere( providedElsewhere );

                dataValueService.saveTrackedEntityDataValue( dataValue );

                programStageInstance.getDataValues().add( dataValue );

                if ( importSummary != null )
                {
                    importSummary.getImportCount().incrementImported();
                }
            }
            else
            {
                dataValue.setValue( value );
                dataValue.setStoredBy( storedBy );
                dataValue.setProvidedElsewhere( providedElsewhere );

                dataValueService.updateTrackedEntityDataValue( dataValue );

                if ( importSummary != null )
                {
                    importSummary.getImportCount().incrementUpdated();
                }
            }
        }
        else if ( dataValue != null )
        {
            dataValueService.deleteTrackedEntityDataValue( dataValue );

            if ( importSummary != null )
            {
                importSummary.getImportCount().incrementDeleted();
            }
        }
    }

    private ProgramStageInstance createProgramStageInstance( Event event, ProgramStage programStage,
        ProgramInstance programInstance, OrganisationUnit organisationUnit, Date dueDate, Date executionDate,
        int status, String completedBy, String storeBy, String programStageInstanceIdentifier,
        CategoryOptionCombo aoc, ImportOptions importOptions )
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

        programStageInstance.setStoredBy( storeBy );

        updateProgramStageInstance( event, programStage, programInstance, organisationUnit, dueDate, executionDate,
            status, storeBy, completedBy, programStageInstance, aoc );

        return programStageInstance;
    }

    private void updateProgramStageInstance( Event event, ProgramStage programStage, ProgramInstance programInstance,
        OrganisationUnit organisationUnit, Date dueDate, Date executionDate, int status,
        String completedBy, String storedBy, ProgramStageInstance programStageInstance, CategoryOptionCombo aoc)
    {
        programStageInstance.setProgramInstance( programInstance );
        programStageInstance.setProgramStage( programStage );
        programStageInstance.setDueDate( dueDate );
        programStageInstance.setExecutionDate( executionDate );
        programStageInstance.setOrganisationUnit( organisationUnit );
        programStageInstance.setAttributeOptionCombo( aoc );
        programStageInstance.setGeometry( event.getGeometry() );

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

        if ( programStageInstance.getId() == 0 )
        {
            programStageInstance.setAutoFields();
            programStageInstanceService.addProgramStageInstance( programStageInstance );
        }
        else
        {
            programStageInstanceService.updateProgramStageInstance( programStageInstance );
        }
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

    private String getValidUsername( String userName, ImportSummary importSummary, String fallbackUsername )
    {
        String validUsername = userName;

        if ( StringUtils.isEmpty( validUsername ) )
        {
            validUsername = User.getSafeUsername( fallbackUsername );
        }
        else if ( validUsername.length() > UserCredentials.USERNAME_MAX_LENGTH )
        {
            if ( importSummary != null )
            {
                importSummary.getConflicts().add( new ImportConflict( "Username",
                    validUsername + " is more than " + UserCredentials.USERNAME_MAX_LENGTH + " characters, using current username instead" ) );
            }

            validUsername = User.getSafeUsername( fallbackUsername );
        }

        return validUsername;
    }

    private Map<String, TrackedEntityDataValue> getDataElementDataValueMap(
        Collection<TrackedEntityDataValue> dataValues, ImportOptions importOptions )
    {
        //idScheme with IdentifiableProperty.ATTRIBUTE has to be specially treated in the code

        IdScheme idScheme = importOptions.getIdSchemes().getDataElementIdScheme();

        if ( idScheme.isNull() || idScheme.is( IdentifiableProperty.UID ) || idScheme.is( IdentifiableProperty.CODE ) ||
            idScheme.is( IdentifiableProperty.NAME ) || idScheme.is( IdentifiableProperty.ID ) )
        {
            return dataValues.stream()
                .collect( Collectors
                    .toMap( dv -> IdentifiableObjectUtils.getIdentifierBasedOnIdScheme( dv.getDataElement(), idScheme ), dv -> dv ) );
        }

        return Collections.emptyMap();
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

    private TrackedEntityInstance getTrackedEntityInstance( String uid )
    {
        if ( uid == null )
        {
            return null;
        }

        TrackedEntityInstance tei =  trackedEntityInstanceCache.get( uid );

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

            if( program != null )
            {
                programCache.put( id, program );

                programStageCache.putAll( program.getProgramStages().stream().collect( Collectors.toMap( ProgramStage::getUid, ps -> ps ) ) );

                for ( ProgramStage programStage : program.getProgramStages() )
                {
                    dataElementCache.putAll( programStage.getAllDataElements().stream().collect( Collectors.toMap( DataElement::getUid, de -> de ) ) );
                }
            }
        }

        return program;
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

                dataElementCache.putAll( programStage.getAllDataElements().stream().collect( Collectors.toMap( DataElement::getUid, de -> de ) ) );
            }
        }

        return programStage;
    }

    private DataElement getDataElement( IdScheme idScheme, String id )
    {
        return dataElementCache.get( id, () -> manager.getObject( DataElement.class, idScheme, id ) );
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
        return defaultObjectsCache.get( key, () -> manager.getByName( CategoryOptionCombo.class , "default" ) );
    }

    @Override
    public void validate( EventSearchParams params, User user )
        throws IllegalQueryException
    {
        String violation = null;

        if ( params.hasLastUpdatedDuration() && (params.hasLastUpdatedStartDate() || params.hasLastUpdatedEndDate()) )
        {
            violation = "Last updated from and/or to and last updated duration cannot be specified simultaneously";
        }

        if ( violation == null && params.hasLastUpdatedDuration()
            && DateUtils.getDuration( params.getLastUpdatedDuration() ) == null )
        {
            violation = "Duration is not valid: " + params.getLastUpdatedDuration();
        }

        if ( violation == null && params.getOrgUnit() != null
            && !trackerAccessManager.canAccess( user, params.getProgram(), params.getOrgUnit() ) )
        {
            violation = "User does not have access to orgUnit: " + params.getOrgUnit().getUid();
        }

        if ( violation == null && params.getOrgUnitSelectionMode() != null )
        {
            violation = getOuModeViolation( params, user );
        }

        if ( violation != null )
        {
            log.warn( "Validation failed: " + violation );

            throw new IllegalQueryException( violation );
        }
    }

    private String getOuModeViolation( EventSearchParams params, User user )
    {
        OrganisationUnitSelectionMode selectedOuMode = params.getOrgUnitSelectionMode();

        String violation = null;

        switch ( selectedOuMode )
        {
        case ALL:
            violation = userCanSearchOuModeALL( user ) ? null
                : "Current user is not authorized to query across all organisation units";
            break;
        case ACCESSIBLE:
        case CAPTURE:
            violation = user == null ? "User is required for ouMode: " + params.getOrgUnitSelectionMode() : null;
            break;
        case CHILDREN:
        case SELECTED:
        case DESCENDANTS:
            violation = params.getOrgUnit() == null
                ? "Organisation unit is required for ouMode: " + params.getOrgUnitSelectionMode()
                : null;
            break;
        default:
            violation = "Invalid ouMode:  " + params.getOrgUnitSelectionMode();
            break;
        }

        return violation;
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

    private void validateExpiryDays( Event event, Program program, ProgramStageInstance programStageInstance )
    {
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
                            "The event's completness date has expired. Not possible to make changes to this event" );
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
                        throw new IllegalQueryException( "The event's date belongs to an expired period. It is not possble to create such event" );
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
        dataElementCache.clear();
        categoryOptionCache.clear();
        categoryOptionComboCache.clear();
        attributeOptionComboCache.clear();
        defaultObjectsCache.clear();

        updateEntities( user );

        dbmsManager.clearSession();
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

    protected void reloadUser( ImportOptions importOptions )
    {
        if ( importOptions == null || importOptions.getUser() == null )
        {
            return;
        }

        importOptions.setUser( userService.getUser( importOptions.getUser().getId() ) );
    }

    private boolean userCanSearchOuModeALL( User user )
    {
        if ( user == null )
        {
            return false;
        }

        return user.isSuper()
            || user.isAuthorized( Authorities.F_TRACKED_ENTITY_INSTANCE_SEARCH_IN_ALL_ORGUNITS.name() );
    }
}
