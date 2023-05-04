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
package org.hisp.dhis.dxf2.events.event;

import static java.util.Collections.emptyMap;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;
import static org.hisp.dhis.common.Pager.DEFAULT_PAGE_SIZE;
import static org.hisp.dhis.common.SlimPager.FIRST_PAGE;
import static org.hisp.dhis.dxf2.events.event.EventSearchParams.EVENT_ATTRIBUTE_OPTION_COMBO_ID;
import static org.hisp.dhis.dxf2.events.event.EventSearchParams.EVENT_COMPLETED_BY_ID;
import static org.hisp.dhis.dxf2.events.event.EventSearchParams.EVENT_COMPLETED_DATE_ID;
import static org.hisp.dhis.dxf2.events.event.EventSearchParams.EVENT_CREATED_BY_USER_INFO_ID;
import static org.hisp.dhis.dxf2.events.event.EventSearchParams.EVENT_CREATED_ID;
import static org.hisp.dhis.dxf2.events.event.EventSearchParams.EVENT_DELETED;
import static org.hisp.dhis.dxf2.events.event.EventSearchParams.EVENT_DUE_DATE_ID;
import static org.hisp.dhis.dxf2.events.event.EventSearchParams.EVENT_ENROLLMENT_ID;
import static org.hisp.dhis.dxf2.events.event.EventSearchParams.EVENT_EXECUTION_DATE_ID;
import static org.hisp.dhis.dxf2.events.event.EventSearchParams.EVENT_GEOMETRY;
import static org.hisp.dhis.dxf2.events.event.EventSearchParams.EVENT_ID;
import static org.hisp.dhis.dxf2.events.event.EventSearchParams.EVENT_LAST_UPDATED_BY_USER_INFO_ID;
import static org.hisp.dhis.dxf2.events.event.EventSearchParams.EVENT_LAST_UPDATED_ID;
import static org.hisp.dhis.dxf2.events.event.EventSearchParams.EVENT_ORG_UNIT_ID;
import static org.hisp.dhis.dxf2.events.event.EventSearchParams.EVENT_ORG_UNIT_NAME;
import static org.hisp.dhis.dxf2.events.event.EventSearchParams.EVENT_PROGRAM_ID;
import static org.hisp.dhis.dxf2.events.event.EventSearchParams.EVENT_PROGRAM_STAGE_ID;
import static org.hisp.dhis.dxf2.events.event.EventSearchParams.EVENT_STATUS_ID;
import static org.hisp.dhis.dxf2.events.event.EventSearchParams.EVENT_STORED_BY_ID;
import static org.hisp.dhis.dxf2.events.event.EventSearchParams.PAGER_META_KEY;
import static org.hisp.dhis.system.notification.NotificationLevel.ERROR;
import static org.hisp.dhis.util.DateUtils.getMediumDateString;

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
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.cache.Cache;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.GridHeader;
import org.hisp.dhis.common.IdSchemes;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.common.Pager;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.common.SlimPager;
import org.hisp.dhis.commons.collection.CachingMap;
import org.hisp.dhis.commons.util.DebugUtils;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dbms.DbmsManager;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.events.EventParams;
import org.hisp.dhis.dxf2.events.NoteHelper;
import org.hisp.dhis.dxf2.events.RelationshipParams;
import org.hisp.dhis.dxf2.events.enrollment.EnrollmentStatus;
import org.hisp.dhis.dxf2.events.importer.EventImporter;
import org.hisp.dhis.dxf2.events.importer.EventManager;
import org.hisp.dhis.dxf2.events.importer.context.WorkContext;
import org.hisp.dhis.dxf2.events.importer.context.WorkContextLoader;
import org.hisp.dhis.dxf2.events.relationship.RelationshipService;
import org.hisp.dhis.dxf2.events.report.EventRow;
import org.hisp.dhis.dxf2.events.report.EventRows;
import org.hisp.dhis.dxf2.importsummary.ImportConflicts;
import org.hisp.dhis.dxf2.importsummary.ImportStatus;
import org.hisp.dhis.dxf2.importsummary.ImportSummaries;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.eventdatavalue.EventDataValue;
import org.hisp.dhis.fileresource.FileResourceService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.EnrollmentService;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.program.EventService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramStageDataElement;
import org.hisp.dhis.program.ProgramType;
import org.hisp.dhis.query.QueryService;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.security.Authorities;
import org.hisp.dhis.system.grid.ListGrid;
import org.hisp.dhis.system.notification.NotificationLevel;
import org.hisp.dhis.system.notification.Notifier;
import org.hisp.dhis.system.util.ValidationUtils;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceService;
import org.hisp.dhis.trackedentity.TrackerAccessManager;
import org.hisp.dhis.trackedentity.TrackerOwnershipManager;
import org.hisp.dhis.trackedentitycomment.TrackedEntityComment;
import org.hisp.dhis.trackedentitycomment.TrackedEntityCommentService;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.util.DateUtils;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.collect.Lists;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Slf4j
public abstract class AbstractEventService implements org.hisp.dhis.dxf2.events.event.EventService
{
    public static final List<String> STATIC_EVENT_COLUMNS = Arrays.asList( EVENT_ID, EVENT_ENROLLMENT_ID,
        EVENT_CREATED_ID,
        EVENT_CREATED_BY_USER_INFO_ID, EVENT_LAST_UPDATED_ID, EVENT_LAST_UPDATED_BY_USER_INFO_ID, EVENT_STORED_BY_ID,
        EVENT_COMPLETED_BY_ID,
        EVENT_COMPLETED_DATE_ID, EVENT_EXECUTION_DATE_ID, EVENT_DUE_DATE_ID, EVENT_ORG_UNIT_ID, EVENT_ORG_UNIT_NAME,
        EVENT_STATUS_ID, EVENT_PROGRAM_STAGE_ID, EVENT_PROGRAM_ID, EVENT_ATTRIBUTE_OPTION_COMBO_ID, EVENT_DELETED,
        EVENT_GEOMETRY );

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    protected EventImporter eventImporter;

