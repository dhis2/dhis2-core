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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.io.Serializable;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.tracker.TrackerType;
import org.locationtech.jts.geom.Geometry;

@JsonDeserialize(as = TrackerEvent.class)
@JsonSerialize(as = TrackerEvent.class)
public interface Event extends TrackerDto, Serializable {

  @Override
  default TrackerType getTrackerType() {
    return TrackerType.EVENT;
  }

  UID getEvent();

  EventStatus getStatus();

  MetadataIdentifier getProgram();

  MetadataIdentifier getProgramStage();

  UID getEnrollment();

  MetadataIdentifier getOrgUnit();

  Instant getOccurredAt();

  String getStoredBy();

  Instant getCreatedAtClient();

  Instant getUpdatedAtClient();

  MetadataIdentifier getAttributeOptionCombo();

  Set<MetadataIdentifier> getAttributeCategoryOptions();

  Instant getCompletedAt();

  Geometry getGeometry();

  User getAssignedUser();

  Set<DataValue> getDataValues();

  List<Note> getNotes();

  @JsonIgnore
  default boolean isCreatableInSearchScope() {
    return this.getStatus() == EventStatus.SCHEDULE
        && this.getDataValues().isEmpty()
        && this.getOccurredAt() == null;
  }

  void setEnrollment(UID of);

  void setStatus(EventStatus eventStatus);

  void setProgram(MetadataIdentifier metadataIdentifier);

  void setProgramStage(MetadataIdentifier metadataIdentifier);

  void setAttributeOptionCombo(MetadataIdentifier categoryOptionComboIdentifier);

  void setNotes(List<Note> notes);
}
