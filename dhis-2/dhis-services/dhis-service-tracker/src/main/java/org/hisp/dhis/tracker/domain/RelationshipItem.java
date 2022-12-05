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
package org.hisp.dhis.tracker.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import org.hisp.dhis.tracker.TrackerType;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@EqualsAndHashCode
@ToString
@Getter
@Builder
@AllArgsConstructor
public class RelationshipItem
{

    // TODO each field should be final
    // TODO can we make this a Value class?
    // TODO I don't want to make this a TrackerDto as this might have unwanted side-effects. It would be nice to just call
    // getUid() to get the id of the entity. We could still add this method without implementing TrackerDto
    // the interface would just fit as we want/have uid and type
    // TODO do we allow relationship items of type relationship?
    @JsonProperty
    private String trackedEntity;

    @JsonProperty
    private String enrollment;

    @JsonProperty
    private String event;

    @JsonIgnore
    private TrackerType type;

    // did NOT use TrackerDto intentionally as we do not allow RelationshipItems of type Relationship
    public RelationshipItem( TrackedEntity trackedEntity )
    {
        this.trackedEntity = trackedEntity.getUid();
        this.type = TrackerType.TRACKED_ENTITY;
    }

    public RelationshipItem( Enrollment enrollment )
    {
        this.enrollment = enrollment.getUid();
        this.type = TrackerType.ENROLLMENT;
    }

    public RelationshipItem( Event event )
    {
        this.event = event.getUid();
        this.type = TrackerType.ENROLLMENT;
    }

    public static RelationshipItem ofTrackedEntity( String uid )
    {
        TrackedEntity trackedEntity = TrackedEntity.builder().trackedEntity( uid ).build();
        return new RelationshipItem( trackedEntity );
    }

    public static RelationshipItem ofEnrollment( String uid )
    {
        Enrollment enrollment = Enrollment.builder().enrollment( uid ).build();
        return new RelationshipItem( enrollment );
    }

    public static RelationshipItem ofEvent( String uid )
    {
        Event event = Event.builder().enrollment( uid ).build();
        return new RelationshipItem( event );
    }
}