    protected EventManager eventManager;

    protected WorkContextLoader workContextLoader;

    protected ProgramService programService;

    protected EnrollmentService enrollmentService;

    protected EventService eventService;

    protected OrganisationUnitService organisationUnitService;

    protected CurrentUserService currentUserService;

    protected TrackedEntityInstanceService entityInstanceService;

    protected TrackedEntityCommentService commentService;

    protected EventStore eventStore;

    protected Notifier notifier;

    protected DbmsManager dbmsManager;

    protected IdentifiableObjectManager manager;

    protected CategoryService categoryService;

    protected FileResourceService fileResourceService;

    protected SchemaService schemaService;

    protected QueryService queryService;

    protected TrackerAccessManager trackerAccessManager;

    protected TrackerOwnershipManager trackerOwnershipAccessManager;

    protected RelationshipService relationshipService;

    protected UserService userService;

    protected EventServiceContextBuilder eventServiceContextBuilder;

    protected Cache<Boolean> dataElementCache;

    private static final int FLUSH_FREQUENCY = 100;

    // -------------------------------------------------------------------------
    // Caches
    // -------------------------------------------------------------------------

    private final CachingMap<String, OrganisationUnit> organisationUnitCache = new CachingMap<>();

    private final Set<TrackedEntityInstance> trackedEntityInstancesToUpdate = new HashSet<>();

    // -------------------------------------------------------------------------
    // CREATE
    // -------------------------------------------------------------------------

    @Override
    public ImportSummaries processEventImport( List<org.hisp.dhis.dxf2.events.event.Event> events,
        ImportOptions importOptions,
        JobConfiguration jobConfiguration )
    {
        return eventImporter.importAll( events, importOptions, jobConfiguration );
    }

