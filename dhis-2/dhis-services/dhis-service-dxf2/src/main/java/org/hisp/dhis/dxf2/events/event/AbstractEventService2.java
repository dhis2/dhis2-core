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

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.AssignedUserSelectionMode;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.common.IdSchemes;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.events.event.mapper.ProgramStageInstanceMapper;
import org.hisp.dhis.dxf2.events.event.preProcess.PreProcessorFactory;
import org.hisp.dhis.dxf2.events.event.validation.ValidationContext;
import org.hisp.dhis.dxf2.events.event.validation.ValidationFactory;
import org.hisp.dhis.dxf2.events.report.EventRows;
import org.hisp.dhis.dxf2.importsummary.ImportConflict;
import org.hisp.dhis.dxf2.importsummary.ImportStatus;
import org.hisp.dhis.dxf2.importsummary.ImportSummaries;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.hisp.dhis.dxf2.metadata.feedback.ImportReportMode;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.organisationunit.FeatureType;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.program.ProgramStatus;
import org.hisp.dhis.programrule.engine.DataValueUpdatedEvent;
import org.hisp.dhis.query.Order;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.system.notification.NotificationLevel;
import org.hisp.dhis.system.notification.Notifier;
import org.hisp.dhis.system.util.Clock;
import org.hisp.dhis.system.util.GeoUtils;
import org.hisp.dhis.user.UserCredentials;
import org.hisp.dhis.util.DateUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.google.common.collect.Lists;

import lombok.extern.slf4j.Slf4j;

/**
 * @author Luciano Fiandesio
 */
