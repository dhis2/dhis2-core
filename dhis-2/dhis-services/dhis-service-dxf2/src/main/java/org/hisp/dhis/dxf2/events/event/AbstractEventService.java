package org.hisp.dhis.dxf2.events.event;

/*
 * Copyright (c) 2004-2017, University of Oslo
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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.SessionFactory;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.GridHeader;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.common.IdSchemes;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.common.Pager;
import org.hisp.dhis.common.QueryFilter;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.common.QueryOperator;
import org.hisp.dhis.commons.collection.CachingMap;
import org.hisp.dhis.commons.util.DebugUtils;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementCategoryOption;
import org.hisp.dhis.dataelement.DataElementCategoryOptionCombo;
import org.hisp.dhis.dataelement.DataElementCategoryService;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.dbms.DbmsManager;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.events.enrollment.EnrollmentStatus;
import org.hisp.dhis.dxf2.events.report.EventRow;
import org.hisp.dhis.dxf2.events.report.EventRows;
import org.hisp.dhis.dxf2.importsummary.ImportConflict;
import org.hisp.dhis.dxf2.importsummary.ImportStatus;
import org.hisp.dhis.dxf2.importsummary.ImportSummaries;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.hisp.dhis.dxf2.utils.InputUtils;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.fileresource.FileResourceService;
import org.hisp.dhis.i18n.I18nManager;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
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
import org.hisp.dhis.query.Order;
import org.hisp.dhis.scheduling.TaskId;
import org.hisp.dhis.system.grid.ListGrid;
import org.hisp.dhis.system.notification.NotificationLevel;
import org.hisp.dhis.system.notification.Notifier;
import org.hisp.dhis.system.util.DateUtils;
import org.hisp.dhis.system.util.ValidationUtils;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceService;
import org.hisp.dhis.trackedentitycomment.TrackedEntityComment;
import org.hisp.dhis.trackedentitycomment.TrackedEntityCommentService;
import org.hisp.dhis.trackedentitydatavalue.TrackedEntityDataValue;
import org.hisp.dhis.trackedentitydatavalue.TrackedEntityDataValueService;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserCredentials;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.hisp.dhis.dxf2.events.event.EventSearchParams.*;
import static org.hisp.dhis.system.notification.NotificationLevel.ERROR;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Transactional
public abstract class AbstractEventService
    implements EventService
{
    private static final Log log = LogFactory.getLog( AbstractEventService.class );

    public static final List<String> STATIC_EVENT_COLUMNS = Arrays.asList( EVENT_ID, EVENT_CREATED_ID,
        EVENT_LAST_UPDATED_ID, EVENT_STORED_BY_ID, EVENT_COMPLETED_BY_ID, EVENT_COMPLETED_DATE_ID,
        EVENT_EXECUTION_DATE_ID, EVENT_DUE_DATE_ID, EVENT_ORG_UNIT_ID, EVENT_ORG_UNIT_NAME, EVENT_STATUS_ID,
        EVENT_LONGITUDE_ID, EVENT_LATITUDE_ID, EVENT_PROGRAM_STAGE_ID, EVENT_PROGRAM_ID,
        EVENT_ATTRIBUTE_OPTION_COMBO_ID, EVENT_DELETED );

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
    protected I18nManager i18nManager;

    @Autowired
    protected Notifier notifier;

    @Autowired
    protected SessionFactory sessionFactory;

    @Autowired
    protected DbmsManager dbmsManager;

    @Autowired
    protected IdentifiableObjectManager manager;

    @Autowired
    protected DataElementCategoryService categoryService;

    @Autowired
    protected InputUtils inputUtils;

    @Autowired
    protected FileResourceService fileResourceService;

    protected static final int FLUSH_FREQUENCY = 50;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    // -------------------------------------------------------------------------
    // Caches
    // -------------------------------------------------------------------------

    private CachingMap<String, OrganisationUnit> organisationUnitCache = new CachingMap<>();

    private CachingMap<String, Program> programCache = new CachingMap<>();

    private CachingMap<String, ProgramStage> programStageCache = new CachingMap<>();

    private CachingMap<String, DataElement> dataElementCache = new CachingMap<>();

    private Set<Program> accessibleProgramsCache = new HashSet<>();

    // -------------------------------------------------------------------------
    // CREATE
    // -------------------------------------------------------------------------

    @Override
    public ImportSummaries addEvents( List<Event> events, ImportOptions importOptions )
    {
        ImportSummaries importSummaries = new ImportSummaries();
        int counter = 0;

        User user = currentUserService.getCurrentUser();

        for ( Event event : events )
        {
            importSummaries.addImportSummary( addEvent( event, user, importOptions ) );

            if ( counter % FLUSH_FREQUENCY == 0 )
            {
                clearSession();
            }

            counter++;
        }

        return importSummaries;
    }

    @Override
    public ImportSummaries addEvents( List<Event> events, ImportOptions importOptions, TaskId taskId )
    {
        notifier.clear( taskId ).notify( taskId, "Importing events" );

        try
        {
            ImportSummaries importSummaries = addEvents( events, importOptions );

            if ( taskId != null )
            {
                notifier.notify( taskId, NotificationLevel.INFO, "Import done", true ).addTaskSummary( taskId,
                    importSummaries );
            }

            return importSummaries;
        }
        catch ( RuntimeException ex )
        {
            log.error( DebugUtils.getStackTrace( ex ) );
            notifier.notify( taskId, ERROR, "Process failed: " + ex.getMessage(), true );
            return new ImportSummaries().addImportSummary(
                new ImportSummary( ImportStatus.ERROR, "The import process failed: " + ex.getMessage() ) );
        }
    }

    @Override
    public ImportSummary addEvent( Event event, ImportOptions importOptions )
    {
        return addEvent( event, currentUserService.getCurrentUser(), importOptions );
    }

    protected ImportSummary addEvent( Event event, User user, ImportOptions importOptions )
    {
        if ( importOptions == null )
        {
            importOptions = new ImportOptions();
        }

        Program program = getProgram( importOptions.getIdSchemes().getProgramIdScheme(), event.getProgram() );
        ProgramStage programStage = getProgramStage( importOptions.getIdSchemes().getProgramStageIdScheme(),
            event.getProgramStage() );

        ProgramInstance programInstance;
        ProgramStageInstance programStageInstance = null;

        if ( program == null )
        {
            return new ImportSummary( ImportStatus.ERROR,
                "Event.program does not point to a valid program: " + event.getProgram() ).incrementIgnored();
        }

        if ( programStage == null && program.isRegistration() )
        {
            return new ImportSummary( ImportStatus.ERROR,
                "Event.programStage does not point to a valid programStage, and program is multi stage: "
                    + event.getProgramStage() ).incrementIgnored();
        }
        else if ( programStage == null )
        {
            programStage = program.getProgramStageByStage( 1 );
        }

        Assert.notNull( programStage, "Program stage cannot be null" );

        if ( !canAccess( program, user ) )
        {
            return new ImportSummary( ImportStatus.ERROR,
                "Current user does not have permission to access this program" ).incrementIgnored();
        }

        if ( program.isRegistration() )
        {
            if ( event.getTrackedEntityInstance() == null )
            {
                return new ImportSummary( ImportStatus.ERROR,
                    "No Event.trackedEntityInstance was provided for registration based program" ).incrementIgnored();
            }

            org.hisp.dhis.trackedentity.TrackedEntityInstance entityInstance = entityInstanceService
                .getTrackedEntityInstance( event.getTrackedEntityInstance() );

            if ( entityInstance == null )
            {
                return new ImportSummary( ImportStatus.ERROR,
                    "Event.trackedEntityInstance does not point to a valid tracked entity instance: "
                        + event.getTrackedEntityInstance() ).incrementIgnored();
            }

            List<ProgramInstance> programInstances = new ArrayList<>(
                programInstanceService.getProgramInstances( entityInstance, program, ProgramStatus.ACTIVE ) );

            if ( programInstances.isEmpty() )
            {
                return new ImportSummary( ImportStatus.ERROR, "Tracked entity instance: " + entityInstance.getUid()
                    + " is not enrolled in program: " + program.getUid() ).incrementIgnored();
            }
            else if ( programInstances.size() > 1 )
            {
                return new ImportSummary( ImportStatus.ERROR, "Tracked entity instance: " + entityInstance.getUid()
                    + " have multiple active enrollments in program: " + program.getUid() ).incrementIgnored();
            }

            programInstance = programInstances.get( 0 );

            if ( !programStage.getRepeatable() )
            {
                programStageInstance = programStageInstanceService.getProgramStageInstance( programInstance,
                    programStage );

                if ( programStageInstance != null && !programStageInstance.getUid().equals( event.getEvent() ) )
                {
                    return new ImportSummary( ImportStatus.ERROR,
                        "Program stage is not repeatable and an event already exists" ).incrementIgnored();
                }
            }
            else
            {
                if ( StringUtils.isNotEmpty( event.getEvent() ) )
                {
                    programStageInstance = manager.getObject( ProgramStageInstance.class, importOptions.getIdSchemes().getProgramStageInstanceIdScheme(), event.getEvent() );
                    
                    if ( programStageInstance == null )
                    {
                        if ( !CodeGenerator.isValidUid( event.getEvent() ) )
                        {
                            return new ImportSummary( ImportStatus.ERROR,
                                "Event.event did not point to a valid event: " + event.getEvent() ).incrementIgnored();
                        }
                    }
                }
            }
        }
        else
        {
            List<ProgramInstance> programInstances = new ArrayList<>(
                programInstanceService.getProgramInstances( program, ProgramStatus.ACTIVE ) );

            if ( programInstances.isEmpty() )
            {
                // Create PI if it doesn't exist (should only be one)
                ProgramInstance pi = new ProgramInstance();
                pi.setEnrollmentDate( new Date() );
                pi.setIncidentDate( new Date() );
                pi.setProgram( program );
                pi.setStatus( ProgramStatus.ACTIVE );

                programInstanceService.addProgramInstance( pi );

                programInstances.add( pi );
            }
            else if ( programInstances.size() > 1 )
            {
                return new ImportSummary( ImportStatus.ERROR,
                    "Multiple active program instances exists for program: " + program.getUid() ).incrementIgnored();
            }

            programInstance = programInstances.get( 0 );

            if ( StringUtils.isNotEmpty( event.getEvent() ) )
            {
                programStageInstance = manager.getObject( ProgramStageInstance.class, importOptions.getIdSchemes().getProgramStageInstanceIdScheme(), event.getEvent() );

                if ( programStageInstance == null )
                {
                    if ( importOptions.getIdSchemes().getProgramStageInstanceIdScheme().equals( IdScheme.UID ) && !CodeGenerator.isValidUid( event.getEvent() ) )
                    {
                        return new ImportSummary( ImportStatus.ERROR,
                            "Event.event did not point to a valid event: " + event.getEvent() ).incrementIgnored();
                    }
                }
            }
        }

        OrganisationUnit organisationUnit = getOrganisationUnit( importOptions.getIdSchemes(), event.getOrgUnit() );

        program = programInstance.getProgram();

        if ( programStageInstance != null )
        {
            programStage = programStageInstance.getProgramStage();
        }

        if ( organisationUnit == null )
        {
            return new ImportSummary( ImportStatus.ERROR,
                "Event.orgUnit does not point to a valid organisation unit: " + event.getOrgUnit() ).incrementIgnored();
        }

        if ( !programInstance.getProgram().hasOrganisationUnit( organisationUnit ) )
        {
            return new ImportSummary( ImportStatus.ERROR,
                "Program is not assigned to this organisation unit: " + event.getOrgUnit() ).incrementIgnored();
        }

        validateExpiryDays( event, program, null );

        return saveEvent( program, programInstance, programStage, programStageInstance, organisationUnit, event, user,
            importOptions );
    }

    // -------------------------------------------------------------------------
    // READ
    // -------------------------------------------------------------------------

    @Override
    public Events getEvents( EventSearchParams params )
    {
        validate( params );

        List<OrganisationUnit> organisationUnits = getOrganisationUnits( params );

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

        events.setEvents( eventList );

        return events;
    }

    @Override
    public Events getEvents( Collection<String> uids )
    {
        Events events = new Events();

        List<ProgramStageInstance> programStageInstances = manager.getByUid( ProgramStageInstance.class, uids );
        programStageInstances.forEach(
            programStageInstance -> events.getEvents().add( convertProgramStageInstance( programStageInstance ) ) );

        return events;
    }

    @Override
    public Grid getEventsGrid( EventSearchParams params )
    {
        List<OrganisationUnit> organisationUnits = getOrganisationUnits( params );

        // ---------------------------------------------------------------------
        // If no data element is specified, use those configured for
        // display in report
        // ---------------------------------------------------------------------
        if ( params.getDataElements().isEmpty() && params.getProgramStage() != null
            && params.getProgramStage().getProgramStageDataElements() != null )
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
    public int getAnonymousEventValuesCountLastUpdatedAfter( Date lastSuccessTime )
    {
        EventSearchParams params = buildAnonymousEventsSearchParams( lastSuccessTime );
        return eventStore.getEventCount( params, null );
    }

    @Override
    public Events getAnonymousEventValuesLastUpdatedAfter( Date lastSuccessTime )
    {
        EventSearchParams params = buildAnonymousEventsSearchParams( lastSuccessTime );
        Events anonymousEvents = new Events();
        List<Event> events = eventStore.getEvents( params, null );
        anonymousEvents.setEvents( events );
        return anonymousEvents;
    }

    private EventSearchParams buildAnonymousEventsSearchParams( Date lastSuccessTime )
    {
        EventSearchParams params = new EventSearchParams();
        params.setProgramType( ProgramType.WITHOUT_REGISTRATION );
        params.setLastUpdatedStartDate( lastSuccessTime );
        return params;
    }

    @Override
    public EventRows getEventRows( EventSearchParams params )
    {
        List<OrganisationUnit> organisationUnits = getOrganisationUnits( params );

        EventRows eventRows = new EventRows();

        List<EventRow> eventRowList = eventStore.getEventRows( params, organisationUnits );

        eventRows.setEventRows( eventRowList );

        return eventRows;
    }

    @Override
    public EventSearchParams getFromUrl( String program, String programStage, ProgramStatus programStatus,
        Boolean followUp, String orgUnit, OrganisationUnitSelectionMode orgUnitSelectionMode,
        String trackedEntityInstance, Date startDate, Date endDate, Date dueDateStart, Date dueDateEnd,
        Date lastUpdatedStartDate, Date lastUpdatedEndDate, EventStatus status,
        DataElementCategoryOptionCombo attributeOptionCombo, IdSchemes idSchemes, Integer page, Integer pageSize,
        boolean totalPages, boolean skipPaging, List<Order> orders, List<String> gridOrders, boolean includeAttributes,
        Set<String> events, Set<String> filters, Set<String> dataElements, boolean includeDeleted )
    {
        UserCredentials userCredentials = currentUserService.getCurrentUser().getUserCredentials();

        EventSearchParams params = new EventSearchParams();

        Program pr = programService.getProgram( program );

        if ( StringUtils.isNotEmpty( program ) && pr == null )
        {
            throw new IllegalQueryException( "Program is specified but does not exist: " + program );
        }

        ProgramStage ps = programStageService.getProgramStage( programStage );

        if ( StringUtils.isNotEmpty( programStage ) && ps == null )
        {
            throw new IllegalQueryException( "Program stage is specified but does not exist: " + programStage );
        }

        OrganisationUnit ou = organisationUnitService.getOrganisationUnit( orgUnit );

        if ( StringUtils.isNotEmpty( orgUnit ) && ou == null )
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

        if ( pr != null && !userCredentials.isSuper() && !userCredentials.canAccessProgram( pr ) )
        {
            throw new IllegalQueryException( "User has no access to program: " + pr.getUid() );
        }

        TrackedEntityInstance tei = entityInstanceService.getTrackedEntityInstance( trackedEntityInstance );

        if ( StringUtils.isNotEmpty( trackedEntityInstance ) && tei == null )
        {
            throw new IllegalQueryException(
                "Tracked entity instance is specified but does not exist: " + trackedEntityInstance );
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
            if ( StringUtils.isNotEmpty( programStage ) && ps == null )
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
        params.setEventStatus( status );
        params.setCategoryOptionCombo( attributeOptionCombo );
        params.setIdSchemes( idSchemes );
        params.setPage( page );
        params.setPageSize( pageSize );
        params.setTotalPages( totalPages );
        params.setSkipPaging( skipPaging );
        params.setIncludeAttributes( includeAttributes );
        params.setOrders( orders );
        params.setGridOrders( gridOrders );
        params.setEvents( events );
        params.setIncludeDeleted( includeDeleted );

        return params;
    }

    @Override
    public Event getEvent( String uid )
    {
        ProgramStageInstance psi = programStageInstanceService.getProgramStageInstance( uid );

        return psi != null ? convertProgramStageInstance( psi ) : null;
    }

    @Override
    public Event getEvent( ProgramStageInstance programStageInstance )
    {
        return convertProgramStageInstance( programStageInstance );
    }

    // -------------------------------------------------------------------------
    // UPDATE
    // -------------------------------------------------------------------------

    @Override
    public ImportSummaries updateEvents( List<Event> events, boolean singleValue )
    {
        ImportSummaries importSummaries = new ImportSummaries();
        int counter = 0;

        User user = currentUserService.getCurrentUser();

        for ( Event event : events )
        {
            importSummaries.addImportSummary( updateEvent( event, user, singleValue, null ) );

            if ( counter % FLUSH_FREQUENCY == 0 )
            {
                clearSession();
            }

            counter++;
        }

        return importSummaries;
    }

    @Override
    public ImportSummary updateEvent( Event event, boolean singleValue )
    {
        return updateEvent( event, singleValue, null );
    }

    @Override
    public ImportSummary updateEvent( Event event, boolean singleValue, ImportOptions importOptions )
    {
        return updateEvent( event, currentUserService.getCurrentUser(), singleValue, importOptions );
    }

    private ImportSummary updateEvent( Event event, User user, boolean singleValue, ImportOptions importOptions )
    {
        if ( importOptions == null )
        {
            importOptions = new ImportOptions();
        }

        ImportSummary importSummary = new ImportSummary( event.getEvent() );
        ProgramStageInstance programStageInstance = programStageInstanceService
            .getProgramStageInstance( event.getEvent() );

        if ( programStageInstance == null )
        {
            importSummary.getConflicts().add( new ImportConflict( "Invalid Event ID.", event.getEvent() ) );
            return importSummary.incrementIgnored();
        }

        OrganisationUnit organisationUnit = getOrganisationUnit( importOptions.getIdSchemes(), event.getOrgUnit() );

        if ( organisationUnit == null )
        {
            organisationUnit = programStageInstance.getOrganisationUnit();
        }

        Date executionDate = new Date();

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

        String storedBy = getStoredBy( event, null, user );
        programStageInstance.setStoredBy( storedBy );

        String completedBy = getCompletedBy( event, null, user );

        if ( event.getStatus() == EventStatus.ACTIVE )
        {
            programStageInstance.setStatus( EventStatus.ACTIVE );
            programStageInstance.setCompletedBy( null );
            programStageInstance.setCompletedDate( null );
        }
        else if ( programStageInstance.getStatus() != event.getStatus() && event.getStatus() == EventStatus.COMPLETED )
        {
            programStageInstance.setStatus( EventStatus.COMPLETED );
            programStageInstance.setCompletedBy( completedBy );
            programStageInstance.setCompletedDate( executionDate );

            if ( programStageInstance.isCompleted() )
            {
                programStageInstanceService.completeProgramStageInstance( programStageInstance,
                    importOptions.isSkipNotifications(), i18nManager.getI18nFormat() );
            }
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

        if ( !singleValue )
        {
            if ( programStageInstance.getProgramStage().getCaptureCoordinates() )
            {
                if ( event.getCoordinate() != null && event.getCoordinate().isValid() )
                {
                    programStageInstance.setLatitude( event.getCoordinate().getLatitude() );
                    programStageInstance.setLongitude( event.getCoordinate().getLongitude() );
                }
                else
                {
                    programStageInstance.setLatitude( null );
                    programStageInstance.setLongitude( null );
                }
            }
        }

        Program program = getProgram( importOptions.getIdSchemes().getProgramIdScheme(), event.getProgram() );

        validateExpiryDays( event, program, programStageInstance );

        if ( ( event.getAttributeCategoryOptions() != null && program.getCategoryCombo() != null ) || event.getAttributeOptionCombo() != null )
        {
            IdScheme idScheme = importOptions.getIdSchemes().getCategoryOptionIdScheme();
            
            DataElementCategoryOptionCombo attributeOptionCombo = inputUtils.getAttributeOptionCombo(
                program.getCategoryCombo(), event.getAttributeCategoryOptions(), event.getAttributeOptionCombo(), idScheme );

            if ( attributeOptionCombo == null )
            {
                importSummary.getConflicts().add( new ImportConflict( "Invalid attribute option combo identifier:",
                    event.getAttributeCategoryOptions() ) );
                return importSummary.incrementIgnored();
            }

            programStageInstance.setAttributeOptionCombo( attributeOptionCombo );
        }

        programStageInstance.setDeleted( event.isDeleted() );

        programStageInstanceService.updateProgramStageInstance( programStageInstance );
        updateTrackedEntityInstance( programStageInstance );

        saveTrackedEntityComment( programStageInstance, event, storedBy );

        Set<TrackedEntityDataValue> dataValues = new HashSet<>(
            dataValueService.getTrackedEntityDataValues( programStageInstance ) );
        Map<String, TrackedEntityDataValue> existingDataValues = getDataElementDataValueMap( dataValues );

        for ( DataValue value : event.getDataValues() )
        {
            DataElement dataElement = getDataElement( importOptions.getIdSchemes().getDataElementIdScheme(),
                value.getDataElement() );
            TrackedEntityDataValue dataValue = dataValueService.getTrackedEntityDataValue( programStageInstance,
                dataElement );

            if ( !validateDataValue( dataElement, value.getValue(), importSummary ) )
            {
                continue;
            }

            if ( dataValue != null )
            {
                if ( StringUtils.isEmpty( value.getValue() ) && dataElement.isFileType()
                    && !StringUtils.isEmpty( dataValue.getValue() ) )
                {
                    fileResourceService.deleteFileResource( dataValue.getValue() );
                }

                dataValue.setValue( value.getValue() );
                dataValue.setProvidedElsewhere( value.getProvidedElsewhere() );
                dataValueService.updateTrackedEntityDataValue( dataValue );

                dataValues.remove( dataValue );
            }
            else
            {
                TrackedEntityDataValue existingDataValue = existingDataValues.get( value.getDataElement() );

                saveDataValue( programStageInstance, event.getStoredBy(), dataElement, value.getValue(),
                    value.getProvidedElsewhere(), existingDataValue, null );
            }
        }

        if ( !singleValue )
        {
            dataValues.forEach( dataValueService::deleteTrackedEntityDataValue );
        }
        
        importSummary.setStatus( importSummary.getConflicts().isEmpty() ? ImportStatus.SUCCESS : ImportStatus.WARNING );
        
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

        saveTrackedEntityComment( programStageInstance, event,
            getStoredBy( event, null, currentUserService.getCurrentUser() ) );
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

        Date executionDate = new Date();

        if ( event.getEventDate() != null )
        {
            executionDate = DateUtils.parseDate( event.getEventDate() );
        }

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

    // -------------------------------------------------------------------------
    // DELETE
    // -------------------------------------------------------------------------

    @Override
    public ImportSummary deleteEvent( String uid )
    {
        ProgramStageInstance programStageInstance = programStageInstanceService.getProgramStageInstance( uid );

        if ( programStageInstance != null )
        {
            programStageInstanceService.deleteProgramStageInstance( programStageInstance );
            return new ImportSummary( ImportStatus.SUCCESS, "Deletion of event " + uid + " was successful" )
                .incrementDeleted();
        }

        return new ImportSummary( ImportStatus.ERROR, "ID " + uid + " does not point to a valid event: " + uid )
            .incrementIgnored();
    }

    @Override
    public ImportSummaries deleteEvents( List<String> uids )
    {
        ImportSummaries importSummaries = new ImportSummaries();
        int counter = 0;

        for ( String uid : uids )
        {
            importSummaries.addImportSummary( deleteEvent( uid ) );

            if ( counter % FLUSH_FREQUENCY == 0 )
            {
                clearSession();
            }

            counter++;
        }

        return importSummaries;
    }

    // -------------------------------------------------------------------------
    // HELPERS
    // -------------------------------------------------------------------------

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

    private Event convertProgramStageInstance( ProgramStageInstance programStageInstance )
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
        event.setEnrollmentStatus(
            EnrollmentStatus.fromProgramStatus( programStageInstance.getProgramInstance().getStatus() ) );
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

        UserCredentials userCredentials = currentUserService.getCurrentUser().getUserCredentials();

        OrganisationUnit ou = programStageInstance.getOrganisationUnit();

        if ( ou != null )
        {
            if ( !organisationUnitService.isInUserHierarchy( ou ) )
            {
                if ( !userCredentials.isSuper()
                    && !userCredentials.isAuthorized( "F_TRACKED_ENTITY_INSTANCE_SEARCH_IN_ALL_ORGUNITS" ) )
                {
                    throw new IllegalQueryException( "User has no access to organisation unit: " + ou.getUid() );
                }
            }

            event.setOrgUnit( ou.getUid() );
            event.setOrgUnitName( ou.getName() );
        }

        Program program = programStageInstance.getProgramInstance().getProgram();

        if ( !userCredentials.isSuper() && !userCredentials.getAllPrograms().contains( program ) )
        {
            throw new IllegalQueryException( "User has no access to program: " + program.getUid() );
        }

        event.setProgram( program.getUid() );
        event.setEnrollment( programStageInstance.getProgramInstance().getUid() );
        event.setProgramStage( programStageInstance.getProgramStage().getUid() );
        event.setAttributeOptionCombo( programStageInstance.getAttributeOptionCombo().getUid() );
        event.setAttributeCategoryOptions(
            String.join( ";", programStageInstance.getAttributeOptionCombo().getCategoryOptions().stream()
                .map( DataElementCategoryOption::getUid ).collect( Collectors.toList() ) ) );

        if ( programStageInstance.getProgramInstance().getEntityInstance() != null )
        {
            event.setTrackedEntityInstance( programStageInstance.getProgramInstance().getEntityInstance().getUid() );
        }

        if ( programStageInstance.getProgramStage().getCaptureCoordinates() )
        {
            Coordinate coordinate = null;

            if ( programStageInstance.getLongitude() != null && programStageInstance.getLatitude() != null )
            {
                coordinate = new Coordinate( programStageInstance.getLongitude(), programStageInstance.getLatitude() );

                try
                {
                    List<Double> list = OBJECT_MAPPER.readValue( coordinate.getCoordinateString(),
                        new TypeReference<List<Double>>()
                        {
                        } );

                    coordinate.setLongitude( list.get( 0 ) );
                    coordinate.setLatitude( list.get( 1 ) );
                }
                catch ( IOException ignored )
                {
                }
            }

            if ( coordinate != null && coordinate.isValid() )
            {
                event.setCoordinate( coordinate );
            }
        }

        Collection<TrackedEntityDataValue> dataValues = dataValueService
            .getTrackedEntityDataValues( programStageInstance );

        for ( TrackedEntityDataValue dataValue : dataValues )
        {
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

            note.setValue( comment.getCommentText() );
            note.setStoredBy( comment.getCreator() );

            if ( comment.getCreatedDate() != null )
            {
                note.setStoredDate( DateUtils.getIso8601NoTz( comment.getCreatedDate() ) );
            }

            event.getNotes().add( note );
        }

        return event;
    }

    private boolean canAccess( Program program, User user )
    {
        if ( accessibleProgramsCache.isEmpty() )
        {
            accessibleProgramsCache = programService.getUserPrograms( user );
        }

        return accessibleProgramsCache.contains( program );
    }

    private boolean validateDataValue( DataElement dataElement, String value, ImportSummary importSummary )
    {
        String status = ValidationUtils.dataValueIsValid( value, dataElement );

        if ( status != null )
        {
            importSummary.getConflicts().add( new ImportConflict( dataElement.getUid(), status ) );
            importSummary.getImportCount().incrementIgnored();

            return false;
        }

        return true;
    }

    private ImportSummary saveEvent( Program program, ProgramInstance programInstance, ProgramStage programStage,
        ProgramStageInstance programStageInstance, OrganisationUnit organisationUnit, Event event, User user,
        ImportOptions importOptions )
    {
        Assert.notNull( program, "Program cannot be null" );
        Assert.notNull( programInstance, "Program instance cannot be null" );
        Assert.notNull( programStage, "Program stage cannot be null" );

        ImportSummary importSummary = new ImportSummary( event.getEvent() );

        if ( importOptions == null )
        {
            importOptions = new ImportOptions();
        }

        boolean existingEvent = programStageInstance != null;
        boolean dryRun = importOptions.isDryRun();

        Date executionDate = null; // = new Date();

        if ( event.getEventDate() != null )
        {
            executionDate = DateUtils.parseDate( event.getEventDate() );
        }

        Date dueDate = new Date();

        if ( event.getDueDate() != null )
        {
            dueDate = DateUtils.parseDate( event.getDueDate() );
        }

        String storedBy = getStoredBy( event, importSummary, user );
        String completedBy = getCompletedBy( event, importSummary, user );

        DataElementCategoryOptionCombo aoc = null;

        if ( ( event.getAttributeCategoryOptions() != null && program.getCategoryCombo() != null ) || event.getAttributeOptionCombo() != null )
        {
            IdScheme idScheme = importOptions.getIdSchemes().getCategoryOptionIdScheme();

            try
            {
                aoc = inputUtils.getAttributeOptionCombo( program.getCategoryCombo(),
                    event.getAttributeCategoryOptions(), event.getAttributeOptionCombo(), idScheme );
            }
            catch ( IllegalQueryException ex )
            {
                importSummary.getConflicts()
                    .add( new ImportConflict( ex.getMessage(), event.getAttributeCategoryOptions() ) );
            }
        }
        else
        {
            aoc = categoryService.getDefaultDataElementCategoryOptionCombo();
        }

        if ( !dryRun )
        {
            if ( programStageInstance == null )
            {
                programStageInstance = createProgramStageInstance( event, programStage, programInstance, organisationUnit,
                    dueDate, executionDate, event.getStatus().getValue(), event.getCoordinate(), completedBy,
                    event.getEvent(), aoc, importOptions );
            }
            else
            {
                updateProgramStageInstance( event, programStage, programInstance, organisationUnit, dueDate, executionDate,
                    event.getStatus().getValue(), event.getCoordinate(), completedBy, programStageInstance, aoc,
                    importOptions );
            }

            updateTrackedEntityInstance( programStageInstance );
            saveTrackedEntityComment( programStageInstance, event, storedBy );

            importSummary.setReference( programStageInstance.getUid() );
        }

        Map<String, TrackedEntityDataValue> dataElementValueMap = Maps.newHashMap();

        if ( existingEvent )
        {
            dataElementValueMap = getDataElementDataValueMap(
                dataValueService.getTrackedEntityDataValues( programStageInstance ) );
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
                if ( validateDataValue( dataElement, dataValue.getValue(), importSummary ) )
                {
                    String dataValueStoredBy = dataValue.getStoredBy() != null ? dataValue.getStoredBy() : storedBy;

                    if ( !dryRun )
                    {
                        TrackedEntityDataValue existingDataValue = dataElementValueMap
                            .get( dataValue.getDataElement() );

                        saveDataValue( programStageInstance, dataValueStoredBy, dataElement, dataValue.getValue(),
                            dataValue.getProvidedElsewhere(), existingDataValue, importSummary );
                    }
                }
            }
            else
            {
                importSummary.getConflicts().add(
                    new ImportConflict( "dataElement", dataValue.getDataElement() + " is not a valid data element" ) );
                importSummary.getImportCount().incrementIgnored();
            }
        }

        importSummary.setStatus( importSummary.getConflicts().isEmpty() ? ImportStatus.SUCCESS : ImportStatus.WARNING );
        
        return importSummary;
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

    private ProgramStageInstance createProgramStageInstance( Event event, ProgramStage programStage, ProgramInstance programInstance,
        OrganisationUnit organisationUnit, Date dueDate, Date executionDate, int status, Coordinate coordinate,
        String completedBy, String programStageInstanceIdentifier, DataElementCategoryOptionCombo aoc,
        ImportOptions importOptions )
    {
        ProgramStageInstance programStageInstance = new ProgramStageInstance();
        if ( importOptions.getIdSchemes().getProgramStageInstanceIdScheme().equals( IdScheme.UID ) )
        {
            programStageInstance.setUid( CodeGenerator.isValidUid( programStageInstanceIdentifier ) ? programStageInstanceIdentifier
                : CodeGenerator.generateUid() );
        }
        else if ( importOptions.getIdSchemes().getProgramStageInstanceIdScheme().equals( IdScheme.CODE ) )
        {
            programStageInstance.setUid( CodeGenerator.generateUid() );
            programStageInstance.setCode( programStageInstanceIdentifier );
        }

        updateProgramStageInstance( event, programStage, programInstance, organisationUnit, dueDate, executionDate, status,
            coordinate, completedBy, programStageInstance, aoc, importOptions );

        return programStageInstance;
    }

    private void updateProgramStageInstance( Event event, ProgramStage programStage, ProgramInstance programInstance,
        OrganisationUnit organisationUnit, Date dueDate, Date executionDate, int status, Coordinate coordinate,
        String completedBy, ProgramStageInstance programStageInstance, DataElementCategoryOptionCombo aoc,
        ImportOptions importOptions )
    {
        programStageInstance.setProgramInstance( programInstance );
        programStageInstance.setProgramStage( programStage );
        programStageInstance.setDueDate( dueDate );
        programStageInstance.setExecutionDate( executionDate );
        programStageInstance.setOrganisationUnit( organisationUnit );
        programStageInstance.setAttributeOptionCombo( aoc );
        programStageInstance.setDeleted( event.isDeleted() );

        if ( programStage.getCaptureCoordinates() )
        {
            if ( coordinate != null && coordinate.isValid() )
            {
                programStageInstance.setLongitude( coordinate.getLongitude() );
                programStageInstance.setLatitude( coordinate.getLatitude() );
            }
        }

        updateDateFields( event, programStageInstance );

        programStageInstance.setStatus( EventStatus.fromInt( status ) );

        if ( programStageInstance.getId() == 0 )
        {
            programStageInstance.setAutoFields();
            sessionFactory.getCurrentSession().save( programStageInstance );
        }
        else
        {
            sessionFactory.getCurrentSession().save( programStageInstance );
            sessionFactory.getCurrentSession().flush();
            sessionFactory.getCurrentSession().refresh( programStageInstance );
        }

        if ( programStageInstance.isCompleted() )
        {
            programStageInstance.setStatus( EventStatus.COMPLETED );
            programStageInstance.setCompletedDate( new Date() );
            programStageInstance.setCompletedBy( completedBy );

            programStageInstanceService.completeProgramStageInstance( programStageInstance,
                importOptions.isSkipNotifications(), i18nManager.getI18nFormat() );
        }
    }

    private void saveTrackedEntityComment( ProgramStageInstance programStageInstance, Event event, String storedBy )
    {
        for ( Note note : event.getNotes() )
        {
            TrackedEntityComment comment = new TrackedEntityComment();
            comment.setCreator( storedBy );
            comment.setCreatedDate( new Date() );
            comment.setCommentText( note.getValue() );

            commentService.addTrackedEntityComment( comment );

            programStageInstance.getComments().add( comment );

            programStageInstanceService.updateProgramStageInstance( programStageInstance );
        }
    }

    private String getCompletedBy( Event event, ImportSummary importSummary, User fallbackUser )
    {
        String completedBy = event.getCompletedBy();

        if ( completedBy == null )
        {
            completedBy = User.getSafeUsername( fallbackUser );
        }
        else if ( completedBy.length() >= 31 )
        {
            if ( importSummary != null )
            {
                importSummary.getConflicts().add( new ImportConflict( "completed by",
                    completedBy + " is more than 31 characters, using current username instead" ) );
            }

            completedBy = User.getSafeUsername( fallbackUser );
        }

        return completedBy;
    }

    private String getStoredBy( Event event, ImportSummary importSummary, User fallbackUser )
    {
        String storedBy = event.getStoredBy();

        if ( storedBy == null )
        {
            storedBy = User.getSafeUsername( fallbackUser );
        }
        else if ( storedBy.length() >= 31 )
        {
            if ( importSummary != null )
            {
                importSummary.getConflicts().add( new ImportConflict( "stored by",
                    storedBy + " is more than 31 characters, using current username instead" ) );
            }

            storedBy = User.getSafeUsername( fallbackUser );
        }

        return storedBy;
    }

    private Map<String, TrackedEntityDataValue> getDataElementDataValueMap(
        Collection<TrackedEntityDataValue> dataValues )
    {
        return dataValues.stream().collect( Collectors.toMap( dv -> dv.getDataElement().getUid(), dv -> dv ) );
    }

    private OrganisationUnit getOrganisationUnit( IdSchemes idSchemes, String id )
    {
        return organisationUnitCache.get( id,
            () -> manager.getObject( OrganisationUnit.class, idSchemes.getOrgUnitIdScheme(), id ) );
    }

    private Program getProgram( IdScheme idScheme, String id )
    {
        return programCache.get( id, () -> manager.getObject( Program.class, idScheme, id ) );
    }

    private ProgramStage getProgramStage( IdScheme idScheme, String id )
    {
        return programStageCache.get( id, () -> manager.getObject( ProgramStage.class, idScheme, id ) );
    }

    private DataElement getDataElement( IdScheme idScheme, String id )
    {
        return dataElementCache.get( id, () -> manager.getObject( DataElement.class, idScheme, id ) );
    }

    @Override
    public void validate( EventSearchParams params )
        throws IllegalQueryException
    {
        String violation = null;

        if ( params == null )
        {
            throw new IllegalQueryException( "Query parameters can not be empty." );
        }

        if ( params.getProgram() == null && params.getOrgUnit() == null && params.getTrackedEntityInstance() == null
            && params.getEvents().isEmpty() )
        {
            violation = "At least one of the following query parameters are required: orgUnit, program, trackedEntityInstance or event.";
        }

        if ( violation != null )
        {
            log.warn( "Validation failed: " + violation );

            throw new IllegalQueryException( violation );
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
                        throw new IllegalQueryException( "Event needs to have completed date." );
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
                        throw new IllegalQueryException( "Event needs to have event date." );
                    }

                    Period period = periodType.createPeriod( programStageInstance.getExecutionDate() );

                    if ( today.after( DateUtils.getDateAfterAddition( period.getEndDate(), program.getExpiryDays() ) ) )
                    {
                        throw new IllegalQueryException(
                            "The program's expiry date has passed. It is not possible to make changes to this event." );
                    }
                }
                else
                {
                    String referenceDate = event.getEventDate() != null ? event.getEventDate()
                        : event.getDueDate() != null ? event.getDueDate() : null;

                    if ( referenceDate == null )
                    {
                        throw new IllegalQueryException(
                            "Event needs to have at least one (event or schedule) date. " );
                    }

                    Period period = periodType.createPeriod( new Date() );

                    if ( DateUtils.parseDate( referenceDate ).before( period.getStartDate() ) )
                    {
                        throw new IllegalQueryException(
                            "The event's date belongs to an expired period. It is not possble to create such event." );
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

    private void clearSession()
    {
        organisationUnitCache.clear();
        programCache.clear();
        programStageCache.clear();
        dataElementCache.clear();
        accessibleProgramsCache.clear();

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

    private void updateTrackedEntityInstance( ProgramStageInstance programStageInstance )
    {
        updateTrackedEntityInstance( Lists.newArrayList( programStageInstance ) );
    }

    private void updateTrackedEntityInstance( List<ProgramStageInstance> programStageInstances )
    {
        Set<ProgramInstance> programInstances = new HashSet<>();
        Set<TrackedEntityInstance> trackedEntityInstances = new HashSet<>();

        for ( ProgramStageInstance programStageInstance : programStageInstances )
        {
            if ( programStageInstance.getProgramInstance() != null )
            {
                programInstances.add( programStageInstance.getProgramInstance() );

                if ( programStageInstance.getProgramInstance().getEntityInstance() != null )
                {
                    trackedEntityInstances.add( programStageInstance.getProgramInstance().getEntityInstance() );
                }
            }
        }

        programInstances.forEach( manager::update );
        trackedEntityInstances.forEach( manager::update );
    }
}
