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
package org.hisp.dhis.tracker.converter;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.eventdatavalue.EventDataValue;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.program.ProgramType;
import org.hisp.dhis.program.UserInfoSnapshot;
import org.hisp.dhis.tracker.domain.DataValue;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.domain.MetadataIdentifier;
import org.hisp.dhis.tracker.domain.User;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.hisp.dhis.util.DateUtils;
import org.springframework.stereotype.Service;

import com.google.common.base.Objects;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Service
public class EventTrackerConverterService
    implements RuleEngineConverterService<Event, ProgramStageInstance>
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

            event.setFollowup( BooleanUtils.toBoolean( psi.getProgramInstance().getFollowup() ) );
            event.setStatus( psi.getStatus() );
            event.setOccurredAt( DateUtils.instantFromDate( psi.getExecutionDate() ) );
            event.setScheduledAt( DateUtils.instantFromDate( psi.getDueDate() ) );
            event.setStoredBy( psi.getStoredBy() );
            event.setCompletedBy( psi.getCompletedBy() );
            event.setCompletedAt( DateUtils.instantFromDate( psi.getCompletedDate() ) );
            event.setCreatedAt( DateUtils.instantFromDate( psi.getCreated() ) );
            event.setCreatedAtClient( DateUtils.instantFromDate( psi.getCreatedAtClient() ) );
            event.setUpdatedAt( DateUtils.instantFromDate( psi.getLastUpdated() ) );
            event.setUpdatedAtClient( DateUtils.instantFromDate( psi.getLastUpdatedAtClient() ) );
            event.setGeometry( psi.getGeometry() );
            event.setDeleted( psi.isDeleted() );

            OrganisationUnit ou = psi.getOrganisationUnit();

            if ( ou != null )
            {
                event.setOrgUnit( MetadataIdentifier.ofUid( ou ) );
            }

            event.setEnrollment( psi.getProgramInstance().getUid() );
            event.setProgramStage( MetadataIdentifier.ofUid( psi.getProgramStage() ) );
            event.setAttributeOptionCombo( MetadataIdentifier.ofUid( psi.getAttributeOptionCombo() ) );
            event.setAttributeCategoryOptions( psi.getAttributeOptionCombo().getCategoryOptions().stream()
                .map( CategoryOption::getUid )
                .map( MetadataIdentifier::ofUid )
                .collect( Collectors.toSet() ) );

            Set<EventDataValue> dataValues = psi.getEventDataValues();

            for ( EventDataValue dataValue : dataValues )
            {
                DataValue value = new DataValue();
                value.setCreatedAt( DateUtils.instantFromDate( dataValue.getCreated() ) );
                value.setUpdatedAt( DateUtils.instantFromDate( dataValue.getLastUpdated() ) );
                value.setDataElement( MetadataIdentifier.ofUid( dataValue.getDataElement() ) );
                value.setValue( dataValue.getValue() );
                value.setProvidedElsewhere( dataValue.getProvidedElsewhere() );
                value.setStoredBy( dataValue.getStoredBy() );
                value.setUpdatedBy( Optional.ofNullable( dataValue.getLastUpdatedByUserInfo() )
                    .map( this::convertUserInfo ).orElse( null ) );
                value.setCreatedBy( Optional.ofNullable( dataValue.getCreatedByUserInfo() )
                    .map( this::convertUserInfo ).orElse( null ) );

                event.getDataValues().add( value );
            }

            events.add( event );
        } );

        return events;
    }

    @Override
    public ProgramStageInstance from( TrackerPreheat preheat, Event event )
    {
        ProgramStageInstance programStageInstance = preheat.getEvent( event.getEvent() );
        return from( preheat, event, programStageInstance );
    }

    @Override
    public List<ProgramStageInstance> from( TrackerPreheat preheat, List<Event> events )
    {
        return events
            .stream()
            .map( e -> from( preheat, e ) )
            .collect( Collectors.toList() );
    }

    @Override
    public ProgramStageInstance fromForRuleEngine( TrackerPreheat preheat, Event event )
    {
        ProgramStageInstance psi = from( preheat, event, null );
        // merge data values from DB
        psi.getEventDataValues().addAll( getProgramStageInstanceDataValues( preheat, event ) );
        return psi;
    }

    private List<EventDataValue> getProgramStageInstanceDataValues( TrackerPreheat preheat, Event event )
    {
        List<EventDataValue> eventDataValues = new ArrayList<>();
        ProgramStageInstance programStageInstance = preheat.getEvent( event.getEvent() );
        if ( programStageInstance == null )
        {
            return eventDataValues;
        }

        // Normalize identifiers as EventDataValue.dataElement are UIDs and
        // payload dataElements can be in any idScheme
        Set<String> dataElements = event.getDataValues()
            .stream()
            .map( DataValue::getDataElement )
            .map( preheat::getDataElement )
            .filter( java.util.Objects::nonNull )
            .map( BaseIdentifiableObject::getUid )
            .collect( Collectors.toSet() );
        for ( EventDataValue eventDataValue : programStageInstance.getEventDataValues() )
        {
            if ( !dataElements.contains( eventDataValue.getDataElement() ) )
            {
                eventDataValues.add( eventDataValue );
            }
        }
        return eventDataValues;
    }

    private ProgramStageInstance from( TrackerPreheat preheat, Event event, ProgramStageInstance programStageInstance )
    {
        ProgramStage programStage = preheat.get( ProgramStage.class, event.getProgramStage() );
        Program program = preheat.getProgram( event.getProgram() );
        OrganisationUnit organisationUnit = preheat.getOrganisationUnit( event.getOrgUnit() );

        Date now = new Date();

        if ( isNewEntity( programStageInstance ) )
        {
            programStageInstance = new ProgramStageInstance();
            programStageInstance.setUid( !StringUtils.isEmpty( event.getEvent() ) ? event.getEvent() : event.getUid() );
            programStageInstance.setCreated( now );
            programStageInstance.setStoredBy( event.getStoredBy() );
            programStageInstance.setCreatedByUserInfo( UserInfoSnapshot.from( preheat.getUser() ) );
        }
        programStageInstance.setLastUpdatedByUserInfo( UserInfoSnapshot.from( preheat.getUser() ) );
        programStageInstance.setLastUpdated( now );
        programStageInstance.setDeleted( false );
        programStageInstance.setCreatedAtClient( DateUtils.fromInstant( event.getCreatedAtClient() ) );
        programStageInstance.setLastUpdatedAtClient( DateUtils.fromInstant( event.getUpdatedAtClient() ) );
        programStageInstance.setProgramInstance(
            getProgramInstance( preheat, event.getEnrollment(), program ) );

        programStageInstance.setProgramStage( programStage );
        programStageInstance.setOrganisationUnit( organisationUnit );
        programStageInstance.setExecutionDate( DateUtils.fromInstant( event.getOccurredAt() ) );
        programStageInstance.setDueDate( DateUtils.fromInstant( event.getScheduledAt() ) );

        if ( !event.getAttributeOptionCombo().isBlank() )
        {
            programStageInstance.setAttributeOptionCombo(
                preheat.getCategoryOptionCombo( event.getAttributeOptionCombo() ) );
        }
        else
        {
            programStageInstance.setAttributeOptionCombo( preheat.getDefault( CategoryOptionCombo.class ) );
        }

        programStageInstance.setGeometry( event.getGeometry() );

        EventStatus previousStatus = programStageInstance.getStatus();

        programStageInstance.setStatus( event.getStatus() );

        if ( !Objects.equal( previousStatus, programStageInstance.getStatus() ) && programStageInstance.isCompleted() )
        {
            programStageInstance.setCompletedDate( new Date() );
            programStageInstance.setCompletedBy( preheat.getUsername() );
        }

        if ( Boolean.TRUE.equals( programStage.isEnableUserAssignment() ) &&
            event.getAssignedUser() != null
            && !event.getAssignedUser().isEmpty() )
        {
            Optional<org.hisp.dhis.user.User> assignedUser = preheat
                .getUserByUsername( event.getAssignedUser().getUsername() );
            assignedUser.ifPresent( programStageInstance::setAssignedUser );
        }

        if ( program.isRegistration() && programStageInstance.getDueDate() == null &&
            programStageInstance.getExecutionDate() != null )
        {
            programStageInstance.setDueDate( programStageInstance.getExecutionDate() );
        }

        for ( DataValue dataValue : event.getDataValues() )
        {
            EventDataValue eventDataValue = new EventDataValue();
            eventDataValue.setValue( dataValue.getValue() );
            eventDataValue.setCreated( DateUtils.fromInstant( dataValue.getCreatedAt() ) );
            eventDataValue.setLastUpdated( new Date() );
            eventDataValue.setProvidedElsewhere( dataValue.isProvidedElsewhere() );
            // ensure dataElement is referred to by UID as multiple
            // dataElementIdSchemes are supported
            DataElement dataElement = preheat.getDataElement( dataValue.getDataElement() );
            eventDataValue.setDataElement( dataElement.getUid() );
            eventDataValue.setLastUpdatedByUserInfo( UserInfoSnapshot.from( preheat.getUser() ) );
            eventDataValue.setCreatedByUserInfo( UserInfoSnapshot.from( preheat.getUser() ) );

            programStageInstance.getEventDataValues().add( eventDataValue );
        }

        if ( isNotEmpty( event.getNotes() ) )
        {
            programStageInstance.getComments().addAll( notesConverterService.from( preheat, event.getNotes() ) );
        }

        return programStageInstance;
    }

    private ProgramInstance getProgramInstance( TrackerPreheat preheat, String enrollment, Program program )
    {
        if ( ProgramType.WITH_REGISTRATION == program.getProgramType() )
        {
            return preheat.getEnrollment( enrollment );
        }

        if ( ProgramType.WITHOUT_REGISTRATION == program.getProgramType() )
        {
            return preheat.getProgramInstancesWithoutRegistration( program.getUid() );
        }

        // no valid enrollment given and program not single event, just return
        // null
        return null;
    }

    private User convertUserInfo( UserInfoSnapshot userInfoSnapshot )
    {
        return User.builder()
            .uid( userInfoSnapshot.getUid() )
            .username( userInfoSnapshot.getUsername() )
            .firstName( userInfoSnapshot.getFirstName() )
            .surname( userInfoSnapshot.getSurname() )
            .build();
    }
}
