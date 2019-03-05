package org.hisp.dhis.tracker.converter;

/*
 * Copyright (c) 2004-2019, University of Oslo
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

import org.hisp.dhis.api.util.DateUtils;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dxf2.events.enrollment.EnrollmentStatus;
import org.hisp.dhis.dxf2.events.event.Coordinate;
import org.hisp.dhis.dxf2.events.event.DataValue;
import org.hisp.dhis.dxf2.events.event.Event;
import org.hisp.dhis.eventdatavalue.EventDataValue;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.program.ProgramType;
import org.hisp.dhis.tracker.TrackerIdentifier;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.preheat.TrackerPreheatParams;
import org.hisp.dhis.tracker.preheat.TrackerPreheatService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Service
@Transactional
public class EventTrackerConverterService
    implements TrackerConverterService<Event, ProgramStageInstance>
{
    private final TrackerPreheatService trackerPreheatService;
    private final IdentifiableObjectManager manager;

    public EventTrackerConverterService(
        TrackerPreheatService trackerPreheatService,
        IdentifiableObjectManager manager )
    {
        this.trackerPreheatService = trackerPreheatService;
        this.manager = manager;
    }

    @Override
    public Event to( ProgramStageInstance programStageInstance )
    {
        List<Event> events = to( Collections.singletonList( programStageInstance ) );

        if ( events.isEmpty() )
        {
            return null;
        }

        return events.get( 0 );
    }

    @Override
    public List<Event> to( List<ProgramStageInstance> programStageInstances )
    {
        return null;
    }

    @Override
    public List<Event> to( TrackerPreheat preheat, List<ProgramStageInstance> programStageInstances )
    {
        List<Event> events = new ArrayList<>();

        programStageInstances.forEach( psi -> {
            Event event = new Event();
            event.setEvent( psi.getUid() );

            if ( psi.getProgramInstance().getEntityInstance() != null )
            {
                event.setTrackedEntityInstance( psi.getProgramInstance().getEntityInstance().getUid() );
            }

            event.setFollowup( psi.getProgramInstance().getFollowup() );
            event.setEnrollmentStatus( EnrollmentStatus.fromProgramStatus( psi.getProgramInstance().getStatus() ) );
            event.setStatus( psi.getStatus() );
            event.setEventDate( DateUtils.getIso8601NoTz( psi.getExecutionDate() ) );
            event.setDueDate( DateUtils.getIso8601NoTz( psi.getDueDate() ) );
            event.setStoredBy( psi.getStoredBy() );
            event.setCompletedBy( psi.getCompletedBy() );
            event.setCompletedDate( DateUtils.getIso8601NoTz( psi.getCompletedDate() ) );
            event.setCreated( DateUtils.getIso8601NoTz( psi.getCreated() ) );
            event.setCreatedAtClient( DateUtils.getIso8601NoTz( psi.getCreatedAtClient() ) );
            event.setLastUpdated( DateUtils.getIso8601NoTz( psi.getLastUpdated() ) );
            event.setLastUpdatedAtClient( DateUtils.getIso8601NoTz( psi.getLastUpdatedAtClient() ) );
            event.setGeometry( psi.getGeometry() );
            event.setDeleted( psi.isDeleted() );

            // Lat and lnt deprecated in 2.30, remove by 2.33
            if ( event.getGeometry() != null && event.getGeometry().getGeometryType().equals( "Point" ) )
            {
                com.vividsolutions.jts.geom.Coordinate geometryCoordinate = event.getGeometry().getCoordinate();
                event.setCoordinate( new Coordinate( geometryCoordinate.x, geometryCoordinate.y ) );
            }

            OrganisationUnit ou = psi.getOrganisationUnit();

            if ( ou != null )
            {
                event.setOrgUnit( ou.getUid() );
                event.setOrgUnitName( ou.getName() );
            }

            Program program = psi.getProgramInstance().getProgram();

            event.setProgram( program.getUid() );
            event.setEnrollment( psi.getProgramInstance().getUid() );
            event.setProgramStage( psi.getProgramStage().getUid() );
            event.setAttributeOptionCombo( psi.getAttributeOptionCombo().getUid() );
            event.setAttributeCategoryOptions( psi.getAttributeOptionCombo()
                .getCategoryOptions().stream().map( CategoryOption::getUid ).collect( Collectors.joining( ";" ) ) );

            Set<EventDataValue> dataValues = psi.getEventDataValues();

            for ( EventDataValue dataValue : dataValues )
            {
                DataValue value = new DataValue();
                value.setCreated( DateUtils.getIso8601NoTz( dataValue.getCreated() ) );
                value.setLastUpdated( DateUtils.getIso8601NoTz( dataValue.getLastUpdated() ) );
                value.setDataElement( dataValue.getDataElement() );
                value.setValue( dataValue.getValue() );
                value.setProvidedElsewhere( dataValue.getProvidedElsewhere() );
                value.setStoredBy( dataValue.getStoredBy() );

                event.getDataValues().add( value );
            }

            events.add( event );
        } );

        return events;
    }

    @Override
    public ProgramStageInstance from( Event event )
    {
        List<ProgramStageInstance> programStageInstances = from( Collections.singletonList( event ) );

        if ( programStageInstances.isEmpty() )
        {
            return null;
        }

        return programStageInstances.get( 0 );
    }

    @Override
    public List<ProgramStageInstance> from( List<Event> events )
    {
        return from( preheat( events ), events );
    }

    @Override
    public List<ProgramStageInstance> from( TrackerPreheat preheat, List<Event> events )
    {
        List<ProgramStageInstance> programStageInstances = new ArrayList<>();

        events.forEach( e -> {
            ProgramStageInstance programStageInstance = preheat.getEvent( TrackerIdentifier.UID, e.getEvent() );
            ProgramStage programStage = preheat.get( TrackerIdentifier.UID, ProgramStage.class, e.getProgramStage() );
            OrganisationUnit organisationUnit = preheat.get( TrackerIdentifier.UID, OrganisationUnit.class, e.getOrgUnit() );

            if ( programStageInstance == null )
            {
                programStageInstance = new ProgramStageInstance();
                programStageInstance.setProgramInstance( getProgramInstance( preheat, TrackerIdentifier.UID, e.getEnrollment(), programStage.getProgram() ) );
            }

            programStageInstance.setProgramStage( programStage );
            programStageInstance.setOrganisationUnit( organisationUnit );
            programStageInstance.setExecutionDate( DateUtils.parseDate( e.getEventDate() ) );
            programStageInstance.setDueDate( DateUtils.parseDate( e.getDueDate() ) );
            // programStageInstance.setAttributeOptionCombo(  ); TODO
            programStageInstance.setGeometry( e.getGeometry() );
            programStageInstance.setStatus( e.getStatus() );
            programStageInstance.setCreatedAtClient( DateUtils.parseDate( e.getCreatedAtClient() ) );
            programStageInstance.setLastUpdatedAtClient( DateUtils.parseDate( e.getLastUpdatedAtClient() ) );

            if ( programStageInstance.isCompleted() )
            {
                Date completedDate = DateUtils.parseDate( e.getCompletedDate() );

                if ( completedDate == null )
                {
                    completedDate = new Date();
                }

                programStageInstance.setCompletedDate( completedDate );
                programStageInstance.setCompletedBy( e.getCompletedBy() );
            }

            programStageInstances.add( programStageInstance );
        } );

        return programStageInstances;
    }

    private ProgramInstance getProgramInstance( TrackerPreheat preheat, TrackerIdentifier identifier, String enrollment, Program program )
    {
        if ( !StringUtils.isEmpty( enrollment ) )
        {
            return preheat.getEnrollment( identifier, enrollment );
        }

        if ( ProgramType.WITHOUT_REGISTRATION == program.getProgramType() )
        {
            return preheat.getEnrollment( identifier, program.getUid() );
        }

        // no valid enrollment given and program not single event, just return null
        return null;
    }

    private TrackerPreheat preheat( List<Event> events )
    {
        TrackerPreheatParams params = new TrackerPreheatParams()
            .setEvents( events );

        return trackerPreheatService.preheat( params );
    }
}