    @Transactional
    @Override
    public ImportSummaries addEvents( List<org.hisp.dhis.dxf2.events.event.Event> events, ImportOptions importOptions,
        boolean clearSession )
    {
        final WorkContext workContext = workContextLoader.load( importOptions, events );
        return eventManager.addEvents( events, workContext );
    }

    @Transactional
    @Override
    public ImportSummaries addEvents( final List<org.hisp.dhis.dxf2.events.event.Event> events,
        ImportOptions importOptions,
        final JobConfiguration jobConfiguration )
    {
        notifier.clear( jobConfiguration ).notify( jobConfiguration, "Importing events" );
        importOptions = updateImportOptions( importOptions );

        try
        {
            final WorkContext workContext = workContextLoader.load( importOptions, events );

            final ImportSummaries importSummaries = eventManager.addEvents( events, workContext );

            if ( jobConfiguration != null )
            {
                notifier.notify( jobConfiguration, NotificationLevel.INFO, "Import done", true )
                    .addJobSummary( jobConfiguration, importSummaries, ImportSummaries.class );
            }

            return importSummaries;
        }
        catch ( RuntimeException ex )
        {
            log.error( DebugUtils.getStackTrace( ex ) );
            notifier.notify( jobConfiguration, ERROR, "Process failed: " + ex.getMessage(), true );
            return new ImportSummaries().addImportSummary(
                new ImportSummary( ImportStatus.ERROR, "The import process failed: " + ex.getMessage() ) );
        }
    }

    @Transactional
    @Override
    public ImportSummary addEvent( org.hisp.dhis.dxf2.events.event.Event event, ImportOptions importOptions,
        boolean bulkImport )
    {
        final WorkContext workContext = workContextLoader.load( importOptions, Collections.singletonList( event ) );

        return eventManager.addEvent( event, workContext );
    }

    // -------------------------------------------------------------------------
    // READ
    // -------------------------------------------------------------------------

    @Transactional( readOnly = true )
    @Override
    public Events getEvents( EventSearchParams params )
    {
        User user = currentUserService.getCurrentUser();

        validate( params, user );

        if ( !params.isPaging() && !params.isSkipPaging() )
        {
            params.setDefaultPaging();
        }

        Events events = new Events();
        List<org.hisp.dhis.dxf2.events.event.Event> eventList = new ArrayList<>();

        if ( params.isSkipPaging() )
        {
            events.setEvents( eventStore.getEvents( params, emptyMap() ) );
            return events;
        }

        Pager pager;
        eventList.addAll( eventStore.getEvents( params, emptyMap() ) );

        if ( params.isTotalPages() )
        {
            int count = eventStore.getEventCount( params );
            pager = new Pager( params.getPageWithDefault(), count, params.getPageSizeWithDefault() );
        }
        else
        {
            pager = handleLastPageFlag( params, eventList );
        }

        events.setPager( pager );
        events.setEvents( eventList );

        return events;
    }

    /**
     * This method will apply the logic related to the parameter
     * 'totalPages=false'. This works in conjunction with the method:
     * {@link EventStore#getEvents(EventSearchParams,List<OrganisationUnit>,Map<String,Set<String>>)}
     *
     * This is needed because we need to query (pageSize + 1) at DB level. The
     * resulting query will allow us to evaluate if we are in the last page or
     * not. And this is what his method does, returning the respective Pager
     * object.
     *
     * @param params the request params
     * @param eventList the reference to the list of Event
     * @return the populated SlimPager instance
     */
    private Pager handleLastPageFlag( EventSearchParams params, List<org.hisp.dhis.dxf2.events.event.Event> eventList )
    {
        Integer originalPage = defaultIfNull( params.getPage(), FIRST_PAGE );
        Integer originalPageSize = defaultIfNull( params.getPageSize(), DEFAULT_PAGE_SIZE );
        boolean isLastPage = false;

        if ( isNotEmpty( eventList ) )
        {
            isLastPage = eventList.size() <= originalPageSize;
            if ( !isLastPage )
            {
                // Get the same number of elements of the pageSize, forcing
                // the removal of the last additional element added at querying
                // time.
                eventList.retainAll( eventList.subList( 0, originalPageSize ) );
            }
        }

        return new SlimPager( originalPage, originalPageSize, isLastPage );
    }

