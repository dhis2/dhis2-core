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

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.tracker.TrackerType;
import org.locationtech.jts.geom.Geometry;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Event
    implements TrackerDto
{
    @JsonProperty
    private String event;

    @JsonProperty
    @Builder.Default
    private EventStatus status = EventStatus.ACTIVE;

    @JsonProperty
    private MetadataIdentifier program;

    @JsonProperty
    private MetadataIdentifier programStage;

    @JsonProperty
    private String enrollment;

    @JsonProperty
    private MetadataIdentifier orgUnit;

    @JsonProperty
    private String orgUnitName;

    @JsonProperty
    @Builder.Default
    private List<Relationship> relationships = new ArrayList<>();

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
    private MetadataIdentifier attributeOptionCombo;

    @JsonProperty
    @Builder.Default
    private Set<MetadataIdentifier> attributeCategoryOptions = new HashSet<>();

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

    @JsonIgnore
    public boolean isCreatableInSearchScope()
    {
        return this.getStatus() == EventStatus.SCHEDULE && this.getDataValues().isEmpty() && this.occurredAt == null;
    }

    @Override
    public String getUid()
    {
        return this.event;
    }

    @Override
    public TrackerType getTrackerType()
    {
        return TrackerType.EVENT;
    }
}
