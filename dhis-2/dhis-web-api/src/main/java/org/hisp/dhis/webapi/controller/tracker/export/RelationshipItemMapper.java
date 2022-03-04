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
package org.hisp.dhis.webapi.controller.tracker.export;

import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.domain.RelationshipItem;
import org.hisp.dhis.tracker.domain.TrackedEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper
interface RelationshipItemMapper
    extends DomainMapper<org.hisp.dhis.dxf2.events.trackedentity.RelationshipItem, RelationshipItem>
{
    // Only map the identifiers inside the relationships.from/to in
    // TrackedEntity, Enrollment, Event
    // Not doing that will cause a StackOverflow as the JSON cannot be built due
    // to the recursive structure without a base
    // case
    @Mapping( target = "trackedEntity", source = "trackedEntityInstance", qualifiedByName = "toTrackedEntity" )
    @Mapping( target = "enrollment", source = "enrollment", qualifiedByName = "toEnrollment" )
    @Mapping( target = "event", source = "event", qualifiedByName = "toEvent" )
    RelationshipItem from( org.hisp.dhis.dxf2.events.trackedentity.RelationshipItem relationshipItem );

    @Named( "toTrackedEntity" )
    default TrackedEntity trackedEntity(
        org.hisp.dhis.dxf2.events.trackedentity.TrackedEntityInstance trackedEntityInstance )
    {
        if ( trackedEntityInstance == null )
        {
            return null;
        }
        return TrackedEntity.builder().trackedEntity( trackedEntityInstance.getTrackedEntityInstance() ).build();

    }

    @Named( "toEnrollment" )
    default Enrollment enrollment( org.hisp.dhis.dxf2.events.enrollment.Enrollment enrollment )
    {
        if ( enrollment == null )
        {
            return null;
        }
        return Enrollment.builder().enrollment( enrollment.getEnrollment() ).build();

    }

    @Named( "toEvent" )
    default Event event( org.hisp.dhis.dxf2.events.event.Event event )
    {
        if ( event == null )
        {
            return null;
        }
        return Event.builder().event( event.getEvent() ).build();

    }
}
