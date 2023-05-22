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
package org.hisp.dhis.webapi.controller.tracker.view;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.webapi.common.UID;
import org.locationtech.jts.geom.Geometry;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@OpenApi.Shared
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RelationshipItem
{
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrackedEntity
    {
        @JsonProperty
        @OpenApi.Property( { UID.class, TrackedEntity.class } )
        private String trackedEntity;

        @JsonProperty
        private String trackedEntityType;

        @JsonProperty
        private Instant createdAt;

        @JsonProperty
        private Instant createdAtClient;

        @JsonProperty
        private Instant updatedAt;

        @JsonProperty
        private Instant updatedAtClient;

        @JsonProperty
        private String orgUnit;

        @JsonProperty
        private boolean inactive;

        @JsonProperty
        private boolean deleted;

        @JsonProperty
        private boolean potentialDuplicate;

        @JsonProperty
        private Geometry geometry;

        @JsonProperty
        private String storedBy;

        @JsonProperty
        private User createdBy;

        @JsonProperty
        private User updatedBy;

        @JsonProperty
        @Builder.Default
        private List<Attribute> attributes = new ArrayList<>();

        @JsonProperty
        @Builder.Default
        private List<Enrollment> enrollments = new ArrayList<>();

        @JsonProperty
        @Builder.Default
        private List<ProgramOwner> programOwners = new ArrayList<>();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Enrollment
    {
        @OpenApi.Property( { UID.class, Enrollment.class } )
        @JsonProperty
        private String enrollment;

        @JsonProperty
        private Instant createdAt;

        @JsonProperty
        private Instant createdAtClient;

        @JsonProperty
        private Instant updatedAt;

        @JsonProperty
        private Instant updatedAtClient;

        @JsonProperty
        private String trackedEntity;

        @JsonProperty
        private String program;

        @JsonProperty
        private EnrollmentStatus status;

        @JsonProperty
        private String orgUnit;

        @JsonProperty
        private String orgUnitName;

        @JsonProperty
        private Instant enrolledAt;

        @JsonProperty
        private Instant occurredAt;

        @JsonProperty
        private boolean followUp;

        @JsonProperty
        private String completedBy;

        @JsonProperty
        private Instant completedAt;

        @JsonProperty
        private boolean deleted;

        @JsonProperty
        private String storedBy;

        @JsonProperty
        private User createdBy;

        @JsonProperty
        private User updatedBy;

        @JsonProperty
        private Geometry geometry;

        @JsonProperty
        @Builder.Default
        private List<Event> events = new ArrayList<>();

        @JsonProperty
        @Builder.Default
        private List<Attribute> attributes = new ArrayList<>();

        @JsonProperty
        @Builder.Default
        private List<Note> notes = new ArrayList<>();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Event
    {
        @OpenApi.Property( { UID.class, Event.class } )
        @JsonProperty
        private String event;

        @JsonProperty
        @Builder.Default
        private EventStatus status = EventStatus.ACTIVE;

        @JsonProperty
        private String program;

        @JsonProperty
        private String programStage;

        @JsonProperty
        private String enrollment;

        @JsonProperty
        private String orgUnit;

        @JsonProperty
        private String orgUnitName;

        @JsonProperty
        private Instant occurredAt;

        @JsonProperty
        private Instant scheduledAt;

        @JsonProperty
        private String storedBy;

        @JsonProperty
        private boolean followup;

        @JsonProperty
        private boolean deleted;

        @JsonProperty
        private Instant createdAt;

        @JsonProperty
        private Instant createdAtClient;

        @JsonProperty
        private Instant updatedAt;

        @JsonProperty
        private Instant updatedAtClient;

        @JsonProperty
        private String attributeOptionCombo;

        @JsonProperty
        private String attributeCategoryOptions;

        @JsonProperty
        private String completedBy;

        @JsonProperty
        private Instant completedAt;

        @JsonProperty
        private Geometry geometry;

        @JsonProperty
        private User assignedUser;

        @JsonProperty
        private User createdBy;

        @JsonProperty
        private User updatedBy;

        @JsonProperty
        @Builder.Default
        private Set<DataValue> dataValues = new HashSet<>();

        @JsonProperty
        @Builder.Default
        private List<Note> notes = new ArrayList<>();
    }

    @JsonProperty
    private TrackedEntity trackedEntity;

    @JsonProperty
    private Enrollment enrollment;

    @JsonProperty
    private Event event;
}