@Slf4j
public abstract class AbstractEventService2
    implements
    EventService
{
    protected Notifier notifier;

    protected ValidationFactory validationFactory;
    protected PreProcessorFactory preProcessorFactory;

    protected JdbcEventStore jdbcEventStore;

    private static final int BATCH_SIZE = 100;

    @Transactional
    @Override
    public ImportSummaries processEventImport( List<Event> events, ImportOptions importOptions, JobConfiguration jobId )
    {
        assert importOptions != null;
        assert importOptions.getUser() != null;

        ImportSummaries importSummaries = new ImportSummaries();

        notifier.clear( jobId ).notify( jobId, "Importing events" );
        Clock clock = new Clock( log ).startClock();

        List<Event> eventsWithUid = new UidGenerator().assignUidToEvents( events );

        long now = System.nanoTime();
        ValidationContext validationContext = validationFactory.getContext( importOptions, eventsWithUid );
        log.debug( "::: validation context load took : " + (System.nanoTime() - now) );

        List<List<Event>> partitions = Lists.partition( eventsWithUid, BATCH_SIZE );

        for ( List<Event> batch : partitions )
        {
            ImportStrategyAccumulator accumulator = new ImportStrategyAccumulator().partitionEvents( batch,
                importOptions.getImportStrategy(), validationContext.getProgramStageInstanceMap() );

            importSummaries
                .addImportSummaries( addEvents( accumulator.getCreate(), importOptions, validationContext ) );
            // -- old code -- : -- please refactor --
            // importSummaries.addImportSummaries( updateEvents( accumulator.getUpdate(),
            // importOptions, false, true ) );
            // importSummaries.addImportSummaries( deleteEvents( accumulator.getDelete(),
            // true ) );
        }

        if ( jobId != null )
        {
            notifier.notify( jobId, NotificationLevel.INFO, "Import done. Completed in " + clock.time() + ".", true )
                .addJobSummary( jobId, importSummaries, ImportSummaries.class );
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
    public ImportSummaries addEvents( List<Event> events, ImportOptions importOptions, ValidationContext ctx )
    {
        ImportSummaries importSummaries = new ImportSummaries();

        // filter out events which are already in the database
        List<Event> validEvents = resolveImportableEvents( events, importSummaries, ctx );

        // pre-process events
        preProcessorFactory.preProcessEvents( ctx, events );
        
        // @formatter:off
        importSummaries.addImportSummaries(
            // Run validation against the remaining "insertable" events //
            validationFactory.validateEvents( ctx, validEvents )
        );
        // @formatter:on

        // collect the UIDs of events that did not pass validation
        List<String> failedUids = importSummaries.getImportSummaries().stream()
            .filter( i -> i.isStatus( ImportStatus.ERROR ) ).map( ImportSummary::getReference )
            .collect( Collectors.toList() );

        ProgramStageInstanceMapper mapper = new ProgramStageInstanceMapper( ctx );

        List<ProgramStageInstance> eventsToPersist;
        if ( failedUids.isEmpty() )
        {
            eventsToPersist = convertToProgramStageInstances( mapper, validEvents );
        }
        else
        {
            // collect the events that passed validation and can be persisted
            // @formatter:off
            eventsToPersist = convertToProgramStageInstances( mapper, validEvents.stream()
                .filter( e -> !failedUids.contains( e.getEvent() ) )
                .collect( Collectors.toList() ) );
            // @formatter:on
        }

        if ( !eventsToPersist.isEmpty() )
        {
            jdbcEventStore.saveEvents( eventsToPersist );
        }

        return importSummaries;
    }

    private List<ProgramStageInstance> convertToProgramStageInstances( ProgramStageInstanceMapper mapper,
        List<Event> events )
    {
        return events.stream().map( mapper::convert ).collect( Collectors.toList() );
    }

    @Override
    public Events getEvents( EventSearchParams params )
    {
        return null;
    }

    @Override
    public EventRows getEventRows( EventSearchParams params )
    {
        return null;
    }

    @Override
    public EventSearchParams getFromUrl( String program, String programStage, ProgramStatus programStatus,
        Boolean followUp, String orgUnit, OrganisationUnitSelectionMode orgUnitSelectionMode,
        String trackedEntityInstance, Date startDate, Date endDate, Date dueDateStart, Date dueDateEnd,
        Date lastUpdatedStartDate, Date lastUpdatedEndDate, String lastUpdatedDuration, EventStatus status,
        CategoryOptionCombo attributeCoc, IdSchemes idSchemes, Integer page, Integer pageSize, boolean totalPages,
        boolean skipPaging, List<Order> orders, List<String> gridOrders, boolean includeAttributes, Set<String> events,
        Boolean skipEventId, AssignedUserSelectionMode assignedUserMode, Set<String> assignedUserIds,
        Set<String> filters, Set<String> dataElements, boolean includeAllDataElements, boolean includeDeleted )
    {
        return null;
    }

    @Override
    public Event getEvent( ProgramStageInstance programStageInstance )
    {
        return null;
    }

    @Override
    public Event getEvent( ProgramStageInstance programStageInstance, boolean isSynchronizationQuery,
        boolean skipOwnershipCheck )
    {
        return null;
    }

    @Override
    public Grid getEventsGrid( EventSearchParams params )
    {
        return null;
    }

    @Override
    public int getAnonymousEventReadyForSynchronizationCount( Date skipChangedBefore )
    {
        return 0;
    }

    @Override
    public Events getAnonymousEventsForSync( int pageSize, Date skipChangedBefore,
        Map<String, Set<String>> psdesWithSkipSyncTrue )
    {
        return null;
    }

    @Override
    public ImportSummary addEvent( Event event, ImportOptions importOptions, boolean bulkImport )
    {
        return null;
    }

    @Override
    public ImportSummaries addEvents( List<Event> events, ImportOptions importOptions, boolean clearSession )
    {
        return null;
    }

    @Override
    public ImportSummaries addEvents( List<Event> events, ImportOptions importOptions, JobConfiguration jobId )
    {
        return null;
    }

    @Override
    public ImportSummary updateEvent( Event event, boolean singleValue, boolean bulkUpdate )
    {
        return null;
    }

    @Override
    public ImportSummary updateEvent( Event event, boolean singleValue, ImportOptions importOptions,
        boolean bulkUpdate )
    {
        importOptions = updateImportOptions( importOptions );

// FIXME: Respective checker is ==> update.EventBasicCheck
//
//        if ( event == null || StringUtils.isEmpty( event.getEvent() ) )
//        {
//            return new ImportSummary( ImportStatus.ERROR, "No event or event ID was supplied" ).incrementIgnored();
//        }

// FIXME: Respective checker is ==> update.ProgramStageInstanceBasicCheck
//
//        ImportSummary importSummary = new ImportSummary( event.getEvent() );
//        ProgramStageInstance programStageInstance = getProgramStageInstance( event.getEvent() );
//
//        if ( programStageInstance == null )
//        {
//            importSummary.setStatus( ImportStatus.ERROR );
//            importSummary.setDescription( "ID " + event.getEvent() + " doesn't point to valid event" );
//            importSummary.getConflicts().add( new ImportConflict( "Invalid Event ID", event.getEvent() ) );
//
//            return importSummary.incrementIgnored();
//        }
//
//        if ( programStageInstance != null && (programStageInstance.isDeleted() || importOptions.getImportStrategy().isCreate()) )
//        {
//            return new ImportSummary( ImportStatus.ERROR, "Event ID " + event.getEvent() + " was already used and/or deleted. This event can not be modified." )
//                .setReference( event.getEvent() ).incrementIgnored();
//        }

// FIXME: Respective checker is ==> update.ProgramStageInstanceAclCheck
//
//        List<String> errors = trackerAccessManager.canUpdate( importOptions.getUser(), programStageInstance, false );
//
//        if ( !errors.isEmpty() )
//        {
//            return new ImportSummary( ImportStatus.ERROR, errors.toString() ).incrementIgnored();
//        }

        OrganisationUnit organisationUnit = getOrganisationUnit( importOptions.getIdSchemes(), event.getOrgUnit() );

        if ( organisationUnit == null )
        {
            organisationUnit = programStageInstance.getOrganisationUnit();
        }

// FIXME: Respective checker is ==> update.ProgramCheck
// TODO: Check the error message with Luciano, maybe the root ProgramCheck can be reused.
//
//        Program program = getProgram( importOptions.getIdSchemes().getProgramIdScheme(), event.getProgram() );
//
//        if ( program == null )
//        {
//            return new ImportSummary( ImportStatus.ERROR, "Program '" + event.getProgram() + "' for event '" + event.getEvent() + "' was not found." );
//        }

// FIXME: Respective checker is ==> root.EventBaseCheck
//
//        errors = validateEvent( event, programStageInstance.getProgramInstance(), importOptions );
//
//        if ( !errors.isEmpty() )
//        {
//            importSummary.setStatus( ImportStatus.ERROR );
//            importSummary.getConflicts().addAll( errors.stream().map( s -> new ImportConflict( "Event", s ) ).collect( Collectors.toList() ) );
//            importSummary.incrementIgnored();
//
//            return importSummary;
//        }

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

        String storedBy = getValidUsername( event.getStoredBy(), null, importOptions.getUser() != null ? importOptions.getUser().getUsername() : "[Unknown]" );
        programStageInstance.setStoredBy( storedBy );

        String completedBy = getValidUsername( event.getCompletedBy(), null, importOptions.getUser() != null ? importOptions.getUser().getUsername() : "[Unknown]" );

// FIXME: Respective checker is ==> update.ProgramStageInstanceAuthCheck
//
//        if ( event.getStatus() != programStageInstance.getStatus()
//            && programStageInstance.getStatus() == EventStatus.COMPLETED )
//        {
//            UserCredentials userCredentials = importOptions.getUser().getUserCredentials();
//
//            if ( !userCredentials.isSuper() && !userCredentials.isAuthorized( "F_UNCOMPLETE_EVENT" ) )
//            {
//                importSummary.setStatus( ImportStatus.ERROR );
//                importSummary.setDescription( "User is not authorized to uncomplete events" );
//
//                return importSummary;
//            }
//        }

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

// TODO: How to validate this piece? Is it being ignored in eventAdd?
//
//            try
//            {
//                aoc = getAttributeOptionCombo( program.getCategoryCombo(),
//                    event.getAttributeCategoryOptions(), event.getAttributeOptionCombo(), idScheme );
//            }
//            catch ( IllegalQueryException ex )
//            {
//                importSummary.setStatus( ImportStatus.ERROR );
//                importSummary.getConflicts().add( new ImportConflict( ex.getMessage(), event.getAttributeCategoryOptions() ) );
//                return importSummary.incrementIgnored();
//            }
        }

// FIXME: Respective checker is ==> root.AttributeOptionComboCheck
//
//        if ( aoc != null && aoc.isDefault() && program.getCategoryCombo() != null && !program.getCategoryCombo().isDefault() )
//        {
//            importSummary.setStatus( ImportStatus.ERROR );
//            importSummary.getConflicts().add( new ImportConflict( "attributeOptionCombo", "Default attribute option combo is not allowed since program has non-default category combo" ) );
//            return importSummary.incrementIgnored();
//        }

        if ( aoc != null )
        {
            programStageInstance.setAttributeOptionCombo( aoc );
        }

        Date eventDate = programStageInstance.getExecutionDate() != null ? programStageInstance.getExecutionDate() : programStageInstance.getDueDate();

// TODO: Maikel, should it become a Checker? At the moment it only throws Exception
        validateAttributeOptionComboDate( aoc, eventDate );

//        if ( event.getGeometry() != null )
//        {
//            if ( programStageInstance.getProgramStage().getFeatureType().equals( FeatureType.NONE ) ||
//                !programStageInstance.getProgramStage().getFeatureType().value().equals( event.getGeometry().getGeometryType() ) )
//            {
//                return new ImportSummary( ImportStatus.ERROR, String.format(
//                    "Geometry '%s' does not conform to the feature type '%s' specified for the program stage: '%s'",
//                    programStageInstance.getProgramStage().getUid(), event.getGeometry().getGeometryType(), programStageInstance.getProgramStage().getFeatureType().value() ) )
//                    .setReference( event.getEvent() ).incrementIgnored();
//            }

// TODO: luciano -> this is a side effect and should take place after validation
            event.getGeometry().setSRID( GeoUtils.SRID );
//        }
//        else if ( event.getCoordinate() != null && event.getCoordinate().hasLatitudeLongitude() )
//        {
//            Coordinate coordinate = event.getCoordinate();
//
//            try
//            {
//                event.setGeometry( GeoUtils.getGeoJsonPoint( coordinate.getLongitude(), coordinate.getLatitude() ) );
//            }
//            catch ( IOException e )
//            {
//                return new ImportSummary( ImportStatus.ERROR,
//                    "Invalid longitude or latitude for property 'coordinates'." );
//            }
//        }

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

    @Override
    public ImportSummaries updateEvents( List<Event> events, ImportOptions importOptions, boolean singleValue,
        boolean clearSession )
    {
        return null;
    }

    @Override
    public void updateEventForNote( Event event )
    {

    }

    @Override
    public void updateEventForEventDate( Event event )
    {

    }

    @Override
    public void updateEventsSyncTimestamp( List<String> eventsUIDs, Date lastSynchronized )
    {

    }

    @Override
    public ImportSummary deleteEvent( String uid )
    {
        return null;
    }

    @Override
    public ImportSummaries deleteEvents( List<String> uids, boolean clearSession )
    {
        return null;
    }

    @Override
    public void validate( EventSearchParams params )
    {

    }

    /**
     * Filters out Events which are already present in the database (regardless of
     * the 'deleted' state)
     *
     * @param events Events to import
     * @param importSummaries ImportSummaries used for import
     * @return Events that is possible to import (pass validation)
     */
    private List<Event> resolveImportableEvents( List<Event> events, ImportSummaries importSummaries,
        ValidationContext ctx )
    {
        Map<String, ProgramStageInstance> programStageInstanceMap = ctx.getProgramStageInstanceMap();
        for ( String foundEventUid : programStageInstanceMap.keySet() )
        {
            ImportSummary is = new ImportSummary( ImportStatus.ERROR,
                "Event " + foundEventUid + " already exists or was deleted earlier" ).setReference( foundEventUid )
                    .incrementIgnored();

            importSummaries.addImportSummary( is );
        }

        return events.stream().filter( e -> !programStageInstanceMap.containsKey( e.getEvent() ) )
            .collect( Collectors.toList() );
    }

    // FIXME: temporary - remove after refactoring ...
    protected ImportOptions updateImportOptions( ImportOptions importOptions )
    {
        if ( importOptions == null )
        {
            importOptions = new ImportOptions();
        }

        // if ( importOptions.getUser() == null )
        // {
        // importOptions.setUser( currentUserService.getCurrentUser() );
        // }

        return importOptions;
    }
}
