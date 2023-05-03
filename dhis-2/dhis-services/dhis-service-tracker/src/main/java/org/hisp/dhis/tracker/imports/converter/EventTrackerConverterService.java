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
package org.hisp.dhis.tracker.imports.converter;

import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.eventdatavalue.EventDataValue;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramType;
import org.hisp.dhis.program.UserInfoSnapshot;
import org.hisp.dhis.tracker.imports.domain.DataValue;
import org.hisp.dhis.tracker.imports.domain.MetadataIdentifier;
import org.hisp.dhis.tracker.imports.domain.User;
import org.hisp.dhis.tracker.imports.preheat.TrackerPreheat;
import org.hisp.dhis.util.DateUtils;
import org.springframework.stereotype.Service;

import com.google.common.base.Objects;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@RequiredArgsConstructor
@Service
public class EventTrackerConverterService
    implements RuleEngineConverterService<org.hisp.dhis.tracker.imports.domain.Event, Event>
{
    private final NotesConverterService notesConverterService;

    @Override
    public org.hisp.dhis.tracker.imports.domain.Event to( Event event )
    {
        List<org.hisp.dhis.tracker.imports.domain.Event> events = to( Collections.singletonList( event ) );

        if ( events.isEmpty() )
        {
            return null;
        }

        return events.get( 0 );
    }

    @Override
    public List<org.hisp.dhis.tracker.imports.domain.Event> to( List<Event> events )
    {
        List<org.hisp.dhis.tracker.imports.domain.Event> result = new ArrayList<>();

        events.forEach( event -> {
            org.hisp.dhis.tracker.imports.domain.Event e = new org.hisp.dhis.tracker.imports.domain.Event();
            e.setEvent( event.getUid() );

            e.setFollowup( BooleanUtils.toBoolean( event.getEnrollment().getFollowup() ) );
            e.setStatus( event.getStatus() );
            e.setOccurredAt( DateUtils.instantFromDate( event.getExecutionDate() ) );
            e.setScheduledAt( DateUtils.instantFromDate( event.getDueDate() ) );
            e.setStoredBy( event.getStoredBy() );
            e.setCompletedBy( event.getCompletedBy() );
            e.setCompletedAt( DateUtils.instantFromDate( event.getCompletedDate() ) );
            e.setCreatedAt( DateUtils.instantFromDate( event.getCreated() ) );
            e.setCreatedAtClient( DateUtils.instantFromDate( event.getCreatedAtClient() ) );
            e.setUpdatedAt( DateUtils.instantFromDate( event.getLastUpdated() ) );
            e.setUpdatedAtClient( DateUtils.instantFromDate( event.getLastUpdatedAtClient() ) );
            e.setGeometry( event.getGeometry() );
            e.setDeleted( event.isDeleted() );

            OrganisationUnit ou = event.getOrganisationUnit();

            if ( ou != null )
            {
                e.setOrgUnit( MetadataIdentifier.ofUid( ou ) );
            }

            e.setEnrollment( event.getEnrollment().getUid() );
            e.setProgramStage( MetadataIdentifier.ofUid( event.getProgramStage() ) );
            e.setAttributeOptionCombo( MetadataIdentifier.ofUid( event.getAttributeOptionCombo() ) );
            e.setAttributeCategoryOptions( event.getAttributeOptionCombo().getCategoryOptions().stream()
                .map( CategoryOption::getUid )
                .map( MetadataIdentifier::ofUid )
                .collect( Collectors.toSet() ) );

            Set<EventDataValue> dataValues = event.getEventDataValues();

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

                e.getDataValues().add( value );
            }

            result.add( e );
        } );

        return result;
    }

    @Override
    public Event from( TrackerPreheat preheat, org.hisp.dhis.tracker.imports.domain.Event event )
    {
        return from( preheat, event, preheat.getEvent( event.getEvent() ) );
    }

    @Override
    public List<Event> from( TrackerPreheat preheat, List<org.hisp.dhis.tracker.imports.domain.Event> events )
    {
        return events
            .stream()
            .map( e -> from( preheat, e ) )
            .collect( Collectors.toList() );
    }

    @Override
    public Event fromForRuleEngine( TrackerPreheat preheat, org.hisp.dhis.tracker.imports.domain.Event event )
    {
        Event result = from( preheat, event, null );
        // merge data values from DB
        result.getEventDataValues().addAll( getDataValues( preheat, event ) );
        return result;
    }

    private List<EventDataValue> getDataValues( TrackerPreheat preheat,
        org.hisp.dhis.tracker.imports.domain.Event event )
    {
        List<EventDataValue> eventDataValues = new ArrayList<>();
        if ( preheat.getEvent( event.getEvent() ) == null )
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
            .map( IdentifiableObject::getUid )
            .collect( Collectors.toSet() );
        for ( EventDataValue eventDataValue : preheat.getEvent( event.getEvent() ).getEventDataValues() )
        {
            if ( !dataElements.contains( eventDataValue.getDataElement() ) )
            {
                eventDataValues.add( eventDataValue );
            }
        }
        return eventDataValues;
    }

    private Event from( TrackerPreheat preheat, org.hisp.dhis.tracker.imports.domain.Event event, Event result )
    {
        ProgramStage programStage = preheat.getProgramStage( event.getProgramStage() );
        Program program = preheat.getProgram( event.getProgram() );
        OrganisationUnit organisationUnit = preheat.getOrganisationUnit( event.getOrgUnit() );

        Date now = new Date();

        if ( isNewEntity( result ) )
        {
            result = new Event();
            result.setUid( !StringUtils.isEmpty( event.getEvent() ) ? event.getEvent() : event.getUid() );
            result.setCreated( now );
            result.setStoredBy( event.getStoredBy() );
            result.setCreatedByUserInfo( UserInfoSnapshot.from( preheat.getUser() ) );
            result.setCreatedAtClient( DateUtils.fromInstant( event.getCreatedAtClient() ) );
        }
        result.setLastUpdatedByUserInfo( UserInfoSnapshot.from( preheat.getUser() ) );
        result.setLastUpdated( now );
        result.setDeleted( false );
        result.setLastUpdatedAtClient( DateUtils.fromInstant( event.getUpdatedAtClient() ) );
        result.setEnrollment( getEnrollment( preheat, event.getEnrollment(), program ) );
        result.setProgramStage( programStage );
        result.setOrganisationUnit( organisationUnit );
        result.setExecutionDate( DateUtils.fromInstant( event.getOccurredAt() ) );
        result.setDueDate( DateUtils.fromInstant( event.getScheduledAt() ) );

        if ( event.getAttributeOptionCombo().isNotBlank() )
        {
            result.setAttributeOptionCombo(
                preheat.getCategoryOptionCombo( event.getAttributeOptionCombo() ) );
        }
        else
        {
            result.setAttributeOptionCombo( preheat.getDefault( CategoryOptionCombo.class ) );
        }

        result.setGeometry( event.getGeometry() );

        EventStatus previousStatus = result.getStatus();

        result.setStatus( event.getStatus() );

        if ( !Objects.equal( previousStatus, result.getStatus() ) && result.isCompleted() )
        {
            result.setCompletedDate( new Date() );
            result.setCompletedBy( preheat.getUsername() );
        }

        if ( Boolean.TRUE.equals( programStage.isEnableUserAssignment() ) &&
            event.getAssignedUser() != null
            && !event.getAssignedUser().isEmpty() )
        {
            Optional<org.hisp.dhis.user.User> assignedUser = preheat
                .getUserByUsername( event.getAssignedUser().getUsername() );
            assignedUser.ifPresent( result::setAssignedUser );
        }

        if ( program.isRegistration() && result.getDueDate() == null &&
            result.getExecutionDate() != null )
        {
            result.setDueDate( result.getExecutionDate() );
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

            result.getEventDataValues().add( eventDataValue );
        }

        if ( isNotEmpty( event.getNotes() ) )
        {
            result.getComments().addAll( notesConverterService.from( preheat, event.getNotes() ) );
        }

        return result;
    }

    private Enrollment getEnrollment( TrackerPreheat preheat, String enrollment, Program program )
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
