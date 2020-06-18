package org.hisp.dhis.tracker.converter;

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

import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.eventdatavalue.EventDataValue;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.program.ProgramType;
import org.hisp.dhis.tracker.TrackerIdScheme;
import org.hisp.dhis.tracker.domain.DataValue;
import org.hisp.dhis.tracker.domain.EnrollmentStatus;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.preheat.TrackerPreheatParams;
import org.hisp.dhis.tracker.preheat.TrackerPreheatService;
import org.hisp.dhis.util.DateUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.api.client.util.Preconditions.checkNotNull;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Service
public class EventTrackerConverterService
    implements TrackerConverterService<Event, ProgramStageInstance>
{

    private final NotesConverterService notesConverterService;

    public EventTrackerConverterService( NotesConverterService notesConverterService )
    {
        checkNotNull( notesConverterService );

        this.notesConverterService = notesConverterService;
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
        List<Event> events = new ArrayList<>();

        programStageInstances.forEach( psi -> {
            Event event = new Event();
            event.setEvent( psi.getUid() );

            if ( psi.getProgramInstance().getEntityInstance() != null )
            {
                event.setTrackedEntity( psi.getProgramInstance().getEntityInstance().getUid() );
            }

            event.setFollowUp( psi.getProgramInstance().getFollowup() );
            event.setEnrollmentStatus( EnrollmentStatus.fromProgramStatus( psi.getProgramInstance().getStatus() ) );
            event.setStatus( psi.getStatus() );
            event.setOccurredAt( DateUtils.getIso8601NoTz( psi.getExecutionDate() ) );
            event.setScheduledAt( DateUtils.getIso8601NoTz( psi.getDueDate() ) );
            event.setStoredBy( psi.getStoredBy() );
            event.setCompletedBy( psi.getCompletedBy() );
            event.setCompletedAt( DateUtils.getIso8601NoTz( psi.getCompletedDate() ) );
            event.setCreatedAt( DateUtils.getIso8601NoTz( psi.getCreated() ) );
            event.setUpdatedAt( DateUtils.getIso8601NoTz( psi.getLastUpdated() ) );
            event.setGeometry( psi.getGeometry() );
            event.setDeleted( psi.isDeleted() );

            OrganisationUnit ou = psi.getOrganisationUnit();

            if ( ou != null )
            {
                event.setOrgUnit( ou.getUid() );
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
                value.setCreatedAt( DateUtils.getIso8601NoTz( dataValue.getCreated() ) );
                value.setUpdatedAt( DateUtils.getIso8601NoTz( dataValue.getLastUpdated() ) );
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
    public ProgramStageInstance from( TrackerPreheat preheat, Event event )
    {
        List<ProgramStageInstance> programStageInstances = from( preheat, Collections.singletonList( event ) );

        if ( programStageInstances.isEmpty() )
        {
            return null;
        }

        return programStageInstances.get( 0 );
    }

    @Override
    public List<ProgramStageInstance> from( TrackerPreheat preheat, List<Event> events )
    {
        List<ProgramStageInstance> programStageInstances = new ArrayList<>();

        events.forEach( e -> {
            ProgramStageInstance programStageInstance = preheat.getEvent( TrackerIdScheme.UID, e.getEvent() );
            ProgramStage programStage = preheat.get( TrackerIdScheme.UID, ProgramStage.class, e.getProgramStage() );
            OrganisationUnit organisationUnit = preheat
                .get( TrackerIdScheme.UID, OrganisationUnit.class, e.getOrgUnit() );

            if ( programStageInstance == null )
            {
                Date now = new Date();

                programStageInstance = new ProgramStageInstance();
                programStageInstance.setUid( !StringUtils.isEmpty( e.getEvent() ) ? e.getEvent() : e.getUid() );
                programStageInstance.setCreated( now );
                programStageInstance.setCreatedAtClient( now );
                programStageInstance.setLastUpdated( now );
                programStageInstance.setLastUpdatedAtClient( now );

                programStageInstance.setProgramInstance(
                    getProgramInstance( preheat, TrackerIdScheme.UID, e.getEnrollment(), programStage.getProgram() ) );
            }

            if ( !CodeGenerator.isValidUid( programStageInstance.getUid() ) )
            {
                programStageInstance.setUid( CodeGenerator.generateUid() );
            }

            programStageInstance.setProgramStage( programStage );
            programStageInstance.setOrganisationUnit( organisationUnit );
            programStageInstance.setExecutionDate( DateUtils.parseDate( e.getOccurredAt() ) );
            programStageInstance.setDueDate( DateUtils.parseDate( e.getScheduledAt() ) );
            programStageInstance.setAttributeOptionCombo(
                preheat.get( TrackerIdScheme.UID, CategoryOptionCombo.class, e.getAttributeOptionCombo() ) );
            programStageInstance.setGeometry( e.getGeometry() );
            programStageInstance.setStatus( e.getStatus() );

            if ( programStageInstance.isCompleted() )
            {
                Date completedDate = DateUtils.parseDate( e.getCompletedAt() );

                if ( completedDate == null )
                {
                    completedDate = new Date();
                }

                programStageInstance.setCompletedDate( completedDate );
                programStageInstance.setCompletedBy( e.getCompletedBy() );
            }

            // data values
            Set<EventDataValue> eventDataValues = new HashSet<>();

            e.getDataValues().forEach( dv -> {
                EventDataValue dataValue = new EventDataValue( dv.getDataElement(), dv.getValue() );
                dataValue.setAutoFields();
                dataValue.setProvidedElsewhere( dv.isProvidedElsewhere() );
                dataValue.setStoredBy( dv.getStoredBy() );

                eventDataValues.add( dataValue );
            } );

            programStageInstance.setEventDataValues( eventDataValues );

            if ( isNotEmpty( e.getNotes() ) )
            {
                programStageInstance.getComments().addAll( notesConverterService.from( preheat, e.getNotes() ) );
            }

            programStageInstances.add( programStageInstance );
        } );

        return programStageInstances;
    }

    private ProgramInstance getProgramInstance( TrackerPreheat preheat, TrackerIdScheme identifier, String enrollment,
        Program program )
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
}