    @Transactional( readOnly = true )
    @Override
    public Grid getEventsGrid( EventSearchParams params )
    {
        User user = currentUserService.getCurrentUser();

        validate( params, user );

        if ( params.getProgramStage() == null || params.getProgramStage().getProgram() == null )
        {
            throw new IllegalQueryException( "Program stage can not be null" );
        }

        if ( params.getProgramStage().getProgramStageDataElements() == null )
        {
            throw new IllegalQueryException( "Program stage should have at least one data element" );
        }

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

        List<Map<String, String>> events = eventStore.getEventsGrid( params );

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
            final Pager pager;

            if ( params.isTotalPages() )
            {
                int count = eventStore.getEventCount( params );
                pager = new Pager( params.getPageWithDefault(), count, params.getPageSizeWithDefault() );
            }
            else
            {
                pager = handleLastPageFlag( params, grid );
            }

            metaData.put( PAGER_META_KEY, pager );
        }

        grid.setMetaData( metaData );

        return grid;
    }

    /**
     * This method will apply the logic related to the parameter
     * 'totalPages=false'. This works in conjunction with the method:
     * {@link org.hisp.dhis.dxf2.events.event.JdbcEventStore#getEventPagingQuery(EventSearchParams)}
     *
     * This is needed because we need to query (pageSize + 1) at DB level. The
     * resulting query will allow us to evaluate if we are in the last page or
     * not. And this is what his method does, returning the respective Pager
     * object.
     *
     * @param params the request params
     * @param grid the populated Grid object
     * @return the populated SlimPager instance
     */
    private Pager handleLastPageFlag( final EventSearchParams params, final Grid grid )
    {
        final Integer originalPage = defaultIfNull( params.getPage(), FIRST_PAGE );
        final Integer originalPageSize = defaultIfNull( params.getPageSize(), DEFAULT_PAGE_SIZE );
        boolean isLastPage = false;

        if ( isNotEmpty( grid.getRows() ) )
        {
            isLastPage = grid.getRows().size() <= originalPageSize;
            if ( !isLastPage )
            {
                // Get the same number of elements of the pageSize, forcing
                // the removal of the last additional element added at querying
                // time.
                grid.getRows().retainAll( grid.getRows().subList( 0, originalPageSize ) );
            }
        }

        return new SlimPager( originalPage, originalPageSize, isLastPage );
    }

    @Transactional( readOnly = true )
    @Override
    public int getAnonymousEventReadyForSynchronizationCount( Date skipChangedBefore )
    {
        EventSearchParams params = new EventSearchParams().setProgramType( ProgramType.WITHOUT_REGISTRATION )
            .setIncludeDeleted( true ).setSynchronizationQuery( true ).setSkipChangedBefore( skipChangedBefore );

        return eventStore.getEventCount( params );
    }

    @Override
    public Events getAnonymousEventsForSync( int pageSize, Date skipChangedBefore,
        Map<String, Set<String>> psdesWithSkipSyncTrue )
    {
        // A page is not specified here as it would lead to SQLGrammarException
        // after a successful sync of few pages, as total count will change
        // and offset won't be valid.

        EventSearchParams params = new EventSearchParams().setProgramType( ProgramType.WITHOUT_REGISTRATION )
            .setIncludeDeleted( true ).setSynchronizationQuery( true ).setPageSize( pageSize )
            .setSkipChangedBefore( skipChangedBefore );

        Events anonymousEvents = new Events();
        List<org.hisp.dhis.dxf2.events.event.Event> events = eventStore.getEvents( params, psdesWithSkipSyncTrue );
        anonymousEvents.setEvents( events );
        return anonymousEvents;
    }

    @Transactional( readOnly = true )
    @Override
    public EventRows getEventRows( EventSearchParams params )
    {
        User user = currentUserService.getCurrentUser();

        EventRows eventRows = new EventRows();

        List<EventRow> eventRowList = eventStore.getEventRows( params );

        EventContext eventContext = eventServiceContextBuilder.build( eventRowList, user );

        for ( EventRow eventRow : eventRowList )
        {
            if ( trackerOwnershipAccessManager.hasAccessUsingContext( user,
                eventRow.getTrackedEntityInstance(),
                eventRow.getProgram(),
                eventContext ) )
            {
                eventRows.getEventRows().add( eventRow );
            }
        }

        return eventRows;
    }

    @Transactional( readOnly = true )
    @Override
    public org.hisp.dhis.dxf2.events.event.Event getEvent( org.hisp.dhis.program.Event event, EventParams eventParams )
    {
        return getEvent( event, false, false, eventParams );
    }

    @Transactional( readOnly = true )
    @Override
    public org.hisp.dhis.dxf2.events.event.Event getEvent( org.hisp.dhis.program.Event programStageInstance,
        boolean isSynchronizationQuery,
        boolean skipOwnershipCheck, EventParams eventParams )
    {
        if ( programStageInstance == null )
        {
            return null;
        }

        org.hisp.dhis.dxf2.events.event.Event event = new org.hisp.dhis.dxf2.events.event.Event();
        event.setEvent( programStageInstance.getUid() );

        if ( programStageInstance.getEnrollment().getEntityInstance() != null )
        {
            event.setTrackedEntityInstance( programStageInstance.getEnrollment().getEntityInstance().getUid() );
        }

        event.setFollowup( programStageInstance.getEnrollment().getFollowup() );
        event.setEnrollmentStatus(
            EnrollmentStatus.fromProgramStatus( programStageInstance.getEnrollment().getStatus() ) );
        event.setStatus( programStageInstance.getStatus() );
        event.setEventDate( DateUtils.getIso8601NoTz( programStageInstance.getExecutionDate() ) );
        event.setDueDate( DateUtils.getIso8601NoTz( programStageInstance.getDueDate() ) );
        event.setStoredBy( programStageInstance.getStoredBy() );
        event.setCompletedBy( programStageInstance.getCompletedBy() );
        event.setCompletedDate( DateUtils.getIso8601NoTz( programStageInstance.getCompletedDate() ) );
        event.setCreated( DateUtils.getIso8601NoTz( programStageInstance.getCreated() ) );
        event.setCreatedByUserInfo( programStageInstance.getCreatedByUserInfo() );
        event.setLastUpdatedByUserInfo( programStageInstance.getLastUpdatedByUserInfo() );
        event.setCreatedAtClient( DateUtils.getIso8601NoTz( programStageInstance.getCreatedAtClient() ) );
        event.setLastUpdated( DateUtils.getIso8601NoTz( programStageInstance.getLastUpdated() ) );
        event.setLastUpdatedAtClient( DateUtils.getIso8601NoTz( programStageInstance.getLastUpdatedAtClient() ) );
        event.setGeometry( programStageInstance.getGeometry() );
        event.setDeleted( programStageInstance.isDeleted() );

        if ( programStageInstance.getAssignedUser() != null )
        {
            event.setAssignedUser( programStageInstance.getAssignedUser().getUid() );
            event.setAssignedUserUsername( programStageInstance.getAssignedUser().getUsername() );
            event.setAssignedUserDisplayName( programStageInstance.getAssignedUser().getName() );
            event.setAssignedUserFirstName( programStageInstance.getAssignedUser().getFirstName() );
            event.setAssignedUserSurname( programStageInstance.getAssignedUser().getSurname() );
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

        Program program = programStageInstance.getEnrollment().getProgram();

        event.setProgram( program.getUid() );
        event.setEnrollment( programStageInstance.getEnrollment().getUid() );
        event.setProgramStage( programStageInstance.getProgramStage().getUid() );
        CategoryOptionCombo attributeOptionCombo = programStageInstance.getAttributeOptionCombo();
        if ( attributeOptionCombo != null )
        {
            event.setAttributeOptionCombo( attributeOptionCombo.getUid() );
            event.setAttributeCategoryOptions( String.join( ";", attributeOptionCombo
                .getCategoryOptions().stream().map( CategoryOption::getUid ).collect( Collectors.toList() ) ) );
        }
        if ( programStageInstance.getEnrollment().getEntityInstance() != null )
        {
            event
                .setTrackedEntityInstance( programStageInstance.getEnrollment().getEntityInstance().getUid() );
        }

        Collection<EventDataValue> dataValues;
        if ( !isSynchronizationQuery )
        {
            dataValues = programStageInstance.getEventDataValues();
        }
        else
        {
            Set<String> dataElementsToSync = programStageInstance.getProgramStage().getProgramStageDataElements()
                .stream().filter( psde -> !psde.getSkipSynchronization() ).map( psde -> psde.getDataElement().getUid() )
                .collect( Collectors.toSet() );

            dataValues = programStageInstance.getEventDataValues().stream()
                .filter( dv -> dataElementsToSync.contains( dv.getDataElement() ) ).collect( Collectors.toSet() );
        }

        for ( EventDataValue dataValue : dataValues )
        {
            if ( getDataElement( user.getUid(), dataValue.getDataElement() ) )
            {
                DataValue value = new DataValue();
                value.setCreated( DateUtils.getIso8601NoTz( dataValue.getCreated() ) );
                value.setCreatedByUserInfo( dataValue.getCreatedByUserInfo() );
                value.setLastUpdated( DateUtils.getIso8601NoTz( dataValue.getLastUpdated() ) );
                value.setLastUpdatedByUserInfo( dataValue.getLastUpdatedByUserInfo() );
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

        event.getNotes().addAll( NoteHelper.convertNotes( programStageInstance.getComments() ) );

        if ( eventParams.isIncludeRelationships() )
        {
            event.setRelationships( programStageInstance.getRelationshipItems()
                .stream()
                .filter( Objects::nonNull )
                .map( r -> relationshipService.findRelationship( r.getRelationship(), RelationshipParams.FALSE,
                    user ) )
                .filter( Optional::isPresent ).map( Optional::get )
                .collect( Collectors.toSet() ) );
        }

        return event;
    }

    // -------------------------------------------------------------------------
    // UPDATE
    // -------------------------------------------------------------------------

    @Transactional
    @Override
    public ImportSummaries updateEvents( List<org.hisp.dhis.dxf2.events.event.Event> events,
        ImportOptions importOptions, boolean singleValue,
        boolean clearSession )
    {
        ImportSummaries importSummaries = new ImportSummaries();
        importOptions = updateImportOptions( importOptions );
        List<List<org.hisp.dhis.dxf2.events.event.Event>> partitions = Lists.partition( events, FLUSH_FREQUENCY );

        for ( List<org.hisp.dhis.dxf2.events.event.Event> _events : partitions )
        {
            reloadUser( importOptions );
            // prepareCaches( importOptions.getUser(), _events );

            for ( org.hisp.dhis.dxf2.events.event.Event event : _events )
            {
                importSummaries.addImportSummary( updateEvent( event, singleValue, importOptions, true ) );
            }

            if ( clearSession && events.size() >= FLUSH_FREQUENCY )
            {
                // clearSession( importOptions.getUser() );
            }
        }

        updateEntities( importOptions.getUser() );

        return importSummaries;
    }

    @Transactional
    @Override
    public ImportSummary updateEvent( org.hisp.dhis.dxf2.events.event.Event event, boolean singleValue,
        ImportOptions importOptions,
        boolean bulkUpdate )
    {
        ImportOptions localImportOptions = importOptions;

        // API allows null import options
        if ( localImportOptions == null )
        {
            localImportOptions = ImportOptions.getDefaultImportOptions();
        }
        // TODO this doesn't make a lot of sense, but I didn't want to change
        // the EventService interface and preserve the "singleValue" flag
        localImportOptions.setMergeDataValues( singleValue );

        return eventManager.updateEvent( event,
            workContextLoader.load( localImportOptions, Collections.singletonList( event ) ) );
    }

    @Transactional
    @Override
    public void updateEventForNote( org.hisp.dhis.dxf2.events.event.Event event )
    {
        org.hisp.dhis.program.Event programStageInstance = eventService
            .getEvent( event.getEvent() );

        if ( programStageInstance == null )
        {
            return;
        }

        User currentUser = currentUserService.getCurrentUser();

        saveTrackedEntityComment( programStageInstance, event, currentUser, getValidUsername( event.getStoredBy(), null,
            currentUser != null ? currentUser.getUsername() : "[Unknown]" ) );

        updateTrackedEntityInstance( programStageInstance, currentUser, false );
    }

    @Transactional
    @Override
    public void updateEventForEventDate( org.hisp.dhis.dxf2.events.event.Event event )
    {
        Event programStageInstance = eventService
            .getEvent( event.getEvent() );

        if ( programStageInstance == null )
        {
            return;
        }

        List<String> errors = trackerAccessManager.canUpdate( currentUserService.getCurrentUser(), programStageInstance,
            false );

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
        eventService.updateEvent( programStageInstance );
    }

    @Transactional
    @Override
    public void updateEventsSyncTimestamp( List<String> eventsUIDs, Date lastSynchronized )
    {
        eventService.updateEventsSyncTimestamp( eventsUIDs, lastSynchronized );
    }

    // -------------------------------------------------------------------------
    // DELETE
    // -------------------------------------------------------------------------

    @Transactional
    @Override
    public ImportSummary deleteEvent( String uid )
    {
        boolean existsEvent = eventService.eventExists( uid );

        if ( existsEvent )
        {
            Event event = eventService.getEvent( uid );

            List<String> errors = trackerAccessManager.canDelete( currentUserService.getCurrentUser(),
                event, false );

            if ( !errors.isEmpty() )
            {
                return new ImportSummary( ImportStatus.ERROR, errors.toString() ).incrementIgnored();
            }

            event.setAutoFields();
            eventService.deleteEvent( event );

            if ( event.getProgramStage().getProgram().isRegistration() )
            {
                entityInstanceService
                    .updateTrackedEntityInstance( event.getEnrollment().getEntityInstance() );
            }

            ImportSummary importSummary = new ImportSummary( ImportStatus.SUCCESS,
                "Deletion of event " + uid + " was successful" ).incrementDeleted();
            importSummary.setReference( uid );
            return importSummary;
        }
        else
        {
            return new ImportSummary( ImportStatus.SUCCESS,
                "Event " + uid + " cannot be deleted as it is not present in the system" ).incrementIgnored();
        }
    }

    @Transactional
    @Override
    public ImportSummaries deleteEvents( List<String> uids, boolean clearSession )
    {
        ImportSummaries importSummaries = new ImportSummaries();
        for ( String uid : uids )
        {
            importSummaries.addImportSummary( deleteEvent( uid ) );
        }

        return importSummaries;
    }

    // -------------------------------------------------------------------------
    // HELPERS
    // -------------------------------------------------------------------------
    private void saveTrackedEntityComment( org.hisp.dhis.program.Event programStageInstance,
        org.hisp.dhis.dxf2.events.event.Event event, User user,
        String storedBy )
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

                comment.setLastUpdatedBy( user );
                comment.setLastUpdated( new Date() );

                commentService.addTrackedEntityComment( comment );

                programStageInstance.getComments().add( comment );
            }
        }
    }

    public static String getValidUsername( String userName, ImportConflicts importConflicts, String fallbackUsername )
    {
        String validUsername = userName;

        if ( StringUtils.isEmpty( validUsername ) )
        {
            validUsername = User.getSafeUsername( fallbackUsername );
        }
        else if ( !ValidationUtils.usernameIsValid( userName, false ) )
        {
            if ( importConflicts != null )
            {
                importConflicts.addConflict( "Username", validUsername + " is more than "
                    + User.USERNAME_MAX_LENGTH + " characters, using current username instead" );
            }

            validUsername = User.getSafeUsername( fallbackUsername );
        }

        return validUsername;
    }

    private OrganisationUnit getOrganisationUnit( IdSchemes idSchemes, String id )
    {
        return organisationUnitCache.get( id,
            () -> manager.getObject( OrganisationUnit.class, idSchemes.getOrgUnitIdScheme(), id ) );
    }

    /**
     * Get DataElement by given uid
     *
     * @return FALSE if currentUser doesn't have READ access to given
     *         DataElement OR no DataElement with given uid exist TRUE if
     *         DataElement exist and currentUser has READ access
     */
    private boolean getDataElement( String userUid, String dataElementUid )
    {
        String key = userUid + "-" + dataElementUid;
        return dataElementCache.get( key, k -> manager.get( DataElement.class, dataElementUid ) != null );
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
            violation = getAccessibleScopeValidation( user, params );
            break;
        case CAPTURE:
            violation = getCaptureScopeValidation( user );
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

    private String getCaptureScopeValidation( User user )
    {
        String violation = null;

        if ( user == null )
        {
            violation = "User is required for ouMode: " + OrganisationUnitSelectionMode.CAPTURE;
        }
        else if ( user.getOrganisationUnits().isEmpty() )
        {
            violation = "User needs to be assigned data capture orgunits";
        }

        return violation;
    }

    private String getAccessibleScopeValidation( User user, EventSearchParams params )
    {
        String violation = null;

        if ( user == null )
        {
            return "User is required for ouMode: " + OrganisationUnitSelectionMode.ACCESSIBLE;
        }

        if ( params.getProgram() == null || params.getProgram().isClosed() || params.getProgram().isProtected() )
        {
            violation = user.getOrganisationUnits().isEmpty() ? "User needs to be assigned data capture orgunits"
                : null;
        }
        else
        {
            violation = user.getTeiSearchOrganisationUnitsWithFallback().isEmpty()
                ? "User needs to be assigned either TEI search, data view or data capture org units"
                : null;
        }

        return violation;
    }

    /**
     * TODO this method duplicates the functionality of
     * AttributeOptionComboDateCheck Remove when refactoring
     * AbstractEventService
     */
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
                throw new IllegalQueryException( "Event date " + getMediumDateString( date ) + " is before start date "
                    + getMediumDateString( option.getStartDate() ) + " for attributeOption '" + option.getName()
                    + "'" );
            }

            if ( option.getEndDate() != null && date.compareTo( option.getEndDate() ) > 0 )
            {
                throw new IllegalQueryException( "Event date " + getMediumDateString( date ) + " is after end date "
                    + getMediumDateString( option.getEndDate() ) + " for attributeOption '" + option.getName() + "'" );
            }
        }
    }

    private void updateEntities( User user )
    {
        trackedEntityInstancesToUpdate.forEach( tei -> manager.update( tei, user ) );
        trackedEntityInstancesToUpdate.clear();
    }

    private void updateTrackedEntityInstance( Event event, User user, boolean bulkUpdate )
    {
        updateTrackedEntityInstance( Lists.newArrayList( event ), user, bulkUpdate );
    }

    private void updateTrackedEntityInstance( List<Event> events, User user,
        boolean bulkUpdate )
    {
        for ( org.hisp.dhis.program.Event event : events )
        {
            if ( event.getEnrollment() != null )
            {
                if ( !bulkUpdate )
                {
                    if ( event.getEnrollment().getEntityInstance() != null )
                    {
                        manager.update( event.getEnrollment().getEntityInstance(), user );
                    }
                }
                else
                {
                    if ( event.getEnrollment().getEntityInstance() != null )
                    {
                        trackedEntityInstancesToUpdate
                            .add( event.getEnrollment().getEntityInstance() );
                    }
                }
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
