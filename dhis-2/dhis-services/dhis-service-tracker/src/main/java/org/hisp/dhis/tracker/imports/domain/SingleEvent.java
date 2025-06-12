/*
 * Copyright (c) 2004-2022, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors 
 * may be used to endorse or promote products derived from this software without
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
package org.hisp.dhis.tracker.imports.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nonnull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.tracker.TrackerType;
import org.locationtech.jts.geom.Geometry;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SingleEvent implements TrackerDto, Serializable {
  @Nonnull @JsonProperty private UID singleEvent;

  @JsonProperty @Builder.Default private EventStatus status = EventStatus.ACTIVE;

  @JsonProperty private MetadataIdentifier program;

  @JsonProperty private MetadataIdentifier orgUnit;

  @JsonProperty private Instant occurredAt;

  @JsonProperty private String storedBy;

  @JsonProperty private Instant createdAtClient;

  @JsonProperty private Instant updatedAtClient;

  @JsonProperty private MetadataIdentifier attributeOptionCombo;

  @JsonProperty @Builder.Default
  private Set<MetadataIdentifier> attributeCategoryOptions = new HashSet<>();

  @JsonProperty private Instant completedAt;

  @JsonProperty private Geometry geometry;

  @JsonProperty private User assignedUser;

  @JsonProperty @Builder.Default private Set<DataValue> dataValues = new HashSet<>();

  @JsonProperty @Builder.Default private List<Note> notes = new ArrayList<>();

  @Override
  public UID getUid() {
    return this.singleEvent;
  }

  @Override
  public TrackerType getTrackerType() {
    return TrackerType.EVENT;
  }
}
